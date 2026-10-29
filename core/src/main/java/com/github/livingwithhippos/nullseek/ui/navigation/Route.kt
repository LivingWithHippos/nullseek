package com.github.livingwithhippos.nullseek.ui.navigation

import kotlinx.serialization.Serializable

/** Navigation destinations shared by the mobile and TV apps. */
sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object Lists : Route

    @Serializable
    data object Search : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object ExternalServices : Route

    @Serializable
    data object ManageProviders : Route

    /**
     * [links] optionally prefills the links field, [files] preselects file URIs.
     * [fromShare] is true when another app shared or opened the links, so we can return to it afterwards.
     */
    @Serializable
    data class NewDownload(
        val links: String? = null,
        val files: List<String> = emptyList(),
        val fromShare: Boolean = false,
    ) : Route
}
