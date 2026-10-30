package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.Customer
import com.example.data.model.DailyReport
import com.example.data.model.Expense
import com.example.data.model.Payment
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.RoomConverters
import com.example.data.model.Sale
import com.example.data.model.StockMovement
import com.example.data.model.Supplier
import com.example.data.model.SyncQueueItem

@Database(
  entities = [
    Product::class,
    StockMovement::class,
    Sale::class,
    Purchase::class,
    Customer::class,
    Supplier::class,
    Payment::class,
    Expense::class,
    DailyReport::class,
    SyncQueueItem::class
  ],
  version = 1,
  exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class ShopDatabase : RoomDatabase() {
  abstract fun shopDao(): ShopDao

  companion object {
    @Volatile
    private var INSTANCE: ShopDatabase? = null

    fun getDatabase(context: Context): ShopDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          ShopDatabase::class.java,
          "my_shop_database"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
