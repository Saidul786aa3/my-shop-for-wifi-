package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.Payment
import com.example.data.model.Supplier
import com.example.ui.ShopViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartiesScreen(
  viewModel: ShopViewModel
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Customers (Receivables)", "Suppliers (Payables)")

  val customers by viewModel.customers.collectAsStateWithLifecycle()
  val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()

  var showAddCustomerDialog by remember { mutableStateOf(false) }
  var showAddSupplierDialog by remember { mutableStateOf(false) }
  var settlingCustomer by remember { mutableStateOf<Customer?>(null) }
  var settlingSupplier by remember { mutableStateOf<Supplier?>(null) }

  val totalCustomerDue = remember(customers) { customers.sumOf { it.outstandingBalance } }
  val totalSupplierPayable = remember(suppliers) { suppliers.sumOf { it.outstandingBalance } }

  Box(modifier = Modifier.fillMaxSize()) {
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

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (selectedTab == 0) {
          // Customers
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("Total Customer Receivables", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                  Text(
                    text = String.format(Locale.getDefault(), "$%.2f", totalCustomerDue),
                    style = MaterialTheme.typography.titleLarge.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (totalCustomerDue > 0) AmberWarning else EmeraldLight
                    )
                  )
                }
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = IndigoAccent.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = "${customers.size} Clients",
                    color = IndigoAccent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                  )
                }
              }
            }
          }

          if (customers.isEmpty()) {
            item {
              EmptyState("No customers registered", "Tap + to add store customers and track credit/dues")
            }
          } else {
            items(customers, key = { it.id }) { customer ->
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
                    color = EmeraldLight.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldLight)
                    }
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name, fontWeight = FontWeight.Bold)
                    Text(
                      text = customer.phone.ifBlank { "No phone" },
                      style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                    Text(
                      text = "Due: $${String.format(Locale.getDefault(), "%.2f", customer.outstandingBalance)}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = if (customer.outstandingBalance > 0) AmberWarning else Color.Gray,
                        fontWeight = FontWeight.SemiBold
                      )
                    )
                  }

                  if (customer.outstandingBalance > 0) {
                    Button(
                      onClick = { settlingCustomer = customer },
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldLight,
                        contentColor = Slate900
                      ),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                      Text("Collect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        } else {
          // Suppliers
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("Total Store Payables to Vendors", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                  Text(
                    text = String.format(Locale.getDefault(), "$%.2f", totalSupplierPayable),
                    style = MaterialTheme.typography.titleLarge.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (totalSupplierPayable > 0) CoralError else EmeraldLight
                    )
                  )
                }
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = IndigoAccent.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = "${suppliers.size} Vendors",
                    color = IndigoAccent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                  )
                }
              }
            }
          }

          if (suppliers.isEmpty()) {
            item {
              EmptyState("No suppliers added", "Tap + to add wholesalers and vendors")
            }
          } else {
            items(suppliers, key = { it.id }) { supplier ->
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
                      Icon(Icons.Default.Business, contentDescription = null, tint = IndigoAccent)
                    }
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(supplier.name, fontWeight = FontWeight.Bold)
                    Text(
                      text = "${supplier.contactPerson.ifBlank { "Contact" }} • ${supplier.phone}",
                      style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                    Text(
                      text = "Payable: $${String.format(Locale.getDefault(), "%.2f", supplier.outstandingBalance)}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = if (supplier.outstandingBalance > 0) CoralError else Color.Gray,
                        fontWeight = FontWeight.SemiBold
                      )
                    )
                  }

                  if (supplier.outstandingBalance > 0) {
                    Button(
                      onClick = { settlingSupplier = supplier },
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoAccent,
                        contentColor = Color.White
                      ),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                      Text("Pay Due", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Add Floating Button
    FloatingActionButton(
      onClick = {
        if (selectedTab == 0) showAddCustomerDialog = true else showAddSupplierDialog = true
      },
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(20.dp)
        .testTag("add_party_fab"),
      containerColor = EmeraldLight,
      contentColor = Slate900
    ) {
      Icon(Icons.Default.Add, contentDescription = "Add Party")
    }
  }

  // Add Customer Dialog
  if (showAddCustomerDialog) {
    AddCustomerDialog(
      onDismiss = { showAddCustomerDialog = false },
      onSave = { cust ->
        viewModel.saveCustomer(cust) {
          showAddCustomerDialog = false
        }
      }
    )
  }

  // Add Supplier Dialog
  if (showAddSupplierDialog) {
    AddSupplierDialog(
      onDismiss = { showAddSupplierDialog = false },
      onSave = { supp ->
        viewModel.saveSupplier(supp) {
          showAddSupplierDialog = false
        }
      }
    )
  }

  // Settle Customer Due Dialog
  settlingCustomer?.let { customer ->
    PaymentSettlementDialog(
      partyName = customer.name,
      currentBalance = customer.outstandingBalance,
      isReceivable = true,
      onDismiss = { settlingCustomer = null },
      onConfirm = { amount, method, note ->
        val newBalance = (customer.outstandingBalance - amount).coerceAtLeast(0.0)
        viewModel.saveCustomer(customer.copy(outstandingBalance = newBalance))
        viewModel.recordPayment(
          Payment(
            type = "CUSTOMER_RECEIPT",
            partyId = customer.id,
            partyName = customer.name,
            amount = amount,
            paymentMethod = method,
            note = note
          )
        )
        settlingCustomer = null
      }
    )
  }

  // Settle Supplier Due Dialog
  settlingSupplier?.let { supplier ->
    PaymentSettlementDialog(
      partyName = supplier.name,
      currentBalance = supplier.outstandingBalance,
      isReceivable = false,
      onDismiss = { settlingSupplier = null },
      onConfirm = { amount, method, note ->
        val newBalance = (supplier.outstandingBalance - amount).coerceAtLeast(0.0)
        viewModel.saveSupplier(supplier.copy(outstandingBalance = newBalance))
        viewModel.recordPayment(
          Payment(
            type = "SUPPLIER_PAYOUT",
            partyId = supplier.id,
            partyName = supplier.name,
            amount = amount,
            paymentMethod = method,
            note = note
          )
        )
        settlingSupplier = null
      }
    )
  }
}

@Composable
fun AddCustomerDialog(
  onDismiss: () -> Unit,
  onSave: (Customer) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("") }
  var initialDueStr by remember { mutableStateOf("0.0") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Customer", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Delivery Address") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = initialDueStr, onValueChange = { initialDueStr = it }, label = { Text("Initial Store Due ($)") }, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank()) return@Button
          onSave(
            Customer(
              name = name.trim(),
              phone = phone.trim(),
              email = email.trim(),
              address = address.trim(),
              outstandingBalance = initialDueStr.toDoubleOrNull() ?: 0.0
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = Slate900)
      ) {
        Text("Save Customer", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun AddSupplierDialog(
  onDismiss: () -> Unit,
  onSave: (Supplier) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var contactPerson by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("") }
  var initialPayableStr by remember { mutableStateOf("0.0") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Vendor / Supplier", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Company / Vendor Name *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = contactPerson, onValueChange = { contactPerson = it }, label = { Text("Contact Person") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Warehouse Address") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = initialPayableStr, onValueChange = { initialPayableStr = it }, label = { Text("Initial Store Payable ($)") }, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank()) return@Button
          onSave(
            Supplier(
              name = name.trim(),
              contactPerson = contactPerson.trim(),
              phone = phone.trim(),
              email = email.trim(),
              address = address.trim(),
              outstandingBalance = initialPayableStr.toDoubleOrNull() ?: 0.0
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = Slate900)
      ) {
        Text("Save Supplier", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
fun PaymentSettlementDialog(
  partyName: String,
  currentBalance: Double,
  isReceivable: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (amount: Double, method: String, note: String) -> Unit
) {
  var amountStr by remember { mutableStateOf(currentBalance.toString()) }
  var paymentMethod by remember { mutableStateOf("CASH") }
  var note by remember { mutableStateOf("Settlement of balance") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (isReceivable) "Collect Payment from $partyName" else "Pay Due to $partyName", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Outstanding Balance: $${String.format(Locale.getDefault(), "%.2f", currentBalance)}")
        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it },
          label = { Text("Settlement Amount ($)") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Note / Memo") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountStr.toDoubleOrNull() ?: 0.0
          if (amt > 0) onConfirm(amt, paymentMethod, note)
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = Slate900)
      ) {
        Text("Record Payment", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
