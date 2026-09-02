package com.joasasso.minitoolbox

import android.app.Application

/**
 * Application mínima para los tests de Robolectric.
 *
 * Robolectric instancia la Application real del manifest, y [MiniToolboxApp.onCreate]
 * inicializa Firebase App Check, AdMob y Meta, además de lanzar el sanitizador de métricas.
 * En un test unitario el ContentProvider que inicializa Firebase no corre, así que
 * FirebaseAppCheck.getInstance() falla con "Default FirebaseApp is not initialized".
 *
 * Los tests de métricas no necesitan nada de eso: sólo un Context sobre el que montar
 * el DataStore. Esta clase evita el arranque completo de la app y además impide que el
 * sanitizador del onCreate interfiera con lo que cada test quiere verificar.
 */
class TestApplication : Application()