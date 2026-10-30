package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ShopDatabase
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
import com.example.data.repository.ShopRepository
import com.example.data.repository.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShopViewModel(application: Application) : AndroidViewModel(application) {

  private val shopDatabase = ShopDatabase.getDatabase(application)
  private val repository = ShopRepository(application, shopDatabase.shopDao())

  val syncState: StateFlow<SyncState> = repository.syncState
  val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val rawProducts: StateFlow<List<Product>> = repository.observeProducts()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val sales: StateFlow<List<Sale>> = repository.observeSales()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val purchases: StateFlow<List<Purchase>> = repository.observePurchases()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val stockMovements: StateFlow<List<StockMovement>> = repository.observeStockMovements()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val customers: StateFlow<List<Customer>> = repository.observeCustomers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val suppliers: StateFlow<List<Supplier>> = repository.observeSuppliers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val payments: StateFlow<List<Payment>> = repository.observePayments()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val expenses: StateFlow<List<Expense>> = repository.observeExpenses()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val dailyReports: StateFlow<List<DailyReport>> = repository.observeDailyReports()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val searchQuery = MutableStateFlow("")
  val selectedCategory = MutableStateFlow("All")

  val filteredProducts: StateFlow<List<Product>> = combine(
    rawProducts,
    searchQuery,
    selectedCategory
  ) { prods, query, category ->
    prods.filter { p ->
      val matchesCategory = (category == "All" || p.category.equals(category, ignoreCase = true))
      val matchesQuery = if (query.isBlank()) true else {
        p.name.contains(query, ignoreCase = true) ||
          p.barcode.contains(query, ignoreCase = true) ||
          p.category.contains(query, ignoreCase = true)
      }
      matchesCategory && matchesQuery
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // POS Cart State
  private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
  val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

  init {
    repository.attachRealtimeListeners()
    repository.triggerSync()
  }

  override fun onCleared() {
    super.onCleared()
    repository.detachRealtimeListeners()
  }

  // --- CART OPERATIONS ---
  fun addToCart(product: Product) {
    val current = _cart.value.toMutableList()
    val index = current.indexOfFirst { it.product.id == product.id }
    if (index >= 0) {
      val existing = current[index]
      current[index] = existing.copy(quantity = existing.quantity + 1)
    } else {
      current.add(CartItem(product = product, quantity = 1))
    }
    _cart.value = current
  }

  fun updateCartQuantity(productId: String, quantity: Int) {
    if (quantity <= 0) {
      removeFromCart(productId)
      return
    }
    val current = _cart.value.toMutableList()
    val index = current.indexOfFirst { it.product.id == productId }
    if (index >= 0) {
      current[index] = current[index].copy(quantity = quantity)
      _cart.value = current
    }
  }

  fun removeFromCart(productId: String) {
    _cart.value = _cart.value.filter { it.product.id != productId }
  }

  fun clearCart() {
    _cart.value = emptyList()
  }

  fun checkout(
    customerName: String,
    customerPhone: String,
    discount: Double,
    tax: Double,
    paidAmount: Double,
    paymentMethod: String,
    onSuccess: (Sale) -> Unit,
    onError: (String) -> Unit
  ) {
    val currentCart = _cart.value
    if (currentCart.isEmpty()) {
      onError("Cart is empty")
      return
    }
    viewModelScope.launch {
      try {
        val sale = repository.completeSale(
          cartItems = currentCart,
          customerName = customerName,
          customerPhone = customerPhone,
          discount = discount,
          tax = tax,
          paidAmount = paidAmount,
          paymentMethod = paymentMethod
        )
        clearCart()
        onSuccess(sale)
      } catch (e: Exception) {
        onError(e.localizedMessage ?: "Checkout failed")
      }
    }
  }

  // --- INVENTORY OPERATIONS ---
  fun saveProduct(product: Product, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.saveProduct(product)
      onComplete()
    }
  }

  fun adjustStock(product: Product, delta: Int, reason: String, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.adjustProductStock(product, delta, reason)
      onComplete()
    }
  }

  fun deleteProduct(product: Product) {
    viewModelScope.launch {
      repository.deleteProduct(product)
    }
  }

  // --- PURCHASES ---
  fun recordPurchase(
    supplierName: String,
    supplierId: String,
    invoiceNumber: String,
    items: List<SaleItemRecord>,
    totalCost: Double,
    paidAmount: Double,
    paymentStatus: String,
    onComplete: () -> Unit = {}
  ) {
    viewModelScope.launch {
      repository.recordPurchase(
        supplierName,
        supplierId,
        invoiceNumber,
        items,
        totalCost,
        paidAmount,
        paymentStatus
      )
      onComplete()
    }
  }

  // --- CUSTOMERS & SUPPLIERS ---
  fun saveCustomer(customer: Customer, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.saveCustomer(customer)
      onComplete()
    }
  }

  fun saveSupplier(supplier: Supplier, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.saveSupplier(supplier)
      onComplete()
    }
  }

  // --- PAYMENTS & EXPENSES ---
  fun recordPayment(payment: Payment, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.recordPayment(payment)
      onComplete()
    }
  }

  fun recordExpense(expense: Expense, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.recordExpense(expense)
      onComplete()
    }
  }

  // --- SYNC & BACKUP ---
  fun triggerSync() {
    repository.triggerSync()
  }

  fun exportJsonBackup(onResult: (String) -> Unit) {
    viewModelScope.launch {
      val json = repository.exportFullJsonBackup()
      onResult(json)
    }
  }

  fun restoreJsonBackup(jsonString: String, onResult: (Result<Int>) -> Unit) {
    viewModelScope.launch {
      val result = repository.restoreFromJsonBackup(jsonString)
      onResult(result)
    }
  }

  fun exportProductsCsv(onResult: (String) -> Unit) {
    viewModelScope.launch {
      val csv = repository.exportProductsCsv()
      onResult(csv)
    }
  }

  fun exportSalesCsv(onResult: (String) -> Unit) {
    viewModelScope.launch {
      val csv = repository.exportSalesCsv()
      onResult(csv)
    }
  }

  fun exportExpensesCsv(onResult: (String) -> Unit) {
    viewModelScope.launch {
      val csv = repository.exportExpensesCsv()
      onResult(csv)
    }
  }
}
