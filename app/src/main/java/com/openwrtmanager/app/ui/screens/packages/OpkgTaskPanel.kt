package com.openwrtmanager.app.ui.screens.packages
import com.openwrtmanager.app.ui.common.AppStrings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.openwrtmanager.app.data.model.OpkgTaskState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpkgTaskPanel(
    taskState: OpkgTaskState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState()

    val log: List<String> = when (taskState) {
        is OpkgTaskState.Running -> taskState.log
        is OpkgTaskState.Success -> taskState.log
        is OpkgTaskState.Failed -> taskState.log
        OpkgTaskState.Idle -> emptyList()
    }

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1)
    }

    val isRunning = taskState is OpkgTaskState.Running

    ModalBottomSheet(
        onDismissRequest = { if (!isRunning) onDismiss() },
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                when (taskState) {
                    is OpkgTaskState.Running -> {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text(AppStrings.text("Running…"), style = MaterialTheme.typography.titleMedium)
                    }
                    is OpkgTaskState.Success -> {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                        Text(
                            " ${taskState.summary}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    is OpkgTaskState.Failed -> {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            AppStrings.text(" Operation failed"),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    OpkgTaskState.Idle -> Unit
                }
            }

            if (taskState is OpkgTaskState.Failed) {
                Text(
                    taskState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .padding(top = 12.dp)
                    .background(Color(0xFF0D1117), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(log) { line ->
                    Text(
                        text = line,
                        color = Color(0xFFC9D1D9),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (!isRunning) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(AppStrings.text("Close"))
                }
            }
        }
    }
}
