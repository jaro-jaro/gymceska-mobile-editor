package cz.jaro.rozvrh

import cz.jaro.rozvrh.rozvrh.AdresaBunky
import cz.jaro.rozvrh.rozvrh.Bunka
import kotlinx.serialization.Serializable

@Serializable
data class Change(
    val from: Bunka,
    val fromLocation: AdresaBunky,
    val to: Bunka,
    val toLocation: AdresaBunky,
)