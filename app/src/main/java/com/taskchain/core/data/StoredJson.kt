package com.taskchain.core.data

import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

internal const val CURRENT_SCHEMA_VERSION = 1

/** Represents a stored file whose schema is newer than this app can safely interpret. Input: `version`; dependency: [IOException]. */
internal class UnsupportedSchemaVersionException(
    val version: Int,
) : IOException("Unsupported stored JSON schema version: $version")

/** Coordinates every repository and startup migration operation that touches local files. Constructor inputs: none; dependency: [Mutex]. */
internal object StorageIo {
    // ponytail: serialize all JSON I/O; use per-file locks if independent writes need more throughput.
    val mutex = Mutex()
}

/** Use this function to decode a stored record or legacy raw JSON payload. Inputs: `text` is file content and `serializer` is the record decoder; dependencies: [Json.parseToJsonElement], [Json.decodeFromJsonElement], and [JsonElement.withLegacyKeysRenamed]. */
internal fun <T> Json.decodeStored(
    text: String,
    serializer: DeserializationStrategy<T>,
): T {
    val stored = parseToJsonElement(text)
    val payload = stored.payloadForCurrentSchema()
    return decodeFromJsonElement(serializer, payload.withLegacyKeysRenamed())
}

/** Use this function when writing a value into the current schema envelope. Inputs: `serializer` and `value`; dependencies: [Json.encodeToJsonElement] and [Json.encodeToString]. */
internal fun <T> Json.encodeStored(
    serializer: SerializationStrategy<T>,
    value: T,
): String = encodeStoredPayload(encodeToJsonElement(serializer, value))

/** Use this function to validate a stored document before replacing or deleting it. Input: `text` is file content; dependencies: [Json.parseToJsonElement] and [JsonElement.payloadForCurrentSchema]. */
internal fun Json.validateStored(text: String) {
    parseToJsonElement(text).payloadForCurrentSchema()
}

/** Use this function to migrate one raw or enveloped JSON document while preserving current keys. Input: `text` is file content; dependencies: [Json.parseToJsonElement], [JsonElement.withLegacyKeysRenamed], and [Json.encodeToString]. */
internal fun Json.migrateStored(text: String): String {
    val stored = parseToJsonElement(text)
    val payload = stored.payloadForCurrentSchema()
    val migrated = payload.withLegacyKeysRenamed()
    return if (stored.isCurrentEnvelope() && migrated == payload) text else encodeStoredPayload(migrated)
}

/** Use this function to wrap a JSON payload in the version-one storage envelope. Input: `payload`; dependency: [Json.encodeToString]. */
private fun Json.encodeStoredPayload(payload: JsonElement): String = encodeToString(
    JsonObject.serializer(),
    buildJsonObject {
        put("schemaVersion", CURRENT_SCHEMA_VERSION)
        put("data", payload)
    },
)

/** Use this function to extract a supported envelope payload or accept a legacy raw JSON value. Input: receiver JSON element; dependency: [UnsupportedSchemaVersionException]. */
private fun JsonElement.payloadForCurrentSchema(): JsonElement {
    if (this !is JsonObject || !containsKey("schemaVersion")) return this
    val version = (get("schemaVersion") as? JsonPrimitive)?.intOrNull
        ?: throw SerializationException("Stored JSON schemaVersion must be an integer")
    if (version != CURRENT_SCHEMA_VERSION) throw UnsupportedSchemaVersionException(version)
    return get("data") ?: throw SerializationException("Stored JSON envelope is missing data")
}

/** Use this function to identify an already-versioned document without decoding its payload. Input: receiver JSON element; dependency: [JsonObject]. */
private fun JsonElement.isCurrentEnvelope(): Boolean = this is JsonObject &&
    (get("schemaVersion") as? JsonPrimitive)?.intOrNull == CURRENT_SCHEMA_VERSION &&
    get("data") != null
