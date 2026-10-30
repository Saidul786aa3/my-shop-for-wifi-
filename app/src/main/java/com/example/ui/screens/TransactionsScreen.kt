package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Sale
import com.example.data.model.SaleItemRecord
import com.example.ui.ShopViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
  viewModel: ShopViewModel
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Sales Receipts", "Purchases", "Stock Movement Ledger")

  val sales by viewModel.sales.collectAsStateWithLifecycle()
  val purchases by viewModel.purchases.collectAsStateWithLifecycle()
  val movements by viewModel.stockMovements.collectAsStateWithLifecycle()

  var selectedSaleForReceipt by remember { mutableStateOf<Sale?>(null) }

  Column(modifier = Modifier.fillMaxSize()) {
    PrimaryTabRow(
      selectedTabIndex = selectedTab,
      containerColor = Slate900,
      contentColor = EmeraldLight
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedTab == index,
          onClick = { selectedTab = index },
          text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
        )
      }
    }

    when (selectedTab) {
      0 -> {
        // Sales Receipts
        if (sales.isEmpty()) {
          EmptyState("No sales completed yet", "Sales rung up at the POS counter will appear here")
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(sales, key = { it.id }) { sale ->
              val dateStr = remember(sale.timestamp) {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(sale.timestamp))
              }
              val itemsCount = remember(sale.itemsJson) {
                SaleItemRecord.listFromJson(sale.itemsJson).sumOf { it.quantity }
              }

              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("sale_card_${sale.id}")
                  .clickable { selectedSaleForReceipt = sale },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldLight.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.Receipt, contentDescription = null, tint = EmeraldLight)
                    }
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = sale.transactionId,
                      style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                      )
                    )
                    Text(
                      text = "$dateStr • ${if (sale.customerName.isNotBlank()) sale.customerName else "Walk-in Customer"}",
                      style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                    Text(
                      text = "$itemsCount items • Payment: ${sale.paymentMethod}",
                      style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                  }

                  Text(
                    text = String.format(Locale.getDefault(), "$%.2f", sale.totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = EmeraldLight
                    )
                  )
                }
              }
            }
          }
        }
      }
      1 -> {
        // Purchases
        if (purchases.isEmpty()) {
          EmptyState("No stock purchases logged", "Supplier orders and stock replenishment will appear here")
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(purchases, key = { it.id }) { purchase ->
              val dateStr = remember(purchase.timestamp) {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(purchase.timestamp))
              }
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IndigoAccent.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = IndigoAccent)
                    }
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(purchase.supplierName, fontWeight = FontWeight.Bold)
                    Text(
                      text = "Inv #${purchase.invoiceNumber.ifBlank { "N/A" }} • $dateStr",
                      style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                    Text(
                      text = "Status: ${purchase.paymentStatus}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = if (purchase.paymentStatus == "PAID") EmeraldLight else AmberWarning,
                        fontWeight = FontWeight.SemiBold
                      )
                    )
                  }

                  Text(
                    text = String.format(Locale.getDefault(), "$%.2f", purchase.totalCost),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                  )
                }
              }
            }
          }
        }
      }
      2 -> {
        // Stock Movement Ledger
        if (movements.isEmpty()) {
          EmptyState("No stock movements recorded", "Every sale, purchase and adjustment produces an audit record")
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(movements, key = { it.id }) { move ->
              val dateStr = remember(move.timestamp) {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(move.timestamp))
              }
              val isPositive = move.deltaQuantity >= 0

              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPositive) EmeraldLight.copy(alpha = 0.15f) else CoralError.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = if (isPositive) EmeraldLight else CoralError,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(10.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(move.productName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                      text = "${move.movementType} • ${move.reason.ifBlank { "No memo" }}",
                      style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
                    )
                    Text(
                      text = dateStr,
                      style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        fontSize = 10.sp
                      )
                    )
                  }

                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = if (isPositive) "+${move.deltaQuantity}" else "${move.deltaQuantity}",
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp,
                      color = if (isPositive) EmeraldLight else CoralError
                    )
                    Text(
                      text = "Stock: ${move.newStock}",
                      fontSize = 11.sp,
                      color = Color.Gray
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  selectedSaleForReceipt?.let { sale ->
    ReceiptDialog(sale = sale, onDismiss = { selectedSaleForReceipt = null })
  }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium, color = Color.Gray)
      Spacer(modifier = Modifier.height(4.dp))
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray.copy(alpha = 0.6f))
    }
  }
}
