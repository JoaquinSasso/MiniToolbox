package com.joasasso.minitoolbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.utils.pro.CreditAccessManager
import kotlinx.coroutines.delay

@Composable
fun ProPassBadge(
    modifier: Modifier = Modifier,
    gold: Color = Color(0xFFFFD700),
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var hasPass by remember { mutableStateOf(CreditAccessManager.hasActivePass(context)) }
    var remainingMs by remember { mutableLongStateOf(CreditAccessManager.passRemainingMs(context)) }

    LaunchedEffect(hasPass) {
        if (!hasPass) return@LaunchedEffect
        while (true) {
            delay(1000)
            remainingMs = CreditAccessManager.passRemainingMs(context)
            hasPass = remainingMs > 0
        }
    }

    if (hasPass) {
        Row(
            modifier = modifier
                .background(gold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .clickable { onClick?.invoke() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.pro_badge_active_time,
                    CreditAccessManager.formatDurationMmSs(remainingMs)
                ),
            )
        }
    }
}
