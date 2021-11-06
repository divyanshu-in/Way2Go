package com.application.myapplication.directions


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MoreProperties(
    @Json(name = "properties")
    val properties: Properties?
) {
    @JsonClass(generateAdapter = true)
    data class Properties(
        @Json(name = "summary")
        val summary: Summary?

    ) {

        @JsonClass(generateAdapter = true)
        data class Summary(
            @Json(name = "distance")
            val distance: Double?,
            @Json(name = "duration")
            val duration: Double?
        )
    }
}