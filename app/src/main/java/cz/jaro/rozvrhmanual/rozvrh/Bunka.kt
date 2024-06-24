package cz.jaro.rozvrhmanual.rozvrh

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cz.jaro.rozvrhmanual.ResponsiveText
import kotlinx.serialization.Serializable

typealias MutableTyden = MutableList<MutableDen>
typealias MutableDen = MutableList<MutableHodina>
typealias MutableHodina = MutableList<Bunka>

typealias Tyden = List<Den>
typealias Den = List<Hodina>
typealias Hodina = List<Bunka>

@Serializable
data class Bunka(
    val ucebna: String,
    val predmet: String,
    val ucitel: String,
    val tridaSkupina: String = "",
    val id: String,
    val typ: TypBunky = TypBunky.Normalni,
) {
    companion object {

        fun prazdna(id: String) = Bunka(
            ucebna = "",
            predmet = "",
            ucitel = "",
            tridaSkupina = "",
            id = id,
            typ = TypBunky.Normalni
        )
    }
}

typealias Tyden2 = List<Den2>
typealias Den2 = List<Hodina2>
typealias Hodina2 = List<Bunka2>

@Serializable
data class Bunka2(
    val ucebna: String,
    val predmet: String,
    val ucitel: String,
    val tridaSkupina: String = "",
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Bunka(
    bunka: Bunka,
    aspectRatio: Float,
    tridy: List<Vjec.TridaVjec>,
    mistnosti: List<Vjec.MistnostVjec>,
    vyucujici: List<Vjec.VyucujiciVjec>,
    kliklNaNeco: (vjec: Vjec) -> Unit,
    kliklNaPredmet: () -> Unit,
    podrzelMistnost: () -> Unit,
    icon: ImageVector?,
) = Box(
    modifier = Modifier
        .border(1.dp, MaterialTheme.colorScheme.secondary)
        .then(
            Modifier
                .aspectRatio(aspectRatio)
                .size(zakladniVelikostBunky, zakladniVelikostBunky / aspectRatio)
        )
        .background(
            when (bunka.typ) {
                TypBunky.Upravena -> MaterialTheme.colorScheme.primaryContainer
                TypBunky.Normalni -> MaterialTheme.colorScheme.background
            }
        ),
    contentAlignment = Alignment.Center
) {
    @Composable
    fun Ucebna(
        bunka: Bunka,
        mistnosti: List<Vjec.MistnostVjec>,
        kliklNaNeco: (vjec: Vjec) -> Unit
    ) {
        ResponsiveText(
            text = bunka.ucebna,
            modifier = Modifier
                .padding(all = 8.dp)
                .combinedClickable(
                    onLongClick = {
                        podrzelMistnost()
                    },
                ) {
                    if (bunka.ucebna.isEmpty()) return@combinedClickable
                    val vjec = mistnosti
                        .find { bunka.ucebna == it.zkratka } ?: return@combinedClickable
                    kliklNaNeco(vjec)
                },
            color = when (bunka.typ) {
                TypBunky.Upravena -> MaterialTheme.colorScheme.onPrimaryContainer
                TypBunky.Normalni -> MaterialTheme.colorScheme.onBackground
            },
        )
    }

    @Composable
    fun Trida(
        bunka: Bunka,
        tridy: List<Vjec.TridaVjec>,
        kliklNaNeco: (vjec: Vjec) -> Unit
    ) {
        ResponsiveText(
            text = bunka.tridaSkupina,
            modifier = Modifier
                .padding(all = 8.dp)
                .clickable {
                    if (bunka.tridaSkupina.isEmpty()) return@clickable
                    val vjec = tridy.find {
                        bunka.tridaSkupina
                            .split(" ")
                            .first() == it.zkratka
                    } ?: return@clickable
                    kliklNaNeco(vjec)
                },
            color = when (bunka.typ) {
                TypBunky.Upravena -> MaterialTheme.colorScheme.onPrimaryContainer
                TypBunky.Normalni -> MaterialTheme.colorScheme.onBackground
            },
        )
    }

    @Composable
    fun Predmet(modifier: Modifier = Modifier) = Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, null, Modifier.size(24.dp).padding(horizontal = 4.dp))
        ResponsiveText(
            text = bunka.predmet,
            modifier = Modifier
                .padding(all = 8.dp)
                .clickable {
                    kliklNaPredmet()
                },
            textAlign = TextAlign.Center,
            color = when (bunka.typ) {
                TypBunky.Upravena -> MaterialTheme.colorScheme.onPrimaryContainer
                TypBunky.Normalni -> MaterialTheme.colorScheme.primary
            }
        )
    }

    @Composable
    fun Ucitel() = ResponsiveText(
        text = bunka.ucitel,
        modifier = Modifier
            .padding(all = 8.dp)
            .clickable {
                if (bunka.ucitel.isEmpty()) return@clickable
                val vjec = vyucujici.find {
                    bunka.ucitel
                        .split(",")
                        .first() == it.zkratka
                } ?: return@clickable
                kliklNaNeco(vjec)
            },
        color = when (bunka.typ) {
            TypBunky.Upravena -> MaterialTheme.colorScheme.onPrimaryContainer
            TypBunky.Normalni -> MaterialTheme.colorScheme.onBackground
        },
    )

    val divnyRozlozeni = aspectRatio > 1F

    Row(
        Modifier
            .matchParentSize(),
        verticalAlignment = Alignment.Top,
    ) {
        if (bunka.ucebna.isNotBlank()) Box(
            Modifier,
            contentAlignment = Alignment.TopStart,
        ) {
            Ucebna(bunka, mistnosti, kliklNaNeco)
        }
        if (bunka.tridaSkupina.isNotBlank()) Box(
            Modifier
                .weight(1F),
            contentAlignment = Alignment.TopEnd,
        ) {
            Trida(bunka, tridy, kliklNaNeco)
        }
    }

    if (divnyRozlozeni) Row(
        Modifier
            .matchParentSize(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            Modifier,
            contentAlignment = Alignment.BottomStart,
        ) {
            Predmet()
        }
        if (bunka.ucitel.isNotBlank()) Box(
            Modifier
                .weight(1F),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Ucitel()
        }
    }

    if (!divnyRozlozeni) Box(
        Modifier
            .matchParentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Predmet(Modifier.fillMaxWidth(1F))
    }
    if (!divnyRozlozeni) Box(
        Modifier
            .matchParentSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Ucitel()
    }
}

val zakladniVelikostBunky = 128.dp

fun Boolean.toInt() = if (this) 1 else 0