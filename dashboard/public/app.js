// ---- Settings ----
const LS_BASE = "metrics_api_base";
const LS_KEY = "metrics_api_key";
const TOP_K = 5;

function getSettings() {
	return {
		base: localStorage.getItem(LS_BASE) || "",
		key: localStorage.getItem(LS_KEY) || "",
	};
}
function setSettings({ base, key }) {
	if (base != null) localStorage.setItem(LS_BASE, base.trim());
	if (key != null) localStorage.setItem(LS_KEY, key.trim());
}

// ---- Helpers ----
const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

function serialize(obj) {
	return JSON.stringify(obj ?? {});
}
function toCSV(rows) {
	const header = [
		"day",
		"app_open",
		"tools",
		"ads",
		"versions",
		"versions_first_seen",
		"lang_primary",
		"lang_secondary",
		"widgets",
		"updatedAt",
	];
	const lines = [header.join(",")];
	for (const r of rows) {
		lines.push(
			[
				r.day,
				r.totals.app_open ?? 0,
				JSON.stringify(r.totals.tools ?? {}),
				JSON.stringify(r.totals.ads ?? {}),
				JSON.stringify(r.totals.versions ?? {}),
				JSON.stringify(r.totals.versions_first_seen ?? {}),
				JSON.stringify(r.totals.lang_primary ?? {}),
				JSON.stringify(r.totals.lang_secondary ?? {}),
				JSON.stringify(r.totals.widgets ?? {}),
				r.meta.updatedAt ?? "",
			]
				.map((s) => `"${String(s).replace(/"/g, '""')}"`)
				.join(",")
		);
	}
	return lines.join("\n");
}
function download(name, text) {
	const blob = new Blob([text], { type: "text/csv;charset=utf-8" });
	const a = document.createElement("a");
	a.href = URL.createObjectURL(blob);
	a.download = name;
	a.click();
	URL.revokeObjectURL(a.href);
}

// ---- API ----
// Resuelve la base efectiva: en *.web.app/*.firebaseapp.com fuerza '/api' (rewrites => sin CORS).
function resolveApiBase() {
	const s = getSettings();
	const onHosting =
		location.origin.endsWith(".web.app") ||
		location.origin.endsWith(".firebaseapp.com");
	const onEmu = location.hostname === "localhost" && location.port === "5000";

	// Base por defecto si no hay setting
	const defaultBase =
		onHosting || onEmu
			? "/api"
			: "https://us-central1-minitoolbox-7ab7d.cloudfunctions.net";

	let base = (s.base || "").trim() || defaultBase;

	// Si estamos en Hosting, ignoramos bases absolutas para evitar CORS y usamos siempre '/api'
	if (onHosting && /^https?:\/\//i.test(base)) {
		base = "/api";
	}
	return base;
}

async function apiGet(path, params = {}) {
	const { key } = getSettings();
	if (!key) throw new Error("missing_settings");

	const API_BASE = resolveApiBase();
	const cleanBase = API_BASE.replace(/\/$/, "");
	const cleanPath = `/${String(path).replace(/^\/+/, "")}`;
	const qs = new URLSearchParams(params).toString();
	const url = `${cleanBase}${cleanPath}${qs ? `?${qs}` : ""}`;

	const rsp = await fetch(url, {
		headers: {
			"X-API-Key": key,
			"Content-Type": "application/json",
		},
	});
	if (rsp.status === 401) throw new Error("unauthorized");
	if (!rsp.ok) throw new Error(`http_${rsp.status}`);
	return rsp.json();
}

// ---- Date range UI ----
function ymd(d) {
	const y = d.getFullYear();
	const m = String(d.getMonth() + 1).padStart(2, "0");
	const da = String(d.getDate()).padStart(2, "0");
	return `${y}-${m}-${da}`;
}
function addDays(d, n) {
	const t = new Date(d);
	t.setDate(t.getDate() + n);
	return t;
}

function showCustomDates(show) {
	$("#fromWrap").classList.toggle("d-none", !show);
	$("#toWrap").classList.toggle("d-none", !show);
}

$("#rangePreset").addEventListener("change", (e) => {
	showCustomDates(e.target.value === "custom");
});

// Altura fija para todos los charts (evita el bucle de resize)
const CHART_HEIGHT = 280;

function lockChartHeights() {
  const ids = [
    "chartOpens","chartTools","chartAds","chartVersions","chartLang",
    "pieTools","pieAds","pieVersions","pieVersionsFS","pieWidgets"
  ];
  ids.forEach(id => {
    const el = document.getElementById(id);
    if (el) {
      el.style.height = CHART_HEIGHT + "px";
      el.style.maxHeight = CHART_HEIGHT + "px";
      el.style.width = "100%";           // importante para el responsive
      el.style.display = "block";        // evita que el canvas herede inline sizing raro
    }
  });
}


// ---- Charts state ----
let chOpens,
	chTools,
	chAds,
	chVersions,
	chLang,
	chPieTools,
	chPieAds,
	chPieVersions,
	chPieVersionsFS,
	chPieWidgets;

function destroyCharts() {
	[
		chOpens,
		chTools,
		chAds,
		chVersions,
		chLang,
		chPieTools,
		chPieAds,
		chPieVersions,
		chPieVersionsFS,
		chPieWidgets,
	]
		.filter(Boolean)
		.forEach((c) => c.destroy());
	chOpens =
		chTools =
		chAds =
		chVersions =
		chLang =
		chPieTools =
		chPieAds =
		chPieVersions =
		chPieVersionsFS =
		chPieWidgets =
			null;
}

// ---- Draw charts ----
function buildLineOpens(labels, series) {
	const ctx = $("#chartOpens");
	chOpens = new Chart(ctx, {
		type: "line",
		data: {
			labels,
			datasets: [{ label: "App opens", data: series, tension: 0.25 }],
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			animation: false,
			resizeDelay: 150,
		},
	});
}
function stackSeriesByTopK(rows, getMap, topK = 5) {
	// Determinar topK sobre el rango completo
	const agg = {};
	for (const r of rows) {
		const m = getMap(r) || {};
		for (const [k, v] of Object.entries(m)) agg[k] = (agg[k] ?? 0) + v;
	}
	const keys = Object.entries(agg)
		.sort((a, b) => b[1] - a[1])
		.slice(0, topK)
		.map(([k]) => k);

	const labels = rows.map((r) => r.day);
	const datasets = keys.map((k) => ({
		label: k,
		data: rows.map((r) => getMap(r)?.[k] ?? 0),
		stack: "stack",
	}));
	// Otros
	const others = rows.map((r) => {
		const m = getMap(r) || {};
		let sum = 0;
		for (const [kk, vv] of Object.entries(m)) {
			if (!keys.includes(kk)) sum += vv;
		}
		return sum;
	});
	if (others.some((x) => x > 0)) {
		datasets.push({ label: "Otros", data: others, stack: "stack" });
	}
	return { labels, datasets };
}
function buildStackedBar(canvasSel, rows, getMap, title) {
	const { labels, datasets } = stackSeriesByTopK(rows, getMap, TOP_K);
	const ctx = $(canvasSel);
	return new Chart(ctx, {
		type: "bar",
		data: { labels, datasets },
		options: {
			responsive: true,
			maintainAspectRatio: false,
			animation: false,
			resizeDelay: 150,
			scales: {
				x: { stacked: true },
				y: { stacked: true },
			},
			plugins: { legend: { position: "bottom" } },
		},
	});
}

function buildPie(canvasSel, map) {
	const entries = Object.entries(map || {});
	if (!entries.length) {
		const ctx = $(canvasSel).getContext("2d");
		ctx.font = "14px system-ui";
		ctx.fillText("Sin datos", 10, 20);
		return null;
	}
	const labels = entries.map(([k]) => k);
	const data = entries.map(([, v]) => v);
	const ctx = $(canvasSel);
	return new Chart(ctx, {
		type: "pie",
		data: { labels, datasets: [{ data }] },
		options: {
			responsive: true,
			maintainAspectRatio: false,
			animation: false,
			resizeDelay: 150,
		},
	});
}

// ---- Table ----
function fillTable(rows) {
	const tbody = $("#tblDaily tbody");
	tbody.innerHTML = "";
	for (const r of rows) {
		const tr = document.createElement("tr");
		tr.innerHTML = `
      <td>${r.day}</td>
      <td class="text-end">${r.totals.app_open ?? 0}</td>
      <td><code>${serialize(r.totals.tools)}</code></td>
      <td><code>${serialize(r.totals.ads)}</code></td>
      <td><code>${serialize(r.totals.versions)}</code></td>
      <td><code>${serialize(r.totals.versions_first_seen)}</code></td>
      <td><code>${serialize(r.totals.lang_primary)}</code></td>
      <td><code>${serialize(r.totals.lang_secondary)}</code></td>
      <td><code>${serialize(r.totals.widgets)}</code></td>
      <td><small>${r.meta.updatedAt ?? ""}</small></td>
    `;
		tbody.appendChild(tr);
	}
}

// ---- UI wiring ----
let settingsModal;
document.addEventListener("DOMContentLoaded", async () => {
	settingsModal = new bootstrap.Modal("#settingsModal");
	$("#btnSettings").addEventListener("click", () => {
		const s = getSettings();
		$("#inpBaseUrl").value = s.base;
		$("#inpApiKey").value = s.key;
		settingsModal.show();
	});
	$("#btnSaveSettings").addEventListener("click", () => {
		setSettings({ base: $("#inpBaseUrl").value, key: $("#inpApiKey").value });
		settingsModal.hide();
	});
	$("#btnReload").addEventListener("click", () => loadRange());

	$("#btnApply").addEventListener("click", () => loadRange());

	// defaults
	const to = new Date();
	const from = addDays(to, -29);
	$("#fromDate").value = ymd(from);
	$("#toDate").value = ymd(to);

	// primera carga
	loadRange();
});

async function loadRange() {
	try {
		const preset = $("#rangePreset").value;
		let from, to;
		if (preset === "custom") {
			showCustomDates(true);
			from = $("#fromDate").value;
			to = $("#toDate").value;
		} else {
			showCustomDates(false);
			const n = Number(preset);
			const end = new Date();
			const start = addDays(end, -(n - 1));
			from = ymd(start);
			to = ymd(end);
		}

		destroyCharts();
		lockChartHeights();

		// summary
		const summary = await apiGet("metricsSummary", {
			last: preset === "custom" ? 30 : Number(preset),
		});
		$("#cardOpens").textContent = summary.total_app_open ?? 0;
		const newTotal = (summary.top?.versions_first_seen ?? []).reduce(
			(acc, [, v]) => acc + v,
			0
		);
		$("#cardNew").textContent = newTotal ?? 0;
		const topVer = (summary.top?.versions ?? [])[0];
		$("#cardTopVer").textContent = topVer ? topVer[0] : "–";
		$("#cardTopVerVal").textContent = topVer ? topVer[1] : "";
		const topLang = (summary.top?.lang_primary ?? [])[0];
		$("#cardTopLang").textContent = topLang ? topLang[0] : "–";
		$("#cardTopLangVal").textContent = topLang ? topLang[1] : "";

		// daily
		const daily = await apiGet("metricsDaily", { from, to });

		// charts
		buildLineOpens(
			daily.map((d) => d.day),
			daily.map((d) => d.totals.app_open ?? 0)
		);
		chTools = buildStackedBar(
			"#chartTools",
			daily,
			(r) => r.totals.tools,
			"Tools"
		);
		chAds = buildStackedBar("#chartAds", daily, (r) => r.totals.ads, "Ads");
		chVersions = buildStackedBar(
			"#chartVersions",
			daily,
			(r) => r.totals.versions,
			"Versions"
		);
		chLang = buildStackedBar(
			"#chartLang",
			daily,
			(r) => r.totals.lang_primary,
			"Lang"
		);

		// pies (distribuciones en el rango)
		const agg = {
			tools: {},
			ads: {},
			versions: {},
			versions_first_seen: {},
			widgets: {},
			lang_primary: {},
		};
		for (const r of daily) {
			sumTo(agg.tools, r.totals.tools);
			sumTo(agg.ads, r.totals.ads);
			sumTo(agg.versions, r.totals.versions);
			sumTo(agg.versions_first_seen, r.totals.versions_first_seen);
			sumTo(agg.widgets, r.totals.widgets);
			sumTo(agg.lang_primary, r.totals.lang_primary);
		}
		chPieTools = buildPie("#pieTools", agg.tools);
		chPieAds = buildPie("#pieAds", agg.ads);
		chPieVersions = buildPie("#pieVersions", agg.versions);
		chPieVersionsFS = buildPie("#pieVersionsFS", agg.versions_first_seen);
		chPieWidgets = buildPie("#pieWidgets", agg.widgets);

		// tabla
		fillTable(daily);

		// export
		$("#btnExport").onclick = () => {
			const csv = toCSV(daily);
			download(`metrics_${from}_${to}.csv`, csv);
		};
	} catch (e) {
		console.error(e);
		if (
			String(e.message).includes("missing_settings") ||
			String(e.message).includes("unauthorized")
		) {
			// Abrir settings
			const s = getSettings();
			$("#inpBaseUrl").value = s.base;
			$("#inpApiKey").value = s.key;
			settingsModal.show();
			alert("Configura API Key (y Base URL solo si lo necesitás en dev).");
		} else {
			alert(`Error cargando datos: ${e.message}`);
		}
	}
}

function sumTo(dst, src) {
	for (const [k, v] of Object.entries(src || {})) {
		dst[k] = (dst[k] ?? 0) + Number(v ?? 0);
	}
}
