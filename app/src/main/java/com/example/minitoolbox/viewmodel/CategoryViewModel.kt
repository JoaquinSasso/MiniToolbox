// app/src/main/java/com/example/minitoolbox/viewmodel/CategoryViewModel.kt
package com.example.minitoolbox.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.minitoolbox.tools.ToolCategory

/** Mantiene la categoría actualmente seleccionada */
class CategoryViewModel : ViewModel() {
    val selectedCategory: MutableState<ToolCategory> =
        mutableStateOf(ToolCategory.Generadores)
}
