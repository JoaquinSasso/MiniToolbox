package com.joasasso.minitoolbox.tools.data

data class CountryResponse(
    val name: String,
    val official: String,
    val native: String,
    val currency: String,
    val capital: List<String>,
    val phoneCode: String,
    val flag: String,
    val population: Long
)

data class MinimalCountry(
    val name: String,
    val flag: String
)
