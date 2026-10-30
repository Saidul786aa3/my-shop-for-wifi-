package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SyncState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun ShopTopBar(
  shopName: String,
  userEmail: String,
  syncState: SyncState,
  onSyncClicked: () -> Unit,
  onSignOutClicked: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding(),
    color = Slate900,
    shadowElevation = 4.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = shopName,
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 20.sp
          )
        )
        Text(
          text = userEmail.ifBlank { "Owner Account" },
          style = MaterialTheme.typography.bodySmall.copy(
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 11.sp
          )
        )
      }

      // Sync Status Badge
      SyncBadge(
        syncState = syncState,
        onClick = onSyncClicked
      )

      Spacer(modifier = Modifier.width(8.dp))

      // Sign out
      IconButton(
        onClick = onSignOutClicked,
        modifier = Modifier.testTag("sign_out_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = "Sign Out",
          tint = Color.White.copy(alpha = 0.75f)
        )
      }
    }
  }
}

@Composable
fun SyncBadge(
  syncState: SyncState,
  onClick: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  val (bgColor, iconColor, text) = when (syncState) {
    is SyncState.Synced -> Triple(
      EmeraldLight.copy(alpha = 0.15f),
      EmeraldLight,
      "Cloud Synced"
    )
    is SyncState.Syncing -> Triple(
      Color(0xFF3B82F6).copy(alpha = 0.2f),
      Color(0xFF60A5FA),
      "Syncing..."
    )
    is SyncState.Offline -> Triple(
      AmberWarning.copy(alpha = 0.2f),
      AmberWarning,
      if (syncState.pendingCount > 0) "Offline (${syncState.pendingCount})" else "Offline Mode"
    )
    is SyncState.Error -> Triple(
      Color(0xFFEF4444).copy(alpha = 0.2f),
      Color(0xFFF87171),
      "Sync Retry"
    )
  }

  Surface(
    modifier = Modifier
      .testTag("sync_status_badge")
      .clickable { onClick() },
    shape = RoundedCornerShape(20.dp),
    color = bgColor
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      when (syncState) {
        is SyncState.Synced -> {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(EmeraldLight, CircleShape)
          )
        }
        is SyncState.Syncing -> {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Syncing",
            tint = iconColor,
            modifier = Modifier
              .size(14.dp)
              .rotate(rotation)
          )
        }
        is SyncState.Offline -> {
          Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = "Offline",
            tint = iconColor,
            modifier = Modifier.size(14.dp)
          )
        }
        is SyncState.Error -> {
          Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Error",
            tint = iconColor,
            modifier = Modifier.size(14.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.SemiBold,
          color = iconColor,
          fontSize = 11.sp
        )
      )
    }
  }
}
