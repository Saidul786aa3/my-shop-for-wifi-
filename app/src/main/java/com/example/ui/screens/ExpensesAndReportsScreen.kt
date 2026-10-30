package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Expense
import com.example.data.model.SaleItemRecord
import com.example.ui.ShopViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.Slate800
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpensesAndReportsScreen(
  viewModel: ShopViewModel
) {
  val context = LocalContext.current
  val sales by viewModel.sales.collectAsStateWithLifecycle()
  val expenses by viewModel.expenses.collectAsStateWithLifecycle()

  var showAddExpenseDialog by remember { mutableStateOf(false) }
  var exportedCsvContent by remember { mutableStateOf<Pair<String, String>?>(null) } // title to content

  val totalRevenue = remember(sales) { sales.sumOf { it.totalAmount } }
  val totalCost = remember(sales) {
    sales.sumOf { s ->
      SaleItemRecord.listFromJson(s.itemsJson).sumOf { it.costPrice * it.quantity }
    }
  }
  val grossProfit = totalRevenue - totalCost
  val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }
  val netProfit = grossProfit - totalExpenses

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Financial Metrics Dashboard Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Shop Financial Performance",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldLight)
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricBlock(label = "Gross Revenue", value = totalRevenue, valueColor = Color.White)
            MetricBlock(label = "Cost of Goods", value = totalCost, valueColor = Color.White.copy(alpha = 0.8f))
            MetricBlock(
              label = "Gross Profit",
              value = grossProfit,
              valueColor = if (grossProfit >= 0) EmeraldLight else CoralError
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricBlock(label = "Total Expenses", value = totalExpenses, valueColor = CoralError)
            MetricBlock(
              label = "Net Shop Profit",
              value = netProfit,
              valueColor = if (netProfit >= 0) EmeraldLight else CoralError,
              isLarge = true
            )
          }
        }
      }
    }

    // CSV Export Actions
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "Data Export (CSV)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                viewModel.exportSalesCsv { csv ->
                  exportedCsvContent = "Sales Report CSV" to csv
                }
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Sales CSV", fontSize = 11.sp)
            }

            OutlinedButton(
              onClick = {
                viewModel.exportProductsCsv { csv ->
                  exportedCsvContent = "Inventory CSV" to csv
                }
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Products CSV", fontSize = 11.sp)
            }

            OutlinedButton(
              onClick = {
                viewModel.exportExpensesCsv { csv ->
                  exportedCsvContent = "Expenses CSV" to csv
                }
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Expenses CSV", fontSize = 11.sp)
            }
          }
        }
      }
    }

    // Expenses Header & Add Button
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Operating Expenses",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Button(
          onClick = { showAddExpenseDialog = true },
          colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldLight,
            contentColor = Slate800
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    if (expenses.isEmpty()) {
      item {
        EmptyState("No expenses recorded", "Log store rent, electricity, wages, or supplies here")
      }
    } else {
      items(expenses, key = { it.id }) { expense ->
        val dateStr = remember(expense.timestamp) {
          SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(expense.timestamp))
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = CoralError.copy(alpha = 0.15f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = CoralError)
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(expense.title, fontWeight = FontWeight.Bold)
              Text(
                text = "${expense.category} • $dateStr",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
              )
              if (expense.note.isNotBlank()) {
                Text(
                  text = expense.note,
                  style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
                )
              }
            }

            Text(
              text = String.format(Locale.getDefault(), "-$%.2f", expense.amount),
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = CoralError
              )
            )
          }
        }
      }
    }
  }

  // Add Expense Dialog
  if (showAddExpenseDialog) {
    AddExpenseDialog(
      onDismiss = { showAddExpenseDialog = false },
      onSave = { expense ->
        viewModel.recordExpense(expense) {
          showAddExpenseDialog = false
        }
      }
    )
  }

  // Exported CSV Modal
  exportedCsvContent?.let { (title, content) ->
    AlertDialog(
      onDismissRequest = { exportedCsvContent = null },
      title = { Text(title, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text(
            text = "Export generated successfully. You can copy it to clipboard:",
            fontSize = 13.sp,
            color = Color.Gray
          )
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Slate800,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = content.take(300) + if (content.length > 300) "\n..." else "",
              color = EmeraldLight,
              fontSize = 10.sp,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(title, content))
            Toast.makeText(context, "$title copied to clipboard", Toast.LENGTH_SHORT).show()
            exportedCsvContent = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = Slate800)
        ) {
          Text("Copy CSV")
        }
      },
      dismissButton = {
        TextButton(onClick = { exportedCsvContent = null }) { Text("Close") }
      }
    )
  }
}

@Composable
fun MetricBlock(label: String, value: Double, valueColor: Color, isLarge: Boolean = false) {
  Column {
    Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
    Text(
      text = String.format(Locale.getDefault(), "$%.2f", value),
      style = if (isLarge) MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        color = valueColor,
        fontSize = 20.sp
      ) else MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        color = valueColor
      )
    )
  }
}

@Composable
fun AddExpenseDialog(
  onDismiss: () -> Unit,
  onSave: (Expense) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Rent") }
  var amountStr by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var paymentMethod by remember { mutableStateOf("CASH") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Log Shop Expense", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (e.g. Rent, Wages, Utility)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Amount ($) *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Notes / Receipt Reference") }, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountStr.toDoubleOrNull() ?: 0.0
          if (title.isBlank() || amt <= 0) return@Button
          onSave(
            Expense(
              title = title.trim(),
              category = category.trim().ifBlank { "General" },
              amount = amt,
              paymentMethod = paymentMethod,
              note = note.trim()
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = Slate800)
      ) {
        Text("Save Expense", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
