package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos.minesweeper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joasasso.minitoolbox.R
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinesweeperScreen(
    onBack: () -> Unit
) {
    val vm: MinesViewModel = viewModel()

    val ui = vm.state.collectAsStateWithLifecycle().value ?: return
    var dialOpen by remember { mutableStateOf(false) }
    var showDifficulty by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    var showInfo by remember { mutableStateOf(false) }
    val board = ui.board
    val lost = board.status == MinesEngine.Status.Lost
    var showLostDialog by remember { mutableStateOf(true) }
    val won = ui.board.status == MinesEngine.Status.Won


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tool_minesweeper)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        dialOpen = !dialOpen
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)}) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.mines_options_cd))
                    }
                    IconButton(onClick = {
                        showInfo = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.padding(horizontal = 12.dp))
                {
                    FilledTonalButton(onClick = {
                        vm.onToggleMode()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)})
                    {
                        val mode = ui.inputMode
                        Icon(if (mode == InputMode.Reveal) Icons.Default.TouchApp else Icons.Default.Flag, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (mode == InputMode.Reveal) stringResource(R.string.mines_mode_reveal) else stringResource(R.string.mines_mode_flags))
                    }
                    Spacer(Modifier.weight(1f))
                    AssistChip(
                        onClick = { vm.onNewGame()
                                    showLostDialog = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)},
                        label = { Text(stringResource(R.string.mines_new_game)) },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(if (lost) Color(0xFFC53737) else Color.Transparent)) {

            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

                // Panel de estado
                StatusPanel(
                    flagsLeft = ui.flagsLeft,
                    elapsedMs = ui.elapsedMs
                )

                Spacer(Modifier.height(8.dp))

                // Tablero
                BoardGrid(
                    ui = ui,
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vm.onCellTap(it)
                    },
                    onLongPress = {
                        vm.onCellLongPress(it)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onChord = { vm.onChord(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Speed-dial anclado al engranaje
            SpeedDial(
                open = dialOpen,
                onDismiss = { dialOpen = false },
                actions = listOf(
                    DialAction(Icons.Default.Refresh, stringResource(R.string.mines_new_game)) {
                        vm.onNewGame()
                        showLostDialog = true},
                    DialAction(Icons.Default.Tune, stringResource(R.string.mines_difficulty)) { showDifficulty = true }
                )
            )


            // Sheet de dificultad
            if (showDifficulty) {
                DifficultySheet(
                    currentLevel = vm.getCurrentLevel(),
                    onDismiss = {   showDifficulty = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)},
                    onPick = { lvl, cfg ->
                        vm.onPickLevel(lvl, cfg)
                        showDifficulty = false
                    }
                )
            }
            if (lost && showLostDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.mines_lost_title)) },
                    text = { Text(stringResource(R.string.mines_lost_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.onNewGame()
                            showLostDialog = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)}) {
                            Text(stringResource(R.string.mines_try_again))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showLostDialog = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)})
                        {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }
            if (won) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.mines_win_title)) },
                    text  = { Text(stringResource(R.string.mines_win_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.onNewGame()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showLostDialog = true}) {
                            Text(stringResource(R.string.mines_new_game))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusPanel(flagsLeft: Int, elapsedMs: Long) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Flag, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.mines_flags_left, flagsLeft), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Timer, null)
            Spacer(Modifier.width(8.dp))
            val s = max(0, (elapsedMs / 1000).toInt())
            Text(String.format("%02d:%02d", s / 60, s % 60), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun BoardGrid(
    ui: MinesUiState,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    onChord: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val b = ui.board
    val cols = b.cols
    val rows = b.rows
    val hSpacing = 4.dp
    val vSpacing = 4.dp
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Tamaño de celda con redondeo a píxel para evitar overflow por fracciones
        val cellSize = remember(maxWidth, maxHeight, cols, rows) {
            with(density) {
                val wPx = ((maxWidth - hSpacing * (cols - 1)).toPx()) / cols
                val hPx = ((maxHeight - vSpacing * (rows - 1)).toPx()) / rows
                val sidePx = kotlin.math.floor(minOf(wPx, hPx))
                sidePx.toDp()
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(hSpacing),
            verticalArrangement = Arrangement.spacedBy(vSpacing),
            userScrollEnabled = false,
            contentPadding = PaddingValues(bottom = vSpacing) // leve margen para no cortar la última fila
        ) {
            items(b.totalCells) { index ->
                Cell(
                    index = index,
                    revealed = b.revealed[index],
                    flagged = b.flags[index],
                    number = b.numbers[index],
                    isMine = b.mineBits[index],
                    isExploded = index == b.explodedIndex,
                    onTap = onTap,
                    onLongPress = onLongPress,
                    onChord = onChord,
                    board = b,
                    modifier = Modifier.size(cellSize)
                )
            }
        }
    }
}


@Composable
private fun Cell(
    index: Int,
    revealed: Boolean,
    flagged: Boolean,
    number: Int,
    isMine: Boolean,
    isExploded: Boolean,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    onChord: (Int) -> Unit,
    board: MinesEngine.Board,
    modifier: Modifier
) {
    val shape = MaterialTheme.shapes.large
    val isDark = isSystemInDarkTheme()

    val bg = when {
        isExploded -> Color(0xFF961B1B)
        revealed && !isMine -> if (isDark) Color(0xFF444444) else Color(0xFFF8F8F8)
        else -> MaterialTheme.colorScheme.surface
    }

    val content = when {
        revealed && isMine -> "💣"
        revealed && number > 0 -> number.toString()
        flagged -> "🚩"
        else -> ""
    }

    Surface(
        modifier = modifier   // <- usar el que viene (size(cellSize))
            .clip(shape)
            .combinedClickable(
                onClick = {
                    if (revealed && number > 0) onChord(index) else onTap(index)
                },
                onLongClick = { onLongPress(index) },
                role = Role.Button,
                onClickLabel = "Descubrir",
                onLongClickLabel = "Bandera"
            ),
        color = bg,
        tonalElevation = if (revealed) 0.dp else 4.dp,
        shadowElevation = if (revealed) 0.dp else 2.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (content.isNotEmpty()) {
                val color = when {
                    isExploded -> MaterialTheme.colorScheme.onErrorContainer
                    revealed && isMine -> MaterialTheme.colorScheme.onSurface
                    revealed && number in 1..8 -> numberColor(number, isDark)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = content,
                    color = color,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}


data class DialAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@Composable
private fun SpeedDial(
    open: Boolean,
    onDismiss: () -> Unit,
    actions: List<DialAction>
) {
    AnimatedVisibility(
        visible = open,
        enter = fadeIn() + scaleIn(initialScale = 0.95f),
        exit = fadeOut() + scaleOut(targetScale = 0.95f)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 8.dp)
                    .wrapContentSize()
                    .align(Alignment.TopEnd),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions.forEach { a ->
                    ExtendedFloatingActionButton(
                        icon = { Icon(a.icon, null) },
                        text = { Text(a.label) },
                        onClick = { onDismiss(); a.onClick() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultySheet(
    currentLevel: Level,
    onDismiss: () -> Unit,
    onPick: (Level, MinesEngine.Config?) -> Unit
) {
    var level by remember { mutableStateOf(currentLevel) }

    // Estado "oficial" (se actualiza al Aplicar si es CUSTOM)
    var rows by remember { mutableIntStateOf(12) }
    var cols by remember { mutableIntStateOf(12) }
    var mines by remember { mutableIntStateOf(20) }

    val easy = MinesEngine.EASY
    val med  = MinesEngine.MEDIUM
    val hard = MinesEngine.HARD

    // --- Límites jugables propuestos ---
    val ROW_MIN = 8;  val ROW_MAX = 20
    val COL_MIN = 6;  val COL_MAX = 15

    // --- Estado crudo para CUSTOM (permite vacío) ---
    var rowsStr by remember { mutableStateOf(rows.toString()) }
    var colsStr by remember { mutableStateOf(cols.toString()) }
    var minesStr by remember { mutableStateOf("") } // arranca vacío para autosugerir
    var autoMinesEnabled by remember { mutableStateOf(true) }

    fun clamp(v: Int, min: Int, max: Int) = v.coerceIn(min, max)
    fun safeInt(s: String, def: Int) = s.toIntOrNull() ?: def

    // Números "de vista previa" (para validar y sugerir mientras se edita)
    val rowsPreview = clamp(safeInt(rowsStr.ifEmpty { "${ROW_MIN}" }, ROW_MIN), ROW_MIN, ROW_MAX)
    val colsPreview = clamp(safeInt(colsStr.ifEmpty { "${COL_MIN}" }, COL_MIN), COL_MIN, COL_MAX)
    val maxMines = (rowsPreview * colsPreview - 9).coerceAtLeast(1)
    val suggestedMines = clamp(kotlin.math.round(rowsPreview * colsPreview * 0.20f).toInt(), 1, maxMines)

    // Si cambia filas/cols y el user NO tocó minas, auto-rellenar 20%
    LaunchedEffect(rowsPreview, colsPreview, autoMinesEnabled, level) {
        if (level == Level.CUSTOM && autoMinesEnabled) {
            minesStr = suggestedMines.toString()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.mines_difficulty_title), style = MaterialTheme.typography.titleLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = level == Level.EASY,
                    onClick = { level = Level.EASY },
                    label = { Text(stringResource(R.string.mines_level_easy)) },
                    leadingIcon = if (level == Level.EASY) { { Icon(Icons.Default.Check, null) } } else null
                )
                FilterChip(
                    selected = level == Level.MEDIUM,
                    onClick = { level = Level.MEDIUM },
                    label = { Text(stringResource(R.string.mines_level_medium)) },
                    leadingIcon = if (level == Level.MEDIUM) { { Icon(Icons.Default.Check, null) } } else null
                )
                FilterChip(
                    selected = level == Level.HARD,
                    onClick = { level = Level.HARD },
                    label = { Text(stringResource(R.string.mines_level_hard)) },
                    leadingIcon = if (level == Level.HARD) { { Icon(Icons.Default.Check, null) } } else null
                )
                FilterChip(
                    selected = level == Level.CUSTOM,
                    onClick = { level = Level.CUSTOM },
                    label = { Text(stringResource(R.string.mines_level_custom)) },
                    leadingIcon = if (level == Level.CUSTOM) { { Icon(Icons.Default.Build, null) } } else null
                )
            }

            // Resumen de la selección actual
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val cur = when (level) {
                    Level.EASY -> easy
                    Level.MEDIUM -> med
                    Level.HARD -> hard
                    Level.CUSTOM -> MinesEngine.Config(rowsPreview, colsPreview,
                        (minesStr.toIntOrNull() ?: suggestedMines).coerceIn(1, maxMines)
                    )
                }
                Text(
                    stringResource(R.string.mines_selected_summary, cur.rows, cur.cols, cur.mines),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (level == Level.CUSTOM) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = rowsStr,
                        onValueChange = { rowsStr = it.filter(Char::isDigit).take(3) }, // permite vacío
                        label = { Text(stringResource(R.string.mines_rows)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { f ->
                                if (!f.isFocused && rowsStr.isNotEmpty()) {
                                    rowsStr = clamp(safeInt(rowsStr, ROW_MIN), ROW_MIN, ROW_MAX).toString()
                                }
                            }
                    )
                    OutlinedTextField(
                        value = colsStr,
                        onValueChange = { colsStr = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.mines_cols)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { f ->
                                if (!f.isFocused && colsStr.isNotEmpty()) {
                                    colsStr = clamp(safeInt(colsStr, COL_MIN), COL_MIN, COL_MAX).toString()
                                }
                            }
                    )
                    OutlinedTextField(
                        value = minesStr,
                        onValueChange = {
                            minesStr = it.filter(Char::isDigit).take(4)
                            autoMinesEnabled = false // el usuario tomó control
                        },
                        label = { Text(stringResource(R.string.mines_mines)) },
                        supportingText = { Text(stringResource(R.string.mines_suggested_20, suggestedMines)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { f ->
                                if (!f.isFocused && minesStr.isNotEmpty()) {
                                    val m = clamp(safeInt(minesStr, suggestedMines), 1, maxMines)
                                    minesStr = m.toString()
                                }
                            }
                    )
                }
                Text(
                    stringResource(R.string.mines_custom_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val cfg = if (level == Level.CUSTOM) {
                        // Clamp final + fallback al sugerido si "minas" quedó vacío
                        val r = rowsPreview
                        val c = colsPreview
                        val maxM = (r * c - 9).coerceAtLeast(1)
                        val suggest = clamp(kotlin.math.round(r * c * 0.20f).toInt(), 1, maxM)
                        val m = if (minesStr.isBlank()) suggest
                        else clamp(safeInt(minesStr, suggest), 1, maxM)
                        // actualiza "oficiales" y retorna config
                        rows = r; cols = c; mines = m
                        MinesEngine.Config(r, c, m)
                    } else null
                    onPick(level, cfg)
                }) { Text(stringResource(R.string.common_apply)) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}


@Composable
private fun numberColor(n: Int, isDark: Boolean): Color = when (n) {
    1 -> if (isDark) Color(0xFF6FA8FF) else Color(0xFF0000FF) // azul
    2 -> if (isDark) Color(0xFF8BE28B) else Color(0xFF007300) // verde
    3 -> if (isDark) Color(0xFFFF8B8B) else Color(0xFFFF0000) // rojo
    4 -> if (isDark) Color(0xFF9CA6FF) else Color(0xFF00007B) // azul oscuro
    5 -> if (isDark) Color(0xFFFF9F9F) else Color(0xFF7B0000) // marrón/rojo oscuro
    6 -> if (isDark) Color(0xFF88E0E0) else Color(0xFF007B7B) // cian/teal
    7 -> if (isDark) Color(0xFFEDEDED) else Color(0xFF000000) // negro/blanco según tema
    8 -> if (isDark) Color(0xFFBDBDBD) else Color(0xFF4D4D4D) // gris
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

