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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.Product
import com.example.ui.ShopViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.util.Locale

@Composable
fun InventoryScreen(
  viewModel: ShopViewModel
) {
  val products by viewModel.rawProducts.collectAsStateWithLifecycle()
  var searchQuery by remember { mutableStateOf("") }
  var showLowStockOnly by remember { mutableStateOf(false) }

  var editingProduct by remember { mutableStateOf<Product?>(null) }
  var isCreatingProduct by remember { mutableStateOf(false) }
  var adjustingProduct by remember { mutableStateOf<Product?>(null) }

  val filtered = remember(products, searchQuery, showLowStockOnly) {
    products.filter { p ->
      val matchesQuery = if (searchQuery.isBlank()) true else {
        p.name.contains(searchQuery, ignoreCase = true) ||
          p.barcode.contains(searchQuery, ignoreCase = true) ||
          p.category.contains(searchQuery, ignoreCase = true)
      }
      val matchesLowStock = if (showLowStockOnly) p.stockQuantity <= p.minStockAlert else true
      matchesQuery && matchesLowStock
    }
  }

  val totalRetailValue = remember(products) { products.sumOf { it.sellingPrice * it.stockQuantity } }
  val totalCostValue = remember(products) { products.sumOf { it.costPrice * it.stockQuantity } }
  val lowStockCount = remember(products) { products.count { it.stockQuantity <= it.minStockAlert } }

  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Inventory Summary Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = Slate800)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Inventory Valuation",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              )
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (lowStockCount > 0) AmberWarning.copy(alpha = 0.2f) else EmeraldLight.copy(alpha = 0.2f),
                modifier = Modifier.clickable { showLowStockOnly = !showLowStockOnly }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = if (lowStockCount > 0) Icons.Default.Warning else Icons.Default.Inventory,
                    contentDescription = null,
                    tint = if (lowStockCount > 0) AmberWarning else EmeraldLight,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "$lowStockCount Low Stock",
                    color = if (lowStockCount > 0) AmberWarning else EmeraldLight,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Column {
                Text("Total Stock Value (Retail)", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                Text(
                  text = String.format(Locale.getDefault(), "$%.2f", totalRetailValue),
                  style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight
                  )
                )
              }
              Column {
                Text("Total Cost", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                Text(
                  text = String.format(Locale.getDefault(), "$%.2f", totalCostValue),
                  style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                )
              }
              Column {
                Text("Total Skus", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                Text(
                  text = products.size.toString(),
                  style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                )
              }
            }
          }
        }
      }

      // Search Field
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Filter products by name or barcode...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          singleLine = true
        )
      }

      // Product List
      items(filtered, key = { it.id }) { product ->
        val isLow = product.stockQuantity <= product.minStockAlert
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_item_${product.id}"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = product.name,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "Barcode: ${product.barcode} • ${product.category}",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                  )
                )
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isLow) AmberWarning.copy(alpha = 0.15f) else EmeraldLight.copy(alpha = 0.15f)
              ) {
                Text(
                  text = "${product.stockQuantity} ${product.unit}",
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  fontWeight = FontWeight.Bold,
                  color = if (isLow) AmberWarning else EmeraldLight,
                  fontSize = 13.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Cost: $${String.format(Locale.getDefault(), "%.2f", product.costPrice)}  |  Sell: $${String.format(Locale.getDefault(), "%.2f", product.sellingPrice)}",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                val margin = if (product.sellingPrice > 0) {
                  ((product.sellingPrice - product.costPrice) / product.sellingPrice) * 100
                } else 0.0
                Text(
                  text = String.format(Locale.getDefault(), "Profit Margin: %.1f%%", margin),
                  style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
              }

              Row {
                OutlinedButton(
                  onClick = { adjustingProduct = product },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Icon(Icons.Default.Tune, contentDescription = "Adjust", modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Adjust", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                  onClick = { editingProduct = product },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate700)
                }
              }
            }
          }
        }
      }
    }

    // Floating Action Button to Add Product
    FloatingActionButton(
      onClick = { isCreatingProduct = true },
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(20.dp)
        .testTag("add_product_fab"),
      containerColor = EmeraldLight,
      contentColor = Slate900
    ) {
      Icon(Icons.Default.Add, contentDescription = "Add Product")
    }
  }

  // Create or Edit Product Dialog
  if (isCreatingProduct || editingProduct != null) {
    ProductFormDialog(
      initialProduct = editingProduct,
      onDismiss = {
        isCreatingProduct = false
        editingProduct = null
      },
      onSave = { product ->
        viewModel.saveProduct(product) {
          isCreatingProduct = false
          editingProduct = null
        }
      },
      onDelete = { product ->
        viewModel.deleteProduct(product)
        editingProduct = null
      }
    )
  }

  // Quick Stock Adjustment Dialog
  adjustingProduct?.let { product ->
    StockAdjustmentDialog(
      product = product,
      onDismiss = { adjustingProduct = null },
      onConfirm = { delta, reason ->
        viewModel.adjustStock(product, delta, reason) {
          adjustingProduct = null
        }
      }
    )
  }
}

@Composable
fun ProductFormDialog(
  initialProduct: Product?,
  onDismiss: () -> Unit,
  onSave: (Product) -> Unit,
  onDelete: (Product) -> Unit
) {
  var name by remember { mutableStateOf(initialProduct?.name ?: "") }
  var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
  var category by remember { mutableStateOf(initialProduct?.category ?: "General") }
  var unit by remember { mutableStateOf(initialProduct?.unit ?: "pcs") }
  var costPriceStr by remember { mutableStateOf(initialProduct?.costPrice?.toString() ?: "0.0") }
  var sellingPriceStr by remember { mutableStateOf(initialProduct?.sellingPrice?.toString() ?: "0.0") }
  var stockQuantityStr by remember { mutableStateOf(initialProduct?.stockQuantity?.toString() ?: "0") }
  var minStockAlertStr by remember { mutableStateOf(initialProduct?.minStockAlert?.toString() ?: "5") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (initialProduct == null) "Add New Product" else "Edit Product", fontWeight = FontWeight.Bold)
    },
    text = {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Product Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
        item {
          OutlinedTextField(
            value = barcode,
            onValueChange = { barcode = it },
            label = { Text("Barcode (UPC/EAN) *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = category,
              onValueChange = { category = it },
              label = { Text("Category") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
            OutlinedTextField(
              value = unit,
              onValueChange = { unit = it },
              label = { Text("Unit") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
          }
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = costPriceStr,
              onValueChange = { costPriceStr = it },
              label = { Text("Cost Price ($)") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
            OutlinedTextField(
              value = sellingPriceStr,
              onValueChange = { sellingPriceStr = it },
              label = { Text("Selling Price ($) *") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
          }
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = stockQuantityStr,
              onValueChange = { stockQuantityStr = it },
              label = { Text("Initial Stock") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
            OutlinedTextField(
              value = minStockAlertStr,
              onValueChange = { minStockAlertStr = it },
              label = { Text("Low Stock Alert") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank() || barcode.isBlank()) return@Button
          val p = (initialProduct ?: Product()).copy(
            name = name.trim(),
            barcode = barcode.trim(),
            category = category.trim().ifBlank { "General" },
            unit = unit.trim().ifBlank { "pcs" },
            costPrice = costPriceStr.toDoubleOrNull() ?: 0.0,
            sellingPrice = sellingPriceStr.toDoubleOrNull() ?: 0.0,
            stockQuantity = stockQuantityStr.toIntOrNull() ?: 0,
            minStockAlert = minStockAlertStr.toIntOrNull() ?: 5
          )
          onSave(p)
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = EmeraldLight,
          contentColor = Slate900
        )
      ) {
        Text("Save Product", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      Row {
        if (initialProduct != null) {
          TextButton(onClick = { onDelete(initialProduct) }) {
            Text("Delete", color = CoralError)
          }
        }
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
      }
    }
  )
}

@Composable
fun StockAdjustmentDialog(
  product: Product,
  onDismiss: () -> Unit,
  onConfirm: (delta: Int, reason: String) -> Unit
) {
  var deltaStr by remember { mutableStateOf("0") }
  var reason by remember { mutableStateOf("Physical count adjustment") }
  val delta = deltaStr.toIntOrNull() ?: 0

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Adjust Stock: ${product.name}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Current On-Hand: ${product.stockQuantity} ${product.unit}")
        OutlinedTextField(
          value = deltaStr,
          onValueChange = { deltaStr = it },
          label = { Text("Stock Change (+ to add, - to reduce)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
        Text(
          text = "New On-Hand will be: ${product.stockQuantity + delta} ${product.unit}",
          fontWeight = FontWeight.Bold,
          color = EmeraldLight
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Reason for adjustment") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(delta, reason) },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = Slate900)
      ) {
        Text("Apply")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
