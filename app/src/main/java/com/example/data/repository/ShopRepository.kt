package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.local.ShopDao
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.DailyReport
import com.example.data.model.Expense
import com.example.data.model.Payment
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.Sale
import com.example.data.model.SaleItemRecord
import com.example.data.model.StockMovement
import com.example.data.model.Supplier
import com.example.data.model.SyncQueueItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

sealed interface SyncState {
  data object Synced : SyncState
  data object Syncing : SyncState
  data class Offline(val pendingCount: Int) : SyncState
  data class Error(val message: String) : SyncState
}

class ShopRepository(
  private val shopDao: ShopDao,
  private val firestore: FirebaseFirestore
) {
  constructor(context: Context, shopDao: ShopDao) : this(
    shopDao,
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth
  private val scope = CoroutineScope(Dispatchers.IO)

  private val _syncState = MutableStateFlow<SyncState>(SyncState.Synced)
  val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

  private var activeListeners = mutableListOf<ListenerRegistration>()

  val pendingSyncCount: Flow<Int> = shopDao.observePendingSyncCount()

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Shop owner must be signed in with Google")
  }

  // --- LOCAL FLOW STREAMS ---
  fun observeProducts(): Flow<List<Product>> = shopDao.observeProducts()
  fun observeStockMovements(): Flow<List<StockMovement>> = shopDao.observeStockMovements()
  fun observeSales(): Flow<List<Sale>> = shopDao.observeSales()
  fun observePurchases(): Flow<List<Purchase>> = shopDao.observePurchases()
  fun observeCustomers(): Flow<List<Customer>> = shopDao.observeCustomers()
  fun observeSuppliers(): Flow<List<Supplier>> = shopDao.observeSuppliers()
  fun observePayments(): Flow<List<Payment>> = shopDao.observePayments()
  fun observeExpenses(): Flow<List<Expense>> = shopDao.observeExpenses()
  fun observeDailyReports(): Flow<List<DailyReport>> = shopDao.observeDailyReports()

  // --- POS CHECKOUT & SALES ---
  suspend fun completeSale(
    cartItems: List<CartItem>,
    customerName: String,
    customerPhone: String,
    discount: Double,
    tax: Double,
    paidAmount: Double,
    paymentMethod: String
  ): Sale = withContext(Dispatchers.IO) {
    val timestamp = System.currentTimeMillis()
    val txnId = "txn_${timestamp}_${UUID.randomUUID().toString().take(8)}"
    val subtotal = cartItems.sumOf { it.lineTotal }
    val totalAmount = (subtotal - discount + tax).coerceAtLeast(0.0)
    val changeAmount = (paidAmount - totalAmount).coerceAtLeast(0.0)

    val itemRecords = cartItems.map {
      SaleItemRecord(
        productId = it.product.id,
        productName = it.product.name,
        barcode = it.product.barcode,
        quantity = it.quantity,
        unitPrice = it.product.sellingPrice,
        costPrice = it.product.costPrice,
        lineTotal = it.lineTotal
      )
    }

    val sale = Sale(
      id = txnId,
      transactionId = txnId,
      customerName = customerName.trim(),
      customerPhone = customerPhone.trim(),
      itemsJson = SaleItemRecord.listToJson(itemRecords),
      subtotal = subtotal,
      discount = discount,
      tax = tax,
      totalAmount = totalAmount,
      paidAmount = paidAmount,
      changeAmount = changeAmount,
      paymentMethod = paymentMethod,
      timestamp = timestamp,
      syncStatus = "PENDING_SYNC"
    )

    // Save sale locally
    shopDao.insertSale(sale)

    // Deduct stock and log movement for each item
    for (cartItem in cartItems) {
      val prod = shopDao.getProductById(cartItem.product.id) ?: cartItem.product
      val newStock = prod.stockQuantity - cartItem.quantity
      shopDao.updateProductStock(prod.id, newStock, timestamp, "PENDING_SYNC")

      val movement = StockMovement(
        id = UUID.randomUUID().toString(),
        productId = prod.id,
        productName = prod.name,
        movementType = "SALE",
        deltaQuantity = -cartItem.quantity,
        previousStock = prod.stockQuantity,
        newStock = newStock,
        referenceId = txnId,
        reason = "POS Sale $txnId",
        timestamp = timestamp,
        syncStatus = "PENDING_SYNC"
      )
      shopDao.insertStockMovement(movement)

      // Queue movement and product update
      queueMutation("STOCK_MOVEMENT", movement.id, "CREATE", movementToJson(movement))
      queueMutation("PRODUCT", prod.id, "UPDATE", productToJson(prod.copy(stockQuantity = newStock, updatedAt = timestamp)))
    }

    // Update customer balance if credit/due
    if (paymentMethod == "STORE_CREDIT" && customerName.isNotBlank()) {
      val customer = shopDao.getCustomerById(customerName)
      if (customer != null) {
        val updated = customer.copy(
          outstandingBalance = customer.outstandingBalance + totalAmount,
          updatedAt = timestamp,
          syncStatus = "PENDING_SYNC"
        )
        shopDao.upsertCustomer(updated)
        queueMutation("CUSTOMER", updated.id, "UPDATE", customerToJson(updated))
      }
    }

    // Queue Sale mutation
    queueMutation("SALE", sale.id, "CREATE", saleToJson(sale))

    // Trigger auto-sync in background
    triggerSync()

    sale
  }

  // --- PRODUCT MANAGEMENT ---
  suspend fun saveProduct(product: Product): Product = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val isExisting = if (product.id.isNotBlank()) shopDao.getProductById(product.id) != null else false
    val finalProduct = if (product.id.isBlank()) {
      product.copy(id = UUID.randomUUID().toString(), createdAt = now, updatedAt = now, syncStatus = "PENDING_SYNC")
    } else {
      product.copy(updatedAt = now, syncStatus = "PENDING_SYNC")
    }

    shopDao.upsertProduct(finalProduct)
    val action = if (isExisting) "UPDATE" else "CREATE"
    queueMutation("PRODUCT", finalProduct.id, action, productToJson(finalProduct))
    triggerSync()
    finalProduct
  }

  suspend fun adjustProductStock(product: Product, delta: Int, reason: String) = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val newStock = product.stockQuantity + delta
    val updatedProduct = product.copy(stockQuantity = newStock, updatedAt = now, syncStatus = "PENDING_SYNC")
    shopDao.upsertProduct(updatedProduct)

    val movement = StockMovement(
      id = UUID.randomUUID().toString(),
      productId = product.id,
      productName = product.name,
      movementType = if (delta >= 0) "ADJUSTMENT" else "RETURN",
      deltaQuantity = delta,
      previousStock = product.stockQuantity,
      newStock = newStock,
      referenceId = "Manual Adjustment",
      reason = reason.ifBlank { "Manual stock adjustment" },
      timestamp = now,
      syncStatus = "PENDING_SYNC"
    )
    shopDao.insertStockMovement(movement)

    queueMutation("PRODUCT", updatedProduct.id, "UPDATE", productToJson(updatedProduct))
    queueMutation("STOCK_MOVEMENT", movement.id, "CREATE", movementToJson(movement))
    triggerSync()
  }

  suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
    shopDao.deleteProduct(product)
    queueMutation("PRODUCT", product.id, "DELETE", JSONObject().put("id", product.id).toString())
    triggerSync()
  }

  // --- PURCHASES ---
  suspend fun recordPurchase(
    supplierName: String,
    supplierId: String,
    invoiceNumber: String,
    items: List<SaleItemRecord>,
    totalCost: Double,
    paidAmount: Double,
    paymentStatus: String
  ) = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val purchaseId = "pur_${now}_${UUID.randomUUID().toString().take(8)}"
    val purchase = Purchase(
      id = purchaseId,
      supplierId = supplierId,
      supplierName = supplierName.trim(),
      invoiceNumber = invoiceNumber.trim(),
      itemsJson = SaleItemRecord.listToJson(items),
      totalCost = totalCost,
      paidAmount = paidAmount,
      paymentStatus = paymentStatus,
      timestamp = now,
      syncStatus = "PENDING_SYNC"
    )

    shopDao.insertPurchase(purchase)

    // Increase product stocks
    for (item in items) {
      val prod = shopDao.getProductById(item.productId)
      if (prod != null) {
        val newStock = prod.stockQuantity + item.quantity
        val updated = prod.copy(stockQuantity = newStock, updatedAt = now, syncStatus = "PENDING_SYNC")
        shopDao.upsertProduct(updated)

        val movement = StockMovement(
          id = UUID.randomUUID().toString(),
          productId = prod.id,
          productName = prod.name,
          movementType = "PURCHASE",
          deltaQuantity = item.quantity,
          previousStock = prod.stockQuantity,
          newStock = newStock,
          referenceId = purchaseId,
          reason = "Supplier Invoice $invoiceNumber",
          timestamp = now,
          syncStatus = "PENDING_SYNC"
        )
        shopDao.insertStockMovement(movement)

        queueMutation("PRODUCT", updated.id, "UPDATE", productToJson(updated))
        queueMutation("STOCK_MOVEMENT", movement.id, "CREATE", movementToJson(movement))
      }
    }

    queueMutation("PURCHASE", purchase.id, "CREATE", purchaseToJson(purchase))
    triggerSync()
  }

  // --- CUSTOMERS & SUPPLIERS ---
  suspend fun saveCustomer(customer: Customer): Customer = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val isExisting = if (customer.id.isNotBlank()) shopDao.getCustomerById(customer.id) != null else false
    val finalCust = if (customer.id.isBlank()) {
      customer.copy(id = UUID.randomUUID().toString(), createdAt = now, updatedAt = now, syncStatus = "PENDING_SYNC")
    } else {
      customer.copy(updatedAt = now, syncStatus = "PENDING_SYNC")
    }
    shopDao.upsertCustomer(finalCust)
    val action = if (isExisting) "UPDATE" else "CREATE"
    queueMutation("CUSTOMER", finalCust.id, action, customerToJson(finalCust))
    triggerSync()
    finalCust
  }

  suspend fun saveSupplier(supplier: Supplier): Supplier = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val isExisting = if (supplier.id.isNotBlank()) shopDao.getSupplierById(supplier.id) != null else false
    val finalSupp = if (supplier.id.isBlank()) {
      supplier.copy(id = UUID.randomUUID().toString(), createdAt = now, updatedAt = now, syncStatus = "PENDING_SYNC")
    } else {
      supplier.copy(updatedAt = now, syncStatus = "PENDING_SYNC")
    }
    shopDao.upsertSupplier(finalSupp)
    val action = if (isExisting) "UPDATE" else "CREATE"
    queueMutation("SUPPLIER", finalSupp.id, action, supplierToJson(finalSupp))
    triggerSync()
    finalSupp
  }

  // --- PAYMENTS & EXPENSES ---
  suspend fun recordPayment(payment: Payment) = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val finalPayment = if (payment.id.isBlank()) {
      payment.copy(id = UUID.randomUUID().toString(), timestamp = now, syncStatus = "PENDING_SYNC")
    } else {
      payment.copy(timestamp = now, syncStatus = "PENDING_SYNC")
    }
    shopDao.insertPayment(finalPayment)
    queueMutation("PAYMENT", finalPayment.id, "CREATE", paymentToJson(finalPayment))
    triggerSync()
  }

  suspend fun recordExpense(expense: Expense) = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val finalExpense = if (expense.id.isBlank()) {
      expense.copy(id = UUID.randomUUID().toString(), timestamp = now, syncStatus = "PENDING_SYNC")
    } else {
      expense.copy(timestamp = now, syncStatus = "PENDING_SYNC")
    }
    shopDao.upsertExpense(finalExpense)
    queueMutation("EXPENSE", finalExpense.id, "CREATE", expenseToJson(finalExpense))
    triggerSync()
  }

  // --- TWO-WAY SYNCHRONIZATION ENGINE ---
  private suspend fun queueMutation(type: String, id: String, action: String, payload: String) {
    val item = SyncQueueItem(
      entityType = type,
      entityId = id,
      action = action,
      payloadJson = payload,
      timestamp = System.currentTimeMillis()
    )
    shopDao.queueMutation(item)
  }

  fun triggerSync() {
    scope.launch {
      syncNow()
    }
  }

  suspend fun syncNow() = withContext(Dispatchers.IO) {
    val uid = auth.currentUser?.uid ?: run {
      val pending = shopDao.getPendingSyncQueue().size
      _syncState.value = SyncState.Offline(pending)
      return@withContext
    }

    val pendingItems = shopDao.getPendingSyncQueue()
    if (pendingItems.isEmpty()) {
      _syncState.value = SyncState.Synced
      pullRemoteChanges(uid)
      return@withContext
    }

    _syncState.value = SyncState.Syncing

    try {
      for (item in pendingItems) {
        val collection = when (item.entityType) {
          "PRODUCT" -> "products"
          "STOCK_MOVEMENT" -> "stock_movements"
          "SALE" -> "sales"
          "PURCHASE" -> "purchases"
          "CUSTOMER" -> "customers"
          "SUPPLIER" -> "suppliers"
          "PAYMENT" -> "payments"
          "EXPENSE" -> "expenses"
          "REPORT" -> "reports"
          else -> continue
        }

        val docRef = firestore.collection("users").document(uid).collection(collection).document(item.entityId)

        try {
          if (item.action == "DELETE") {
            docRef.delete().await()
          } else if (item.action == "UPDATE") {
            val json = JSONObject(item.payloadJson)
            val payloadMap = jsonToFirestoreMap(json, isCreate = false)
            docRef.set(payloadMap, SetOptions.merge()).await()
          } else {
            val json = JSONObject(item.payloadJson)
            val payloadMap = jsonToFirestoreMap(json, isCreate = true)
            docRef.set(payloadMap).await()
          }

          // Mark as synced locally
          markEntitySynced(item.entityType, item.entityId)
          shopDao.removeSyncQueueItem(item.queueId)
        } catch (itemError: Exception) {
          Log.e("ShopSync", "Failed syncing item ${item.entityId} in $collection", itemError)
          handleFirestoreError(itemError, if (item.action == "DELETE") OperationType.DELETE else OperationType.WRITE, docRef.path)
        }
      }

      // After pushing pending mutations, pull recent changes
      pullRemoteChanges(uid)
      val remaining = shopDao.getPendingSyncQueue().size
      _syncState.value = if (remaining == 0) SyncState.Synced else SyncState.Offline(remaining)
    } catch (e: Exception) {
      Log.e("ShopSync", "Sync error", e)
      val remaining = shopDao.getPendingSyncQueue().size
      _syncState.value = SyncState.Offline(remaining)
    }
  }

  private suspend fun markEntitySynced(type: String, id: String) {
    when (type) {
      "PRODUCT" -> {
        val p = shopDao.getProductById(id)
        if (p != null) shopDao.upsertProduct(p.copy(syncStatus = "SYNCED"))
      }
      "CUSTOMER" -> {
        val c = shopDao.getCustomerById(id)
        if (c != null) shopDao.upsertCustomer(c.copy(syncStatus = "SYNCED"))
      }
      "SUPPLIER" -> {
        val s = shopDao.getSupplierById(id)
        if (s != null) shopDao.upsertSupplier(s.copy(syncStatus = "SYNCED"))
      }
    }
  }

  fun attachRealtimeListeners() {
    val uid = auth.currentUser?.uid ?: return
    detachRealtimeListeners()

    try {
      val prodReg = firestore.collection("users").document(uid).collection("products")
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            handleFirestoreError(error, OperationType.LIST, "users/$uid/products")
            return@addSnapshotListener
          }
          if (snapshot != null) {
            scope.launch {
              for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val remoteUpdatedAt = (data["updatedAt"] as? Timestamp)?.toDate()?.time ?: 0L
                val localProd = shopDao.getProductById(doc.id)
                if (localProd == null || remoteUpdatedAt > localProd.updatedAt) {
                  val product = Product(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    barcode = data["barcode"] as? String ?: "",
                    category = data["category"] as? String ?: "General",
                    unit = data["unit"] as? String ?: "pcs",
                    costPrice = (data["costPrice"] as? Number)?.toDouble() ?: 0.0,
                    sellingPrice = (data["sellingPrice"] as? Number)?.toDouble() ?: 0.0,
                    stockQuantity = (data["stockQuantity"] as? Number)?.toInt() ?: 0,
                    minStockAlert = (data["minStockAlert"] as? Number)?.toInt() ?: 5,
                    createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                    updatedAt = remoteUpdatedAt,
                    syncStatus = "SYNCED"
                  )
                  shopDao.upsertProduct(product)
                }
              }
            }
          }
        }
      activeListeners.add(prodReg)
    } catch (e: Exception) {
      Log.e("ShopRepo", "Listener attach error", e)
    }
  }

  fun detachRealtimeListeners() {
    activeListeners.forEach { it.remove() }
    activeListeners.clear()
  }

  private suspend fun pullRemoteChanges(uid: String) {
    try {
      val prodsSnapshot = firestore.collection("users").document(uid).collection("products").get().await()
      for (doc in prodsSnapshot.documents) {
        val data = doc.data ?: continue
        val remoteUpdatedAt = (data["updatedAt"] as? Timestamp)?.toDate()?.time ?: 0L
        val localProd = shopDao.getProductById(doc.id)
        if (localProd == null || remoteUpdatedAt > localProd.updatedAt) {
          val product = Product(
            id = doc.id,
            name = data["name"] as? String ?: "",
            barcode = data["barcode"] as? String ?: "",
            category = data["category"] as? String ?: "General",
            unit = data["unit"] as? String ?: "pcs",
            costPrice = (data["costPrice"] as? Number)?.toDouble() ?: 0.0,
            sellingPrice = (data["sellingPrice"] as? Number)?.toDouble() ?: 0.0,
            stockQuantity = (data["stockQuantity"] as? Number)?.toInt() ?: 0,
            minStockAlert = (data["minStockAlert"] as? Number)?.toInt() ?: 5,
            createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
            updatedAt = remoteUpdatedAt,
            syncStatus = "SYNCED"
          )
          shopDao.upsertProduct(product)
        }
      }
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "users/$uid/products")
    }
  }

  // --- JSON FULL BACKUP & RESTORE ---
  suspend fun exportFullJsonBackup(): String = withContext(Dispatchers.IO) {
    val root = JSONObject()
    root.put("version", 1)
    root.put("exportedAt", System.currentTimeMillis())
    root.put("shopId", auth.currentUser?.uid ?: "local")

    val prodsArray = JSONArray()
    shopDao.getAllProductsSnapshot().forEach { p ->
      prodsArray.put(JSONObject(productToJson(p)))
    }
    root.put("products", prodsArray)

    val salesArray = JSONArray()
    shopDao.getAllSalesSnapshot().forEach { s ->
      salesArray.put(JSONObject(saleToJson(s)))
    }
    root.put("sales", salesArray)

    val moveArray = JSONArray()
    shopDao.getAllStockMovementsSnapshot().forEach { m ->
      moveArray.put(JSONObject(movementToJson(m)))
    }
    root.put("stock_movements", moveArray)

    val purArray = JSONArray()
    shopDao.getAllPurchasesSnapshot().forEach { pu ->
      purArray.put(JSONObject(purchaseToJson(pu)))
    }
    root.put("purchases", purArray)

    val custArray = JSONArray()
    shopDao.getAllCustomersSnapshot().forEach { c ->
      custArray.put(JSONObject(customerToJson(c)))
    }
    root.put("customers", custArray)

    val suppArray = JSONArray()
    shopDao.getAllSuppliersSnapshot().forEach { su ->
      suppArray.put(JSONObject(supplierToJson(su)))
    }
    root.put("suppliers", suppArray)

    val payArray = JSONArray()
    shopDao.getAllPaymentsSnapshot().forEach { py ->
      payArray.put(JSONObject(paymentToJson(py)))
    }
    root.put("payments", payArray)

    val expArray = JSONArray()
    shopDao.getAllExpensesSnapshot().forEach { ex ->
      expArray.put(JSONObject(expenseToJson(ex)))
    }
    root.put("expenses", expArray)

    root.toString(2)
  }

  suspend fun restoreFromJsonBackup(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
    try {
      val root = JSONObject(jsonString)
      var restoredCount = 0

      if (root.has("products")) {
        val array = root.getJSONArray("products")
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val p = Product(
            id = obj.getString("id"),
            name = obj.getString("name"),
            barcode = obj.getString("barcode"),
            category = obj.optString("category", "General"),
            unit = obj.optString("unit", "pcs"),
            costPrice = obj.getDouble("costPrice"),
            sellingPrice = obj.getDouble("sellingPrice"),
            stockQuantity = obj.getInt("stockQuantity"),
            minStockAlert = obj.optInt("minStockAlert", 5),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            syncStatus = "PENDING_SYNC"
          )
          shopDao.upsertProduct(p)
          queueMutation("PRODUCT", p.id, "UPDATE", productToJson(p))
          restoredCount++
        }
      }

      if (root.has("customers")) {
        val array = root.getJSONArray("customers")
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val c = Customer(
            id = obj.getString("id"),
            name = obj.getString("name"),
            phone = obj.optString("phone"),
            email = obj.optString("email"),
            address = obj.optString("address"),
            outstandingBalance = obj.optDouble("outstandingBalance", 0.0),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            syncStatus = "PENDING_SYNC"
          )
          shopDao.upsertCustomer(c)
          queueMutation("CUSTOMER", c.id, "UPDATE", customerToJson(c))
          restoredCount++
        }
      }

      if (root.has("suppliers")) {
        val array = root.getJSONArray("suppliers")
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val s = Supplier(
            id = obj.getString("id"),
            name = obj.getString("name"),
            contactPerson = obj.optString("contactPerson"),
            phone = obj.optString("phone"),
            email = obj.optString("email"),
            address = obj.optString("address"),
            outstandingBalance = obj.optDouble("outstandingBalance", 0.0),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            syncStatus = "PENDING_SYNC"
          )
          shopDao.upsertSupplier(s)
          queueMutation("SUPPLIER", s.id, "UPDATE", supplierToJson(s))
          restoredCount++
        }
      }

      triggerSync()
      Result.success(restoredCount)
    } catch (e: Exception) {
      Log.e("ShopRepo", "Backup restore failed", e)
      Result.failure(e)
    }
  }

  // --- CSV EXPORT HELPERS ---
  suspend fun exportProductsCsv(): String = withContext(Dispatchers.IO) {
    val prods = shopDao.getAllProductsSnapshot()
    val sb = StringBuilder()
    sb.append("Barcode,Name,Category,Unit,Cost Price,Selling Price,Stock Quantity,Min Stock Alert\n")
    for (p in prods) {
      sb.append("\"${p.barcode}\",\"${p.name}\",\"${p.category}\",\"${p.unit}\",${p.costPrice},${p.sellingPrice},${p.stockQuantity},${p.minStockAlert}\n")
    }
    sb.toString()
  }

  suspend fun exportSalesCsv(): String = withContext(Dispatchers.IO) {
    val sales = shopDao.getAllSalesSnapshot()
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val sb = StringBuilder()
    sb.append("Transaction ID,Date,Customer Name,Customer Phone,Payment Method,Subtotal,Discount,Tax,Total Amount\n")
    for (s in sales) {
      val dateStr = sdf.format(Date(s.timestamp))
      sb.append("\"${s.transactionId}\",\"$dateStr\",\"${s.customerName}\",\"${s.customerPhone}\",\"${s.paymentMethod}\",${s.subtotal},${s.discount},${s.tax},${s.totalAmount}\n")
    }
    sb.toString()
  }

  suspend fun exportExpensesCsv(): String = withContext(Dispatchers.IO) {
    val expenses = shopDao.getAllExpensesSnapshot()
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val sb = StringBuilder()
    sb.append("Date,Title,Category,Amount,Payment Method,Notes\n")
    for (e in expenses) {
      val dateStr = sdf.format(Date(e.timestamp))
      sb.append("\"$dateStr\",\"${e.title}\",\"${e.category}\",${e.amount},\"${e.paymentMethod}\",\"${e.note}\"\n")
    }
    sb.toString()
  }

  // --- PAYLOAD SERIALIZERS ---
  private fun productToJson(p: Product): String = JSONObject().apply {
    put("id", p.id)
    put("name", p.name)
    put("barcode", p.barcode)
    put("category", p.category)
    put("unit", p.unit)
    put("costPrice", p.costPrice)
    put("sellingPrice", p.sellingPrice)
    put("stockQuantity", p.stockQuantity)
    put("minStockAlert", p.minStockAlert)
    put("createdAt", p.createdAt)
    put("updatedAt", p.updatedAt)
  }.toString()

  private fun saleToJson(s: Sale): String = JSONObject().apply {
    put("id", s.id)
    put("transactionId", s.transactionId)
    put("customerName", s.customerName)
    put("customerPhone", s.customerPhone)
    put("itemsJson", s.itemsJson)
    put("subtotal", s.subtotal)
    put("discount", s.discount)
    put("tax", s.tax)
    put("totalAmount", s.totalAmount)
    put("paidAmount", s.paidAmount)
    put("changeAmount", s.changeAmount)
    put("paymentMethod", s.paymentMethod)
    put("timestamp", s.timestamp)
  }.toString()

  private fun movementToJson(m: StockMovement): String = JSONObject().apply {
    put("id", m.id)
    put("productId", m.productId)
    put("productName", m.productName)
    put("movementType", m.movementType)
    put("deltaQuantity", m.deltaQuantity)
    put("previousStock", m.previousStock)
    put("newStock", m.newStock)
    put("referenceId", m.referenceId)
    put("reason", m.reason)
    put("timestamp", m.timestamp)
  }.toString()

  private fun purchaseToJson(pu: Purchase): String = JSONObject().apply {
    put("id", pu.id)
    put("supplierId", pu.supplierId)
    put("supplierName", pu.supplierName)
    put("invoiceNumber", pu.invoiceNumber)
    put("itemsJson", pu.itemsJson)
    put("totalCost", pu.totalCost)
    put("paidAmount", pu.paidAmount)
    put("paymentStatus", pu.paymentStatus)
    put("timestamp", pu.timestamp)
  }.toString()

  private fun customerToJson(c: Customer): String = JSONObject().apply {
    put("id", c.id)
    put("name", c.name)
    put("phone", c.phone)
    put("email", c.email)
    put("address", c.address)
    put("outstandingBalance", c.outstandingBalance)
    put("createdAt", c.createdAt)
    put("updatedAt", c.updatedAt)
  }.toString()

  private fun supplierToJson(s: Supplier): String = JSONObject().apply {
    put("id", s.id)
    put("name", s.name)
    put("contactPerson", s.contactPerson)
    put("phone", s.phone)
    put("email", s.email)
    put("address", s.address)
    put("outstandingBalance", s.outstandingBalance)
    put("createdAt", s.createdAt)
    put("updatedAt", s.updatedAt)
  }.toString()

  private fun paymentToJson(py: Payment): String = JSONObject().apply {
    put("id", py.id)
    put("type", py.type)
    put("partyId", py.partyId)
    put("partyName", py.partyName)
    put("amount", py.amount)
    put("paymentMethod", py.paymentMethod)
    put("note", py.note)
    put("timestamp", py.timestamp)
  }.toString()

  private fun expenseToJson(ex: Expense): String = JSONObject().apply {
    put("id", ex.id)
    put("title", ex.title)
    put("category", ex.category)
    put("amount", ex.amount)
    put("paymentMethod", ex.paymentMethod)
    put("note", ex.note)
    put("timestamp", ex.timestamp)
  }.toString()

  private fun jsonToFirestoreMap(json: JSONObject, isCreate: Boolean): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    val keys = json.keys()
    while (keys.hasNext()) {
      val key = keys.next()
      if (key == "id") continue
      if (key == "createdAt") {
        if (isCreate) {
          map[key] = FieldValue.serverTimestamp()
        }
      } else if (key == "updatedAt" || key == "timestamp") {
        map[key] = FieldValue.serverTimestamp()
      } else {
        map[key] = json.get(key)
      }
    }
    return map
  }
}
