export type IngestItem = {
	day: string; // "YYYY-MM-DD"
	app_open?: number;
	tools?: Record<string, number>;
	ads?: Record<string, number>;

	// NUEVO
	versions?: Record<string, number>; // DAU por versión
	versions_first_seen?: Record<string, number>; // first-seen por versión
	lang_primary?: Record<string, number>; // idioma principal
	lang_secondary?: Record<string, number>; // idioma secundario
	widgets?: Record<string, number>; // uso de widgets
};

export type IngestBody = {
	batch_id: string;
	platform: "android" | "ios" | string;
	app_version: string;
	items: IngestItem[];
};

const DAY_RE = /^\d{4}-\d{2}-\d{2}$/;
const KEY_RE = /^[a-zA-Z0-9._-]{1,64}$/; // claves seguras (toolId, adType, versión, idioma, widgetKind)

export function validateBody(
	body: any
): { ok: true; data: IngestBody } | { ok: false; error: string } {
	if (!body || typeof body !== "object")
		return { ok: false, error: "invalid_body" };

	if (typeof body.batch_id !== "string" || body.batch_id.trim() === "")
		return { ok: false, error: "invalid_batch_id" };

	if (typeof body.platform !== "string" || body.platform.trim() === "")
		return { ok: false, error: "invalid_platform" };

	if (typeof body.app_version !== "string" || body.app_version.trim() === "")
		return { ok: false, error: "invalid_app_version" };

	if (!Array.isArray(body.items) || body.items.length === 0)
		return { ok: false, error: "invalid_items" };

	const checkMap = (m: any): boolean => {
		if (m == null) return true;
		if (typeof m !== "object") return false;
		for (const k of Object.keys(m)) {
			if (!KEY_RE.test(k)) return false; // rechazar claves vacías o raras
			const v = m[k];
			if (!Number.isInteger(v) || v < 0) return false;
		}
		return true;
	};

	for (const it of body.items) {
		if (!it || typeof it !== "object")
			return { ok: false, error: "invalid_item" };
		if (typeof it.day !== "string" || !DAY_RE.test(it.day))
			return { ok: false, error: "invalid_day" };

		if (
			it.app_open != null &&
			(!Number.isInteger(it.app_open) || it.app_open < 0)
		)
			return { ok: false, error: "invalid_app_open" };

		if (!checkMap(it.tools)) return { ok: false, error: "invalid_tools" };
		if (!checkMap(it.ads)) return { ok: false, error: "invalid_ads" };

		// Nuevos
		if (!checkMap(it.versions)) return { ok: false, error: "invalid_versions" };
		if (!checkMap(it.versions_first_seen))
			return { ok: false, error: "invalid_versions_first_seen" };
		if (!checkMap(it.lang_primary))
			return { ok: false, error: "invalid_lang_primary" };
		if (!checkMap(it.lang_secondary))
			return { ok: false, error: "invalid_lang_secondary" };
		if (!checkMap(it.widgets)) return { ok: false, error: "invalid_widgets" };
	}

	return { ok: true, data: body as IngestBody };
}
