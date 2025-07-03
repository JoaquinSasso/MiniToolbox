package com.joasasso.minitoolbox.tools.data

import com.squareup.moshi.Json


// MODELOS
data class CountryResponse(
    @Json(name = "name") val name: NameData?,
    @Json(name = "capital") val capital: List<String>?,
    @Json(name = "currencies") val currencies: Map<String, CurrencyData>?,
    @Json(name = "population") val population: Long?,
    @Json(name = "flag") val flagEmoji: String?,
    @Json(name = "idd") val idd: IddData?
)

data class NameData(
    @Json(name = "common") val common: String?,
    @Json(name = "official") val official: String?,
    @Json(name = "nativeName") val nativeName: Map<String, NativeNameData>?
)

data class NativeNameData(
    @Json(name = "common") val common: String?,
    @Json(name = "official") val official: String?
)

data class CurrencyData(
    @Json(name = "name") val name: String?
)

data class IddData(
    @Json(name = "root") val root: String?,
    @Json(name = "suffixes") val suffixes: List<String>?
)

