package com.joasasso.minitoolbox.metrics.storage

import android.content.Context
import android.os.LocaleList
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AggregatesRepository(private val context: Context) {

    data class DayDelta(
        val day: String,
        val appOpen: Int,
        val tools: MutableMap<String, Int>,
        val ads: MutableMap<String, Int>,
        // NUEVO:
        val versions: MutableMap<String, Int>,
        val versionsFirstSeen: MutableMap<String, Int>,
        val langPrimary: MutableMap<String, Int>,
        val langSecondary: MutableMap<String, Int>,
        val widgets: MutableMap<String, Int>
    )

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun today(): String = dayFmt.format(Date())

    // ---------------------------
    // Helpers de versión e idioma
    // ---------------------------

    private fun safeVersionName(): String = try {
        val pm = context.packageManager
        val p = pm.getPackageInfo(context.packageName, 0)
        p.versionName ?: "unknown"
    } catch (_: Throwable) { "unknown" }

    private fun primaryLanguage(): String {
        val loc: Locale = try {
            context.resources.configuration.locales.get(0)
        } catch (_: Throwable) { Locale.getDefault() }
        return loc.language.lowercase(Locale.ROOT).ifBlank { "und" }
    }

    private fun secondaryLanguage(): String? {
        val list: LocaleList = context.resources.configuration.locales
        if (list.size() > 1) {
            val second = list.get(1).language.lowercase(Locale.ROOT)
            val first = list.get(0).language.lowercase(Locale.ROOT)
            if (second.isNotBlank() && second != first) return second
        }
        return null
    }

    // ---------------------------
    // Escrituras de contadores
    // ---------------------------

    /** Marca version/lang “una vez por día” y “first seen por versión” ANTES de contar app open. */
    private suspend fun ensureDailyVersionAndLangMarks() {
        val ds = context.metricsDataStore
        val day = today()
        val version = safeVersionName()
        val lang1 = primaryLanguage()
        val lang2 = secondaryLanguage()

        ds.edit { e ->
            // --- Version DAU (1x/día) ---
            val lastV = e[MetricsKeys.LAST_VERSION_HEARTBEAT_DAY]
            if (lastV != day) {
                val dauByDay = JsonUtils.fromDayNestedIntMap(e[MetricsKeys.VERSION_DAU_BY_DAY_JSON])
                val bucket = dauByDay.getOrPut(day) { mutableMapOf() }
                bucket[version] = (bucket[version] ?: 0) + 1
                e[MetricsKeys.VERSION_DAU_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(dauByDay)
                e[MetricsKeys.LAST_VERSION_HEARTBEAT_DAY] = day
            }

            // --- Version FIRST SEEN (1x por versión en este dispositivo) ---
            val firstSeenSet = parseStringSet(e[MetricsKeys.FIRST_SEEN_VERSIONS_JSON])
            if (!firstSeenSet.contains(version)) {
                val fsByDay = JsonUtils.fromDayNestedIntMap(e[MetricsKeys.VERSION_FIRST_SEEN_BY_DAY_JSON])
                val bucket = fsByDay.getOrPut(day) { mutableMapOf() }
                bucket[version] = (bucket[version] ?: 0) + 1
                e[MetricsKeys.VERSION_FIRST_SEEN_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(fsByDay)

                firstSeenSet.add(version)
                e[MetricsKeys.FIRST_SEEN_VERSIONS_JSON] = toJsonArrayString(firstSeenSet)
            }

            // --- Idiomas (1x/día) ---
            val lastL = e[MetricsKeys.LAST_LANG_HEARTBEAT_DAY]
            if (lastL != day) {
                val lp = JsonUtils.fromDayNestedIntMap(e[MetricsKeys.LANG_PRIMARY_BY_DAY_JSON])
                val lpb = lp.getOrPut(day) { mutableMapOf() }
                lpb[lang1] = (lpb[lang1] ?: 0) + 1
                e[MetricsKeys.LANG_PRIMARY_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(lp)

                if (!lang2.isNullOrBlank() && lang2 != lang1) {
                    val ls = JsonUtils.fromDayNestedIntMap(e[MetricsKeys.LANG_SECONDARY_BY_DAY_JSON])
                    val lsb = ls.getOrPut(day) { mutableMapOf() }
                    lsb[lang2] = (lsb[lang2] ?: 0) + 1
                    e[MetricsKeys.LANG_SECONDARY_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(ls)
                }

                e[MetricsKeys.LAST_LANG_HEARTBEAT_DAY] = day
            }
        }
    }

    private fun parseStringSet(json: String?): MutableSet<String> {
        if (json.isNullOrBlank()) return mutableSetOf()
        return try {
            val arr = JSONArray(json)
            val out = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                val v = arr.optString(i)
                if (v.isNotBlank()) out += v
            }
            out
        } catch (_: Throwable) { mutableSetOf() }
    }

    private fun toJsonArrayString(set: Set<String>): String {
        val arr = JSONArray()
        set.forEach { arr.put(it) }
        return arr.toString()
    }

    suspend fun incrementAppOpen() {
        // Asegura marcas de versión/idioma (1x/día y first-seen) antes del conteo
        ensureDailyVersionAndLangMarks()

        val ds = context.metricsDataStore
        val day = today()
        ds.edit { e ->
            val byDay = JsonUtils.fromDayIntMap(e[MetricsKeys.APP_OPEN_COUNT_BY_DAY])
            byDay[day] = (byDay[day] ?: 0) + 1
            e[MetricsKeys.APP_OPEN_COUNT_BY_DAY] = JsonUtils.toDayIntMap(byDay)
        }
    }

    suspend fun incrementToolUse(toolId: String) {
        val ds = context.metricsDataStore
        val day = today()
        ds.edit { e ->
            val byDay = JsonUtils.fromDayNestedIntMap(e[MetricsKeys.TOOL_USE_BY_DAY_JSON])
            val bucket = byDay.getOrPut(day) { mutableMapOf() }
            bucket[toolId] = (bucket[toolId] ?: 0) + 1
            e[MetricsKeys.TOOL_USE_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(byDay)
        }
    }

    suspend fun incrementAdImpression(type: String) {
        val ds = context.metricsDataStore
        val day = today()
        ds.edit { e ->
            val byDay = JsonUtils.fromDayNestedIntMap(e[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON])
            val bucket = byDay.getOrPut(day) { mutableMapOf() }
            bucket[type] = (bucket[type] ?: 0) + 1
            e[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(byDay)
        }
    }

    suspend fun incrementWidgetUse(widgetType: String) {
        val ds = context.metricsDataStore
        val day = today()
        ds.edit { e ->
            val byDay = JsonUtils.fromDayNestedIntMap(e[MetricsKeys.WIDGET_USE_BY_DAY_JSON])
            val bucket = byDay.getOrPut(day) { mutableMapOf() }
            bucket[widgetType] = (bucket[widgetType] ?: 0) + 1
            e[MetricsKeys.WIDGET_USE_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(byDay)
        }
    }

    // ----------------------------------
    // Lecturas
    // ----------------------------------

    suspend fun getAppOpenCounts(): Map<String, Int> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }
        return JsonUtils.fromDayIntMap(prefs[MetricsKeys.APP_OPEN_COUNT_BY_DAY])
    }

    suspend fun getToolUseCounts(): Map<String, Map<String, Int>> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }
        return JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.TOOL_USE_BY_DAY_JSON])
    }

    suspend fun getAdImpressionCounts(): Map<String, Map<String, Int>> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }
        return JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON])
    }

    // ----------------------------------
    // Cálculo de DELTAS
    // ----------------------------------

    suspend fun buildDeltasSinceLastSent(): List<DayDelta> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }

        val currentApp  = JsonUtils.fromDayIntMap(prefs[MetricsKeys.APP_OPEN_COUNT_BY_DAY])
        val currentTool = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.TOOL_USE_BY_DAY_JSON])
        val currentAds  = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON])

        val currentVerDAU  = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.VERSION_DAU_BY_DAY_JSON])
        val currentVerFS   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.VERSION_FIRST_SEEN_BY_DAY_JSON])

        val currentLangP   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.LANG_PRIMARY_BY_DAY_JSON])
        val currentLangS   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.LANG_SECONDARY_BY_DAY_JSON])

        val currentWidgets = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.WIDGET_USE_BY_DAY_JSON])

        val sentApp   = JsonUtils.fromDayIntMap(prefs[MetricsKeys.SENT_APP_OPEN_BY_DAY])
        val sentTool  = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON])
        val sentAds   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_AD_IMPR_BY_DAY_JSON])

        val sentVerDAU = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_VERSION_DAU_BY_DAY_JSON])
        val sentVerFS  = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_VERSION_FIRST_SEEN_BY_DAY_JSON])

        val sentLangP  = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_LANG_PRIMARY_BY_DAY_JSON])
        val sentLangS  = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_LANG_SECONDARY_BY_DAY_JSON])

        val sentWidgets = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_WIDGET_USE_BY_DAY_JSON])

        val allDays = (currentApp.keys +
                currentTool.keys + currentAds.keys +
                currentVerDAU.keys + currentVerFS.keys +
                currentLangP.keys + currentLangS.keys +
                currentWidgets.keys +
                sentApp.keys + sentTool.keys + sentAds.keys +
                sentVerDAU.keys + sentVerFS.keys +
                sentLangP.keys + sentLangS.keys +
                sentWidgets.keys).toSortedSet()

        val out = mutableListOf<DayDelta>()
        for (day in allDays) {
            val appDelta = (currentApp[day] ?: 0) - (sentApp[day] ?: 0)

            fun diffMap(cur: Map<String, Int>?, sent: Map<String, Int>?): MutableMap<String, Int> {
                val c = cur ?: emptyMap()
                val s = sent ?: emptyMap()
                val keys = (c.keys + s.keys).toSet()
                val m = mutableMapOf<String, Int>()
                for (k in keys) {
                    val d = (c[k] ?: 0) - (s[k] ?: 0)
                    if (d > 0) m[k] = d
                }
                return m
            }

            val toolsDelta   = diffMap(currentTool[day],   sentTool[day])
            val adsDelta     = diffMap(currentAds[day],    sentAds[day])
            val verDauDelta  = diffMap(currentVerDAU[day], sentVerDAU[day])
            val verFsDelta   = diffMap(currentVerFS[day],  sentVerFS[day])
            val langPDelta   = diffMap(currentLangP[day],  sentLangP[day])
            val langSDelta   = diffMap(currentLangS[day],  sentLangS[day])
            val widgetsDelta = diffMap(currentWidgets[day], sentWidgets[day])

            if (appDelta > 0 || toolsDelta.isNotEmpty() || adsDelta.isNotEmpty() ||
                verDauDelta.isNotEmpty() || verFsDelta.isNotEmpty() ||
                langPDelta.isNotEmpty() || langSDelta.isNotEmpty() ||
                widgetsDelta.isNotEmpty()
            ) {
                out += DayDelta(
                    day = day,
                    appOpen = appDelta.coerceAtLeast(0),
                    tools = toolsDelta,
                    ads = adsDelta,
                    versions = verDauDelta,
                    versionsFirstSeen = verFsDelta,
                    langPrimary = langPDelta,
                    langSecondary = langSDelta,
                    widgets = widgetsDelta
                )
            }
        }
        return out
    }

    // --------------------------------------------------
    // Commit de envío (idempotencia local)
    // --------------------------------------------------

    /** Marca como "enviados" SOLO los deltas efectivamente enviados en el último batch. */
    suspend fun commitSent(deltas: List<DayDelta>) {
        val ds = context.metricsDataStore
        val prefs = try { ds.data.first() } catch (_: Throwable) { emptyPreferences() }

        val sentApp     = JsonUtils.fromDayIntMap(prefs[MetricsKeys.SENT_APP_OPEN_BY_DAY])
        val sentTools   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON])
        val sentAds     = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_AD_IMPR_BY_DAY_JSON])

        val sentVerDAU  = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_VERSION_DAU_BY_DAY_JSON])
        val sentVerFS   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_VERSION_FIRST_SEEN_BY_DAY_JSON])

        val sentLangP   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_LANG_PRIMARY_BY_DAY_JSON])
        val sentLangS   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_LANG_SECONDARY_BY_DAY_JSON])

        val sentWidgets = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_WIDGET_USE_BY_DAY_JSON])

        for (d in deltas) {
            // app
            sentApp[d.day] = (sentApp[d.day] ?: 0) + d.appOpen
            // tools
            val t = sentTools.getOrPut(d.day) { mutableMapOf() }
            for ((k, v) in d.tools) t[k] = (t[k] ?: 0) + v
            // ads
            val a = sentAds.getOrPut(d.day) { mutableMapOf() }
            for ((k, v) in d.ads) a[k] = (a[k] ?: 0) + v
            // versions (DAU)
            val vd = sentVerDAU.getOrPut(d.day) { mutableMapOf() }
            for ((k, v) in d.versions) vd[k] = (vd[k] ?: 0) + v
            // versions first-seen
            val vf = sentVerFS.getOrPut(d.day) { mutableMapOf() }
            for ((k, v) in d.versionsFirstSeen) vf[k] = (vf[k] ?: 0) + v
            // lang primary
            val lp = sentLangP.getOrPut(d.day) { mutableMapOf() }
            for ((k, v) in d.langPrimary) lp[k] = (lp[k] ?: 0) + v
            // lang secondary
            val ls = sentLangS.getOrPut(d.day) { mutableMapOf() }
            for ((k, v) in d.langSecondary) ls[k] = (ls[k] ?: 0) + v
            // widgets
            val w = sentWidgets.getOrPut(d.day) { mutableMapOf() }
            for ((k, v) in d.widgets) w[k] = (w[k] ?: 0) + v
        }

        ds.edit { e ->
            e[MetricsKeys.SENT_APP_OPEN_BY_DAY]             = JsonUtils.toDayIntMap(sentApp)
            e[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON]        = JsonUtils.toDayNestedIntMap(sentTools)
            e[MetricsKeys.SENT_AD_IMPR_BY_DAY_JSON]         = JsonUtils.toDayNestedIntMap(sentAds)

            e[MetricsKeys.SENT_VERSION_DAU_BY_DAY_JSON]     = JsonUtils.toDayNestedIntMap(sentVerDAU)
            e[MetricsKeys.SENT_VERSION_FIRST_SEEN_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(sentVerFS)

            e[MetricsKeys.SENT_LANG_PRIMARY_BY_DAY_JSON]    = JsonUtils.toDayNestedIntMap(sentLangP)
            e[MetricsKeys.SENT_LANG_SECONDARY_BY_DAY_JSON]  = JsonUtils.toDayNestedIntMap(sentLangS)

            e[MetricsKeys.SENT_WIDGET_USE_BY_DAY_JSON]      = JsonUtils.toDayNestedIntMap(sentWidgets)

            e[MetricsKeys.PENDING_BATCH_ID]                 = ""
            e[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON]       = ""
        }
    }
}
