package com.github.livingwithhippos.nullseek.core.data.database.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.github.livingwithhippos.nullseek.core.data.stremio.ImdbTitleResult

/** An IMDb title the user starred in the Stremio search. Keeps enough to show it without a network call. */
@Entity(tableName = "imdb_favourites")
data class ImdbFavouriteEntity(
    @PrimaryKey val imdbId: String,
    val title: String,
    val year: String? = null,
    val type: String = "movie",
    val posterUrl: String? = null,
    val imdbRating: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis(),
) {
    fun toTitle(): ImdbTitleResult =
        ImdbTitleResult(
            imdbId = imdbId,
            title = title,
            year = year,
            type = type,
            posterUrl = posterUrl,
            imdbRating = imdbRating,
        )

    companion object {
        fun fromTitle(title: ImdbTitleResult): ImdbFavouriteEntity =
            ImdbFavouriteEntity(
                imdbId = title.imdbId,
                title = title.title,
                year = title.year,
                type = title.type,
                posterUrl = title.posterUrl,
                imdbRating = title.imdbRating,
            )
    }
}
