package com.joasasso.minitoolbox.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.joasasso.minitoolbox.tools.entretenimiento.minijuegos.minesweeper.MinesEngine
import kotlinx.coroutines.flow.first
import java.util.BitSet

private val Context.minesDataStore by preferencesDataStore("mines_store")

data class SavedGame(
    val rows: Int,
    val cols: Int,
    val mines: Int,
    val seed: Long,
    val firstTapIndex: Int,
    val status: String,              // Ready/InProgress/Won/Lost
    val revealedB64: String,
    val flagsB64: String,
    val startEpochMs: Long,
    val elapsedMs: Long,
    val explodedIndex: Int,
    val policy: String = "Square3x3",
    val minesB64: String = ""
)

object MinesStore {

    private val K_ROWS = intPreferencesKey("rows")
    private val K_COLS = intPreferencesKey("cols")
    private val K_MINES = intPreferencesKey("mines")
    private val K_SEED = longPreferencesKey("seed")
    private val K_FIRST = intPreferencesKey("first")
    private val K_STATUS = stringPreferencesKey("status")
    private val K_REVEALED = stringPreferencesKey("revealed_b64")
    private val K_FLAGS = stringPreferencesKey("flags_b64")
    private val K_START = longPreferencesKey("start_epoch")
    private val K_ELAPSED = longPreferencesKey("elapsed")
    private val K_EXPLODED = intPreferencesKey("exploded")
    private val K_LEVEL = stringPreferencesKey("last_level")     // EASY/MEDIUM/HARD/CUSTOM
    private val K_CUSTOM_R = intPreferencesKey("custom_r")
    private val K_CUSTOM_C = intPreferencesKey("custom_c")
    private val K_CUSTOM_M = intPreferencesKey("custom_m")
    private val K_POLICY = stringPreferencesKey("policy")
    private val K_MINESBITS = stringPreferencesKey("mines_bits_b64")


    // ---- Public API ----

    suspend fun save(context: Context, s: SavedGame) {
        context.minesDataStore.edit {
            it[K_ROWS] = s.rows
            it[K_COLS] = s.cols
            it[K_MINES] = s.mines
            it[K_SEED] = s.seed
            it[K_FIRST] = s.firstTapIndex
            it[K_STATUS] = s.status
            it[K_REVEALED] = s.revealedB64
            it[K_FLAGS] = s.flagsB64
            it[K_START] = s.startEpochMs
            it[K_ELAPSED] = s.elapsedMs
            it[K_EXPLODED] = s.explodedIndex
            it[K_POLICY] = s.policy
            it[K_MINESBITS] = s.minesB64
        }
    }

    suspend fun load(context: Context): SavedGame? {
        val prefs = context.minesDataStore.data.first()
        val rows = prefs[K_ROWS] ?: return null
        return SavedGame(
            rows = rows,
            cols = prefs[K_COLS] ?: return null,
            mines = prefs[K_MINES] ?: return null,
            seed = prefs[K_SEED] ?: return null,
            firstTapIndex = prefs[K_FIRST] ?: -1,
            status = prefs[K_STATUS] ?: MinesEngine.Status.Ready.name,
            revealedB64 = prefs[K_REVEALED] ?: "",
            flagsB64 = prefs[K_FLAGS] ?: "",
            startEpochMs = prefs[K_START] ?: 0L,
            elapsedMs = prefs[K_ELAPSED] ?: 0L,
            explodedIndex = prefs[K_EXPLODED] ?: -1,
            policy = prefs[K_POLICY] ?: "Square3x3",
            minesB64 = prefs[K_MINESBITS] ?: ""
        )
    }

    suspend fun clear(context: Context) {
        context.minesDataStore.edit { it.clear() }
    }

    suspend fun saveLastLevel(context: Context, level: String, customR: Int?, customC: Int?, customM: Int?) {
        context.minesDataStore.edit {
            it[K_LEVEL] = level
            customR?.let { r -> it[K_CUSTOM_R] = r }
            customC?.let { c -> it[K_CUSTOM_C] = c }
            customM?.let { m -> it[K_CUSTOM_M] = m }
        }
    }

    suspend fun loadLastLevel(context: Context): Pair<String, Triple<Int, Int, Int>?> {
        val prefs = context.minesDataStore.data.first()
        val lvl = prefs[K_LEVEL] ?: "EASY"
        val custom = if (lvl == "CUSTOM") Triple(
            prefs[K_CUSTOM_R] ?: 12,
            prefs[K_CUSTOM_C] ?: 12,
            prefs[K_CUSTOM_M] ?: 20
        ) else null
        return lvl to custom
    }

    // ---- Utils ----
    fun BitSet.toBase64(): String =
        Base64.encodeToString(this.toByteArray(), Base64.NO_WRAP)

    fun base64ToBitSet(b64: String): BitSet =
        if (b64.isEmpty()) BitSet()
        else BitSet.valueOf(Base64.decode(b64, Base64.NO_WRAP))
}
