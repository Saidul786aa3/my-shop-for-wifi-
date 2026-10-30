package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Customer
import com.example.data.model.DailyReport
import com.example.data.model.Expense
import com.example.data.model.Payment
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.Sale
import com.example.data.model.StockMovement
import com.example.data.model.Supplier
import com.example.data.model.SyncQueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {

  // Products
  @Query("SELECT * FROM products ORDER BY name ASC")
  fun observeProducts(): Flow<List<Product>>

  @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
  suspend fun getProductById(id: String): Product?

  @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
  suspend fun getProductByBarcode(barcode: String): Product?

  @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
  fun searchProducts(query: String): Flow<List<Product>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProduct(product: Product)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProducts(products: List<Product>)

  @Query("UPDATE products SET stockQuantity = :newStock, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
  suspend fun updateProductStock(id: String, newStock: Int, updatedAt: Long, syncStatus: String)

  @Delete
  suspend fun deleteProduct(product: Product)

  @Query("DELETE FROM products WHERE id = :id")
  suspend fun deleteProductById(id: String)

  // Stock Movements
  @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
  fun observeStockMovements(): Flow<List<StockMovement>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStockMovement(movement: StockMovement)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStockMovements(movements: List<StockMovement>)

  // Sales
  @Query("SELECT * FROM sales ORDER BY timestamp DESC")
  fun observeSales(): Flow<List<Sale>>

  @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
  suspend fun getSaleById(id: String): Sale?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSale(sale: Sale)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSales(sales: List<Sale>)

  // Purchases
  @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
  fun observePurchases(): Flow<List<Purchase>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPurchase(purchase: Purchase)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPurchases(purchases: List<Purchase>)

  // Customers
  @Query("SELECT * FROM customers ORDER BY name ASC")
  fun observeCustomers(): Flow<List<Customer>>

  @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
  suspend fun getCustomerById(id: String): Customer?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCustomer(customer: Customer)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCustomers(customers: List<Customer>)

  @Delete
  suspend fun deleteCustomer(customer: Customer)

  // Suppliers
  @Query("SELECT * FROM suppliers ORDER BY name ASC")
  fun observeSuppliers(): Flow<List<Supplier>>

  @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
  suspend fun getSupplierById(id: String): Supplier?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSupplier(supplier: Supplier)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSuppliers(suppliers: List<Supplier>)

  @Delete
  suspend fun deleteSupplier(supplier: Supplier)

  // Payments
  @Query("SELECT * FROM payments ORDER BY timestamp DESC")
  fun observePayments(): Flow<List<Payment>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPayment(payment: Payment)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPayments(payments: List<Payment>)

  // Expenses
  @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
  fun observeExpenses(): Flow<List<Expense>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertExpense(expense: Expense)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertExpenses(expenses: List<Expense>)

  @Delete
  suspend fun deleteExpense(expense: Expense)

  // Daily Reports
  @Query("SELECT * FROM daily_reports ORDER BY reportDate DESC")
  fun observeDailyReports(): Flow<List<DailyReport>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertDailyReport(report: DailyReport)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertDailyReports(reports: List<DailyReport>)

  // Offline Sync Queue
  @Query("SELECT * FROM sync_queue ORDER BY queueId ASC")
  suspend fun getPendingSyncQueue(): List<SyncQueueItem>

  @Query("SELECT COUNT(*) FROM sync_queue")
  fun observePendingSyncCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun queueMutation(item: SyncQueueItem): Long

  @Query("DELETE FROM sync_queue WHERE queueId = :queueId")
  suspend fun removeSyncQueueItem(queueId: Long)

  @Query("DELETE FROM sync_queue")
  suspend fun clearSyncQueue()

  // Full Local Data Dump for Backup
  @Query("SELECT * FROM products")
  suspend fun getAllProductsSnapshot(): List<Product>

  @Query("SELECT * FROM stock_movements")
  suspend fun getAllStockMovementsSnapshot(): List<StockMovement>

  @Query("SELECT * FROM sales")
  suspend fun getAllSalesSnapshot(): List<Sale>

  @Query("SELECT * FROM purchases")
  suspend fun getAllPurchasesSnapshot(): List<Purchase>

  @Query("SELECT * FROM customers")
  suspend fun getAllCustomersSnapshot(): List<Customer>

  @Query("SELECT * FROM suppliers")
  suspend fun getAllSuppliersSnapshot(): List<Supplier>

  @Query("SELECT * FROM payments")
  suspend fun getAllPaymentsSnapshot(): List<Payment>

  @Query("SELECT * FROM expenses")
  suspend fun getAllExpensesSnapshot(): List<Expense>

  @Query("SELECT * FROM daily_reports")
  suspend fun getAllDailyReportsSnapshot(): List<DailyReport>
}
