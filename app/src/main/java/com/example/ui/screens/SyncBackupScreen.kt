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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.SyncState
import com.example.ui.ShopViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun SyncBackupScreen(
  viewModel: ShopViewModel
) {
  val context = LocalContext.current
  val syncState by viewModel.syncState.collectAsStateWithLifecycle()
  val pendingCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()

  var exportedJsonModal by remember { mutableStateOf<String?>(null) }
  var showRestoreDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Sync Status Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("sync_status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Cloud Database Sync",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              )
              Text(
                text = "Google Cloud Firestore Enterprise",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
              )
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = when (syncState) {
                is SyncState.Synced -> EmeraldLight.copy(alpha = 0.2f)
                is SyncState.Syncing -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                is SyncState.Offline -> AmberWarning.copy(alpha = 0.2f)
                is SyncState.Error -> CoralError.copy(alpha = 0.2f)
              }
            ) {
              Text(
                text = when (syncState) {
                  is SyncState.Synced -> "Synced"
                  is SyncState.Syncing -> "Syncing..."
                  is SyncState.Offline -> "Offline"
                  is SyncState.Error -> "Sync Error"
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = when (syncState) {
                  is SyncState.Synced -> EmeraldLight
                  is SyncState.Syncing -> Color(0xFF60A5FA)
                  is SyncState.Offline -> AmberWarning
                  is SyncState.Error -> CoralError
                }
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = if (pendingCount == 0) {
              "All local transactions and stock records are in sync with Cloud Firestore."
            } else {
              "$pendingCount pending operation(s) queued locally. Automatic two-way sync will push changes immediately when network is available."
            },
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f))
          )

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = {
              viewModel.triggerSync()
              Toast.makeText(context, "Synchronization triggered", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("force_sync_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = EmeraldLight,
              contentColor = Slate900
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sync Now to Firestore", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Manual Full JSON Backup and Restore
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Disaster Recovery & Portability",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = "Generate a full JSON backup of all products, sales, customers, suppliers and movements, or restore from a previously saved JSON file.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
          )

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                viewModel.exportJsonBackup { json ->
                  exportedJsonModal = json
                }
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = IndigoAccent,
                contentColor = Color.White
              )
            ) {
              Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Export JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = { showRestoreDialog = true },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Restore JSON", fontSize = 12.sp)
            }
          }
        }
      }
    }

    // Data Safety & Integrity Architecture
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldLight)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Data Integrity Guarantees", fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(10.dp))

          SafetyPill(
            title = "Zero Duplicate Transactions",
            description = "Each sale and movement is signed with a deterministic client UUID (e.g. txn_...); repeated offline sync attempts can never double-charge or duplicate records."
          )
          Spacer(modifier = Modifier.height(8.dp))
          SafetyPill(
            title = "Permanent Multi-Platform Storage",
            description = "Business data is permanently stored in Cloud Firestore. Cashiers can access identical records on Android phones, tablets, or desktop browsers."
          )
          Spacer(modifier = Modifier.height(8.dp))
          SafetyPill(
            title = "Conflict Resolution: Last-Write-Wins",
            description = "Room and Firestore synchronize using timestamp vectors and audit ledgers, ensuring concurrent edits from multiple devices merge reliably."
          )
        }
      }
    }
  }

  // Export JSON Modal
  exportedJsonModal?.let { json ->
    AlertDialog(
      onDismissRequest = { exportedJsonModal = null },
      title = { Text("Full Store Backup (JSON)", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Complete database snapshot exported. Copy to safe storage:", fontSize = 12.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Slate900,
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp)
          ) {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
              item {
                Text(
                  text = json,
                  color = EmeraldLight,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("MyShopBackup", json))
            Toast.makeText(context, "Backup JSON copied to clipboard", Toast.LENGTH_SHORT).show()
            exportedJsonModal = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = Slate900)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Copy to Clipboard")
        }
      },
      dismissButton = {
        TextButton(onClick = { exportedJsonModal = null }) { Text("Close") }
      }
    )
  }

  // Restore JSON Dialog
  if (showRestoreDialog) {
    var jsonInput by remember { mutableStateOf("") }
    var isRestoring by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { if (!isRestoring) showRestoreDialog = false },
      title = { Text("Restore Store Backup", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Paste a previously exported My Shop JSON backup here to restore all products, customers and records:",
            fontSize = 12.sp,
            color = Color.Gray
          )
          OutlinedTextField(
            value = jsonInput,
            onValueChange = { jsonInput = it },
            placeholder = { Text("{\"version\": 1, \"products\": [...]}") },
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp),
            maxLines = 10
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (jsonInput.isBlank()) return@Button
            isRestoring = true
            viewModel.restoreJsonBackup(jsonInput) { result ->
              isRestoring = false
              showRestoreDialog = false
              if (result.isSuccess) {
                Toast.makeText(context, "Restored ${result.getOrNull()} items successfully", Toast.LENGTH_LONG).show()
              } else {
                Toast.makeText(context, "Restore failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
              }
            }
          },
          enabled = !isRestoring && jsonInput.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight, contentColor = Slate900)
        ) {
          Text(if (isRestoring) "Restoring..." else "Restore & Sync")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
      }
    )
  }
}

@Composable
fun SafetyPill(title: String, description: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Text(
      text = "• $title",
      fontWeight = FontWeight.SemiBold,
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = description,
      style = MaterialTheme.typography.bodySmall.copy(
        color = Color.Gray,
        fontSize = 11.sp
      ),
      modifier = Modifier.padding(start = 12.dp, top = 2.dp)
    )
  }
}
