import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minitoolbox.tools.generadores.QrContacto
import com.example.minitoolbox.tools.generadores.QrContactoDataStore
import com.lightspark.composeqr.QrCodeView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneradorQrContactoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dataStore = remember { QrContactoDataStore(context) }
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val datosGuardados by dataStore.datos.collectAsState(initial = QrContacto())

    var nombre    by remember { mutableStateOf("") }
    var telefono  by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var showInfo  by remember { mutableStateOf(false) }

    // Cargar datos guardados al iniciar
    LaunchedEffect(datosGuardados) {
        nombre    = datosGuardados.nombre
        telefono  = datosGuardados.telefono
        email     = datosGuardados.email
    }

    fun guardarDatos() {
        scope.launch {
            dataStore.guardar(
                QrContacto(
                    nombre    = nombre,
                    telefono  = telefono,
                    email     = email
                )
            )
        }
    }

    // vCard builder minimalista
    fun generarVCard(): String {
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            if (nombre.isNotBlank())   appendLine("FN:${nombre.trim()}")
            if (telefono.isNotBlank()) appendLine("TEL;TYPE=CELL:${telefono.trim()}")
            if (email.isNotBlank())    appendLine("EMAIL:${email.trim()}")
            appendLine("END:VCARD")
        }
    }

    val vCard = generarVCard()
    val qrEnabled = nombre.isNotBlank() && telefono.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR de Contacto (vCard)") },
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        guardarDatos()
                    },
                    label = { Text("Nombre completo*") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = {
                        telefono = it.filter { c -> c.isDigit() || c == '+' || c == ' ' || c == '-' }
                        guardarDatos()
                    },
                    label = { Text("Teléfono*") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        guardarDatos()
                    },
                    label = { Text("Email (opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(vCard))
                            scope.launch { snackbarHostState.showSnackbar("vCard copiada") }
                        },
                        enabled = qrEnabled
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar vCard")
                        Spacer(Modifier.width(4.dp))
                        Text("Copiar vCard")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            nombre = ""
                            telefono = ""
                            email = ""
                            scope.launch { dataStore.limpiar() }
                        },
                        enabled = nombre.isNotBlank() || telefono.isNotBlank() || email.isNotBlank()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpiar")
                        Spacer(Modifier.width(4.dp))
                        Text("Limpiar")
                    }
                }
                Divider(Modifier.padding(vertical = 8.dp))
                Card(
                    modifier = Modifier
                        .size(300.dp)
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (qrEnabled) {
                            QrCodeView(
                                data = vCard,
                                modifier = Modifier.size(270.dp)
                            )
                        } else {
                            Text(
                                "Completa nombre y teléfono",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                    Text("• Crea un código QR de contacto estándar (vCard) que puedes escanear desde cualquier celular para agregar la info a tu agenda rápidamente.")
                    Text("• Solo pide nombre completo y teléfono (email opcional).")
                    Text("• Puedes copiar el texto vCard para compartirlo por otros medios.")
                    Text("• El QR y la vCard se generan de manera local y nunca se envían a ningún servidor.")
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
