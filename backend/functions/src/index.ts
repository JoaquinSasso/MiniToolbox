import { onRequest } from "firebase-functions/v2/https";
import type { Request, Response } from "express";

import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { defineSecret } from "firebase-functions/params";
import { validateBody, type IngestBody } from "./validate.js";

initializeApp();
const db = getFirestore();

const METRICS_API_KEY = defineSecret("METRICS_API_KEY");

function json(res: Response, code: number, obj: any) {
	res.setHeader("Content-Type", "application/json; charset=utf-8");
	res.status(code).send(JSON.stringify(obj));
}

export const ingest = onRequest(
	{ region: "us-central1", secrets: [METRICS_API_KEY] },
	async (req: Request, res: Response) => {
		try {
			if (req.method !== "POST")
				return json(res, 405, { ok: false, error: "method_not_allowed" });

			// Seguridad
			const apiKey = req.get("X-API-Key") || "";
			const serverKey = METRICS_API_KEY.value();
			if (!serverKey || apiKey !== serverKey) {
				res.status(401).json({ ok: false, error: "unauthorized" });
				return;
			}

			// Validación
			const parsed = validateBody(req.body);
			if (!parsed.ok) return json(res, 400, { ok: false, error: parsed.error });

			const body = parsed.data as IngestBody;

			const ingRef = db.collection("ingestions").doc(body.batch_id);
			const dailyCol = db.collection("metrics_daily");
			let deduped = false;

			await db.runTransaction(async (tx) => {
				const ingSnap = await tx.get(ingRef);
				if (ingSnap.exists) {
					deduped = true;
					return;
				}

				for (const it of body.items) {
					const dayRef = dailyCol.doc(it.day);

					const updates: Record<string, any> = {
						"meta.updatedAt": FieldValue.serverTimestamp(),
						[`meta.versions.${body.platform}.${body.app_version}`]: true,
					};
					const inc = (n: number) => FieldValue.increment(n);

					if ((it.app_open ?? 0) > 0)
						updates["totals.app_open"] = inc(it.app_open ?? 0);
					if (it.tools)
						for (const [toolId, n] of Object.entries(it.tools))
							if (n > 0) updates[`totals.tools.${toolId}`] = inc(n);
					if (it.ads)
						for (const [adType, n] of Object.entries(it.ads))
							if (n > 0) updates[`totals.ads.${adType}`] = inc(n);

					tx.set(dayRef, updates, { merge: true });
				}

				tx.set(ingRef, { createdAt: FieldValue.serverTimestamp() });
			});

			if (deduped) return json(res, 200, { ok: true, deduped: true });

			const totalItems = body.items.length;
			const totalOpens = body.items.reduce((a, b) => a + (b.app_open ?? 0), 0);
			console.log("ingest_ok", {
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
