package com.napsak.app.domain.model

/** A place found through map search. */
data class Place(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)
