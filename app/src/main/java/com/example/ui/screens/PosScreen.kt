package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItemRecord
import com.example.ui.ShopViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
  viewModel: ShopViewModel
) {
  val context = LocalContext.current
  val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
  val rawProducts by viewModel.rawProducts.collectAsStateWithLifecycle()
  val cart by viewModel.cart.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

  var showCartSheet by remember { mutableStateOf(false) }
  var showCheckoutDialog by remember { mutableStateOf(false) }
  var latestSaleReceipt by remember { mutableStateOf<Sale?>(null) }

  val categories = remember(rawProducts) {
    listOf("All") + rawProducts.map { it.category }.distinct().filter { it.isNotBlank() }
  }

  val totalCartItems = cart.sumOf { it.quantity }
  val totalCartAmount = cart.sumOf { it.lineTotal }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = if (cart.isNotEmpty()) 80.dp else 16.dp)
    ) {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { viewModel.searchQuery.value = it },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .testTag("pos_search_input"),
        placeholder = { Text("Search by name, barcode or category...") },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear search")
            }
          } else {
            Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode ready", tint = EmeraldLight)
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
      )

      // Category Chips
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(categories) { category ->
          FilterChip(
            selected = selectedCategory.equals(category, ignoreCase = true),
            onClick = { viewModel.selectedCategory.value = category },
            label = { Text(category) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = EmeraldLight,
              selectedLabelColor = Slate900
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Product Catalog List
      if (products.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = if (rawProducts.isEmpty()) "No products in store yet" else "No matching products",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
              text = if (rawProducts.isEmpty()) "Go to Inventory tab to add your first product" else "Try another barcode or keyword",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(products, key = { it.id }) { product ->
            ProductCatalogCard(
              product = product,
              onAddToCart = { viewModel.addToCart(product) }
            )
          }
        }
      }
    }

    // Floating Cart Summary Bar
    if (cart.isNotEmpty()) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .padding(16.dp)
          .testTag("floating_cart_bar"),
        shape = RoundedCornerShape(20.dp),
        color = Slate800,
        shadowElevation = 8.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showCartSheet = true }
            .padding(horizontal = 20.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .background(EmeraldLight, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.ShoppingCart,
              contentDescription = "Cart",
              tint = Slate900,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "$totalCartItems item${if (totalCartItems > 1) "s" else ""}",
              style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White.copy(alpha = 0.7f)
              )
            )
            Text(
              text = String.format(Locale.getDefault(), "$%.2f", totalCartAmount),
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
          }

          Button(
            onClick = { showCheckoutDialog = true },
            colors = ButtonDefaults.buttonColors(
              containerColor = EmeraldLight,
              contentColor = Slate900
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("checkout_button")
          ) {
            Text("Checkout", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }

  // Cart Bottom Sheet
  if (showCartSheet) {
    ModalBottomSheet(
      onDismissRequest = { showCartSheet = false },
      sheetState = rememberModalBottomSheetState()
    ) {
      CartDetailsSheet(
        cart = cart,
        totalAmount = totalCartAmount,
        onUpdateQuantity = { id, qty -> viewModel.updateCartQuantity(id, qty) },
        onRemove = { id -> viewModel.removeFromCart(id) },
        onClear = {
          viewModel.clearCart()
          showCartSheet = false
        },
        onProceedCheckout = {
          showCartSheet = false
          showCheckoutDialog = true
        }
      )
    }
  }

  // Checkout Dialog
  if (showCheckoutDialog) {
    CheckoutDialog(
      cart = cart,
      subtotal = totalCartAmount,
      onDismiss = { showCheckoutDialog = false },
      onConfirm = { name, phone, discount, tax, paid, method ->
        viewModel.checkout(
          customerName = name,
          customerPhone = phone,
          discount = discount,
          tax = tax,
          paidAmount = paid,
          paymentMethod = method,
          onSuccess = { sale ->
            showCheckoutDialog = false
            latestSaleReceipt = sale
          },
          onError = { err ->
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
          }
        )
      }
    )
  }

  // Receipt Modal
  latestSaleReceipt?.let { sale ->
    ReceiptDialog(
      sale = sale,
      onDismiss = { latestSaleReceipt = null }
    )
  }
}

@Composable
fun ProductCatalogCard(
  product: Product,
  onAddToCart: () -> Unit
) {
  val isLowStock = product.stockQuantity <= product.minStockAlert
  val isOutOfStock = product.stockQuantity <= 0

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("product_card_${product.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = product.name,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 2.dp)
        ) {
          Text(
            text = "Barcode: ${product.barcode}",
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "• ${product.category}",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = String.format(Locale.getDefault(), "$%.2f", product.sellingPrice),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = EmeraldLight
            )
          )

          Spacer(modifier = Modifier.width(12.dp))

          // Stock Badge
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = when {
              isOutOfStock -> CoralError.copy(alpha = 0.15f)
              isLowStock -> AmberWarning.copy(alpha = 0.15f)
              else -> EmeraldLight.copy(alpha = 0.15f)
            }
          ) {
            Text(
              text = when {
                isOutOfStock -> "Out of Stock"
                isLowStock -> "${product.stockQuantity} ${product.unit} (Low)"
                else -> "${product.stockQuantity} ${product.unit}"
              },
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = when {
                  isOutOfStock -> CoralError
                  isLowStock -> AmberWarning
                  else -> EmeraldLight
                }
              )
            )
          }
        }
      }

      Button(
        onClick = onAddToCart,
        enabled = !isOutOfStock,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = EmeraldLight,
          contentColor = Slate900
        ),
        modifier = Modifier.testTag("add_to_cart_btn_${product.id}")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add to cart", modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add")
      }
    }
  }
}

@Composable
fun CartDetailsSheet(
  cart: List<CartItem>,
  totalAmount: Double,
  onUpdateQuantity: (String, Int) -> Unit,
  onRemove: (String) -> Unit,
  onClear: () -> Unit,
  onProceedCheckout: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Current Sale Items",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
      )

      TextButton(onClick = onClear) {
        Text("Clear All", color = CoralError)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f, fill = false),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(cart, key = { it.product.id }) { item ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, fontWeight = FontWeight.SemiBold)
            Text(
              text = String.format(Locale.getDefault(), "$%.2f each", item.product.sellingPrice),
              style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { onUpdateQuantity(item.product.id, item.quantity - 1) },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }

            Text(
              text = item.quantity.toString(),
              modifier = Modifier.padding(horizontal = 8.dp),
              fontWeight = FontWeight.Bold
            )

            IconButton(
              onClick = { onUpdateQuantity(item.product.id, item.quantity + 1) },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Increase")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
              text = String.format(Locale.getDefault(), "$%.2f", item.lineTotal),
              fontWeight = FontWeight.Bold,
              color = EmeraldLight
            )

            IconButton(
              onClick = { onRemove(item.product.id) },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Gray)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Total Payable", style = MaterialTheme.typography.titleMedium)
      Text(
        text = String.format(Locale.getDefault(), "$%.2f", totalAmount),
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.Bold,
          color = EmeraldLight
        )
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
      onClick = onProceedCheckout,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = EmeraldLight,
        contentColor = Slate900
      )
    ) {
      Text("Proceed to Checkout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }

    Spacer(modifier = Modifier.height(20.dp))
  }
}

@Composable
fun CheckoutDialog(
  cart: List<CartItem>,
  subtotal: Double,
  onDismiss: () -> Unit,
  onConfirm: (customerName: String, customerPhone: String, discount: Double, tax: Double, paid: Double, method: String) -> Unit
) {
  var customerName by remember { mutableStateOf("") }
  var customerPhone by remember { mutableStateOf("") }
  var discountStr by remember { mutableStateOf("0.0") }
  var taxStr by remember { mutableStateOf("0.0") }
  var paidAmountStr by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", subtotal)) }
  var selectedPaymentMethod by remember { mutableStateOf("CASH") }

  val discount = discountStr.toDoubleOrNull() ?: 0.0
  val tax = taxStr.toDoubleOrNull() ?: 0.0
  val total = (subtotal - discount + tax).coerceAtLeast(0.0)
  val paid = paidAmountStr.toDoubleOrNull() ?: total
  val change = (paid - total).coerceAtLeast(0.0)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("POS Checkout & Payment", fontWeight = FontWeight.Bold)
    },
    text = {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
          OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Customer Name (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
        item {
          OutlinedTextField(
            value = customerPhone,
            onValueChange = { customerPhone = it },
            label = { Text("Customer Phone (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = discountStr,
              onValueChange = { discountStr = it },
              label = { Text("Discount ($)") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
            OutlinedTextField(
              value = taxStr,
              onValueChange = { taxStr = it },
              label = { Text("Tax ($)") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
          }
        }
        item {
          Text("Payment Method", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            val methods = listOf("CASH", "CARD", "MOBILE_PAYMENT", "STORE_CREDIT")
            for (method in methods) {
              FilterChip(
                selected = selectedPaymentMethod == method,
                onClick = { selectedPaymentMethod = method },
                label = { Text(method.replace("_", " "), fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = EmeraldLight,
                  selectedLabelColor = Slate900
                )
              )
            }
          }
        }
        item {
          OutlinedTextField(
            value = paidAmountStr,
            onValueChange = { paidAmountStr = it },
            label = { Text("Tendered / Paid Amount ($)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate800,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Subtotal", color = Color.White.copy(alpha = 0.7f))
                Text(String.format(Locale.getDefault(), "$%.2f", subtotal), color = Color.White)
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Net Payable", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                  String.format(Locale.getDefault(), "$%.2f", total),
                  color = EmeraldLight,
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp
                )
              }
              if (selectedPaymentMethod == "CASH") {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Change Due", color = AmberWarning)
                  Text(
                    String.format(Locale.getDefault(), "$%.2f", change),
                    color = AmberWarning,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(customerName, customerPhone, discount, tax, paid, selectedPaymentMethod)
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = EmeraldLight,
          contentColor = Slate900
        )
      ) {
        Text("Complete Sale", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun ReceiptDialog(
  sale: Sale,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val items = remember(sale.itemsJson) {
    SaleItemRecord.listFromJson(sale.itemsJson)
  }
  val dateFormatted = remember(sale.timestamp) {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(sale.timestamp))
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldLight)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sale Receipt", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Slate900, RoundedCornerShape(12.dp))
          .padding(16.dp)
      ) {
        Text(
          text = "MY SHOP RETAIL POS",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
          ),
          modifier = Modifier.fillMaxWidth()
        )
        Text(
          text = dateFormatted,
          style = MaterialTheme.typography.bodySmall.copy(
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
          ),
          modifier = Modifier.fillMaxWidth()
        )
        Text(
          text = "ID: ${sale.transactionId}",
          style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            color = EmeraldLight,
            textAlign = TextAlign.Center,
            fontSize = 10.sp
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(10.dp))

        // Items Table
        for (item in items) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "${item.quantity}x ${item.productName}",
              color = Color.White,
              fontSize = 12.sp,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = String.format(Locale.getDefault(), "$%.2f", item.lineTotal),
              color = Color.White,
              fontSize = 12.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Subtotal", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
          Text(String.format(Locale.getDefault(), "$%.2f", sale.subtotal), color = Color.White, fontSize = 12.sp)
        }
        if (sale.discount > 0) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Discount", color = AmberWarning, fontSize = 12.sp)
            Text(String.format(Locale.getDefault(), "-$%.2f", sale.discount), color = AmberWarning, fontSize = 12.sp)
          }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Total Amount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text(
            String.format(Locale.getDefault(), "$%.2f", sale.totalAmount),
            color = EmeraldLight,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Payment Method", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
          Text(sale.paymentMethod, color = Color.White, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          // Copy receipt summary to clipboard
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          val text = "Receipt: ${sale.transactionId}\nTotal: $${sale.totalAmount}\nDate: $dateFormatted"
          clipboard.setPrimaryClip(ClipData.newPlainText("Receipt", text))
          Toast.makeText(context, "Receipt copied to clipboard", Toast.LENGTH_SHORT).show()
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = EmeraldLight,
          contentColor = Slate900
        )
      ) {
        Text("Copy & Done")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}
