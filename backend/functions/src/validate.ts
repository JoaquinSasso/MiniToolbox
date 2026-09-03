export type IngestItem = {
	day: string; // "YYYY-MM-DD"
	app_open?: number;
	daily_active?: number;
	tools?: Record<string, number>;
	/** Dispositivos-día por herramienta: 0 o 1 por clave. Denominador de tools. */
	tools_dau?: Record<string, number>;
	/** Aperturas por herramienta y origen, clave "<toolId>.<source>". */
	tool_entry?: Record<string, number>;
	/** Retencion: 1 marca por dia en "<antiguedad>.<intensidad>". */
	retention?: Record<string, number>;
	ads?: Record<string, number>;

	versions?: Record<string, number>; // DAU por versión
	versions_first_seen?: Record<string, number>; // first-seen por versión
	lang_primary?: Record<string, number>; // idioma principal
	lang_secondary?: Record<string, number>; // idioma secundario
	widgets?: Record<string, number>; // uso de widgets
};

/**
 * Salud del pipeline de envio, reportada por el cliente.
 *
 * OJO: son contadores ACUMULADOS de por vida del dispositivo, no deltas. Cada lote
 * repite el total historico, asi que sumarlos entre lotes no significa nada: lo que
 * se puede contar es cuantos lotes vienen de dispositivos con problemas.
 */
export type ClientHealth = {
	dropped_batches: number;
	sanitized_keys: number;
	consecutive_failures: number;
};

export type IngestBody = {
	batch_id: string;
	platform: "android" | "ios" | string;
	app_version: string;
	items: IngestItem[];
	client_health?: ClientHealth;
};

/** Claves descartadas o corregidas durante el saneo de un lote. */
export type Dropped = {
	/** Claves originales que hubo que normalizar o descartar. */
	keys: string[];
	/** Items completos descartados (día inválido o mapa no-objeto). */
	items: number;
};

export const DAY_RE = /^\d{4}-\d{2}-\d{2}$/;
export const KEY_RE = /^[a-zA-Z0-9._-]{1,64}$/;

/** Tope de claves distintas por mapa y por item. Defensa contra payloads abusivos. */
const MAX_DISTINCT_KEYS = 200;

/**
 * Segmentos que son identificadores y no aportan información agregable:
 * UUIDs, hashes hex y números.
 */
const ID_SEGMENT_RE =
	/^(?:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|[0-9a-f]{8,}|\d+)$/i;

/**
 * Convierte una clave con forma de ruta en una clave estable, descartando los
 * segmentos que son identificadores.
 *
 *   "pomodoro/detail/2f7a1b3c-4d5e-6f70-8a9b-0c1d2e3f4a5b" -> "pomodoro_detail"
 *   "pomodoro/detail/{timerId}"                            -> "pomodoro_detail_timerId"
 *   "dev/metrics"                                          -> "dev_metrics"
 *
 * Sin este colapso, cada timer generaría su propia clave y explotaría la
 * cardinalidad de los mapas de metrics_daily.
 */
export function collapseRouteKey(raw: string): string {
	return raw
		.split(/[/?#]/)
		.map((s) => s.trim())
		.filter(Boolean)
		.filter((s) => !ID_SEGMENT_RE.test(s))
		.map((s) => s.replace(/^\{|\}$/g, ""))
		.join("_");
}

/**
 * Convierte una clave arbitraria en una que cumpla KEY_RE.
 * Devuelve null si no queda nada utilizable.
 */
export function normalizeKey(raw: unknown): string | null {
	if (typeof raw !== "string") return null;
	const collapsed = /[/?#]/.test(raw) ? collapseRouteKey(raw) : raw;
	const clean = collapsed
		.trim()
		.replace(/[^a-zA-Z0-9._-]/g, "_")
		.replace(/_+/g, "_")
		.replace(/^_+|_+$/g, "")
		.slice(0, 64);
	return clean.length > 0 ? clean : null;
}

/**
 * Sanea un mapa clave -> contador.
 *
 * - Claves que no cumplen KEY_RE se normalizan; si colisionan, los contadores
 *   se SUMAN en lugar de pisarse.
 * - Valores que no son enteros >= 0 se descartan.
 * - Devuelve null solo si el valor recibido no es un objeto: eso es
 *   estructural y descarta el item completo.
 */
function sanitizeMap(m: any, dropped: Dropped): Record<string, number> | null {
	if (m == null) return {};
	if (typeof m !== "object" || Array.isArray(m)) return null;

	const out: Record<string, number> = {};
	let distinct = 0;

	for (const k of Object.keys(m)) {
		const v = m[k];

		if (!Number.isInteger(v) || v < 0) {
			dropped.keys.push(k);
			continue;
		}

		let key = k;
		if (!KEY_RE.test(k)) {
			dropped.keys.push(k);
			const normalized = normalizeKey(k);
			if (!normalized) continue;
			key = normalized;
		}

		if (!(key in out)) {
			if (distinct >= MAX_DISTINCT_KEYS) {
				dropped.keys.push(k);
				continue;
			}
			distinct += 1;
		}

		out[key] = (out[key] ?? 0) + v;
	}

	return out;
}

/** Sanea el bloque de salud del cliente. Valores no numéricos cuentan como 0. */
function sanitizeHealth(raw: any): ClientHealth | undefined {
	if (!raw || typeof raw !== "object") return undefined;
	const n = (v: any) => (Number.isInteger(v) && v >= 0 ? v : 0);
	return {
		dropped_batches: n(raw.dropped_batches),
		sanitized_keys: n(raw.sanitized_keys),
		consecutive_failures: n(raw.consecutive_failures),
	};
}

/** Devuelve el número si es un entero >= 0, o undefined. */
function safeCount(v: any): number | undefined {
	return Number.isInteger(v) && v >= 0 ? v : undefined;
}

/**
 * Valida y sanea el cuerpo de un lote de ingesta.
 *
 * Filosofía: un lote NUNCA se rechaza por una clave rara. Se descarta la clave
 * o el item y se acepta el resto. El 400 queda reservado para fallas
 * estructurales que hacen imposible procesar el lote.
 *
 * Motivo: un rechazo determinista deja al cliente reintentando el mismo payload
 * congelado para siempre, lo que bloquea las métricas del dispositivo de forma
 * permanente.
 */
export function validateBody(
	body: any,
):
	| { ok: true; data: IngestBody; dropped: Dropped }
	| { ok: false; error: string } {
	if (!body || typeof body !== "object")
		return { ok: false, error: "invalid_body" };

	if (typeof body.batch_id !== "string" || body.batch_id.trim() === "")
		return { ok: false, error: "invalid_batch_id" };

	if (typeof body.platform !== "string" || body.platform.trim() === "")
		return { ok: false, error: "invalid_platform" };

	if (typeof body.app_version !== "string" || body.app_version.trim() === "")
		return { ok: false, error: "invalid_app_version" };

	if (!Array.isArray(body.items)) return { ok: false, error: "invalid_items" };

	const dropped: Dropped = { keys: [], items: 0 };
	const items: IngestItem[] = [];

	for (const it of body.items) {
		if (
			!it ||
			typeof it !== "object" ||
			typeof it.day !== "string" ||
			!DAY_RE.test(it.day)
		) {
			dropped.items += 1;
			continue;
		}

		const tools = sanitizeMap(it.tools, dropped);
		const toolsDau = sanitizeMap(it.tools_dau, dropped);
		const toolEntry = sanitizeMap(it.tool_entry, dropped);
		const retention = sanitizeMap(it.retention, dropped);
		const ads = sanitizeMap(it.ads, dropped);
		const versions = sanitizeMap(it.versions, dropped);
		const versionsFirstSeen = sanitizeMap(it.versions_first_seen, dropped);
		const langPrimary = sanitizeMap(it.lang_primary, dropped);
		const langSecondary = sanitizeMap(it.lang_secondary, dropped);
		const widgets = sanitizeMap(it.widgets, dropped);

		const maps = [
			tools,
			toolsDau,
			toolEntry,
			retention,
			ads,
			versions,
			versionsFirstSeen,
			langPrimary,
			langSecondary,
			widgets,
		];
		if (maps.some((m) => m === null)) {
			dropped.items += 1;
			continue;
		}

		items.push({
			day: it.day,
			app_open: safeCount(it.app_open),
			daily_active: safeCount(it.daily_active),
			tools: tools as Record<string, number>,
			tools_dau: toolsDau as Record<string, number>,
			tool_entry: toolEntry as Record<string, number>,
			retention: retention as Record<string, number>,
			ads: ads as Record<string, number>,
			versions: versions as Record<string, number>,
			versions_first_seen: versionsFirstSeen as Record<string, number>,
			lang_primary: langPrimary as Record<string, number>,
			lang_secondary: langSecondary as Record<string, number>,
			widgets: widgets as Record<string, number>,
		});
	}

	return {
		ok: true,
		data: {
			batch_id: body.batch_id,
			client_health: sanitizeHealth(body.client_health),
			platform: body.platform,
			app_version: body.app_version,
			items,
		},
		dropped,
	};
}