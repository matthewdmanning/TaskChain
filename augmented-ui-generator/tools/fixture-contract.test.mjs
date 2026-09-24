import assert from "node:assert/strict";
import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const idPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const datePattern = /^\d{4}-\d{2}-\d{2}$/;
const colorPattern = /^#[0-9A-Fa-f]{8}$/;
const cornerNames = ["topLeft", "topRight", "bottomRight", "bottomLeft"];
const edgeNames = ["top", "right", "bottom", "left"];
const sizedCornerTypes = new Set([
  "clip", "chamfer", "clip-x", "clip-y", "scoop", "scoop-x", "scoop-y",
  "rect", "rect-x", "rect-y", "step",
]);
const sizedEdgeTypes = new Set([
  "rect", "rect-x", "rect-y", "clip-x", "clip-y", "scoop", "scoop-x", "scoop-y",
]);

// Use this function to require a non-null JSON object for a named fixture field.
// Inputs: value is the parsed field; label identifies it in failures. Dependencies: assert.
function requireRecord(value, label) {
  assert.ok(value && typeof value === "object" && !Array.isArray(value), `${label} must be an object`);
}

// Use this function to require a finite non-negative Dp value in a fixture.
// Inputs: value is the candidate number; label identifies it in failures. Dependencies: assert.
function requireDp(value, label) {
  assert.ok(Number.isFinite(value) && value >= 0, `${label} must be a non-negative number`);
}

// Use this function to validate one corner primitive against the Kotlin Cut vocabulary.
// Inputs: primitive is the parsed corner object; label identifies its location. Dependencies: requireRecord, requireDp, sizedCornerTypes, assert.
function validateCorner(primitive, label) {
  requireRecord(primitive, label);
  assert.ok(primitive.type === "none" || sizedCornerTypes.has(primitive.type), `${label}.type is unsupported`);
  if (primitive.type !== "none") requireDp(primitive.sizeDp, `${label}.sizeDp`);
}

// Use this function to validate one edge primitive against the Kotlin EdgeCut vocabulary.
// Inputs: primitive is the parsed edge object; label identifies its location. Dependencies: requireRecord, requireDp, sizedEdgeTypes, assert.
function validateEdge(primitive, label) {
  requireRecord(primitive, label);
  if (primitive.type === "center-notch") {
    requireDp(primitive.widthDp, `${label}.widthDp`);
    requireDp(primitive.depthDp, `${label}.depthDp`);
    return;
  }
  assert.ok(primitive.type === "none" || sizedEdgeTypes.has(primitive.type), `${label}.type is unsupported`);
  if (primitive.type !== "none") requireDp(primitive.sizeDp, `${label}.sizeDp`);
}

// Use this function to validate an optional border or inlay declaration.
// Inputs: value is the declaration; label identifies it; sizeKey names its Dp field. Dependencies: requireRecord, requireDp, colorPattern, assert.
function validatePaint(value, label, sizeKey) {
  if (value === null || value === undefined) return;
  requireRecord(value, label);
  requireDp(value[sizeKey], `${label}.${sizeKey}`);
  assert.match(value.color, colorPattern, `${label}.color must be #AARRGGBB`);
}

// Use this function to validate one shape before it is consumed by a renderer or agent evaluation.
// Inputs: shape is the parsed definition; label identifies it in failures. Dependencies: cornerNames, edgeNames, validateCorner, validateEdge, validatePaint, assert.
function validateShape(shape, label) {
  requireRecord(shape, label);
  assert.match(shape.id, idPattern, `${label}.id must be kebab-case`);
  requireRecord(shape.corners, `${label}.corners`);
  requireRecord(shape.edges, `${label}.edges`);
  assert.deepEqual(Object.keys(shape.corners).sort(), [...cornerNames].sort(), `${label}.corners must name all four corners`);
  assert.deepEqual(Object.keys(shape.edges).sort(), [...edgeNames].sort(), `${label}.edges must name all four edges`);
  cornerNames.forEach((name) => validateCorner(shape.corners[name], `${label}.corners.${name}`));
  edgeNames.forEach((name) => validateEdge(shape.edges[name], `${label}.edges.${name}`));
  validatePaint(shape.border, `${label}.border`, "widthDp");
  validatePaint(shape.inlay, `${label}.inlay`, "insetDp");
}

// Use this function to enforce the shared fixture contract and origin-specific metadata.
// Inputs: fixture is parsed JSON; file is its path; origin is manual or agent. Dependencies: validateShape, idPattern, datePattern, path, assert.
function validateFixture(fixture, file, origin) {
  requireRecord(fixture, file);
  assert.equal(fixture.schemaVersion, 1, `${file}: unsupported schemaVersion`);
  assert.equal(fixture.origin, origin, `${file}: origin must match its directory`);
  assert.match(fixture.id, idPattern, `${file}: id must be kebab-case`);
  assert.equal(path.basename(file), `${fixture.id}.shape.json`, `${file}: filename must match id`);
  assert.ok(typeof fixture.description === "string" && fixture.description.trim(), `${file}: description is required`);
  assert.ok(Array.isArray(fixture.shapes) && fixture.shapes.length > 0, `${file}: shapes must be non-empty`);
  if (origin === "agent") {
    assert.ok(typeof fixture.producer === "string" && idPattern.test(fixture.producer), `${file}: producer must be kebab-case`);
    assert.match(fixture.createdOn, datePattern, `${file}: createdOn must be YYYY-MM-DD`);
    assert.ok(fixture.id.startsWith(`${fixture.createdOn}-${fixture.producer}-`), `${file}: agent id must start with date and producer`);
  }
  const ids = fixture.shapes.map((shape) => shape.id);
  assert.equal(new Set(ids).size, ids.length, `${file}: shape ids must be unique`);
  fixture.shapes.forEach((shape, index) => validateShape(shape, `${file}.shapes[${index}]`));
}

// Use this function to load every fixture from one origin without a registry file.
// Inputs: origin is the fixture directory and declared source. Dependencies: readdir, readFile, JSON.parse, validateFixture, root, path.
async function loadFixtures(origin) {
  const directory = path.join(root, "fixtures", origin);
  const files = (await readdir(directory)).filter((name) => name.endsWith(".shape.json")).sort();
  assert.ok(files.length > 0, `${directory}: at least one fixture is required`);
  for (const name of files) {
    const file = path.join(directory, name);
    validateFixture(JSON.parse(await readFile(file, "utf8")), file, origin);
  }
  return files.length;
}

test("manual fixtures satisfy the shape contract", async () => {
  assert.ok(await loadFixtures("manual") > 0);
});

test("agent fixtures satisfy the shape contract", async () => {
  assert.ok(await loadFixtures("agent") > 0);
});
