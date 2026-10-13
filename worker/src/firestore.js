/**
 * Minimal Firestore REST client. Requests carry the service account's token, so they run with
 * admin access and bypass security rules — every handler must do its own permission checks.
 */
export class Firestore {
  constructor(projectId, accessToken) {
    this.base = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
    this.root = `projects/${projectId}/databases/(default)/documents`;
    this.headers = { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" };
  }

  /** The document at [path] as plain JS, or null if it doesn't exist. */
  async get(path) {
    const response = await fetch(`${this.base}/${path}`, { headers: this.headers });
    if (response.status === 404) return null;
    if (!response.ok) throw new Error(`Firestore get ${path}: ${response.status}`);
    return decodeDocument(await response.json());
  }

  /** Several documents by path, keyed by path; missing ones are left out. */
  async getAll(paths) {
    if (paths.length === 0) return {};
    const response = await fetch(`${this.base}:batchGet`, {
      method: "POST",
      headers: this.headers,
      body: JSON.stringify({ documents: paths.map((p) => `${this.root}/${p}`) }),
    });
    if (!response.ok) throw new Error(`Firestore batchGet: ${response.status}`);
    const out = {};
    for (const entry of await response.json()) {
      if (entry.found) out[entry.found.name.slice(this.root.length + 1)] = decodeDocument(entry.found);
    }
    return out;
  }

  /** Ids of documents in [collection] whose [field] equals [value]. */
  async idsWhere(collection, field, value) {
    const response = await fetch(`${this.base}:runQuery`, {
      method: "POST",
      headers: this.headers,
      body: JSON.stringify({
        structuredQuery: {
          from: [{ collectionId: collection }],
          where: { fieldFilter: { field: { fieldPath: field }, op: "EQUAL", value: { stringValue: value } } },
          select: { fields: [{ fieldPath: "__name__" }] },
        },
      }),
    });
    if (!response.ok) throw new Error(`Firestore query ${collection}: ${response.status}`);
    return (await response.json())
      .filter((row) => row.document)
      .map((row) => row.document.name.split("/").pop());
  }

  /**
   * Documents matching every [where] clause, as `{ path, data }`. [where] is a list of
   * `[field, op, value]` using Firestore's REST operators (EQUAL, IN, ARRAY_CONTAINS).
   * [parent] scopes the query under a document; [group] searches every collection named
   * [collection], wherever it sits.
   */
  async query(collection, where = [], { parent = "", group = false } = {}) {
    const filters = where.map(([field, op, value]) => ({
      fieldFilter: { field: { fieldPath: field }, op, value: encodeValue(value) },
    }));
    const response = await fetch(`${this.base}${parent ? `/${parent}` : ""}:runQuery`, {
      method: "POST",
      headers: this.headers,
      body: JSON.stringify({
        structuredQuery: {
          from: [{ collectionId: collection, allDescendants: group }],
          ...(filters.length === 1 ? { where: filters[0] } : {}),
          ...(filters.length > 1 ? { where: { compositeFilter: { op: "AND", filters } } } : {}),
        },
      }),
    });
    if (!response.ok) throw new Error(`Firestore query ${collection}: ${response.status} ${await response.text()}`);
    return (await response.json())
      .filter((row) => row.document)
      .map((row) => ({ path: row.document.name.slice(this.root.length + 1), data: decodeDocument(row.document) }));
  }

  /**
   * Applies [writes] atomically, 500 at a time (Firestore's limit per commit). Each write is
   * one of:
   *   { delete: path }
   *   { update: path, fields: { a: 1, "b.c": "x" } }        -- only the named fields change
   *   { transform: path, increment: { field: n }, arrayRemove: { field: [values] } }
   * Updates and transforms require the document to exist, so they never recreate one.
   */
  async commit(writes) {
    for (let i = 0; i < writes.length; i += 500) {
      const response = await fetch(`${this.base}:commit`, {
        method: "POST",
        headers: this.headers,
        body: JSON.stringify({ writes: writes.slice(i, i + 500).map((w) => this.encodeWrite(w)) }),
      });
      if (!response.ok) throw new Error(`Firestore commit: ${response.status} ${await response.text()}`);
    }
  }

  encodeWrite(write) {
    const name = (path) => `${this.root}/${path}`;
    if (write.delete) return { delete: name(write.delete) };
    if (write.update) {
      const fields = {};
      for (const [path, value] of Object.entries(write.fields)) setNested(fields, path.split("."), encodeValue(value));
      return {
        update: { name: name(write.update), fields },
        updateMask: { fieldPaths: Object.keys(write.fields).map(quotePath) },
        currentDocument: { exists: true },
      };
    }
    const fieldTransforms = [
      ...Object.entries(write.increment || {}).map(([fieldPath, n]) => ({
        fieldPath: quotePath(fieldPath),
        increment: { integerValue: String(n) },
      })),
      ...Object.entries(write.arrayRemove || {}).map(([fieldPath, values]) => ({
        fieldPath: quotePath(fieldPath),
        removeAllFromArray: { values: values.map(encodeValue) },
      })),
    ];
    return { transform: { document: name(write.transform), fieldTransforms }, currentDocument: { exists: true } };
  }

  /**
   * Creates [path] only if it doesn't exist yet. Returns false when it already did — used as a
   * once-only marker so the same push can't be triggered twice.
   */
  async createOnce(path, fields) {
    const response = await fetch(`${this.base}/${path}?currentDocument.exists=false`, {
      method: "PATCH",
      headers: this.headers,
      body: JSON.stringify({ fields: encodeFields(fields) }),
    });
    if (response.ok) return true;
    // "Already exists" comes back as a failed precondition; anything else is a real error.
    const status = (await response.json().catch(() => ({})))?.error?.status;
    if (status === "FAILED_PRECONDITION" || status === "ALREADY_EXISTS") return false;
    throw new Error(`Firestore createOnce ${path}: ${response.status} ${status}`);
  }

  /** Removes [field] from the document at [path]. */
  async deleteField(path, field) {
    await fetch(`${this.base}/${path}?updateMask.fieldPaths=${encodeURIComponent(field)}&currentDocument.exists=true`, {
      method: "PATCH",
      headers: this.headers,
      body: JSON.stringify({ fields: {} }),
    });
  }
}

export function decodeDocument(doc) {
  return decodeFields(doc.fields || {});
}

function decodeFields(fields) {
  const out = {};
  for (const [key, value] of Object.entries(fields)) out[key] = decodeValue(value);
  return out;
}

export function decodeValue(value) {
  if ("stringValue" in value) return value.stringValue;
  if ("integerValue" in value) return Number(value.integerValue);
  if ("doubleValue" in value) return value.doubleValue;
  if ("booleanValue" in value) return value.booleanValue;
  if ("nullValue" in value) return null;
  if ("timestampValue" in value) return value.timestampValue;
  if ("mapValue" in value) return decodeFields(value.mapValue.fields || {});
  if ("arrayValue" in value) return (value.arrayValue.values || []).map(decodeValue);
  return undefined;
}

function encodeValue(value) {
  if (value === null || value === undefined) return { nullValue: null };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(encodeValue) } };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  return { stringValue: String(value) };
}

/** Builds the nested map value that a dotted update path like "a.b" refers to. */
function setNested(fields, [head, ...rest], value) {
  if (rest.length === 0) {
    fields[head] = value;
    return;
  }
  fields[head] ??= { mapValue: { fields: {} } };
  setNested(fields[head].mapValue.fields, rest, value);
}

/** Field paths need backticks around segments that aren't plain identifiers, such as uids starting with a digit. */
function quotePath(path) {
  return path
    .split(".")
    .map((part) => (/^[A-Za-z_][A-Za-z0-9_]*$/.test(part) ? part : `\`${part}\``))
    .join(".");
}

function encodeFields(fields) {
  const out = {};
  for (const [key, value] of Object.entries(fields)) {
    if (typeof value === "number") out[key] = { integerValue: String(Math.trunc(value)) };
    else out[key] = { stringValue: String(value) };
  }
  return out;
}
