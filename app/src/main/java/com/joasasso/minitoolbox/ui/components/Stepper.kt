package com.joasasso.minitoolbox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R

@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val cdMinus = stringResource(R.string.stepper_decrease)
    val cdPlus = stringResource(R.string.stepper_increase)
    val contentDesc = stringResource(R.string.stepper_content_desc)

    Row(
        modifier = modifier.semantics { contentDescription = contentDesc },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            enabled = enabled && value > range.first,
            onClick = { onValueChange((value - 1).coerceAtLeast(range.first)) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Filled.Remove, contentDescription = cdMinus)
        }

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium
        )

        IconButton(
            enabled = enabled && value < range.last,
            onClick = { onValueChange((value + 1).coerceAtMost(range.last)) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = cdPlus)
        }
    }
}

