package com.joasasso.minitoolbox.data

data class CountryResponse(
    val name: String,
    val englishName: String,
    val official: String,
    val native: String,
    val currency: String,
    val capital: List<String>,
    val phoneCode: String,
    val flag: String,
    val population: Long
)

data class MinimalCountry(
    val name: String,          // Español
    val englishName: String,   // Inglés
    val flag: String           // Emoji o símbolo
)


data class CapitalOfCountry(
    val name: String,          // nombre en español
    val englishName: String,   // nombre en inglés
    val capital: String
)

