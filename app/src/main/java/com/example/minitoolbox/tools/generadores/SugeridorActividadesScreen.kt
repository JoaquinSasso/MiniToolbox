package com.example.minitoolbox.tools.generadores

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SugeridorActividadScreen(onBack: () -> Unit) {
    val actividades = listOf(
        "Lee un capítulo de un libro 📚",
        "Dibuja algo que te guste ✏️",
        "Sal a caminar 10 minutos 🚶‍♂️",
        "Toma un vaso de agua 💧",
        "Haz 10 flexiones 💪",
        "Llama a un amigo 📞",
        "Ordena tu escritorio 🧹",
        "Busca una nueva canción para escuchar 🎵",
        "Medita durante 3 minutos 🧘",
        "Haz una lista de agradecimientos 🙏",
        "Escribe una idea para un proyecto nuevo 💡",
        "Prepara un snack saludable 🍎",
        "Aprende una palabra nueva en otro idioma 🌍",
        "Toma una foto creativa 📸",
        "Desconéctate del celular por 15 minutos 📵",
        "Haz una pequeña limpieza en tu cuarto 🛏️",
        "Practica respiración profunda 🌬️",
        "Haz un gesto amable por alguien hoy 🤗",
        "Organiza tu día con una lista de tareas 📝",
        "Saluda a alguien que no conocés 👋",
        "Escribe tres cosas que te hacen feliz 😊",
        "Baila una canción que te guste 💃",
        "Mira un corto inspirador en internet 🎬",
        "Haz un estiramiento durante 5 minutos 🧘‍♂️",
        "Escucha un podcast sobre un tema nuevo 🎧",
        "Tómate un té relajante ☕️",
        "Revisa y limpia tus emails 📧",
        "Planifica un viaje imaginario 🗺️",
        "Escribe una pequeña poesía ✍️",
        "Prueba una receta sencilla y rápida 🍽️",
        "Dedica 5 minutos a observar el cielo 🌤️",
        "Riega tus plantas 🌱",
        "Haz un dibujo con los ojos cerrados 🎨",
        "Busca un dato curioso sobre historia 📖",
        "Escribe tres objetivos semanales 🗒️",
        "Haz una pausa y sonríe por 30 segundos 😊",
        "Organiza tu biblioteca o estante 📕",
        "Escribe algo positivo sobre ti mismo 💖",
        "Juega un minijuego de ingenio 🧩",
        "Escucha sonidos de la naturaleza 🌳",
        "Recuerda un momento divertido 😄",
        "Planifica una actividad para el fin de semana 📅",
        "Escribe una carta breve a tu futuro tú 📨",
        "Haz una pausa para observar tu entorno 👀",
        "Busca inspiración en imágenes bonitas 📷",
        "Practica un poco de yoga simple 🧘",
        "Intenta hacer malabares con objetos pequeños 🤹",
        "Haz una donación simbólica o ayuda online 🌟",
        "Aprende sobre una cultura distinta 🌎",
        "Prueba una fruta que no sueles comer 🍍",
        "Prepara una bebida refrescante 🍹",
        "Escribe sobre algo que te gustaría aprender 🖋️",
        "Organiza una pequeña reunión virtual con amigos 💻",
        "Saca la basura o recicla algo ♻️",
        "Escribe una frase inspiradora para compartir ✨",
        "Busca una cita motivacional 📌",
        "Dedica tiempo a cuidar tu piel 🧖",
        "Escribe o dibuja sobre un sueño reciente 💤",
        "Pasa tiempo con tu mascota o animal favorito 🐶",
        "Haz una pausa consciente sin hacer nada 🙌",
        "Busca formas creativas de reutilizar un objeto 🔄",
        "Explora una afición que hayas olvidado 🎭",
        "Observa el atardecer o amanecer 🌅",
        "Visita virtualmente un museo o galería 🖼️",
        "Dale mantenimiento o limpieza a tu bicicleta 🚲",
        "Revisa y actualiza tus objetivos personales 🎯",
        "Haz un breve ejercicio visual para descansar la vista 👁️",
        "Prueba escribir con la mano no dominante ✍️",
        "Escucha música relajante durante 5 minutos 🎶",
        "Haz una pequeña lista de cosas por soltar 🚮",
        "Prepara algo creativo con materiales reciclados ♻️",
        "Busca una anécdota inspiradora o divertida 🌟",
        "Piensa en tres logros recientes que has tenido 🏆",
        "Comparte algo positivo en redes sociales 📲",
        "Juega a tu videojuego favorito 🎮",
        "Mira una pelicula o serie que no has visto 📺",
        "Mira un documental 🐆"
    )

    var actividadActual by remember { mutableStateOf(actividades.random()) }
    var showInfo by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun nuevaActividad() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val otras = actividades.filter { it != actividadActual }
        actividadActual = if (otras.isNotEmpty()) otras.random() else actividades.random()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sugeridor de Actividades") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showInfo = true
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                "¿Qué podrías hacer ahora?",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                        .heightIn(min = 90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        actividadActual,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { nuevaActividad() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Otra sugerencia")
                    Spacer(Modifier.width(4.dp))
                    Text("Otra sugerencia")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(actividadActual))
                        scope.launch { snackbarHostState.showSnackbar("Actividad copiada") }
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar sugerencia")
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar")
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showInfo = false
            },
            title = { Text("¿Para qué sirve?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Te da ideas rápidas para hacer cuando no sabés qué hacer, querés moverte o distraerte.")
                    Text("• Usalo para romper la rutina, salir de un bloqueo, o encontrar un pequeño desafío o descanso en tu día.")
                    Text("• Podés copiar la sugerencia para compartirla o anotarla.")
                    Text("• Si no te convence la idea, tocá “Otra sugerencia”.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showInfo = false
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
