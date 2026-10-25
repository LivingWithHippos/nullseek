package com.github.livingwithhippos.nullseek.core.data.stremio

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Serializer for [AddonResource] that supports both Stremio string resources (`"stream"`)
 * and object resources (`{"name": "stream", "types": [...], "idPrefixes": [...]}`).
 */
object AddonResourceSerializer : KSerializer<AddonResource> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("AddonResource") {
            element<String>("name")
            element<List<String>>("types", isOptional = true)
            element<List<String>>("idPrefixes", isOptional = true)
        }

    override fun deserialize(decoder: Decoder): AddonResource {
        val jsonDecoder = decoder as? JsonDecoder ?: error("AddonResourceSerializer only supports JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                AddonResource(
                    name = element.contentOrNull.orEmpty(),
                    types = emptyList(),
                    idPrefixes = null,
                )
            }

            else -> {
                val obj = element.jsonObject
                val name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val types =
                    (obj["types"] as? JsonArray)
                        ?.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf(String::isNotBlank) }
                        .orEmpty()
                val idPrefixes =
                    (obj["idPrefixes"] as? JsonArray)
                        ?.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf(String::isNotBlank) }
                AddonResource(
                    name = name,
                    types = types,
                    idPrefixes = idPrefixes,
                )
            }
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: AddonResource,
    ) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("AddonResourceSerializer only supports JSON")
        val jsonObj =
            buildJsonObject {
                put("name", value.name)
                put("types", JsonArray(value.types.map { JsonPrimitive(it) }))
                if (value.idPrefixes != null) {
                    put("idPrefixes", JsonArray(value.idPrefixes.map { JsonPrimitive(it) }))
                } else {
                    put("idPrefixes", JsonNull)
                }
            }
        jsonEncoder.encodeJsonElement(jsonObj)
    }
}

/**
 * Stremio addon resource definition
 */
@Keep
@Serializable(with = AddonResourceSerializer::class)
@Parcelize
data class AddonResource(
    @SerialName("name") val name: String,
    @SerialName("types") val types: List<String> = emptyList(),
    @SerialName("idPrefixes") val idPrefixes: List<String>? = null,
) : Parcelable

/**
 * Stremio addon manifest
 * https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/api/responses/manifest.md
 */
@Keep
@Serializable
@Parcelize
data class AddonManifest(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String = "",
    @SerialName("version") val version: String,
    @SerialName("resources") val resources: List<AddonResource>,
    @SerialName("types") val types: List<String> = emptyList(),
    @SerialName("catalogs") val catalogs: List<AddonCatalog> = emptyList(),
    @SerialName("idPrefixes") val idPrefixes: List<String>? = null,
    @SerialName("background") val background: String? = null,
    @SerialName("logo") val logo: String? = null,
    @SerialName("contactEmail") val contactEmail: String? = null,
    @SerialName("behaviorHints") val behaviorHints: AddonBehaviorHints? = null,
) : Parcelable {
    /**
     * Returns resources with fallback `types` and `idPrefixes` inherited from the root manifest.
     */
    fun normalizedResources(): List<AddonResource> =
        resources
            .filter { it.name.isNotBlank() }
            .map { resource ->
                AddonResource(
                    name = resource.name,
                    types = resource.types.ifEmpty { types },
                    idPrefixes =
                        if (resource.idPrefixes.isNullOrEmpty()) {
                            idPrefixes
                        } else {
                            resource.idPrefixes
                        },
                )
            }
}

@Keep
@Serializable
@Parcelize
data class AddonCatalog(
    @SerialName("type") val type: String,
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("extra") val extra: List<AddonCatalogExtra>? = null,
) : Parcelable

@Keep
@Serializable
@Parcelize
data class AddonCatalogExtra(
    @SerialName("name") val name: String,
    @SerialName("isRequired") val isRequired: Boolean? = null,
    @SerialName("options") val options: List<String>? = null,
    @SerialName("optionsLimit") val optionsLimit: Int? = null,
) : Parcelable

@Keep
@Serializable
@Parcelize
data class AddonBehaviorHints(
    @SerialName("adult") val adult: Boolean? = null,
    @SerialName("p2p") val p2p: Boolean? = null,
    @SerialName("configurable") val configurable: Boolean? = null,
    @SerialName("configurationRequired") val configurationRequired: Boolean? = null,
) : Parcelable

/**
 * Stremio stream response
 * https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/api/responses/stream.md
 */
@Keep
@Serializable
@Parcelize
data class AddonStream(
    @SerialName("url") val url: String? = null,
    @SerialName("ytId") val ytId: String? = null,
    @SerialName("infoHash") val infoHash: String? = null,
    @SerialName("fileIdx") val fileIdx: Int? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("externalUrl") val externalUrl: String? = null,
    @SerialName("sources") val sources: List<String>? = null,
    @SerialName("behaviorHints") val behaviorHints: StreamBehaviorHints? = null,
) : Parcelable

@Keep
@Serializable
@Parcelize
data class StreamBehaviorHints(
    @SerialName("bingeGroup") val bingeGroup: String? = null,
    @SerialName("notWebReady") val notWebReady: Boolean? = null,
    @SerialName("videoSize") val videoSize: Long? = null,
    @SerialName("filename") val filename: String? = null,
) : Parcelable

/**
 * Stremio API response wrapper
 */
@Keep
@Serializable
data class AddonResponse<T>(
    @SerialName("streams") val streams: List<T>? = null,
)

/**
 * Meta item preview for catalog responses
 */
@Keep
@Serializable
@Parcelize
data class AddonMetaPreview(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("name") val name: String,
    @SerialName("poster") val poster: String? = null,
    @SerialName("description") val description: String? = null,
) : Parcelable
