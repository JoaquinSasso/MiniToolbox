package com.joasasso.minitoolbox.utils.pro

import android.content.Context
import androidx.core.content.edit

/**
 * Sistema de acceso PRO con dos modos intercambiables:
 *  - TIMED_PASS: ver un rewarded otorga un pase temporal (ej. 10 min) para todas las tools PRO.
 *  - CREDITS: ver un rewarded otorga N créditos; cada apertura PRO consume 1 crédito.
 *
 * Además:
 *  - "Grace access" si no hay fill (NO_FILL): deja pasar una vez sin sumar créditos, con cupos diarios y cooldown.
 *  - Tope de créditos para evitar farmeo.
 *  - Persistencia en SharedPreferences (simple y robusto para apps offline).
 */
object CreditAccessManager {

    // --------- CONFIG (ajústalo a gusto) ---------

    // Modo por defecto
    private val DEFAULT_MODE = RewardMode.TIMED_PASS

    // TIMED PASS
    private const val DEFAULT_TIMED_PASS_MS = 10 * 60_000L // 10 minutos

    // CREDITS
    private const val DEFAULT_CREDIT_PER_AD = 3
    private const val DEFAULT_CREDIT_CAP = 5

    // GRACE (cuando no hay fill)
    private const val DEFAULT_GRACE_DAILY_MAX = 5
    private const val DEFAULT_GRACE_COOLDOWN_MS = 5 * 60_000L // 5 min

    // --------- KEYS / PREFS ---------
    private const val PREFS = "pro_credits_prefs"

    private const val KEY_MODE = "mode" // "TIMED_PASS" | "CREDITS"

    private const val KEY_PASS_UNTIL = "pass_until_ts"

    private const val KEY_CREDITS = "credits"
    private const val KEY_CREDIT_CAP = "credit_cap"
    private const val KEY_CREDIT_PER_AD = "credit_per_ad"

    private const val KEY_TIMED_PASS_MS = "timed_pass_ms"

    private const val KEY_GRACE_DATE = "grace_date" // yyyymmdd
    private const val KEY_GRACE_USED = "grace_used"
    private const val KEY_GRACE_DAILY_MAX = "grace_daily_max"
    private const val KEY_GRACE_LAST_TS = "grace_last_ts"
    private const val KEY_GRACE_COOLDOWN_MS = "grace_cooldown_ms"

    // --------- Tipos ---------
    enum class RewardMode { TIMED_PASS, CREDITS }

    // --------- Setup opcional (para tunear valores en runtime) ---------

    fun setMode(context: Context, mode: RewardMode) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit { putString(KEY_MODE, mode.name) }
    }

    fun getMode(context: Context): RewardMode {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY_MODE, null) ?: return DEFAULT_MODE
        return runCatching { RewardMode.valueOf(raw) }.getOrDefault(DEFAULT_MODE)
    }

    fun configureTimedPass(context: Context, durationMs: Long = DEFAULT_TIMED_PASS_MS) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit { putLong(KEY_TIMED_PASS_MS, durationMs) }
    }

    fun configureCredits(context: Context, creditPerAd: Int = DEFAULT_CREDIT_PER_AD, creditCap: Int = DEFAULT_CREDIT_CAP) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit {
            putInt(KEY_CREDIT_PER_AD, creditPerAd)
            putInt(KEY_CREDIT_CAP, creditCap)
        }
    }

    fun configureGrace(context: Context, dailyMax: Int = DEFAULT_GRACE_DAILY_MAX, cooldownMs: Long = DEFAULT_GRACE_COOLDOWN_MS) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit {
            putInt(KEY_GRACE_DAILY_MAX, dailyMax)
            putLong(KEY_GRACE_COOLDOWN_MS, cooldownMs)
        }
    }

    // --------- Estado TIMED PASS ---------

    fun hasActivePass(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val until = sp.getLong(KEY_PASS_UNTIL, 0L)
        return now < until
    }

    fun passRemainingMs(context: Context, now: Long = System.currentTimeMillis()): Long {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val until = sp.getLong(KEY_PASS_UNTIL, 0L)
        return (until - now).coerceAtLeast(0L)
    }

    fun startTimedPassForAd(context: Context, now: Long = System.currentTimeMillis()) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val duration = sp.getLong(KEY_TIMED_PASS_MS, DEFAULT_TIMED_PASS_MS)
        sp.edit { putLong(KEY_PASS_UNTIL, now + duration) }
    }

    fun endTimedPass(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit { putLong(KEY_PASS_UNTIL, 0L) }
    }

    // --------- Estado CREDITS ---------

    fun credits(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getInt(KEY_CREDITS, 0)
    }

    fun creditCap(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getInt(KEY_CREDIT_CAP, DEFAULT_CREDIT_CAP)
    }

    fun creditPerAd(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getInt(KEY_CREDIT_PER_AD, DEFAULT_CREDIT_PER_AD)
    }

    fun grantCreditsForAd(context: Context, amountOverride: Int? = null) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val perAd = amountOverride ?: creditPerAd(context)
        val cap = creditCap(context)
        val newVal = (credits(context) + perAd).coerceAtMost(cap)
        sp.edit { putInt(KEY_CREDITS, newVal) }
    }

    /** Intenta consumir 1 crédito. Devuelve true si pudo. */
    fun consumeOneCreditIfAvailable(context: Context): Boolean {
        val current = credits(context)
        if (current <= 0) return false
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit { putInt(KEY_CREDITS, current - 1) }
        return true
    }

    // --------- GRACE (cuando no hay fill) ---------

    fun graceLeftToday(context: Context, now: Long = System.currentTimeMillis()): Int {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = yyyymmdd(now)
        val lastDay = sp.getString(KEY_GRACE_DATE, null)
        val used = if (lastDay == today) sp.getInt(KEY_GRACE_USED, 0) else 0
        val max = sp.getInt(KEY_GRACE_DAILY_MAX, DEFAULT_GRACE_DAILY_MAX)
        return (max - used).coerceAtLeast(0)
    }

    fun canUseGrace(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = yyyymmdd(now)
        val lastDay = sp.getString(KEY_GRACE_DATE, null)
        val usedToday = if (lastDay == today) sp.getInt(KEY_GRACE_USED, 0) else 0
        val max = sp.getInt(KEY_GRACE_DAILY_MAX, DEFAULT_GRACE_DAILY_MAX)
        val lastTs = sp.getLong(KEY_GRACE_LAST_TS, 0L)
        val cooldownMs = sp.getLong(KEY_GRACE_COOLDOWN_MS, DEFAULT_GRACE_COOLDOWN_MS)
        val cooldownOk = (now - lastTs) >= cooldownMs
        return usedToday < max && cooldownOk
    }

    /** Consume 1 pase de gracia (sin sumar créditos). Devuelve true si pudo. */
    fun consumeGrace(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        if (!canUseGrace(context, now)) return false
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = yyyymmdd(now)
        val lastDay = sp.getString(KEY_GRACE_DATE, null)
        val usedToday = if (lastDay == today) sp.getInt(KEY_GRACE_USED, 0) else 0
        sp.edit {
            putString(KEY_GRACE_DATE, today)
            putInt(KEY_GRACE_USED, usedToday + 1)
            putLong(KEY_GRACE_LAST_TS, now)
        }
        return true
    }

    // --------- Helpers ---------

    fun hasProAccessNow(context: Context): Boolean {
        return when (getMode(context)) {
            RewardMode.TIMED_PASS -> hasActivePass(context)
            RewardMode.CREDITS -> credits(context) > 0
        }
    }

    fun formatDurationMmSs(ms: Long): String {
        val totalSec = (ms / 1000).toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }

    private fun yyyymmdd(ts: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
        val y = cal.get(java.util.Calendar.YEAR)
        val m = cal.get(java.util.Calendar.MONTH) + 1
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return String.format("%04d%02d%02d", y, m, d)
    }
}
