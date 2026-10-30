package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.firebase.firestore.IgnoreExtraProperties
import org.json.JSONArray
import org.json.JSONObject

class RoomConverters {
  @TypeConverter
  fun fromStringList(value: List<String>?): String = value?.joinToString(",") ?: ""

  @TypeConverter
  fun toStringList(value: String?): List<String> =
    if (value.isNullOrBlank()) emptyList() else value.split(",")
}

@IgnoreExtraProperties
@Entity(tableName = "products")
data class Product(
  @PrimaryKey val id: String = "",
  val name: String = "",
  val barcode: String = "",
  val category: String = "General",
  val unit: String = "pcs",
  val costPrice: Double = 0.0,
  val sellingPrice: Double = 0.0,
  val stockQuantity: Int = 0,
  val minStockAlert: Int = 5,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val syncStatus: String = "SYNCED"
)

@IgnoreExtraProperties
@Entity(tableName = "stock_movements")
data class StockMovement(
  @PrimaryKey val id: String = "",
  val productId: String = "",
  val productName: String = "",
  val movementType: String = "ADJUSTMENT", // SALE, PURCHASE, ADJUSTMENT, RETURN
  val deltaQuantity: Int = 0,
  val previousStock: Int = 0,
  val newStock: Int = 0,
  val referenceId: String = "",
  val reason: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val syncStatus: String = "SYNCED"
)

@IgnoreExtraProperties
@Entity(tableName = "sales")
data class Sale(
  @PrimaryKey val id: String = "", // Unique transaction ID (e.g. txn_1730000000000_abc)
  val transactionId: String = "",
  val customerName: String = "",
  val customerPhone: String = "",
  val itemsJson: String = "[]",
  val subtotal: Double = 0.0,
  val discount: Double = 0.0,
  val tax: Double = 0.0,
  val totalAmount: Double = 0.0,
  val paidAmount: Double = 0.0,
  val changeAmount: Double = 0.0,
  val paymentMethod: String = "CASH", // CASH, CARD, MOBILE_PAYMENT, STORE_CREDIT
  val timestamp: Long = System.currentTimeMillis(),
  val syncStatus: String = "SYNCED"
)

@IgnoreExtraProperties
@Entity(tableName = "purchases")
data class Purchase(
  @PrimaryKey val id: String = "",
  val supplierId: String = "",
  val supplierName: String = "",
  val invoiceNumber: String = "",
  val itemsJson: String = "[]",
  val totalCost: Double = 0.0,
  val paidAmount: Double = 0.0,
  val paymentStatus: String = "PAID", // PAID, PARTIAL, DUE
  val timestamp: Long = System.currentTimeMillis(),
  val syncStatus: String = "SYNCED"
)

@IgnoreExtraProperties
@Entity(tableName = "customers")
data class Customer(
  @PrimaryKey val id: String = "",
  val name: String = "",
  val phone: String = "",
  val email: String = "",
  val address: String = "",
  val outstandingBalance: Double = 0.0,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val syncStatus: String = "SYNCED"
)

@IgnoreExtraProperties
@Entity(tableName = "suppliers")
data class Supplier(
  @PrimaryKey val id: String = "",
  val name: String = "",
  val contactPerson: String = "",
  val phone: String = "",
  val email: String = "",
  val address: String = "",
  val outstandingBalance: Double = 0.0,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val syncStatus: String = "SYNCED"
)

@IgnoreExtraProperties
@Entity(tableName = "payments")
data class Payment(
  @PrimaryKey val id: String = "",
  val type: String = "CUSTOMER_RECEIPT", // CUSTOMER_RECEIPT, SUPPLIER_PAYOUT
  val partyId: String = "",
  val partyName: String = "",
  val amount: Double = 0.0,
  val paymentMethod: String = "CASH", // CASH, BANK_TRANSFER, CHEQUE, MOBILE_PAYMENT
  val note: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val syncStatus: String = "SYNCED"
)

@IgnoreExtraProperties
@Entity(tableName = "expenses")
data class Expense(
  @PrimaryKey val id: String = "",
  val title: String = "",
  val category: String = "General", // Rent, Utilities, Salary, Maintenance, Supplies, Other
  val amount: Double = 0.0,
  val paymentMethod: String = "CASH", // CASH, BANK_TRANSFER, CARD, MOBILE_PAYMENT
  val note: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val syncStatus: String = "SYNCED"
)

@IgnoreExtraProperties
@Entity(tableName = "daily_reports")
data class DailyReport(
  @PrimaryKey val id: String = "", // e.g. 2026-10-30
  val reportDate: String = "",
  val totalSales: Double = 0.0,
  val totalCost: Double = 0.0,
  val grossProfit: Double = 0.0,
  val totalExpenses: Double = 0.0,
  val netProfit: Double = 0.0,
  val transactionCount: Int = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val syncStatus: String = "SYNCED"
)

@Entity(tableName = "sync_queue")
data class SyncQueueItem(
  @PrimaryKey(autoGenerate = true) val queueId: Long = 0,
  val entityType: String = "", // PRODUCT, STOCK_MOVEMENT, SALE, PURCHASE, CUSTOMER, SUPPLIER, PAYMENT, EXPENSE, REPORT
  val entityId: String = "",
  val action: String = "CREATE", // CREATE, UPDATE, DELETE
  val payloadJson: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val attempts: Int = 0
)

data class CartItem(
  val product: Product,
  var quantity: Int = 1
) {
  val lineTotal: Double get() = product.sellingPrice * quantity
  val costTotal: Double get() = product.costPrice * quantity
}

data class SaleItemRecord(
  val productId: String,
  val productName: String,
  val barcode: String,
  val quantity: Int,
  val unitPrice: Double,
  val costPrice: Double,
  val lineTotal: Double
) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("productId", productId)
    put("productName", productName)
    put("barcode", barcode)
    put("quantity", quantity)
    put("unitPrice", unitPrice)
    put("costPrice", costPrice)
    put("lineTotal", lineTotal)
  }

  companion object {
    fun fromJson(obj: JSONObject): SaleItemRecord = SaleItemRecord(
      productId = obj.optString("productId"),
      productName = obj.optString("productName"),
      barcode = obj.optString("barcode"),
      quantity = obj.optInt("quantity", 1),
      unitPrice = obj.optDouble("unitPrice", 0.0),
      costPrice = obj.optDouble("costPrice", 0.0),
      lineTotal = obj.optDouble("lineTotal", 0.0)
    )

    fun listFromJson(json: String): List<SaleItemRecord> {
      val result = mutableListOf<SaleItemRecord>()
      try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
          result.add(fromJson(array.getJSONObject(i)))
        }
      } catch (e: Exception) {
        // Fallback or empty
      }
      return result
    }

    fun listToJson(list: List<SaleItemRecord>): String {
      val array = JSONArray()
      list.forEach { array.put(it.toJson()) }
      return array.toString()
    }
  }
}
