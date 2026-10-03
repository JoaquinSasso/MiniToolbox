package com.joasasso.minitoolbox.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.flujoOnboardingVisto
import com.joasasso.minitoolbox.data.marcarOnboardingVisto
import kotlinx.coroutines.launch

/**
 * Banner reactivo conectado a DataStore que se muestra únicamente la primera vez
 * que el usuario accede a una herramienta ("empujón"), hasta que decide descartarlo.
 */
@Composable
fun ToolOnboardingCard(
    toolKey: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Lightbulb,
    actionLabel: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dismissedState by context.flujoOnboardingVisto(toolKey).collectAsStateWithLifecycle(initialValue = null)
    var localDismissed by rememberSaveable { mutableStateOf(false) }

    val isVisible = (dismissedState == false) && !localDismissed

    ToolOnboardingBanner(
        visible = isVisible,
        message = message,
        onDismiss = {
            localDismissed = true
            coroutineScope.launch {
                context.marcarOnboardingVisto(toolKey)
            }
        },
        modifier = modifier,
        icon = icon,
        actionLabel = actionLabel
    )
}

/**
 * Componente visual desacoplado y animado con AnimatedVisibility para mostrar pistas o
 * sugerencias iniciales en herramientas de sensores o utilidades.
 */
@Composable
fun ToolOnboardingBanner(
    visible: Boolean,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Lightbulb,
    actionLabel: String? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                if (actionLabel != null) {
                    TextButton(onClick = onDismiss) {
                        Text(actionLabel)
                    }
                } else {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.onboarding_dismiss),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
