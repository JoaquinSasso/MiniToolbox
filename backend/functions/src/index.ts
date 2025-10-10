import { onRequest } from "firebase-functions/v2/https";
import { initializeApp } from "firebase-admin/app";
import {
	getFirestore,
	FieldValue,
	Firestore,
	FieldPath,
} from "firebase-admin/firestore";
import { defineSecret } from "firebase-functions/params";
import { validateBody, type IngestBody } from "./validate.js";

initializeApp();
const db = getFirestore();

const METRICS_API_KEY = defineSecret("METRICS_API_KEY");
const READ_METRICS_API_KEY = defineSecret("READ_METRICS_API_KEY");

// ------------ Helpers ------------
function monthFromDay(day: string): string {
	// "YYYY-MM-DD" -> "YYYY-MM"
	return day.slice(0, 7);
}
function dailyCollection(db: Firestore, month: string) {
	return db.collection("metrics_daily").doc(month).collection("days");
}
function sendJson(res: any, code: number, obj: any) {
	try {
		if (typeof res.setHeader === "function") {
			res.setHeader("Content-Type", "application/json; charset=utf-8");
		}
		if (typeof res.status === "function" && typeof res.send === "function") {
			res.status(code).send(JSON.stringify(obj));
			return;
		}
		res.writeHead?.(code, {
			"Content-Type": "application/json; charset=utf-8",
		});
		res.end?.(JSON.stringify(obj));
	} catch {
		// no-op
	}
}

function readHeader(req: any, name: string): string {
	// Express
	const v1 = req.get?.(name) ?? req.header?.(name);
	if (typeof v1 === "string") return v1;
	// Fetch/Undici
	const v2 =
		req.headers?.get?.(name) ?? req.headers?.[String(name).toLowerCase()];
	if (typeof v2 === "string") return v2;
	return "";
}

function getHeaderApiKey(req: any): string {
	const k1 = readHeader(req, "x-api-key");
	const k2 = readHeader(req, "X-API-Key");
	const auth = readHeader(req, "authorization");
	const m = /^Bearer\s+(.+)$/i.exec(auth || "");
	const bearer = m ? m[1] : "";
	return (k1 || k2 || bearer || "").trim();
}

// --- Tools: legacy route -> canonical route (SIN "tool." aquí) ---
const TOOL_ROUTE_MAP: Record<string, string> = {
	// Ignoradas (no son herramientas)
	"tool.": "",
	tools: "",
	dev: "",

	// tools
	random_color_generator: "random_color",
	group_selector: "group_selector",
	coin_flip: "coin_flip",
	decimal_binary_converter: "decimal_binary",
	text_binary_converter: "text_binary",
	truco_score_board: "truco_scoreboard",
	age_calculator: "age_calculator",
	zodiac_sign: "zodiac_sign",
	pomodoro: "pomodoro",
	bubble_level: "bubble_level",
	porcentaje: "percentage",
	conversor_horas: "time_converter",
	calculadora_de_imc: "bmi_calculator",
	conversor_romanos: "roman_numerals",
	conversor_unidades: "unit_converter",
	generador_contrasena: "password_generator",
	sugeridor_actividades: "activity_suggester",
	generador_nombres: "name_generator",
	generador_qr: "qr_generator",
	generador_vcard: "vcard_generator",
	lorem_ipsum: "lorem_ipsum",
	regla: "ruler",
	medidor_luz: "light_meter",
	linterna: "flashlight",
	rachas: "streaks",
	agua: "water",
	estadisticas_agua: "water_stats",
	tiempo_hasta: "countdown",
	paises_info: "countries_info",
	ruleta_selectora: "selector_wheel",
	adivina_bandera: "guess_flag",
	crear_reunion: "meeting_create",
	reuniones: "meetings",
	detalles_reunion: "meeting_detail",
	editar_gasto: "expense_edit",
	agregar_gasto: "expense_add",
	dados: "dice",
	calculos_rapidos: "quick_calcs",
	frases: "quotes",
	mi_yo_del_multiverso: "multiverse_me",
	adivina_capital: "guess_capital",
	brujula: "compass",
	to_do: "todo",
	eventos: "events",
	interes_compuesto: "compound_interest",
	scoreboard: "scoreboard",
	magnifier: "magnifier",
	ar_ruler: "ar_ruler",
	ruido: "noise",
};

function mergeCounts(
	a: Record<string, number>,
	b: Record<string, number>
): Record<string, number> {
	const out: Record<string, number> = { ...(a || {}) };
	for (const [k, v] of Object.entries(b || {})) {
		out[k] = (out[k] ?? 0) + Number(v || 0);
	}
	return out;
}

// Quita prefijos accidentales en keys de tools dentro de maps guardados
function stripToolKeyInMap(
	map: Record<string, number>
): Record<string, number> {
	const out: Record<string, number> = {};
	for (const [k, v] of Object.entries(map || {})) {
		let kk = k;
		if (kk.startsWith("tools.")) kk = kk.slice(6);
		else if (kk.startsWith("tool.")) kk = kk.slice(5);
		const ck = canonToolKey(kk);
		if (!ck) continue;
		out[ck] = (out[ck] ?? 0) + Number(v || 0);
	}
	return out;
}

// Quita prefijos comunes que puedan venir del cliente
function stripToolPrefix(raw: string): string {
	if (!raw) return raw;
	if (raw.startsWith("tools.")) return raw.slice("tools.".length);
	if (raw.startsWith("tool.")) return raw.slice("tool.".length);
	return raw;
}

// Canonicaliza una clave (route vieja → nueva) y quita prefijos si vinieron en payload
function canonToolKey(raw: string): string | null {
	if (!raw) return null;
	const noPrefix = stripToolPrefix(raw);
	const m = TOOL_ROUTE_MAP[noPrefix];
	if (m === "") return null; // explícitamente ignorada
	return (m || noPrefix).trim();
}

// Re-mapea un objeto {clave: número} a sus claves canónicas, agregando si hay colisiones
function remapToolCounters(
	map: Record<string, number> | undefined
): Record<string, number> {
	const out: Record<string, number> = {};
	for (const [k, v] of Object.entries(map || {})) {
		const ck = canonToolKey(k);
		if (!ck) continue;
		out[ck] = (out[ck] ?? 0) + Number(v || 0);
	}
	return out;
}

// ------------ Date utils ------------
function parseYmd(s: string): Date | null {
	if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return null;
	const [y, m, d] = s.split("-").map((x) => parseInt(x, 10));
	const dt = new Date(Date.UTC(y, m - 1, d));
	return isNaN(dt.getTime()) ? null : dt;
}

function addDays(d: Date, n: number): Date {
	const t = new Date(d.getTime());
	t.setUTCDate(t.getUTCDate() + n);
	return t;
}
function monthsBetween(from: string, to: string): string[] {
	const ym = (s: string) => s.slice(0, 7);
	const out: string[] = [];
	const [fy, fm] = ym(from).split("-").map(Number);
	const [ty, tm] = ym(to).split("-").map(Number);
	let y = fy,
		m = fm;
	while (y < ty || (y === ty && m <= tm)) {
		out.push(`${y}-${String(m).padStart(2, "0")}`);
		m++;
		if (m > 12) {
			m = 1;
			y++;
		}
	}
	return out;
}

// TZ helper para summary
const DEFAULT_TZ = "America/Argentina/San_Juan";
function ymdTZ(d: Date, tz: string): string {
	// 'en-CA' => YYYY-MM-DD
	return new Intl.DateTimeFormat("en-CA", {
		timeZone: tz,
		year: "numeric",
		month: "2-digit",
		day: "2-digit",
	}).format(d);
}

// ------------ Tipos + normalización ------------
type DailyDoc = {
	day: string;
	totals: {
		app_open: number;
		tools: Record<string, number>;
		ads: Record<string, number>;
		versions: Record<string, number>;
		versions_first_seen: Record<string, number>;
		lang_primary: Record<string, number>;
		lang_secondary: Record<string, number>;
		widgets: Record<string, number>;
	};
	meta: {
		updatedAt: string | null;
	};
};

// Para docs con claves planas (ej. "tools.linterna": 3)
function pickPrefix(
	obj: Record<string, any>,
	prefix: string
): Record<string, number> {
	const out: Record<string, number> = {};
	for (const [k, v] of Object.entries(obj)) {
		if (k.startsWith(prefix + ".")) {
			const kk = k.slice(prefix.length + 1);
			out[kk] = Number(v || 0);
		}
	}
	return out;
}

function normDoc(id: string, data: FirebaseFirestore.DocumentData): DailyDoc {
	// Para CADA familia combinamos: (map si existe) + (planos con prefijo),
	// en lugar de elegir solo uno. Así soporta docs "mixtos".

	// --- tools ---
	const toolsNested =
		data.tools && typeof data.tools === "object" ? data.tools : {};
	const toolsFlat = pickPrefix(data, "tools");
	// Limpia prefijos dentro del map y mapea legacy→canónico; también suma flat
	const toolsCanon = mergeCounts(
		stripToolKeyInMap(toolsNested),
		remapToolCounters(toolsFlat)
	);

	// --- ads ---
	const adsNested = data.ads && typeof data.ads === "object" ? data.ads : {};
	const adsFlat = pickPrefix(data, "ads");
	const ads = mergeCounts(adsNested, adsFlat);

	// --- versions ---
	const versionsNested =
		data.versions && typeof data.versions === "object" ? data.versions : {};
	const versionsFlat = pickPrefix(data, "versions");
	const versions = mergeCounts(versionsNested, versionsFlat);

	// --- versions_first_seen ---
	const vfsNested =
		data.versions_first_seen && typeof data.versions_first_seen === "object"
			? data.versions_first_seen
			: {};
	const vfsFlat = pickPrefix(data, "versions_first_seen");
	const versions_first_seen = mergeCounts(vfsNested, vfsFlat);

	// --- widgets ---
	const widgetsNested =
		data.widgets && typeof data.widgets === "object" ? data.widgets : {};
	const widgetsFlat = pickPrefix(data, "widgets");
	const widgets = mergeCounts(widgetsNested, widgetsFlat);

	// --- lang ---
	const langNested =
		data.lang && typeof data.lang === "object" ? data.lang : {};
	const lang_primary = mergeCounts(
		langNested.primary || {},
		pickPrefix(data, "lang.primary")
	);
	const lang_secondary = mergeCounts(
		langNested.secondary || {},
		pickPrefix(data, "lang.secondary")
	);

	// --- totals.app_open ---
	const appOpenNested = Number((data.totals && data.totals.app_open) || 0);
	const appOpenFlat = Number((data as any)["totals.app_open"] || 0);
	const app_open = appOpenNested + appOpenFlat;

	// --- meta.updatedAt ---
	const updatedAtNested = (data as any)?.meta?.updatedAt;
	const updatedAtFlat = (data as any)["meta.updatedAt"];
	const updatedAt: any = updatedAtNested ?? updatedAtFlat;

	return {
		day: id,
		totals: {
			app_open,
			tools: toolsCanon,
			ads,
			versions,
			versions_first_seen,
			lang_primary,
			lang_secondary,
			widgets,
		},
		meta: {
			updatedAt: updatedAt?.toDate
				? updatedAt.toDate().toISOString()
				: updatedAt?._seconds
				? new Date(updatedAt._seconds * 1000).toISOString()
				: null,
		},
	};
}

async function fetchDailyRange(from: string, to: string): Promise<DailyDoc[]> {
	const months = monthsBetween(from, to);
	const docs: DailyDoc[] = [];
	for (const month of months) {
		const start = month === from.slice(0, 7) ? from : `${month}-01`;
		const end = month === to.slice(0, 7) ? to : `${month}-31`;
		const snap = await dailyCollection(db, month)
			.orderBy(FieldPath.documentId())
			.startAt(start)
			.endAt(end)
			.get();

		snap.docs.forEach((d) => docs.push(normDoc(d.id, d.data())));
	}
	docs.sort((a, b) => (a.day < b.day ? -1 : a.day > b.day ? 1 : 0));
	return docs;
}

function sumMap(dst: Record<string, number>, src: Record<string, number>) {
	for (const [k, v] of Object.entries(src ?? {})) {
		dst[k] = (dst[k] ?? 0) + Number(v ?? 0);
	}
}
function topK(map: Record<string, number>, k: number) {
	return Object.entries(map)
		.sort((a, b) => b[1] - a[1])
		.slice(0, k);
}

// ------------ Functions ------------
export const ingest = onRequest(
	{ secrets: [METRICS_API_KEY] },
	async (req, res) => {
		try {
			if (req.method !== "POST") {
				sendJson(res, 405, { ok: false, error: "method_not_allowed" });
				return;
			}
			const apiKey = getHeaderApiKey(req);
			const expected = (METRICS_API_KEY.value() || "").trim();
			if (!expected || apiKey !== expected) {
				sendJson(res, 401, { ok: false, error: "unauthorized" });
				return;
			}

			const parsed = validateBody(req.body);
			if (!parsed.ok) {
				sendJson(res, 400, { ok: false, error: parsed.error ?? "invalid" });
				return;
			}
			const body = parsed.data as IngestBody;

			const batchRef = db
				.collection("metrics_ingest_batches")
				.doc(body.batch_id);

			let totalItems = 0;
			let totalOpens = 0;

			await db.runTransaction(async (tx) => {
				const existsSnap = await tx.get(batchRef);
				if (existsSnap.exists) return;

				tx.create(batchRef, {
					seenAt: FieldValue.serverTimestamp(),
					platform: body.platform,
					app_version: body.app_version,
					items: (body.items ?? []).length,
				});

				for (const it of body.items) {
					totalItems += 1;
					const month = monthFromDay(it.day);
					const dayRef = dailyCollection(db, month).doc(it.day);

					const updates: Record<string, any> = {
						"meta.updatedAt": FieldValue.serverTimestamp(),
						[`meta.seenVersions.${body.platform}.${body.app_version}`]: true,
					};
					const inc = (n: number) => FieldValue.increment(n);

					// pares de FieldPath para versiones (evita dividir "1.1.1")
					const verPairs: Array<[FieldPath, any]> = [];
					const vfsPairs: Array<[FieldPath, any]> = [];

					if ((it.app_open ?? 0) > 0) {
						updates["totals.app_open"] = inc(it.app_open ?? 0);
						totalOpens += it.app_open ?? 0;
					}

					if (it.tools) {
						const tmp: Record<string, number> = {};
						for (const [rawKey, v] of Object.entries(it.tools)) {
							const ck = canonToolKey(rawKey); // quita prefijos y mapea
							if (!ck) continue;
							const n = Number(v || 0);
							if (n > 0) tmp[ck] = (tmp[ck] || 0) + n;
						}
						for (const [ck, n] of Object.entries(tmp)) {
							updates[`tools.${ck}`] = inc(n); // siempre tools.<canónica>
						}
					}

					if (it.ads) {
						for (const [k, v] of Object.entries(it.ads)) {
							const n = Number(v || 0);
							if (n > 0) updates[`ads.${k}`] = inc(n);
						}
					}

					if ((it as any).versions) {
						for (const [ver, v] of Object.entries(
							(it as any).versions as Record<string, number>
						)) {
							const n = Number(v || 0);
							if (n > 0)
								verPairs.push([new FieldPath("versions", ver), inc(n)]);
						}
					}
					if ((it as any).versions_first_seen) {
						for (const [ver, v] of Object.entries(
							(it as any).versions_first_seen as Record<string, number>
						)) {
							const n = Number(v || 0);
							if (n > 0)
								vfsPairs.push([
									new FieldPath("versions_first_seen", ver),
									inc(n),
								]);
						}
					}

					const lp = (it as any).lang_primary as
						| Record<string, number>
						| undefined;
					if (lp) {
						for (const [lang, v] of Object.entries(lp)) {
							const n = Number(v || 0);
							if (n > 0) updates[`lang.primary.${lang}`] = inc(n);
						}
					}
					const ls = (it as any).lang_secondary as
						| Record<string, number>
						| undefined;
					if (ls) {
						for (const [lang, v] of Object.entries(ls)) {
							const n = Number(v || 0);
							if (n > 0) updates[`lang.secondary.${lang}`] = inc(n);
						}
					}

					const w = (it as any).widgets as Record<string, number> | undefined;
					if (w) {
						for (const [kind, v] of Object.entries(w)) {
							const n = Number(v || 0);
							if (n > 0) updates[`widgets.${kind}`] = inc(n);
						}
					}

					// Asegura doc y escribe
					tx.set(dayRef, {}, { merge: true });

					if (Object.keys(updates).length) {
						tx.update(dayRef, updates); // app_open, tools, ads, lang, widgets
					}
					// versions y versions_first_seen con FieldPath (sin spread)
					for (const [path, val] of verPairs) {
						tx.update(dayRef, path, val);
					}
					for (const [path, val] of vfsPairs) {
						tx.update(dayRef, path, val);
					}
				}
			});

			await db.collection("metrics_ingest_logs").doc().set({
				at: FieldValue.serverTimestamp(),
				batch_id: body.batch_id,
				platform: body.platform,
				app_version: body.app_version,
				total_items: totalItems,
				total_app_open_delta: totalOpens,
			});

			sendJson(res, 200, { ok: true });
			return;
		} catch (e) {
			console.error("ingest_error", e);
			sendJson(res, 500, { ok: false, error: "internal" });
			return;
		}
	}
);

export const metricsDaily = onRequest(
	{ secrets: [READ_METRICS_API_KEY, METRICS_API_KEY] },
	async (req, res) => {
		try {
			if (req.method !== "GET") {
				sendJson(res, 405, { ok: false, error: "method_not_allowed" });
				return;
			}
			const apiKey = getHeaderApiKey(req);
			const readK = (READ_METRICS_API_KEY.value() || "").trim();
			const writeK = (METRICS_API_KEY.value() || "").trim();
			// Lectura acepta READ o WRITE
			if (!(apiKey && (apiKey === readK || apiKey === writeK))) {
				sendJson(res, 401, { ok: false, error: "unauthorized" });
				return;
			}

			const from = String(req.query.from || "").trim();
			const to = String(req.query.to || "").trim();
			const df = parseYmd(from);
			const dt = parseYmd(to);
			if (!df || !dt) {
				sendJson(res, 400, { ok: false, error: "bad_range" });
				return;
			}
			if (df.getTime() > dt.getTime()) {
				sendJson(res, 400, { ok: false, error: "bad_range_order" });
				return;
			}
			const max = addDays(df, 400);
			if (dt.getTime() > max.getTime()) {
				sendJson(res, 400, { ok: false, error: "range_too_large" });
				return;
			}

			const rows = await fetchDailyRange(from, to);
			sendJson(res, 200, rows);
			return;
		} catch (e) {
			console.error("metricsDaily_error", e);
			sendJson(res, 500, { ok: false, error: "internal" });
			return;
		}
	}
);

export const metricsSummary = onRequest(
	{ secrets: [READ_METRICS_API_KEY, METRICS_API_KEY] },
	async (req, res) => {
		try {
			if (req.method !== "GET") {
				sendJson(res, 405, { ok: false, error: "method_not_allowed" });
				return;
			}
			const apiKey = getHeaderApiKey(req);
			const readK = (READ_METRICS_API_KEY.value() || "").trim();
			const writeK = (METRICS_API_KEY.value() || "").trim();
			// Lectura acepta READ o WRITE
			if (!(apiKey && (apiKey === readK || apiKey === writeK))) {
				sendJson(res, 401, { ok: false, error: "unauthorized" });
				return;
			}

			const last = Math.max(1, Math.min(400, Number(req.query.last ?? 30)));
			const tz = String(req.query.tz || DEFAULT_TZ);
			const now = new Date();
			const to = ymdTZ(now, tz);
			const from = ymdTZ(addDays(now, -(last - 1)), tz);

			const rows = await fetchDailyRange(from, to);

			let total_app_open = 0;
			const agg_tools: Record<string, number> = {};
			const agg_ads: Record<string, number> = {};
			const agg_versions: Record<string, number> = {};
			const agg_versions_fs: Record<string, number> = {};
			const agg_lang_primary: Record<string, number> = {};
			const agg_lang_secondary: Record<string, number> = {};
			const agg_widgets: Record<string, number> = {};

			for (const r of rows) {
				total_app_open += Number(r.totals.app_open ?? 0);
				sumMap(agg_tools, r.totals.tools);
				sumMap(agg_ads, r.totals.ads);
				sumMap(agg_versions, r.totals.versions);
				sumMap(agg_versions_fs, r.totals.versions_first_seen);
				sumMap(agg_lang_primary, r.totals.lang_primary);
				sumMap(agg_lang_secondary, r.totals.lang_secondary);
				sumMap(agg_widgets, r.totals.widgets);
			}

			const payload = {
				range: { from, to, days: rows.length },
				total_app_open,
				top: {
					tools: topK(agg_tools, 10),
					ads: topK(agg_ads, 10),
					versions: topK(agg_versions, 10),
					versions_first_seen: topK(agg_versions_fs, 10),
					lang_primary: topK(agg_lang_primary, 10),
					lang_secondary: topK(agg_lang_secondary, 10),
					widgets: topK(agg_widgets, 10),
				},
			};
			sendJson(res, 200, payload);
			return;
		} catch (e) {
			console.error("metricsSummary_error", e);
			sendJson(res, 500, { ok: false, error: "internal" });
			return;
		}
	}
);
