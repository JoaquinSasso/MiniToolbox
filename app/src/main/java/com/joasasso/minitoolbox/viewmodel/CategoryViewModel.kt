// app/src/main/java/com/example/minitoolbox/viewmodel/CategoryViewModel.kt
package com.joasasso.minitoolbox.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.joasasso.minitoolbox.tools.ToolCategory

/** Mantiene la categoría actualmente seleccionada */
class CategoryViewModel : ViewModel() {
    val selectedCategory = mutableStateOf<ToolCategory>(ToolCategory.Herramientas)
}

