package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ShopTopBar
import com.example.ui.screens.ExpensesAndReportsScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PartiesScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.SyncBackupScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

enum class ShopNavTab(val title: String, val icon: ImageVector) {
  POS("POS", Icons.Default.PointOfSale),
  INVENTORY("Inventory", Icons.Default.Inventory),
  TRANSACTIONS("Records", Icons.Default.ReceiptLong),
  PARTIES("Parties", Icons.Default.Group),
  REPORTS("Reports", Icons.Default.TrendingUp),
  SYNC("Sync", Icons.Default.CloudDone)
}

@Composable
fun MainShopScreen(
  onSignOut: () -> Unit,
  viewModel: ShopViewModel = viewModel()
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = ShopNavTab.entries
  val syncState by viewModel.syncState.collectAsStateWithLifecycle()
  val currentUser = Firebase.auth.currentUser

  BackHandler(enabled = selectedTab != 0) {
    selectedTab = 0
  }

  Scaffold(
    topBar = {
      ShopTopBar(
        shopName = "My Shop",
        userEmail = currentUser?.email ?: "Shop Owner",
        syncState = syncState,
        onSyncClicked = { viewModel.triggerSync() },
        onSignOutClicked = onSignOut
      )
    },
    bottomBar = {
      NavigationBar(
        modifier = Modifier
          .navigationBarsPadding()
          .testTag("main_navigation_bar"),
        containerColor = Slate900,
        tonalElevation = 8.dp
      ) {
        tabs.forEachIndexed { index, tab ->
          val isSelected = selectedTab == index
          NavigationBarItem(
            selected = isSelected,
            onClick = { selectedTab = index },
            icon = {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = if (isSelected) Slate900 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
              )
            },
            label = {
              Text(
                text = tab.title,
                fontSize = 10.sp,
                color = if (isSelected) EmeraldLight else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
              )
            },
            colors = NavigationBarItemDefaults.colors(
              indicatorColor = EmeraldLight
            ),
            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (selectedTab) {
        0 -> PosScreen(viewModel = viewModel)
        1 -> InventoryScreen(viewModel = viewModel)
        2 -> TransactionsScreen(viewModel = viewModel)
        3 -> PartiesScreen(viewModel = viewModel)
        4 -> ExpensesAndReportsScreen(viewModel = viewModel)
        5 -> SyncBackupScreen(viewModel = viewModel)
      }
    }
  }
}
