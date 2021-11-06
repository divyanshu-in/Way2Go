package com.mechanizo.android.customer.data.model.response.directions


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Geometry(
    @Json(name = "coordinates")
    val coordinates: List<List<Double>>?,
)
