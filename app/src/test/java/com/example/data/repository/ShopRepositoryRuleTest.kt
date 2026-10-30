package com.example.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.ShopDatabase
import com.example.data.model.CartItem
import com.example.data.model.Product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShopRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private lateinit var db: ShopDatabase
  private lateinit var repository: ShopRepository

  @Before
  fun setUpLocalDb() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    db = Room.inMemoryDatabaseBuilder(context, ShopDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = ShopRepository(db.shopDao(), firestore)
  }

  @After
  fun tearDownLocalDb() {
    db.close()
  }

  @Test
  fun testCreateProductAndSyncToFirestore() = runBlocking {
    val aliceEmail = "alice_shop_${System.currentTimeMillis()}@test.com"
    val uid = signInTestUser(aliceEmail)

    val product = Product(
      id = "prod_test_1",
      name = "Organic Honey 500g",
      barcode = "990011223344",
      category = "Pantry",
      unit = "jar",
      costPrice = 4.50,
      sellingPrice = 7.99,
      stockQuantity = 25,
      minStockAlert = 5
    )

    repository.saveProduct(product)

    // Check saved in local Room database
    val localProds = repository.observeProducts().first()
    assertTrue(localProds.any { it.id == "prod_test_1" })

    // Execute sync to push queue item to Firestore
    repository.syncNow()

    // Verify document exists in Firestore under /users/{uid}/products/prod_test_1
    val firestoreDoc = firestore.collection("users").document(uid).collection("products").document("prod_test_1").get().await()
    assertTrue(firestoreDoc.exists())
    assertEquals("Organic Honey 500g", firestoreDoc.getString("name"))
    assertEquals(7.99, firestoreDoc.getDouble("sellingPrice") ?: 0.0, 0.01)
  }

  @Test
  fun testCompleteSaleAndStockDeduction() = runBlocking {
    val aliceEmail = "alice_pos_${System.currentTimeMillis()}@test.com"
    val uid = signInTestUser(aliceEmail)

    val product = Product(
      id = "prod_juice",
      name = "Apple Juice 1L",
      barcode = "123456789012",
      costPrice = 1.00,
      sellingPrice = 2.50,
      stockQuantity = 10
    )
    repository.saveProduct(product)

    val cart = listOf(CartItem(product, quantity = 3))
    val sale = repository.completeSale(
      cartItems = cart,
      customerName = "John Doe",
      customerPhone = "123-456",
      discount = 0.50,
      tax = 0.0,
      paidAmount = 10.0,
      paymentMethod = "CASH"
    )

    assertNotNull(sale.transactionId)
    assertEquals(7.00, sale.totalAmount, 0.01)

    // Verify local stock decremented (10 - 3 = 7)
    val updatedProduct = db.shopDao().getProductById("prod_juice")
    assertNotNull(updatedProduct)
    assertEquals(7, updatedProduct!!.stockQuantity)

    // Sync to Firestore
    repository.syncNow()

    // Verify sale document in Firestore
    val firestoreSale = firestore.collection("users").document(uid).collection("sales").document(sale.id).get().await()
    assertTrue(firestoreSale.exists())
    assertEquals(sale.transactionId, firestoreSale.getString("transactionId"))
  }
}
