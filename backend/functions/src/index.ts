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
import { getAppCheck } from "firebase-admin/app-check";
import * as logger from "firebase-functions/logger";

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

/**
 * Verifica el token de Firebase App Check enviado por la app.
 * Devuelve false tanto si no vino token como si es invalido.
 */
async function verifyAppCheck(req: any): Promise<boolean> {
	const token = readHeader(req, "X-Firebase-AppCheck");
	if (!token) return false;
	try {
		await getAppCheck().verifyToken(token);
		return true;
	} catch (e) {
		logger.warn("appcheck_invalid", { reason: (e as Error).message });
		return false;
	}
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
	// La ruta cambio a "pomodoro_list" en el cliente; se mapea a la clave historica
	// para no partir la serie en dos.
	pomodoro_list: "pomodoro",
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
	b: Record<string, number>,
): Record<string, number> {
	const out: Record<string, number> = { ...(a || {}) };
	for (const [k, v] of Object.entries(b || {})) {
		out[k] = (out[k] ?? 0) + Number(v || 0);
	}
	return out;
}

/**
 * Herramientas conocidas. Cualquier clave fuera de este conjunto se agrupa en
 * UNKNOWN_TOOL_BUCKET en lugar de crear un campo nuevo en el documento diario.
 *
 * Motivo: los documentos de metrics_daily tienen un limite de tamano y cada clave nueva
 * es un campo mas. Sin este tope, una version del cliente con un bug de claves puede
 * hacer crecer el documento hasta romperlo, y ahi se pierde el dia entero.
 *
 * Costo asumido: al agregar una herramienta nueva a la app hay que sumarla aca o sus
 * datos caen en "other" hasta el proximo deploy. El log "ingest_unknown_tools" avisa.
 */
const EXTRA_KNOWN_TOOLS = ["minesweeper", "about"];

const KNOWN_TOOLS = new Set<string>([
	...Object.values(TOOL_ROUTE_MAP).filter((v) => v),
	...EXTRA_KNOWN_TOOLS,
]);

const UNKNOWN_TOOL_BUCKET = "other";

/** Origenes de apertura aceptados. Debe coincidir con MetricsSource del cliente. */
const KNOWN_SOURCES = new Set(["nav", "notification", "widget", "shortcut", "unknown"]);
const UNKNOWN_SOURCE = "unknown";

/** Categorias de retencion aceptadas. Debe coincidir con RetentionBuckets del cliente. */
const KNOWN_AGE = new Set(["age0_6", "age7_29", "age30_89", "age90_179", "age180p"]);
const KNOWN_INTENSITY = new Set(["d1", "d2_3", "d4_7", "d8_14", "d15_28"]);

function bucketToolKey(ck: string, seenUnknown: Set<string>): string {
	if (KNOWN_TOOLS.has(ck)) return ck;
	seenUnknown.add(ck);
	return UNKNOWN_TOOL_BUCKET;
}

// Quita prefijos accidentales en keys de tools dentro de maps guardados
function stripToolKeyInMap(
	map: Record<string, number>,
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
	map: Record<string, number> | undefined,
): Record<string, number> {
	const out: Record<string, number> = {};
	for (const [k, v] of Object.entries(map || {})) {
		const ck = canonToolKey(k);
		if (!ck) continue;
		out[ck] = (out[ck] ?? 0) + Number(v || 0);
	}
	return out;
}

/** Días que se conservan los documentos operativos de ingesta. */
const RETENTION_DAYS = 90;

/**
 * Momento de expiración para la política TTL de Firestore.
 * Estas colecciones crecen un documento por lote y sin esto no dejan de crecer nunca.
 */
function expiryTimestamp(days: number): Date {
	return new Date(Date.now() + days * 24 * 60 * 60 * 1000);
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
		daily_active: number;
		tools: Record<string, number>;
		/** Dispositivos-dia por herramienta. Denominador de tools. */
		tools_dau: Record<string, number>;
		/** Aperturas por herramienta y origen: tool_entry[tool][source]. */
		tool_entry: Record<string, Record<string, number>>;
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
	prefix: string,
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
		remapToolCounters(toolsFlat),
	);

	// --- tools_dau ---
	const toolsDauNested =
		data.tools_dau && typeof data.tools_dau === "object" ? data.tools_dau : {};
	const toolsDauFlat = pickPrefix(data, "tools_dau");
	const tools_dau = mergeCounts(
		remapToolCounters(toolsDauNested),
		remapToolCounters(toolsDauFlat),
	);

	// --- tool_entry ---
	// Se guarda anidado (tool_entry.<tool>.<source>), pero documentos viejos pueden
	// tener la clave plana. Se reconstruyen los dos niveles en cualquier caso.
	const tool_entry: Record<string, Record<string, number>> = {};
	const addEntry = (tool: string, source: string, n: number) => {
		if (!tool || !source || !(n > 0)) return;
		const canon = canonToolKey(tool);
		if (!canon) return;
		if (!tool_entry[canon]) tool_entry[canon] = {};
		tool_entry[canon][source] = (tool_entry[canon][source] ?? 0) + n;
	};

	const entryNested =
		data.tool_entry && typeof data.tool_entry === "object"
			? data.tool_entry
			: {};
	for (const [tool, bySource] of Object.entries(entryNested)) {
		if (!bySource || typeof bySource !== "object") continue;
		for (const [source, v] of Object.entries(bySource as Record<string, any>)) {
			addEntry(tool, source, Number(v || 0));
		}
	}
	for (const [k, v] of Object.entries(pickPrefix(data, "tool_entry"))) {
		const cut = k.lastIndexOf(".");
		if (cut <= 0) continue;
		addEntry(k.slice(0, cut), k.slice(cut + 1), Number(v || 0));
	}

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
		pickPrefix(data, "lang.primary"),
	);
	const lang_secondary = mergeCounts(
		langNested.secondary || {},
		pickPrefix(data, "lang.secondary"),
	);

	// --- totals.app_open ---
	const appOpenNested = Number((data.totals && data.totals.app_open) || 0);
	const appOpenFlat = Number((data as any)["totals.app_open"] || 0);
	const app_open = appOpenNested + appOpenFlat;

	// --- totals.daily_active ---
	const dailyActiveNested = Number(
		(data.totals && data.totals.daily_active) || 0,
	);
	const dailyActiveFlat = Number((data as any)["totals.daily_active"] || 0);
	const daily_active = dailyActiveNested + dailyActiveFlat;

	// --- meta.updatedAt ---
	const updatedAtNested = (data as any)?.meta?.updatedAt;
	const updatedAtFlat = (data as any)["meta.updatedAt"];
	const updatedAt: any = updatedAtNested ?? updatedAtFlat;

	return {
		day: id,
		totals: {
			app_open,
			daily_active,
			tools: toolsCanon,
			tools_dau,
			tool_entry,
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
			// Autenticacion: App Check (1.3.2+) o API key (versiones anteriores).
			// La rama de API key se elimina cuando auth_method sea 100% "appcheck".
			const appCheckOk = await verifyAppCheck(req);
			const apiKey = getHeaderApiKey(req);
			const expected = (METRICS_API_KEY.value() || "").trim();
			const apiKeyOk = Boolean(expected) && apiKey === expected;

			if (!appCheckOk && !apiKeyOk) {
				logger.warn("ingest_unauthorized", {
					app_version:
						typeof req.body?.app_version === "string"
							? req.body.app_version
							: null,
				});
				sendJson(res, 401, { ok: false, error: "unauthorized" });
				return;
			}

			const authMethod = appCheckOk ? "appcheck" : "api_key";

			// Claves de herramienta no reconocidas en este lote. Se acumulan para
			// loguear una sola vez: si aparece algo aca, o hay una herramienta nueva
			// sin registrar en KNOWN_TOOLS, o un cliente genera claves que no deberia.
			const unknownTools = new Set<string>();

			const parsed = validateBody(req.body);
			if (!parsed.ok) {
				sendJson(res, 400, { ok: false, error: parsed.error ?? "invalid" });
				return;
			}
			const body = parsed.data as IngestBody;
			const dropped = parsed.dropped;

			// Senal de alarma que reemplaza al 400: si algo se saneo, queda registrado.
			if (dropped.keys.length > 0 || dropped.items > 0) {
				logger.warn("ingest_sanitized", {
					app_version: body.app_version,
					auth_method: authMethod,
					dropped_keys: [...new Set(dropped.keys)].slice(0, 20),
					dropped_items: dropped.items,
				});
			}

			const batchRef = db
				.collection("metrics_ingest_batches")
				.doc(body.batch_id);

			let totalItems = 0;
			let totalOpens = 0;
			let totalDaily = 0;

			await db.runTransaction(async (tx) => {
				const existsSnap = await tx.get(batchRef);
				if (existsSnap.exists) return;

				tx.create(batchRef, {
					seenAt: FieldValue.serverTimestamp(),
					platform: body.platform,
					app_version: body.app_version,
					items: (body.items ?? []).length,
					auth_method: authMethod,
					// Campo de expiracion para la politica TTL de Firestore. Esta coleccion
					// solo sirve para deduplicar reenvios y el cliente descarta un lote
					// pendiente a los 14 dias, asi que 90 es margen de sobra.
					expiresAt: expiryTimestamp(RETENTION_DAYS),
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

					if ((it.daily_active ?? 0) > 0) {
						updates["totals.daily_active"] = inc(it.daily_active ?? 0);
						totalDaily += it.daily_active ?? 0;
					}

					if (it.tools) {
						const tmp: Record<string, number> = {};
						for (const [rawKey, v] of Object.entries(it.tools)) {
							const canon = canonToolKey(rawKey); // quita prefijos y mapea
							if (!canon) continue;
							const ck = bucketToolKey(canon, unknownTools);
							const n = Number(v || 0);
							if (n > 0) tmp[ck] = (tmp[ck] || 0) + n;
						}
						for (const [ck, n] of Object.entries(tmp)) {
							updates[`tools.${ck}`] = inc(n); // siempre tools.<canónica>
						}
					}

					// Dispositivos-día por herramienta. Cada cliente aporta como mucho 1
					// por herramienta y por día, así que la suma es el número de
					// dispositivos distintos que la usaron: el denominador de tools.
					if (it.tools_dau) {
						const tmp: Record<string, number> = {};
						for (const [rawKey, v] of Object.entries(it.tools_dau)) {
							const canon = canonToolKey(rawKey);
							if (!canon) continue;
							const ck = bucketToolKey(canon, unknownTools);
							const n = Number(v || 0) > 0 ? 1 : 0;
							if (n > 0) tmp[ck] = 1;
						}
						for (const ck of Object.keys(tmp)) {
							updates[`tools_dau.${ck}`] = inc(1);
						}
					}

					// Origen de la apertura: clave "<herramienta>.<origen>".
					// No pasa por canonToolKey porque la clave es compuesta; se guarda
					// tal cual llega, ya saneada por validateBody.
					if (it.tool_entry) {
						for (const [k, v] of Object.entries(it.tool_entry)) {
							const n = Number(v || 0);
							if (n <= 0) continue;

							// La clave llega como "<herramienta>.<origen>". Se separa por el
							// ultimo punto porque el origen nunca contiene puntos.
							const cut = k.lastIndexOf(".");
							if (cut <= 0) continue;

							const canon = canonToolKey(k.slice(0, cut));
							if (!canon) continue;
							const tool = bucketToolKey(canon, unknownTools);

							const rawSource = k.slice(cut + 1);
							const source = KNOWN_SOURCES.has(rawSource)
								? rawSource
								: UNKNOWN_SOURCE;

							// Se guarda anidado: tool_entry.<herramienta>.<origen>
							updates[`tool_entry.${tool}.${source}`] = inc(n);
						}
					}

					// Retencion: clave "<antiguedad>.<intensidad>", calculada en el
					// dispositivo. Se guarda anidada para poder leer las marginales.
					if (it.retention) {
						for (const [k, v] of Object.entries(it.retention)) {
							const n = Number(v || 0) > 0 ? 1 : 0;
							if (n <= 0) continue;
							const cut = k.indexOf(".");
							if (cut <= 0) continue;
							const age = k.slice(0, cut);
							const intensity = k.slice(cut + 1);
							if (!KNOWN_AGE.has(age) || !KNOWN_INTENSITY.has(intensity)) continue;
							updates[`retention.${age}.${intensity}`] = inc(1);
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
							(it as any).versions as Record<string, number>,
						)) {
							const n = Number(v || 0);
							if (n > 0)
								verPairs.push([new FieldPath("versions", ver), inc(n)]);
						}
					}
					if ((it as any).versions_first_seen) {
						for (const [ver, v] of Object.entries(
							(it as any).versions_first_seen as Record<string, number>,
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

			if (unknownTools.size > 0) {
				logger.warn("ingest_unknown_tools", {
					app_version: body.app_version,
					tools: [...unknownTools].slice(0, 20),
				});
			}

			// Rollup mensual del metodo de autenticacion.
			// Es el contador que decide cuando se puede retirar la rama de API key:
			// mientras haya trafico con "api_key", hay dispositivos que quedarian
			// bloqueados de forma permanente si se elimina.
			const authMonth = (body.items?.[0]?.day || ymdTZ(new Date(), DEFAULT_TZ)).slice(
				0,
				7,
			);
			await db
				.collection("metrics_auth")
				.doc(authMonth)
				.set(
					{
						[authMethod]: FieldValue.increment(1),
						// Las claves con puntos ("1.3.2") van dentro de un objeto literal,
						// no como field path, asi que no se interpretan como anidamiento.
						by_version: {
							[body.app_version]: { [authMethod]: FieldValue.increment(1) },
						},
						updatedAt: FieldValue.serverTimestamp(),
					},
					{ merge: true },
				);

			await db.collection("metrics_ingest_logs").doc().set({
				at: FieldValue.serverTimestamp(),
				batch_id: body.batch_id,
				platform: body.platform,
				app_version: body.app_version,
				auth_method: authMethod,
				total_items: totalItems,
				total_app_open_delta: totalOpens,
				total_daily_active_delta: totalDaily,
				dropped_keys: dropped.keys.length,
				dropped_items: dropped.items,
				expiresAt: expiryTimestamp(RETENTION_DAYS),
			});

			sendJson(res, 200, {
				ok: true,
				dropped_keys: dropped.keys.length,
				dropped_items: dropped.items,
			});
			return;
		} catch (e) {
			console.error("ingest_error", e);
			sendJson(res, 500, { ok: false, error: "internal" });
			return;
		}
	},
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
	},
);

/**
 * Estado de la migracion a App Check: cuantos lotes llegaron con cada metodo de
 * autenticacion, por mes y por version de app.
 *
 * Lee documentos de rollup (uno por mes), no la coleccion de lotes, asi que el costo
 * no crece con el volumen de ingesta.
 */
export const metricsAuth = onRequest(
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
			if (!(apiKey && (apiKey === readK || apiKey === writeK))) {
				sendJson(res, 401, { ok: false, error: "unauthorized" });
				return;
			}

			const months = Math.max(1, Math.min(12, Number(req.query.months ?? 3)));
			const now = new Date();
			const ids: string[] = [];
			for (let i = 0; i < months; i++) {
				const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
				ids.push(
					`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`,
				);
			}

			const snaps = await db.getAll(
				...ids.map((id) => db.collection("metrics_auth").doc(id)),
			);

			const out = snaps
				.filter((snap) => snap.exists)
				.map((snap) => {
					const d = snap.data() || {};
					return {
						month: snap.id,
						appcheck: Number(d.appcheck || 0),
						api_key: Number(d.api_key || 0),
						by_version: (d.by_version || {}) as Record<
							string,
							Record<string, number>
						>,
					};
				});

			sendJson(res, 200, { months: out });
			return;
		} catch (e) {
			console.error("metricsAuth_error", e);
			sendJson(res, 500, { ok: false, error: "internal" });
			return;
		}
	},
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
			let total_daily_active = 0;
			const agg_tools: Record<string, number> = {};
			const agg_ads: Record<string, number> = {};
			const agg_versions: Record<string, number> = {};
			const agg_versions_fs: Record<string, number> = {};
			const agg_lang_primary: Record<string, number> = {};
			const agg_lang_secondary: Record<string, number> = {};
			const agg_widgets: Record<string, number> = {};
			const agg_tools_dau: Record<string, number> = {};
			const agg_entry_sources: Record<string, number> = {};

			for (const r of rows) {
				total_app_open += Number(r.totals.app_open ?? 0);
				total_daily_active += Number(r.totals.daily_active ?? 0);
				sumMap(agg_tools, r.totals.tools);
				sumMap(agg_ads, r.totals.ads);
				sumMap(agg_versions, r.totals.versions);
				sumMap(agg_versions_fs, r.totals.versions_first_seen);
				sumMap(agg_lang_primary, r.totals.lang_primary);
				sumMap(agg_lang_secondary, r.totals.lang_secondary);
				sumMap(agg_widgets, r.totals.widgets);
				sumMap(agg_tools_dau, r.totals.tools_dau);
				// El origen se agrega a nivel global: por herramienta se consulta
				// con metricsDaily, que devuelve el detalle completo.
				for (const bySource of Object.values(r.totals.tool_entry || {})) {
					sumMap(agg_entry_sources, bySource);
				}
			}

			const payload = {
				range: { from, to, days: rows.length },
				total_app_open,
				total_daily_active,
				top: {
					tools: topK(agg_tools, 10),
					ads: topK(agg_ads, 10),
					versions: topK(agg_versions, 10),
					versions_first_seen: topK(agg_versions_fs, 10),
					lang_primary: topK(agg_lang_primary, 10),
					lang_secondary: topK(agg_lang_secondary, 10),
					widgets: topK(agg_widgets, 10),
					tools_dau: topK(agg_tools_dau, 10),
					entry_sources: topK(agg_entry_sources, 10),
				},
			};
			sendJson(res, 200, payload);
			return;
		} catch (e) {
			console.error("metricsSummary_error", e);
			sendJson(res, 500, { ok: false, error: "internal" });
			return;
		}
	},
);