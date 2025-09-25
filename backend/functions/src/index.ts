import { onRequest } from "firebase-functions/v2/https";
import type { Request, Response } from "express";

import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue, Firestore } from "firebase-admin/firestore";
import { defineSecret } from "firebase-functions/params";
import { validateBody, type IngestBody } from "./validate.js";

initializeApp();
const db = getFirestore();

const METRICS_API_KEY = defineSecret("METRICS_API_KEY");

function json(res: Response, code: number, obj: any) {
	res.setHeader("Content-Type", "application/json; charset=utf-8");
	res.status(code).send(JSON.stringify(obj));
}

function monthFromDay(day: string): string {
	// "YYYY-MM-DD" -> "YYYY-MM"
	return day.slice(0, 7);
}

function dailyCollection(db: Firestore, month: string) {
	return db.collection("metrics_daily").doc(month).collection("days");
}

export const ingest = onRequest(
	{ secrets: [METRICS_API_KEY] },
	async (req: Request, res: Response) => {
		try {
			// Method + Auth
			if (req.method !== "POST") {
				return json(res, 405, { ok: false, error: "method_not_allowed" });
			}
			const apiKey = (
				req.header("x-api-key") ??
				req.header("X-API-Key") ??
				""
			).trim();
			const expected = METRICS_API_KEY.value();
			if (!expected || apiKey !== expected) {
				return json(res, 401, { ok: false, error: "unauthorized" });
			}

			// Validate
			const parsed = validateBody(req.body);
			if (!parsed.ok) {
				return json(res, 400, { ok: false, error: parsed.error ?? "invalid" });
			}
			const body = parsed.data as IngestBody;

			// Idempotency via batch_id
			const batchRef = db
				.collection("metrics_ingest_batches")
				.doc(body.batch_id);

			// Totals for logging
			let totalItems = 0;
			let totalOpens = 0;

			await db.runTransaction(async (tx) => {
				const existsSnap = await tx.get(batchRef);
				if (existsSnap.exists) {
					// already processed, do nothing
					return;
				}

				// Reserve this batch id (create fails if exists)
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

					// app_open
					if ((it.app_open ?? 0) > 0) {
						updates["totals.app_open"] = inc(it.app_open ?? 0);
						totalOpens += it.app_open ?? 0;
					}

					// tools
					if (it.tools) {
						for (const [k, v] of Object.entries(it.tools)) {
							if (v > 0) updates[`tools.${k}`] = inc(v);
						}
					}

					// ads
					if (it.ads) {
						for (const [k, v] of Object.entries(it.ads)) {
							if (v > 0) updates[`ads.${k}`] = inc(v);
						}
					}

					// NEW: DAU por versión
					if ((it as any).versions) {
						for (const [ver, v] of Object.entries(
							(it as any).versions as Record<string, number>
						)) {
							if (v > 0) updates[`versions.${ver}`] = inc(v);
						}
					}

					// NEW: first-seen por versión
					if ((it as any).versions_first_seen) {
						for (const [ver, v] of Object.entries(
							(it as any).versions_first_seen as Record<string, number>
						)) {
							if (v > 0) updates[`versions_first_seen.${ver}`] = inc(v);
						}
					}

					// NEW: lenguajes
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

					// NEW: widgets
					const w = (it as any).widgets as Record<string, number> | undefined;
					if (w) {
						for (const [kind, v] of Object.entries(w)) {
							if (v > 0) updates[`widgets.${kind}`] = inc(v);
						}
					}

					tx.set(dayRef, updates, { merge: true });
				}
			});

			// Log (best-effort)
			await db.collection("metrics_ingest_logs").doc().set({
				at: FieldValue.serverTimestamp(),
				batch_id: body.batch_id,
				platform: body.platform,
				app_version: body.app_version,
				total_items: totalItems,
				total_app_open_delta: totalOpens,
			});

			return json(res, 200, { ok: true });
		} catch (e) {
			console.error("ingest_error", e);
			return json(res, 500, { ok: false, error: "internal" });
		}
	}
);
