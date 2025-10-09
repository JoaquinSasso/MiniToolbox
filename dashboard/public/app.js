// ---- Settings ----
const LS_BASE = "metrics_api_base";
const LS_KEY = "metrics_api_key";
const TOP_K = 5;

// ---- Theme ----
const LS_THEME = "metrics_theme";

// ---- Chart Instances ----
let chOpens,
	chTools,
	chAds,
	chVersions,
	chLang,
	chBarTools,
	chPieAds,
	chPieVersions,
	chPieVersionsFS,
	chPieWidgets;

/**
 * Aplica el tema (claro u oscuro) a la página y a los gráficos.
 * @param {'light' | 'dark'} theme - El tema a aplicar.
 */
function applyTheme(theme) {
	// 1. Aplica el tema al HTML para que el CSS de Bootstrap reaccione
	document.documentElement.setAttribute("data-bs-theme", theme);

	// 2. Define los colores para los gráficos según el tema
	const isDark = theme === "dark";
	const gridColor = isDark ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.1)";
	const textColor = isDark ? "#dee2e6" : "#212529";

	// 3. Actualiza los colores por defecto para TODOS los futuros gráficos de Chart.js
	Chart.defaults.color = textColor;
	Chart.defaults.borderColor = gridColor;
}

// ---- Helpers ----
const $ = (sel) => document.querySelector(sel);

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
				serialize(r.totals.tools),
				serialize(r.totals.ads),
				serialize(r.totals.versions),
				serialize(r.totals.versions_first_seen),
				serialize(r.totals.lang_primary),
				serialize(r.totals.lang_secondary),
				serialize(r.totals.widgets),
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
// ... (El resto de tus funciones como getSettings, setSettings, resolveApiBase, apiGet, ymd, addDays, etc. van aquí sin cambios)
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
function resolveApiBase() {
	const s = getSettings();
	const onHosting =
		location.origin.endsWith(".web.app") ||
		location.origin.endsWith(".firebaseapp.com");
	const onEmu = location.hostname === "localhost" && location.port === "5000";
	const defaultBase =
		onHosting || onEmu
			? "/api"
			: "https://us-central1-minitoolbox-7ab7d.cloudfunctions.net";
	let base = (s.base || "").trim() || defaultBase;
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

const CHART_HEIGHT = 280;

function lockChartHeights() {
	const ids = [
		"chartOpens",
		"chartTools",
		"chartAds",
		"chartVersions",
		"chartLang",
		"pieAds",
		"pieVersions",
		"pieVersionsFS",
		"pieWidgets",
		"barTools",
	];
	ids.forEach((id) => {
		const el = document.getElementById(id);
		if (el) {
			if (id === "barTools") {
				el.parentElement.style.height = "800px";
			} else {
				el.style.height = CHART_HEIGHT + "px";
				el.style.maxHeight = CHART_HEIGHT + "px";
			}
			el.style.width = "100%";
			el.style.display = "block";
		}
	});
}

function destroyCharts() {
	const charts = [
		chOpens,
		chTools,
		chAds,
		chVersions,
		chLang,
		chBarTools,
		chPieAds,
		chPieVersions,
		chPieVersionsFS,
		chPieWidgets,
	];
	charts.filter(Boolean).forEach((c) => c.destroy());
}

// ---- Draw charts ----
// ... (Todas tus funciones build* como buildLineOpens, buildStackedBar, etc. van aquí sin cambios)
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
function buildStackedBar(canvasSel, rows, getMap) {
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
function buildHorizontalBarChart(canvasSel, map, title) {
	const entries = Object.entries(map || {});
	if (!entries.length) {
		const ctx = $(canvasSel).getContext("2d");
		ctx.font = "14px system-ui";
		ctx.fillText("Sin datos", 10, 20);
		return null;
	}

	entries.sort((a, b) => a[1] - b[1]);

	const labels = entries.map(([k]) => k);
	const data = entries.map(([, v]) => v);

	const colors = [
		"rgba(255, 99, 132, 0.5)",
		"rgba(54, 162, 235, 0.5)",
		"rgba(255, 206, 86, 0.5)",
		"rgba(75, 192, 192, 0.5)",
		"rgba(153, 102, 255, 0.5)",
		"rgba(255, 159, 64, 0.5)",
		"rgba(255, 99, 132, 0.8)",
		"rgba(54, 162, 235, 0.8)",
		"rgba(255, 206, 86, 0.8)",
		"rgba(75, 192, 192, 0.8)",
		"rgba(153, 102, 255, 0.8)",
		"rgba(255, 159, 64, 0.8)",
	];

	const backgroundColors = data.map((_, i) => colors[i % colors.length]);

	const ctx = $(canvasSel);
	return new Chart(ctx, {
		type: "bar",
		data: {
			labels: labels,
			datasets: [
				{
					label: title,
					data: data,
					backgroundColor: backgroundColors,
					borderColor: backgroundColors.map((color) =>
						color.replace("0.5", "1").replace("0.8", "1")
					),
					borderWidth: 1,
				},
			],
		},
		options: {
			indexAxis: "y",
			responsive: true,
			maintainAspectRatio: false,
			scales: { x: { beginAtZero: true } },
			plugins: { legend: { display: false } },
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
function sumTo(dst, src) {
	for (const [k, v] of Object.entries(src || {})) {
		dst[k] = (dst[k] ?? 0) + Number(v ?? 0);
	}
}

/**
 * Carga todos los datos y renderiza la UI.
 */
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

		// Summary
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

		// Daily
		const daily = await apiGet("metricsDaily", { from, to });

		// Charts
		buildLineOpens(
			daily.map((d) => d.day),
			daily.map((d) => d.totals.app_open ?? 0)
		);
		chTools = buildStackedBar("#chartTools", daily, (r) => r.totals.tools);
		chAds = buildStackedBar("#chartAds", daily, (r) => r.totals.ads);
		chVersions = buildStackedBar(
			"#chartVersions",
			daily,
			(r) => r.totals.versions
		);
		chLang = buildStackedBar("#chartLang", daily, (r) => r.totals.lang_primary);

		// Pies & Bars
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
		chBarTools = buildHorizontalBarChart("#barTools", agg.tools, "Tools");
		chPieAds = buildPie("#pieAds", agg.ads);
		chPieVersions = buildPie("#pieVersions", agg.versions);
		chPieVersionsFS = buildPie("#pieVersionsFS", agg.versions_first_seen);
		chPieWidgets = buildPie("#pieWidgets", agg.widgets);

		// Tabla
		fillTable(daily);

		// Export
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
			const s = getSettings();
			$("#inpBaseUrl").value = s.base;
			$("#inpApiKey").value = s.key;
			new bootstrap.Modal("#settingsModal").show();
			alert("Configura API Key (y Base URL solo si lo necesitás en dev).");
		} else {
			alert(`Error cargando datos: ${e.message}`);
		}
	}
}

// ---- UI wiring ----
document.addEventListener("DOMContentLoaded", () => {
	console.log("✔️ Página cargada. Iniciando setup del tema.");
	// --- Theme Switcher ---
	const themeSwitch = $("#themeSwitch");

	// 1. Determinar el tema inicial
	const storedTheme = localStorage.getItem(LS_THEME);
	const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
	const initialTheme = storedTheme || (prefersDark ? "dark" : "light");

	console.log("Tema inicial detectado:", initialTheme);

	// 2. Aplicar tema inicial y estado del switch
	themeSwitch.checked = initialTheme === "dark";
	applyTheme(initialTheme);

	// 3. Añadir el listener para futuros cambios
	themeSwitch.addEventListener("change", () => {
		console.log("🔘 Switch pulsado!");
		const newTheme = themeSwitch.checked ? "dark" : "light";
		localStorage.setItem(LS_THEME, newTheme);
		applyTheme(newTheme);
		loadRange(); // Vuelve a cargar los graficos con el nuevo tema
	});

	// --- Resto de la configuración de la UI ---
	const settingsModal = new bootstrap.Modal("#settingsModal");
	$("#btnSettings").addEventListener("click", () => {
		const s = getSettings();
		$("#inpBaseUrl").value = s.base;
		$("#inpApiKey").value = s.key;
		settingsModal.show();
	});
	$("#btnSaveSettings").addEventListener("click", () => {
		setSettings({ base: $("#inpBaseUrl").value, key: $("#inpApiKey").value });
		settingsModal.hide();
		loadRange();
	});

	$("#btnReload").addEventListener("click", loadRange);
	$("#btnApply").addEventListener("click", loadRange);

	// Valores por defecto para el rango de fechas
	const to = new Date();
	const from = addDays(to, -29);
	$("#fromDate").value = ymd(from);
	$("#toDate").value = ymd(to);

	// Carga inicial de datos
	loadRange();
});
