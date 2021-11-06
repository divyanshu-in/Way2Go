package com.mechanizo.android.customer.data.model.response.directions


import com.application.myapplication.directions.MoreProperties
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Feature(
    @Json(name = "geometry")
    val geometry: Geometry?,
    @Json(name = "properties")
    val properties: MoreProperties.Properties
)