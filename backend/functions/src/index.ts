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
import crypto from "crypto";

initializeApp();
const db = getFirestore();

const METRICS_API_KEY = defineSecret("METRICS_API_KEY");

// NEW (read endpoints): API key separada para lectura (opcional)
const READ_METRICS_API_KEY = defineSecret("READ_METRICS_API_KEY");

+function json(res: any, code: number, obj: any) {
	res.setHeader("Content-Type", "application/json; charset=utf-8");
	res.status(code).send(JSON.stringify(obj));
};

function monthFromDay(day: string): string {
	// "YYYY-MM-DD" -> "YYYY-MM"
	return day.slice(0, 7);
}

function dailyCollection(db: Firestore, month: string) {
	return db.collection("metrics_daily").doc(month).collection("days");
}

function sendJson(res: any, code: number, obj: any) {
	// Express-style
	if (typeof res.setHeader === "function") {
		res.setHeader("Content-Type", "application/json; charset=utf-8");
	}
	if (typeof res.status === "function" && typeof res.send === "function") {
		return res.status(code).send(JSON.stringify(obj));
	}
	// Fallback (por si cambia algo en el futuro)
	try {
		res.writeHead?.(code, {
			"Content-Type": "application/json; charset=utf-8",
		});
		res.end?.(JSON.stringify(obj));
	} catch {
		// no-op
	}
}

function h8(s: string) {
	try {
		return crypto
			.createHash("sha256")
			.update(s || "")
			.digest("hex")
			.slice(0, 8);
	} catch {
		return "errhash";
	}
}

function getHeaderApiKey(req: any): string {
	const k1 = req.get?.("x-api-key") ?? req.header?.("x-api-key");
	const k2 = req.get?.("X-API-Key") ?? req.header?.("X-API-Key");
	const auth = req.get?.("authorization") ?? req.header?.("authorization");
	const bearer = auth && /^Bearer\s+(.+)$/i.test(auth) ? RegExp.$1 : "";
	return (k1 || k2 || bearer || "").trim();
}


// ---------- AUTH HELPERS ----------
function readApiKey(): string {
	// Si hay READ_METRICS_API_KEY úsalo, sino cae a METRICS_API_KEY
	const readK = READ_METRICS_API_KEY.value();
	if (readK && readK.trim() !== "") return readK.trim();
	return METRICS_API_KEY.value() || "";
}



// ---------- INGEST (TU CÓDIGO EXISTENTE) ----------
export const ingest = onRequest({ secrets: [METRICS_API_KEY] }, async (req, res) => {
	try {
		if (req.method !== "POST") {
			return sendJson(res, 405, { ok: false, error: "method_not_allowed" });
		}
		const apiKey = getHeaderApiKey(req);

		//DEBUG POR ERROR DE KEY
		const readK = (READ_METRICS_API_KEY.value() || "").trim();
		const writeK = (METRICS_API_KEY.value() || "").trim();

		console.log("[metricsSummary] auth_debug", {
			got_len: apiKey.length,
			got_h8: h8(apiKey),
			read_len: readK.length,
			read_h8: h8(readK),
			write_len: writeK.length,
			write_h8: h8(writeK),
			eq_read: apiKey === readK,
			eq_write: apiKey === writeK,
			hdrs: Object.keys(req.headers || {}).slice(0, 20),
			origin: req.headers?.origin || "",
			host: req.headers?.host || "",
		});

		// *** parche robusto: aceptar READ o WRITE en endpoints de LECTURA ***
		if (!(apiKey && (apiKey === readK || apiKey === writeK))) {
			return sendJson(res, 401, { ok: false, error: "unauthorized" });
		}
		//FIN DE LA PARTE DEL DEBUG

		const expected = METRICS_API_KEY.value();
		if (!expected || apiKey !== expected) {
			return sendJson(res, 401, { ok: false, error: "unauthorized" });
		}

		const parsed = validateBody(req.body);
		if (!parsed.ok) {
			return sendJson(res, 400, {
				ok: false,
				error: parsed.error ?? "invalid",
			});
		}
		const body = parsed.data as IngestBody;

		const batchRef = db.collection("metrics_ingest_batches").doc(body.batch_id);

		let totalItems = 0;
		let totalOpens = 0;

		await db.runTransaction(async (tx) => {
			const existsSnap = await tx.get(batchRef);
			if (existsSnap.exists) {
				return;
			}

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

				if ((it.app_open ?? 0) > 0) {
					updates["totals.app_open"] = inc(it.app_open ?? 0);
					totalOpens += it.app_open ?? 0;
				}

				if (it.tools) {
					for (const [k, v] of Object.entries(it.tools)) {
						if (v > 0) updates[`tools.${k}`] = inc(v);
					}
				}

				if (it.ads) {
					for (const [k, v] of Object.entries(it.ads)) {
						if (v > 0) updates[`ads.${k}`] = inc(v);
					}
				}

				if ((it as any).versions) {
					for (const [ver, v] of Object.entries(
						(it as any).versions as Record<string, number>
					)) {
						if (v > 0) updates[`versions.${ver}`] = inc(v);
					}
				}

				if ((it as any).versions_first_seen) {
					for (const [ver, v] of Object.entries(
						(it as any).versions_first_seen as Record<string, number>
					)) {
						if (v > 0) updates[`versions_first_seen.${ver}`] = inc(v);
					}
				}

				const lp = (it as any).lang_primary as
					| Record<string, number>
					| undefined;
				if (lp) {
					for (const [lang, v] of Object.entries(lp)) {
						if (v > 0) updates[`lang.primary.${lang}`] = inc(v);
					}
				}
				const ls = (it as any).lang_secondary as
					| Record<string, number>
					| undefined;
				if (ls) {
					for (const [lang, v] of Object.entries(ls)) {
						if (v > 0) updates[`lang.secondary.${lang}`] = inc(v);
					}
				}

				const w = (it as any).widgets as Record<string, number> | undefined;
				if (w) {
					for (const [kind, v] of Object.entries(w)) {
						if (v > 0) updates[`widgets.${kind}`] = inc(v);
					}
				}

				tx.set(dayRef, updates, { merge: true });
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

		return sendJson(res, 200, { ok: true });
	} catch (e) {
		console.error("ingest_error", e);
		return sendJson(res, 500, { ok: false, error: "internal" });
	}
});

// ---------- NEW (read endpoints) ----------

// Utils de fechas
function parseYmd(s: string): Date | null {
	if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return null;
	const [y, m, d] = s.split("-").map((x) => parseInt(x, 10));
	const dt = new Date(Date.UTC(y, m - 1, d));
	return isNaN(dt.getTime()) ? null : dt;
}
function ymd(d: Date): string {
	const y = d.getUTCFullYear();
	const m = String(d.getUTCMonth() + 1).padStart(2, "0");
	const da = String(d.getUTCDate()).padStart(2, "0");
	return `${y}-${m}-${da}`;
}
function addDays(d: Date, n: number): Date {
	const t = new Date(d.getTime());
	t.setUTCDate(t.getUTCDate() + n);
	return t;
}
function monthsBetween(from: string, to: string): string[] {
	// from/to: YYYY-MM-DD
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
	// Detección: si tiene data.tools → estructura nueva (mapa). Si no → plana.
	const hasNested = !!data.tools || !!data.totals;

	const totals = hasNested ? data.totals || {} : {};
	const tools = hasNested ? data.tools || {} : pickPrefix(data, "tools");
	const ads = hasNested ? data.ads || {} : pickPrefix(data, "ads");
	const versions = hasNested
		? data.versions || {}
		: pickPrefix(data, "versions");
	const versions_first_seen = hasNested
		? data.versions_first_seen || {}
		: pickPrefix(data, "versions_first_seen");

	const lang_nested = hasNested ? data.lang || {} : {};
	const lang_primary = hasNested
		? lang_nested.primary || {}
		: pickPrefix(data, "lang.primary");
	const lang_secondary = hasNested
		? lang_nested.secondary || {}
		: pickPrefix(data, "lang.secondary");

	const widgets = hasNested ? data.widgets || {} : pickPrefix(data, "widgets");

	const updatedAt = hasNested ? data?.meta?.updatedAt : data["meta.updatedAt"];

	return {
		day: id,
		totals: {
			app_open: Number(
				hasNested ? totals.app_open ?? 0 : data["totals.app_open"] ?? 0
			),
			tools,
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
		// Acotamos por docId (día) dentro del mes
		const start = month === from.slice(0, 7) ? from : `${month}-01`;
		const end = month === to.slice(0, 7) ? to : `${month}-31`;
		const snap = await dailyCollection(db, month)
			.orderBy(FieldPath.documentId())
			.startAt(start)
			.endAt(end)
			.get();

		snap.docs.forEach((d) => docs.push(normDoc(d.id, d.data())));
	}
	// Orden ascendente por día
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

export const metricsDaily = onRequest(
	{ secrets: [READ_METRICS_API_KEY, METRICS_API_KEY] },
	async (req, res) => {
		try {
			if (req.method !== "GET")
				return sendJson(res, 405, { ok: false, error: "method_not_allowed" });

			const apiKey = getHeaderApiKey(req);

			//DEBUG POR ERROR DE KEY
			const readK = (READ_METRICS_API_KEY.value() || "").trim();
			const writeK = (METRICS_API_KEY.value() || "").trim();

			console.log("[metricsSummary] auth_debug", {
				got_len: apiKey.length,
				got_h8: h8(apiKey),
				read_len: readK.length,
				read_h8: h8(readK),
				write_len: writeK.length,
				write_h8: h8(writeK),
				eq_read: apiKey === readK,
				eq_write: apiKey === writeK,
				hdrs: Object.keys(req.headers || {}).slice(0, 20),
				origin: req.headers?.origin || "",
				host: req.headers?.host || "",
			});

			// *** parche robusto: aceptar READ o WRITE en endpoints de LECTURA ***
			if (!(apiKey && (apiKey === readK || apiKey === writeK))) {
				return sendJson(res, 401, { ok: false, error: "unauthorized" });
			}
			//FIN DE LA PARTE DEL DEBUG

			const expected = readApiKey();
			if (!expected || apiKey !== expected)
				return sendJson(res, 401, { ok: false, error: "unauthorized" });

			const from = String(req.query.from || "").trim();
			const to = String(req.query.to || "").trim();
			const df = parseYmd(from);
			const dt = parseYmd(to);
			if (!df || !dt)
				return sendJson(res, 400, { ok: false, error: "bad_range" });
			if (df.getTime() > dt.getTime())
				return sendJson(res, 400, { ok: false, error: "bad_range_order" });

			// límite de seguridad: 400 días
			const max = addDays(df, 400);
			if (dt.getTime() > max.getTime())
				return sendJson(res, 400, { ok: false, error: "range_too_large" });

			const rows = await fetchDailyRange(from, to);
			return sendJson(res, 200, rows);
		} catch (e) {
			console.error("metricsDaily_error", e);
			return sendJson(res, 500, { ok: false, error: "internal" });
		}
	}
);

export const metricsSummary = onRequest(
	{ secrets: [READ_METRICS_API_KEY, METRICS_API_KEY] },
	async (req, res) => {
		try {
			if (req.method !== "GET")
				return sendJson(res, 405, { ok: false, error: "method_not_allowed" });

			const apiKey = getHeaderApiKey(req);
			
			//DEBUG POR ERROR DE KEY
			const readK = (READ_METRICS_API_KEY.value() || "").trim();
			const writeK = (METRICS_API_KEY.value() || "").trim();

			console.log("[metricsSummary] auth_debug", {
				got_len: apiKey.length,
				got_h8: h8(apiKey),
				read_len: readK.length,
				read_h8: h8(readK),
				write_len: writeK.length,
				write_h8: h8(writeK),
				eq_read: apiKey === readK,
				eq_write: apiKey === writeK,
				hdrs: Object.keys(req.headers || {}).slice(0, 20),
				origin: req.headers?.origin || "",
				host: req.headers?.host || "",
			});

			// *** parche robusto: aceptar READ o WRITE en endpoints de LECTURA ***
			if (!(apiKey && (apiKey === readK || apiKey === writeK))) {
				return sendJson(res, 401, { ok: false, error: "unauthorized" });
			}
			//FIN DE LA PARTE DEL DEBUG

			const expected = readApiKey();
			if (!expected || apiKey !== expected)
				return sendJson(res, 401, { ok: false, error: "unauthorized" });

			const last = Math.max(1, Math.min(400, Number(req.query.last ?? 30)));
			const today = new Date();
			const to = ymd(today);
			const from = ymd(addDays(today, -(last - 1)));

			const rows = await fetchDailyRange(from, to);

			// Agregados del rango
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
			return sendJson(res, 200, payload);
		} catch (e) {
			console.error("metricsSummary_error", e);
			return sendJson(res, 500, { ok: false, error: "internal" });
		}
	}
);
