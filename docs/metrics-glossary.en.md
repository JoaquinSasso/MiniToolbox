# Metrics and telemetry glossary

[Español](metrics-glossary.md)

This document defines the exact meaning of the counters in MiniToolbox's own telemetry system. Its purpose is to avoid statistical misreadings when writing reports or public documents.

---

## 1. General activity metrics

### `app_open`
- **What it measures:** Every time `MainActivity` is created, including a recreation caused by a configuration change (theme or language). Returning to the app from the background without the activity being destroyed does not count.
- **When it increments:** In the `LaunchedEffect(Unit)` of `MainActivity.onCreate` (`MainActivity.kt:58`).
- **Historical limitation:** **[BIAS DETECTED]** Between 2025-09-24 and 2026-08-17 this counter duplicated the first open of the day because of a collision with `daily_active`. The whole historical series in that range carries a bias of roughly +1 open per device per day.
- **Correct use:** Raw usage frequency. It cannot be used to count users.

### `daily_active` (DAU)
- **What it measures:** Unique active devices per day.
- **When it increments:** Once per calendar day, by the device's local clock, on the first open of that day. It is not a 24-hour window: opening at 23:59 and at 00:01 counts as two days.
- **Guard:** The last counted day is stored in `SharedPreferences` (`metrics_daily_once`, `Metrics.kt:122`).
- **Correct use:** The most faithful metric for the size of the daily audience.

---

## 2. Install and version metrics

### `versions_first_seen`
- **What it measures:** The first time a device reports using a specific version.
- **When it increments:** Once for each app version that goes through the device.
- **Semantics:** If a user installs v1.0, updates to v1.1 and then to v1.2, this counter adds **3 units** in total (one per version).
- **WARNING:** It does **NOT** measure total unique users. Its cumulative total is inflated by the update factor (average number of versions installed per user).

### `versions`
- **What it measures:** Device-days per version. Each device adds **1** per day to the version it has installed, regardless of how many times it opens the app (`ensureDailyVersionAndLangMarks` in `AggregatesRepository.kt`). It is not equivalent to `app_open`.
- **Correct use:** Find out what share of the user base is still on old versions. The sum over all versions in a period gives the device-days for that period.

---

## 3. Feature metrics

### `tools`
- **What it measures:** Tool opens.
- **When it increments:** In the `NavGraph`, when the user enters a tool from outside it (`NavGraph.kt:69-82`, `ToolRoutes.visitToCount`). Moving between sub-screens of the same tool (Pomodoro list and detail, water and statistics) does not count again. Going through a screen that is not a tool, such as Categories, ends the visit: entering again counts again. A debouncer also drops repeats of the same tool within 5 seconds (`TOOL_USAGE_METRIC_COOLDOWN_MS`).
- **Historical limitation:** With `schema_version` 4 or earlier the route was compared, not the tool. Moving between sub-screens of the same tool counted again if more than 5 seconds had passed (mostly inflating `pomodoro`, `water` and `meetings`), and returning to the same tool after going through Categories did not count when the route matched the last tool's route. The two effects go in opposite directions, so the break at `schema_version` 5 has no fixed direction per tool.
- **Structure:** Map of `tool_id` -> `count`.
- **`about` key:** About is in `ToolRegistry` but it is not a tool. Up to `schema_version` 4 it added to `tools`, `tools_dau` and `tool_entry`; from `schema_version` 5 it does not count (`ToolRoutes.usageKey`). Exclude it when analyzing earlier data. The backend still accepts it (`EXTRA_KNOWN_TOOLS`) for old clients.
- **Note:** It does not measure time spent, only intent to use.
- **Key:** Since v1.3.2 it comes from `Tool.metricsKey`, a stable identifier independent of the navigation route. With the migration to type-safe navigation, sub-screens report under their tool's main key: `pomodoro_list` and `pomodoro/detail` report as `pomodoro`, `water_stats` as `water`, and the expense splitter sub-screens as `meetings`.
- **Retired tools (2026-10-30):** The following tools were removed from the app catalog: `zodiac_sign`, `multiverse_me`, `quick_calcs`, `basic_phrases` (whose historical backend key was `quotes`), `countries_info`, `percentage`, `coin_flip`, `todo`, `age_calculator`, `countdown`, `guess_capital`, `decimal_binary`, `password_generator` and `dice`. Their keys are kept in the backend's `TOOL_ROUTE_MAP` and the dashboard's `TOOL_CATALOG` so historical data can still be read, but they stop receiving events from the versions after this change.
- **Historical limitation:** **[BIAS DETECTED]** Up to v1.3.2, opens through a deep link (notification, widget, shortcut) were recorded **twice**: once in `MainActivity` and once in the `NavGraph` while navigating. The bias only affects tools with notifications or widgets, mainly `water` and `pomodoro`. The drop visible in those series from v1.3.2 onward is the bias being fixed, not a drop in usage.
- **Historical limitation:** **[DATA LOST]** Devices that opened a Pomodoro detail from a notification ended up with uploads permanently blocked (see `metrics-pipeline-blockage-postmortem.md`, Spanish). The whole series before August 2026 under-represents users of that tool.

### `tools_dau`
- **What it measures:** Device-days per tool. Each device contributes at most **1** per tool per day, regardless of how many times it opened it.
- **Structure:** Map of `tool_id` -> `count`.
- **Available since:** v1.3.2. There is no earlier series and it cannot be rebuilt.
- **Correct use:** It is the **denominator** that gives `tools` its meaning. It allows computing:
  - *Reach:* `tools_dau[t] / daily_active`, the share of the audience that uses the tool.
  - *Intensity:* `tools[t] / tools_dau[t]`, how many times those who use it open it.
- **WARNING:** `tools` alone cannot tell many people using something once apart from a few people using it a lot. Without `tools_dau`, any "most popular tools" ranking is ambiguous.

### `tool_entry`
- **What it measures:** Opens of each tool, broken down by where the user came from.
- **Structure:** Nested, `tool_entry[tool_id][source]` -> `count`.
- **Possible sources:** `nav` (in-app navigation), `notification`, `widget`, `shortcut`, `unknown`.
- **Available since:** v1.3.2.
- **Semantics:** The total per tool matches `tools[t]`, since it is recorded at the same point and with the same cooldown deduplication.
- **Correct use:** Assess whether notifications and widgets bring real usage, and put the menu position in perspective for tools that are mostly opened from outside the app.
- **Note:** An unrecognized source is grouped under `unknown` instead of creating a new key.

### `retention`
- **What it measures:** Device **age** crossed with usage **intensity**.
- **Structure:** Nested, `retention[age][intensity]` -> `count`. One mark per device per day.
- **Age buckets:** `age0_6`, `age7_29`, `age30_89`, `age90_179`, `age180p` (days since the first recorded day).
- **Intensity buckets:** `d1`, `d2_3`, `d4_7`, `d8_14`, `d15_28` (active days within a 28-day window).
- **How it preserves anonymity:** The computation happens **on the device**, with data that is never sent (the list of active days and the first day seen). Only a `1` in one of 25 possible buckets reaches the server. There are no identifiers or individual dates.
- **WARNING: this is NOT cohort retention.** It does not answer "of those who installed in week X, how many are still here after 7 days": that would require following individuals over time. What it gives is the intensity distribution crossed with age.
- **Limitation:** Reinstalling the app or clearing its data **resets the history**. A two-year user shows up again as `age0_6.d1`. This skews the distribution downward and cannot be corrected without persistent identifiers.
- **Limitation:** The window takes 28 days to mature. Every new install falls into the low buckets by construction, so reading this data during a version's first month leads to false conclusions.

### `widgets`
- **What it measures:** Taps on widgets, not updates. Structure: map of key -> `count`.
- **Keys:**
  - `widget_water_normal`, `widget_water_mini`: the add and remove water buttons (`AguaCallback.kt`).
  - `widget_flashlight`, `widget_flashlight_mini`: turning the flashlight on or off.
  - `widget_shortcuts`: taps on a tool in the favorites widget. Counted in `MainActivity` only when the Intent carries the `widget` source and the `startWidget` marker set by `FavoriteToolsWidget` (`MetricsSource.deepLinkWidgetKey`). Opens from notifications and from the water widgets do not add here: they are recorded in `tool_entry` with their source.
- **Historical limitation:** From `22f6c43` (2025-09-24) until the `fix/water-widget-metrics-optout` branch, removing water from the widget added to the metric even when the user had turned metrics off. Since uploads did not check the opt-out either until the `fix/upload-respects-optout` branch, those taps may have reached the server.
- **Historical limitation:** With `schema_version` 4 or earlier, `widget_shortcuts` counted **any** open with a start route, including the water and Pomodoro notifications and the water widgets. From `schema_version` 5 (`fix/widget-shortcuts-source` branch, first release after 1.4.0) it measures the favorites widget only. The series breaks at that version and values drop; since the backend does not store `schema_version`, the break can only be located in Firestore by the release date.
- **Correct use:** Compare usage between the water and flashlight widgets. `widget_shortcuts` measures the favorites widget only in data from `schema_version` 5 onwards.

### `ads` (retired)
- **What it measured:** Ad impressions (banner, interstitial, rewarded).
- **Status:** Retired together with ads and the PRO mode. The first version after 1.4.0 no longer sends it (`schema_version` 4), and the backend ignores the field if an old client still sends it: it is not written to Firestore or returned by `metricsDaily` or `metricsSummary`.
- **Historical data:** `metrics_daily` documents from before the change keep the `ads` map; they were not deleted. Reading them requires querying Firestore directly, because the API no longer exposes them.
- **Related key:** `pro` in `tools` was never a tool: it was the PRO purchase screen. It can show up in historical data and is excluded when analyzing tool usage.

---

## 3.b. Batch metadata

### `schema_version`
- **What it is:** Version of the payload format sent by the client.
- **Values:** `1` (initial v1.3.2), `2` (adds `tools_dau`), `3` (adds `tool_entry` and `retention`), `4` (removes `ads`), `5` (`widgets.widget_shortcuts` counts the favorites widget only; `tools`, `tools_dau` and `tool_entry` count per tool visit instead of per route, and do not count `about`; `client_health.dropped_batches` no longer counts transient errors).
- **Note:** The backend does not persist this number today; it travels in the payload but is not stored in Firestore.
- **What it is for:** Separating historical data if a field's meaning ever changes. It cannot be added retroactively: without this number, a change in meaning mixes old and new data forever.

### `client_health`
- **What it measures:** Health of the upload pipeline itself on the device.
- **Fields:** `dropped_batches`, `sanitized_keys`, `consecutive_failures`.
- **`dropped_batches`:** Batches discarded without a confirmed upload. It adds 1 when the server permanently rejects a batch and sanitizing does not fix it (or the attempts for that rejection run out). Transient errors do not add: after the retries run out the batch is kept with the same `batch_id` and sent again on the next trigger. **Historical limitation:** up to `schema_version` 4, running out of transient retries deleted the batch and added 1; the batch was sent again with a different `batch_id`, so that counter overstates losses and aggregates may be double counted if the server had stored the original.
- **What it is for:** Detecting devices with upload problems. Without it, a device that stopped reporting cannot be told apart from one that is not being used.
- **Note:** It is not part of the daily aggregates; it is recorded per batch.

### `auth_method` (retired)
- **What it measured:** How each batch was authenticated: `appcheck` or `api_key`.
- **Status:** Ingest accepts App Check only (`docs/decisions/appcheck-only-ingest.md`, Spanish), so the field is no longer written to `metrics_ingest_batches` or `metrics_ingest_logs`.
- **`metrics_auth` rollup:** Keeps its name and shape for continuity. Since the change, `appcheck` and `by_version.<version>.appcheck` mean accepted batches. Earlier months also have `api_key`, which remains in Firestore but `metricsAuth` no longer returns. For a total of accepted batches that also holds for those months, use `batches`.
- **Rejected batches:** Not counted in Firestore. They show up in Cloud Logging as `ingest_unauthorized`, with the declared `app_version`.

---

## 4. Localization metrics

### `lang_primary` / `lang_secondary`
- **What it measures:** Languages configured in the user's system.
- **When it increments:** Once per device per day, together with `versions`. `lang_secondary` only counts if the second language differs from the first.
- **Note:** As with `versions`, the sum of `lang_primary` over a period gives the device-days for that period.
- **Correct use:** Prioritize translations and local support.

---

## Note on the opt-out

Turning metrics off in About stops both recording and uploading. Queued uploads are cancelled and anything recorded but not yet sent is discarded: it is marked as sent without sending it (`AggregatesRepository.discardUnsent`), so turning metrics back on uploads only what is recorded afterwards. The worker checks again before sending, in case it was already queued.

- **Historical limitation:** until the `fix/upload-respects-optout` branch, uploads did not check the opt-out. Data recorded before turning metrics off was still sent, and an upload was queued every time the app went to the background.
- **Known limit:** if an upload was already in flight (the HTTP request had left) when the user turns metrics off, that batch reaches the server.

---

## Note on debug builds

Debug builds send to the same endpoint as production and their data adds to the same aggregates. This is the author's decision (2026-11-10): the build they use every day on their phone is a debug build and accounts for a large share of the app's usage, so discarding it would remove real data.

- **Bias:** with few active users, the author's device weighs heavily on every counter, and its usage is not a typical user's (trying new tools, opening development screens). Any public figure has to say so.
- **Cannot be separated:** the payload does not carry `build_type` and the backend does not store it. Separating it some day would require adding it on both sides; it cannot be recovered for past data.
- **Test keys:** `dev/DevInspector.kt` can generate `metricTest` and `metricsTest`. They are not in the backend's `KNOWN_TOOLS`, so they are stored as `other`; exclude `other` when analyzing tools.

---

## Note on time zones

The `day` field is computed with the **device's local clock** (`SimpleDateFormat("yyyy-MM-dd")` with no fixed zone). A single day groups up to 24 different time zones.

This is acceptable for daily counts, but it rules out any hourly analysis and any exact-day comparison between devices. Day boundaries overlap by ±1 day, and that cannot be undone afterwards.

---

## Integrity summary
For any public figure (README, portfolio, LinkedIn), prefer:
1. **Google Play Console** for "Total installs" and "Ratings".
2. **`daily_active`** for "Active users" (series only exists from 2026-08-17).
3. **`tools_dau`** for "Most popular features", with `tools` as the intensity measure. If only `tools` is available (for example, in a Markdown export generated before it included `tools_dau`), state that the figures are opens, not people.

**Never use `versions_first_seen` as an equivalent of "Users" without explaining the update semantics.**
