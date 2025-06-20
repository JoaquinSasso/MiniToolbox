package com.example.minitoolbox.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ToolsList(
    tools: List<String>,
    modifier: Modifier = Modifier,
    onToolSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tools) { tool ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToolSelected(tool) },
            ) {
                Box(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = tool)
                }
            }
        }
    }
}

