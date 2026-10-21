package com.github.livingwithhippos.nullseek.core.data.stremio

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stremio addon resource definition
 */
@Keep
@Serializable
@Parcelize
data class AddonResource(
    @SerialName("name") val name: String,
    @SerialName("types") val types: List<String>,
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
    @SerialName("description") val description: String,
    @SerialName("version") val version: String,
    @SerialName("resources") val resources: List<AddonResource>,
    @SerialName("types") val types: List<String>,
    @SerialName("catalogs") val catalogs: List<AddonCatalog> = emptyList(),
    @SerialName("idPrefixes") val idPrefixes: List<String>? = null,
    @SerialName("background") val background: String? = null,
    @SerialName("logo") val logo: String? = null,
    @SerialName("contactEmail") val contactEmail: String? = null,
    @SerialName("behaviorHints") val behaviorHints: AddonBehaviorHints? = null,
) : Parcelable

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
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
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
