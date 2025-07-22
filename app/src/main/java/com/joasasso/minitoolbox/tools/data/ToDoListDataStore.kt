package com.joasasso.minitoolbox.tools.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "todo_list")

@Serializable
data class ToDoItem(val id: Int, val text: String, val isDone: Boolean)

object ToDoDataStore {
    private val TODO_LIST = stringPreferencesKey("todo_list")

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun getToDoList(context: Context): Flow<List<ToDoItem>> {
        return context.dataStore.data.map { prefs ->
            prefs[TODO_LIST]?.let {
                try {
                    json.decodeFromString<List<ToDoItem>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }
    }

    suspend fun saveToDoList(context: Context, list: List<ToDoItem>) {
        context.dataStore.edit { prefs ->
            prefs[TODO_LIST] = json.encodeToString(list)
        }
    }
}
