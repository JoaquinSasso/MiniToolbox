export type IngestItem = {
	day: string; // "YYYY-MM-DD"
	app_open?: number;
	tools?: Record<string, number>;
	ads?: Record<string, number>;
};

export type IngestBody = {
	batch_id: string;
	platform: "android" | "ios" | string;
	app_version: string;
	items: IngestItem[];
};

const DAY_RE = /^\d{4}-\d{2}-\d{2}$/;

export function validateBody(
	body: any
): { ok: true; data: IngestBody } | { ok: false; error: string } {
	if (!body || typeof body !== "object")
		return { ok: false, error: "invalid_json" };

	const { batch_id, platform, app_version, items } = body;

	if (typeof batch_id !== "string" || batch_id.trim() === "")
		return { ok: false, error: "invalid_batch_id" };
	if (typeof platform !== "string" || platform.trim() === "")
		return { ok: false, error: "invalid_platform" };
	if (typeof app_version !== "string" || app_version.trim() === "")
		return { ok: false, error: "invalid_app_version" };
	if (!Array.isArray(items) || items.length === 0)
		return { ok: false, error: "items_empty" };

	for (const it of items) {
		if (!it || typeof it !== "object")
			return { ok: false, error: "invalid_item" };
		if (typeof it.day !== "string" || !DAY_RE.test(it.day))
			return { ok: false, error: "invalid_day" };

		const app = it.app_open ?? 0;
		if (!Number.isInteger(app) || app < 0)
			return { ok: false, error: "invalid_app_open" };

		const checkMap = (m: any) => {
			if (m == null) return true;
			if (typeof m !== "object") return false;
			for (const k of Object.keys(m)) {
				if (k.trim() === "") return false; // rechazar claves vacías
				const v = m[k];
				if (!Number.isInteger(v) || v < 0) return false;
			}
			return true;
		};

		if (!checkMap(it.tools)) return { ok: false, error: "invalid_tools" };
		if (!checkMap(it.ads)) return { ok: false, error: "invalid_ads" };
	}

	return { ok: true, data: body as IngestBody };
}
