package com.example.blelearning.presentation.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blelearning.presentation.common.BleScaffold

@Composable
fun NotificationScreen(onBack: () -> Unit, vm: NotificationViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()

    BleScaffold(title = "Live notifications", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${items.size} received", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = vm::clear) { Text("Clear") }
            }
            if (items.isEmpty()) {
                Text(
                    "Nothing yet. Enable notifications on a characteristic that supports notify.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LazyColumn {
                items(items, key = { it.id }) { n ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text("${n.time}  ·  ${n.source}", style = MaterialTheme.typography.labelMedium)
                        Text(n.text, style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
