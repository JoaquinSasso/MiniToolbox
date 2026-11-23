package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import com.joasasso.minitoolbox.utils.vibrate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdivinaBanderaScreen(
    onBack: () -> Unit,
    viewModel: GuessFlagViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val espanol = LocalLocale.current.platformLocale.language == "es"
    var showInfo by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                GuessFlagEvent.Correct -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                GuessFlagEvent.Wrong -> vibrate(context, duration = 400, amplitude = 255)
            }
        }
    }

    val progress = state.tenthsLeft / GuessFlagViewModel.ROUND_TENTHS.toFloat()
    val animatedBgColor by animateColorAsState(
        targetValue = if (state.showError) Color(0xFFC53737) else MaterialTheme.colorScheme.background,
        label = "bgColor",
    )

    Scaffold(
        topBar = {
            TopBarReusable(
                title = stringResource(R.string.tool_guess_flag),
                onBack = onBack,
                onShowInfo = { showInfo = true }
            )
        },
        bottomBar = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.capital_remaining_time, (state.tenthsLeft / 10).toString()),
                    style = MaterialTheme.typography.titleMedium
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(animatedBgColor)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            state.question?.let { question ->
                Text(stringResource(R.string.flag_question), style = MaterialTheme.typography.titleLarge)
                Text(question.flag, style = MaterialTheme.typography.displayLarge)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(state.options, key = { it.isoCode }) { option ->
                        val bgColor = when {
                            state.selectedIso == null -> MaterialTheme.colorScheme.primaryContainer
                            option.isoCode == question.isoCode -> Color(0xFF4CAF50)
                            else -> Color(0xFFF44336)
                        }

                        Button(
                            onClick = { viewModel.answer(option.isoCode) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = bgColor,
                                disabledContainerColor = bgColor,
                                contentColor = Color.Black,
                                disabledContentColor = Color.Black
                            ),
                            enabled = !state.revealing
                        ) {
                            Text(if (espanol) option.nameEs else option.nameEn)
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.score, state.score), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.record, state.record), style = MaterialTheme.typography.bodyLarge)
            }

            state.lastResult?.let { result ->
                val text = when (result) {
                    FlagRoundResult.NEW_RECORD -> stringResource(R.string.result_new_record)
                    FlagRoundResult.WRONG -> stringResource(R.string.result_wrong)
                    FlagRoundResult.TIMEOUT -> stringResource(R.string.result_timeout)
                }
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.flag_info_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.flag_info_line1))
                    Text(stringResource(R.string.flag_info_line2))
                    Text(stringResource(R.string.flag_info_line3))
                    Text(stringResource(R.string.flag_info_line4))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
