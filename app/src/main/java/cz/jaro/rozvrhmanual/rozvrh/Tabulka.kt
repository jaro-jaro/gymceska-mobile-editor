package cz.jaro.rozvrhmanual.rozvrh

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.ContentPasteOff
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.dp
import cz.jaro.rozvrhmanual.ResponsiveText
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

data class AdresaBunky(
    val iDne: Int,
    val iHodiny: Int,
    val iBunky: Int,
)

context(ColumnScope)
@Composable
fun Tabulka(
    vjec: Vjec,
    tabulka: Tyden,
    kliklNaNeco: (vjec: Vjec) -> Unit,
    tridy: List<Vjec.TridaVjec>,
    mistnosti: List<Vjec.MistnostVjec>,
    vyucujici: List<Vjec.VyucujiciVjec>,
    edit: (AdvancedTyden.() -> AdvancedTyden) -> Unit,
    pamet: AdresaBunky?,
    zapamatovat: (AdresaBunky?) -> Unit,
    ziskatRozvrh: (Vjec) -> Tyden,
    vybratRozvrh: (Vjec) -> Unit,
) {
    if (tabulka.isEmpty()) return

    val horScrollState = rememberScrollState(Int.MAX_VALUE)
    val verScrollState = rememberScrollState(Int.MAX_VALUE)

    Column(
        Modifier.doubleScrollable(horScrollState, verScrollState)
    ) {
        val maxy = tabulka.map { radek -> radek.maxOf { hodina -> hodina.size } }
        val polovicniBunky = remember(tabulka) {
            val minLimit = if (vjec !is Vjec.TridaVjec) 2 else 4
            tabulka.map { radek -> radek.maxBy { it.size }.size >= minLimit }
        }

        Row(
            modifier = Modifier
                .verticalScroll(rememberScrollState(), enabled = false)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        ) {

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState(), enabled = false)
                    .border(1.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1F)
                        .border(1.dp, MaterialTheme.colorScheme.secondary)
                        .size(zakladniVelikostBunky / 2, zakladniVelikostBunky / 2),
                    contentAlignment = Alignment.Center,
                ) {
                    ResponsiveText(
                        text = tabulka[0][0][0].predmet,
                        modifier = Modifier
                            .padding(all = 8.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(horScrollState, enabled = false, reverseScrolling = true)
                    .border(1.dp, MaterialTheme.colorScheme.secondary)
            ) {
                tabulka.first().drop(1).map { it.first() }.forEachIndexed { i, bunka ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(2F / 1)
                            .border(1.dp, MaterialTheme.colorScheme.secondary)
                            .size(zakladniVelikostBunky, zakladniVelikostBunky / 2),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.matchParentSize(),
                            contentAlignment = if (bunka.ucitel.isBlank()) Alignment.Center else Alignment.TopCenter,
                        ) {
                            ResponsiveText(
                                text = bunka.predmet,
                                modifier = Modifier
                                    .padding(all = 8.dp)
                                    .clickable {
                                        if (bunka.predmet.isEmpty()) return@clickable
                                        kliklNaNeco(if (vjec is Vjec.HodinaVjec) tridy.find {
                                            bunka.predmet == it.zkratka
                                        } ?: return@clickable else Vjec.HodinaVjec(
                                            zkratka = bunka.predmet.split(".")[0],
                                            jmeno = "${bunka.predmet} hodina",
                                            index = i + 1
                                        ))
                                    },
                            )
                        }
                        Box(
                            Modifier.matchParentSize(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            ResponsiveText(
                                text = bunka.ucitel,
                                modifier = Modifier
                                    .padding(all = 8.dp),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .verticalScroll(verScrollState, enabled = false, reverseScrolling = true),
        ) {
            Row {
                Column(
                    Modifier.horizontalScroll(rememberScrollState())
                ) {
                    tabulka.drop(1).map { it.first().first() }.forEachIndexed { i, bunka ->
                        Column(
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio((if (polovicniBunky[i + 1]) 2F else 1F) / maxy[i + 1])
                                    .border(1.dp, MaterialTheme.colorScheme.secondary)
                                    .size(zakladniVelikostBunky / 2, zakladniVelikostBunky * maxy[i + 1] / (if (polovicniBunky[i + 1]) 2F else 1F)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier.matchParentSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ResponsiveText(
                                        text = bunka.predmet,
                                        modifier = Modifier
                                            .padding(all = 8.dp)
                                            .clickable {
                                                if (bunka.predmet.isEmpty()) return@clickable
                                                kliklNaNeco(
                                                    if (vjec is Vjec.DenVjec) tridy.find {
                                                        bunka.predmet == it.zkratka
                                                    } ?: return@clickable else Seznamy.dny.find { it.zkratka == bunka.predmet }!!
                                                )
                                            },
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    Modifier.horizontalScroll(horScrollState, enabled = false, reverseScrolling = true)
                ) {
                    tabulka.drop(1).forEachIndexed { iDne, radek ->
                        val nasobitelVyskyCeleRadky = when {
                            polovicniBunky[iDne + 1] -> 1F / 2F
                            else -> 1F
                        }
                        Row {
                            radek.drop(1).forEachIndexed { iHodiny, hodina ->
                                Column(
                                    modifier = Modifier
                                        .border(1.dp, MaterialTheme.colorScheme.secondary)
                                ) {
                                    hodina.forEachIndexed { iBunky, bunka ->
                                        BunkaAOkoli(
                                            vjec = vjec,
                                            hodina = hodina,
                                            bunka = bunka,
                                            pamet = pamet,
                                            edit = edit,
                                            zapamatovat = zapamatovat,
                                            maxy = maxy,
                                            nasobitelVyskyCeleRadky = nasobitelVyskyCeleRadky,
                                            tridy = tridy,
                                            mistnosti = mistnosti,
                                            vyucujici = vyucujici,
                                            kliklNaNeco = kliklNaNeco,
                                            adresa = AdresaBunky(iDne + 1, iHodiny + 1, iBunky),
                                            ziskatRozvrh = ziskatRozvrh,
                                            vybratRozvrh = vybratRozvrh,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BunkaAOkoli(
    vjec: Vjec,
    hodina: Hodina,
    bunka: Bunka,
    pamet: AdresaBunky?,
    edit: (AdvancedTyden.() -> AdvancedTyden) -> Unit,
    zapamatovat: (AdresaBunky?) -> Unit,
    maxy: List<Int>,
    nasobitelVyskyCeleRadky: Float,
    tridy: List<Vjec.TridaVjec>,
    mistnosti: List<Vjec.MistnostVjec>,
    vyucujici: List<Vjec.VyucujiciVjec>,
    kliklNaNeco: (vjec: Vjec) -> Unit,
    ziskatRozvrh: (Vjec) -> Tyden,
    adresa: AdresaBunky,
    vybratRozvrh: (Vjec) -> Unit,
) {
    val nasobitelVyskyTetoBunky = when {
        vjec is Vjec.TridaVjec && hodina.size == 1 && bunka.tridaSkupina.isNotBlank() -> 4F / 5F
        else -> 1F
    }
    var novaUcebna by remember { mutableStateOf("") }
    var novaBunkaDialog by remember { mutableStateOf(false) }
    var mistnostDialog by remember { mutableStateOf(false) }
    var vysledkyDialog by remember { mutableStateOf(emptyList<String>()) }
    var vysledkyDialog2 by remember { mutableStateOf(emptyList<String>()) }

    fun presun() {
        require(pamet != null)
        edit {
            if (pamet.jeCelaHodina())
                presunoutHodinu(pamet.iDne, pamet.iHodiny, adresa.iDne, adresa.iHodiny)
                    .pridatBunku(pamet, Bunka.prazdna("${vjec.zkratka}-${pamet.iDne}-${pamet.iHodiny}-#"))
                    .let { at ->
                        val i = at[adresa.iDne, adresa.iHodiny].indexOfFirst { it.id.last() == '#' }
                        if (i == -1) at
                        else at.odstranitBunku(adresa.iDne, adresa.iHodiny, i)
                    }
            else
                presunoutBunku(pamet, adresa.iDne, adresa.iHodiny)
                    .let { at ->
                        if (at[pamet.iDne, pamet.iHodiny].isEmpty())
                            at.pridatBunku(pamet, Bunka.prazdna("${vjec.zkratka}-${pamet.iDne}-${pamet.iHodiny}-#"))
                        else
                            at
                    }
                    .let { at ->
                        val i = at[adresa.iDne, adresa.iHodiny].indexOfFirst { it.id.last() == '#' }
                        if (i == -1) at
                        else at.odstranitBunku(adresa.iDne, adresa.iHodiny, i)
                    }
        }
        zapamatovat(null)
    }

    fun prohodit() {
        require(pamet != null)
        edit {
            if (pamet.jeCelaHodina()) prohoditHodiny(pamet.iDne, pamet.iHodiny, adresa.iDne, adresa.iHodiny)
            else prohoditBunky(pamet, adresa)
        }
        zapamatovat(null)
    }

    fun najitProhozeni() {
        require(vjec is Vjec.TridaVjec)
        val plniVyucujici = tridy.drop(1).flatMap { trida ->
            val h = ziskatRozvrh(trida)[adresa.iDne][adresa.iHodiny]
            h.map { bunka ->
                bunka.ucitel
            }
        }

        val rozvrh = ziskatRozvrh(vjec)
        val vyucujiciTridy = rozvrh.drop(1).flatMap { den ->
            den.drop(1).flatMap { hodina ->
                hodina.map { bunka ->
                    bunka.ucitel
                }
            }
        }

        val volniVyucujici = vyucujici.map { it.zkratka }.filter { it !in plniVyucujici }.filter {
            it in vyucujiciTridy
        }

        val vyucujiciNyni = rozvrh[adresa.iDne][adresa.iHodiny].map { it.ucitel }

        val rozvrhyVyucujicich = vyucujiciNyni.map { v ->
            ziskatRozvrh(vyucujici.first { it.zkratka == v })
        }

        val volneHodiny = rozvrhyVyucujicich.first().drop(1).indices.flatMap { iDne ->
            rozvrhyVyucujicich.first().drop(1).first().drop(1).indices.map { iHodiny ->
                if (rozvrhyVyucujicich.all { tyden ->
                        val h = tyden[iDne + 1][iHodiny + 1]
                    h.singleOrNull()?.predmet?.isEmpty() == true
                }) iDne to iHodiny else null
            }
        }.filterNotNull()

        val vymenitelneHodiny = rozvrh.drop(1).flatMapIndexed { iDne, den ->
            den.drop(1).mapIndexed { iHodiny, hodina ->
                if (hodina.all { b -> b.ucitel in volniVyucujici } && (iDne to iHodiny) in volneHodiny) iDne to iHodiny else null
            }
        }.filterNotNull()

        vysledkyDialog2 = vymenitelneHodiny.map {
            "H ${Seznamy.dny1Pad[it.first]} ${Seznamy.hodiny1Pad[it.second]}"
        }
    }

    fun najitVolnou() {
        val plneTridy = tridy.drop(1).flatMap { trida ->
            val h = ziskatRozvrh(trida)[adresa.iDne][adresa.iHodiny]
            h.map { bunka ->
                bunka.ucebna
            }
        }

        val vysledek = mistnosti.filter { it.zkratka !in plneTridy }

        vysledkyDialog = vysledek.map { "M ${it.zkratka}" }
    }

    Bunka(
        bunka = bunka,
        aspectRatio = hodina.size / (maxy[adresa.iDne] * nasobitelVyskyTetoBunky * nasobitelVyskyCeleRadky),
        tridy = tridy,
        mistnosti = mistnosti,
        vyucujici = vyucujici,
        kliklNaNeco = kliklNaNeco,
        kliklNaPredmet = {
            when {
                vjec !is Vjec.TridaVjec -> Unit

                pamet == null -> if (bunka.predmet.isNotEmpty()) zapamatovat(adresa)
                pamet == adresa.celaHodina -> novaBunkaDialog = true
                pamet.celaHodina == adresa.celaHodina -> zapamatovat(pamet.celaHodina)

                bunka.predmet.isEmpty() -> presun()
                pamet.jeCelaHodina() -> prohodit()
                else -> novaBunkaDialog = true
            }
        },
        podrzelMistnost = {
            if (vjec !is Vjec.TridaVjec) return@Bunka
            novaUcebna = bunka.ucebna
            mistnostDialog = true
        },
        icon = when {
            vjec !is Vjec.TridaVjec -> null

            pamet == null -> if (bunka.predmet.isNotEmpty()) Icons.Default.FileCopy else null
            pamet == adresa.celaHodina -> Icons.Default.MoreVert
            pamet.celaHodina == adresa.celaHodina -> Icons.Default.FolderCopy

            bunka.predmet.isEmpty() -> Icons.AutoMirrored.Filled.ArrowRightAlt
            pamet.jeCelaHodina() -> Icons.Default.Shuffle
            else -> Icons.Default.MoreVert
        }
    )
    if (nasobitelVyskyTetoBunky < 1F) Bunka(
        bunka = Bunka.prazdna("${vjec.zkratka}-${adresa.iDne}-${adresa.iHodiny}-#"),
        aspectRatio = hodina.size / (maxy[adresa.iDne] * (1F - nasobitelVyskyTetoBunky) * nasobitelVyskyCeleRadky),
        tridy = tridy,
        mistnosti = mistnosti,
        vyucujici = vyucujici,
        kliklNaNeco = {},
        kliklNaPredmet = {
            if (pamet.neniNullAniCelaHodina()) presun()
        },
        podrzelMistnost = {},
        icon = when {
            pamet.neniNullAniCelaHodina() -> Icons.AutoMirrored.Filled.ArrowRightAlt
            else -> null
        }
    )

    if (novaBunkaDialog) AlertDialog(
        onDismissRequest = {
            novaBunkaDialog = false
        },
        confirmButton = {
            TextButton(
                onClick = {
                    novaBunkaDialog = false
                }
            ) {
                Text("Zrušit")
            }
        },
        text = {
            Column {
                require(pamet != adresa)
                if (pamet == adresa.celaHodina)  {
                    TextButton(
                        onClick = {
                            zapamatovat(null)
                            novaBunkaDialog = false
                        },
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                    ) {
                        Icon(Icons.Default.ContentPasteOff, null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Zapomenout")
                    }
                    TextButton(
                        onClick = {
                            najitProhozeni()
                            novaBunkaDialog = false
                        },
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                    ) {
                        Icon(Icons.Default.QuestionMark, null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Najít prohození")
                    }
                }
                else if (bunka.predmet.isNotEmpty() && pamet.neniNullAniCelaHodina())  {
                    TextButton(
                        onClick = {
                            presun()
                            novaBunkaDialog = false
                        },
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowRightAlt, null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Přesunout sem")
                    }
                    TextButton(
                        onClick = {
                            prohodit()
                            novaBunkaDialog = false
                        },
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                    ) {
                        Icon(Icons.Default.Shuffle, null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Prohodit")
                    }
                }
            }
        }
    )
    if (mistnostDialog) AlertDialog(
        onDismissRequest = {
            mistnostDialog = false
        },
        confirmButton = {
            TextButton(
                onClick = {
                    edit {
                        zmenitBunku(adresa) {
                            it.copy(ucebna = novaUcebna)
                        }
                    }
                    mistnostDialog = false
                }
            ) {
                Text("OK")
            }
        },
        text = {
            Column {
                TextField(
                    value = novaUcebna,
                    onValueChange = {
                        novaUcebna = it
                    },
                    Modifier
                        .fillMaxWidth(1F)
                        .padding(8.dp),
                    label = {
                        Text("Učebna")
                    },
                )
                TextButton(
                    onClick = {
                        najitVolnou()
                        novaBunkaDialog = false
                    },
                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                ) {
                    Icon(Icons.Default.QuestionMark, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Najít volnou")
                }
            }
        }
    )
    if (vysledkyDialog.isNotEmpty()) AlertDialog(
        onDismissRequest = {
            vysledkyDialog = emptyList()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    vysledkyDialog = emptyList()
                }
            ) {
                Text("OK")
            }
        },
        text = {
            LazyColumn {
                item {
                    Text("Nalezeno:")
                }
                items(vysledkyDialog.toList()) { text ->
                    Text(text.drop(2), Modifier.clickable {
                        if (text.startsWith("M")) {
                            mistnosti.find { it.zkratka == text.split(" ")[1] }?.let { vybratRozvrh(it) }
                        }
                        if (text.startsWith("V")) {
                            vyucujici.find { it.zkratka == text.split(" ")[1] }?.let { vybratRozvrh(it) }
                        }
                        vysledkyDialog = emptyList()
                    })
                }
            }
        }
    )
    if (vysledkyDialog2.isNotEmpty() && vysledkyDialog.isEmpty()) AlertDialog(
        onDismissRequest = {
            vysledkyDialog2 = emptyList()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    vysledkyDialog2 = emptyList()
                }
            ) {
                Text("OK")
            }
        },
        text = {
            LazyColumn {
                item {
                    Text("Nalezeno:")
                }
                items(vysledkyDialog2.toList()) { text ->
                    Text(text.drop(2), Modifier.clickable {
                        val iDne = Seznamy.dny1Pad.indexOf(text.split(" ")[1])
                        val iHodiny = Seznamy.hodiny1Pad.indexOf(text.split(" ", limit = 3)[2])

                        edit {
                            val at = prohoditHodiny(iDne + 1, iHodiny + 1, adresa.iDne, adresa.iHodiny)

                            println(at[1, 1])

                            val p = najitProblemy(vyucujici, mistnosti) { v1 ->
                                when (v1) {
                                    vjec -> at.dny
                                    is Vjec.TridaVjec -> ziskatRozvrh(v1)
                                    is Vjec.MistnostVjec, is Vjec.VyucujiciVjec -> TvorbaRozvrhu.vytvoritRozvrhPodleJinych(v1, tridy.drop(1)) { v2 ->
                                        when (v2) {
                                            vjec -> at.dny
                                            else -> ziskatRozvrh(v2)
                                        }
                                    }

                                    else -> throw IllegalArgumentException()
                                }
                            }

                            vysledkyDialog = p

                            this
                        }
                    })
                }
            }
        }
    )
}

val AdresaBunky.celaHodina get() = copy(iBunky = CELA_HODINA)

fun AdresaBunky.jeCelaHodina() = iBunky == CELA_HODINA
fun AdresaBunky?.neniNullAleCelaHodina() = this != null && iBunky == CELA_HODINA
fun AdresaBunky?.jeNullNeboCelaHodina() = this == null || iBunky == CELA_HODINA
fun AdresaBunky?.neniNullAniCelaHodina() = !jeNullNeboCelaHodina()

private fun Modifier.doubleScrollable(
    scrollStateX: ScrollState,
    scrollStateY: ScrollState
) = composed {
    val coroutineScope = rememberCoroutineScope()

    val flingBehaviorX = ScrollableDefaults.flingBehavior()
    val flingBehaviorY = ScrollableDefaults.flingBehavior()

    val velocityTracker = remember { VelocityTracker() }
    val nestedScrollDispatcher = remember { NestedScrollDispatcher() }

    pointerInput(Unit) {
        detectDragGestures(
            onDrag = { pointerInputChange, offset ->
                coroutineScope.launch {
                    velocityTracker.addPointerInputChange(pointerInputChange)
                    scrollStateX.scrollBy(offset.x)
                    scrollStateY.scrollBy(offset.y)
                }
            },
            onDragEnd = {
                val velocity = velocityTracker.calculateVelocity()
                velocityTracker.resetTracking()
                coroutineScope.launch {
                    scrollStateX.scroll {
                        val scrollScope = object : ScrollScope {
                            override fun scrollBy(pixels: Float): Float {
                                val consumedByPreScroll = nestedScrollDispatcher.dispatchPreScroll(Offset(pixels, 0F), NestedScrollSource.Fling).x
                                val scrollAvailableAfterPreScroll = pixels - consumedByPreScroll
                                val consumedBySelfScroll = this@scroll.scrollBy(scrollAvailableAfterPreScroll)
                                val deltaAvailableAfterScroll = scrollAvailableAfterPreScroll - consumedBySelfScroll
                                val consumedByPostScroll = nestedScrollDispatcher.dispatchPostScroll(
                                    Offset(consumedBySelfScroll, 0F),
                                    Offset(deltaAvailableAfterScroll, 0F),
                                    NestedScrollSource.Fling
                                ).x
                                return consumedByPreScroll + consumedBySelfScroll + consumedByPostScroll
                            }
                        }

                        with(flingBehaviorX) {
                            scrollScope.performFling(velocity.x)
                        }
                    }
                }
                coroutineScope.launch {
                    scrollStateY.scroll {
                        val scrollScope = object : ScrollScope {
                            override fun scrollBy(pixels: Float): Float {
                                val consumedByPreScroll = nestedScrollDispatcher.dispatchPreScroll(Offset(0F, pixels), NestedScrollSource.Fling).y
                                val scrollAvailableAfterPreScroll = pixels - consumedByPreScroll
                                val consumedBySelfScroll = this@scroll.scrollBy(scrollAvailableAfterPreScroll)
                                val deltaAvailableAfterScroll = scrollAvailableAfterPreScroll - consumedBySelfScroll
                                val consumedByPostScroll = nestedScrollDispatcher.dispatchPostScroll(
                                    Offset(0F, consumedBySelfScroll),
                                    Offset(0F, deltaAvailableAfterScroll),
                                    NestedScrollSource.Fling
                                ).y
                                return consumedByPreScroll + consumedBySelfScroll + consumedByPostScroll
                            }
                        }

                        with(flingBehaviorY) {
                            scrollScope.performFling(velocity.y)
                        }
                    }
                }
            },
            onDragStart = {
                velocityTracker.resetTracking()
            }
        )
    }
}

@Serializable(AdvancedTyden.Serializer::class)
class AdvancedTyden {

    @PublishedApi
    internal val schovka: Hodina?
    val dny: Tyden

    @PublishedApi
    internal constructor(
        dny: Tyden,
        schovka: Hodina?,
    ) {
        this.dny = dny
        this.schovka = schovka
    }

    constructor(
        dny: Tyden,
    ) {
        this.dny = dny
        this.schovka = null
    }

    operator fun get(iDne: Int) = dny[iDne]
    operator fun get(iDne: Int, iHodiny: Int) = dny[iDne][iHodiny]
    operator fun get(iDne: Int, iHodiny: Int, iBunky: Int) = dny[iDne][iHodiny][iBunky]

    inline fun zmnitTyden(mutate: (MutableTyden) -> Unit) = AdvancedTyden(dny.toMutableTyden().also(mutate), schovka)
    inline fun zmenitDen(iDne: Int, mutate: (MutableDen) -> Unit) = zmnitTyden { it[iDne].also(mutate) }
    inline fun zmenitHodinu(iDne: Int, iHodiny: Int, mutate: (MutableHodina) -> Unit) = zmenitDen(iDne) { it[iHodiny].also(mutate) }
    inline fun zmenitBunku(iDne: Int, iHodiny: Int, iBunky: Int, mutate: (Bunka) -> Bunka) = zmenitHodinu(iDne, iHodiny) {
        it[iBunky] = mutate(it[iBunky])
    }

    fun presunoutBunku(iDne1: Int, iHodiny1: Int, iBunky1: Int, iDne2: Int, iHodiny2: Int) =
        pridatBunku(iDne2, iHodiny2, this[iDne1, iHodiny1, iBunky1]).odstranitBunku(iDne1, iHodiny1, iBunky1)

    fun presunoutHodinu(iDne1: Int, iHodiny1: Int, iDne2: Int, iHodiny2: Int) =
        pridatHodinu(iDne2, iHodiny2, this[iDne1, iHodiny1]).odstranitHodinu(iDne1, iHodiny1)

    fun prohoditBunky(iDne1: Int, iHodiny1: Int, iBunky1: Int, iDne2: Int, iHodiny2: Int, iBunky2: Int) =
        presunoutBunku(iDne1, iHodiny1, iBunky1, iDne2, iHodiny2).presunoutBunku(iDne2, iHodiny2, iBunky2, iDne1, iHodiny1)

    fun prohoditHodiny(iDne1: Int, iHodiny1: Int, iDne2: Int, iHodiny2: Int) =
        schovatHodinu(iDne1, iHodiny1).presunoutHodinu(iDne2, iHodiny2, iDne1, iHodiny1).vratitHodinu(iDne2, iHodiny2)

    fun odstranitBunku(iDne: Int, iHodiny: Int, iBunky: Int) = zmenitHodinu(iDne, iHodiny) { hodina ->
        hodina.removeAt(iBunky)
    }

    fun odstranitHodinu(iDne: Int, iHodiny: Int) = zmenitHodinu(iDne, iHodiny) { hodina ->
        hodina.clear()
    }

    fun pridatBunku(iDne: Int, iHodiny: Int, bunka: Bunka) = zmenitHodinu(iDne, iHodiny) { bunkas ->
        bunkas.add(bunka)
    }

    fun pridatHodinu(iDne: Int, iHodiny: Int, hodina: Hodina) = hodina.fold(this) { acc, bunka ->
        acc.pridatBunku(iDne, iHodiny, bunka)
    }

    fun schovatHodinu(iDne: Int, iHodiny: Int) = AdvancedTyden(dny, this[iDne, iHodiny]).odstranitHodinu(iDne, iHodiny)

    fun vratitHodinu(iDne: Int, iHodiny: Int) = AdvancedTyden(pridatHodinu(iDne, iHodiny, schovka!!).dny, null)

    inline fun zmenitDny(mutate: (MutableDen) -> Unit) = zmnitTyden { it.forEach(mutate) }
    inline fun zmenitHodiny(mutate: (MutableHodina) -> Unit) = zmenitDny { it.forEach(mutate) }
    inline fun zmenitBunky(map: (Bunka) -> Bunka) = zmenitHodiny { hodina ->
        hodina.forEachIndexed { i, bunka ->
            hodina[i] = map(bunka)
        }
    }

    inline fun <T> transformTyden(transform: (Tyden) -> T) = transform(dny)
    inline fun <T> mapDny(transform: (Den) -> T) = transformTyden { it.map(transform) }
    inline fun <T> mapHodiny(transform: (Hodina) -> T) = mapDny { it.map(transform) }
    inline fun <T> mapBunky(transform: (Bunka) -> T) = mapHodiny { it.map(transform) }
    inline fun filterDny(predicate: (Den) -> Boolean) = zmnitTyden { it.filter(predicate) }
    inline fun filterHodiny(predicate: (Hodina) -> Boolean) = zmenitDny { it.filter(predicate) }
    inline fun filterBunky(predicate: (Bunka) -> Boolean) = zmenitHodiny { it.filter(predicate) }

    @OptIn(ExperimentalSerializationApi::class)
    class Serializer : KSerializer<AdvancedTyden> {
        private val delegateSerializer = serializer<Tyden>()
        override val descriptor = SerialDescriptor("AdvancedTyden", delegateSerializer.descriptor)
        override fun serialize(encoder: Encoder, value: AdvancedTyden) = encoder.encodeSerializableValue(delegateSerializer, value.dny)
        override fun deserialize(decoder: Decoder) = AdvancedTyden(decoder.decodeSerializableValue(delegateSerializer))
    }
}

operator fun AdvancedTyden.get(adresa: AdresaBunky) = dny[adresa.iDne][adresa.iHodiny][adresa.iBunky]

inline fun AdvancedTyden.zmenitBunku(adresa: AdresaBunky, mutate: (Bunka) -> Bunka) =
    zmenitBunku(adresa.iDne, adresa.iHodiny, adresa.iBunky, mutate)

fun AdvancedTyden.presunoutBunku(adresa1: AdresaBunky, iDne2: Int, iHodiny2: Int) =
    pridatBunku(iDne2, iHodiny2, this[adresa1]).odstranitBunku(adresa1)

fun AdvancedTyden.prohoditBunky(adresa1: AdresaBunky, adresa2: AdresaBunky) =
    presunoutBunku(adresa1, adresa2.iDne, adresa2.iHodiny).presunoutBunku(adresa2, adresa1.iDne, adresa1.iHodiny)

fun AdvancedTyden.odstranitBunku(adresa: AdresaBunky) = zmenitHodinu(adresa.iDne, adresa.iHodiny) { bunkas ->
    bunkas.removeAt(adresa.iBunky)
}

@Suppress("NOTHING_TO_INLINE")
inline fun AdvancedTyden.pridatBunku(adresa: AdresaBunky, bunka: Bunka) = pridatBunku(adresa.iDne, adresa.iHodiny, bunka)

fun Tyden.toMutableTyden(): MutableTyden = map(Den::toMutableDen).toMutableList()
fun Den.toMutableDen(): MutableDen = map(Hodina::toMutableHodina).toMutableList()
fun Hodina.toMutableHodina(): MutableHodina = toMutableList()
