package com.mechanizo.android.customer.data.model.response.directions


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RouteDirections(
    @Json(name = "features")
    val features: List<Feature>?,
)