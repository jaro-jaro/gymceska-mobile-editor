package cz.jaro.rozvrh.rozvrh

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RozvrhScreen(
    tridy: List<Vjec.TridaVjec>
) {
    val launcher = rememberResultLauncher(ActivityResultContracts.StartActivityForResult())
    val launcherOpen = rememberResultLauncher(ActivityResultContracts.OpenDocument())

    val viewModel = koinViewModel<RozvrhViewModel> {
        parametersOf(RozvrhViewModel.Parameters(tridy, launcher, launcherOpen))
    }

    val tabulka: Tyden? by viewModel.tabulka.collectAsStateWithLifecycle()
    val realVjec by viewModel.vjec.collectAsStateWithLifecycle()
    val new by viewModel.new.collectAsStateWithLifecycle()

    Rozvrh(
        tabulka = tabulka,
        vjec = realVjec,
        vybratRozvrh = viewModel::vybratRozvrh,
        tridy = tridy,
        reset = viewModel::reset,
        upload = viewModel::upload,
        changeView = viewModel::changeView,
        download = viewModel::download,
        mistnosti = viewModel.mistnosti,
        vyucujici = viewModel.vyucujici,
        new = new,
        edit = viewModel.edit,
        ziskatRozvrh = {
            viewModel.ziskatRozvrh(it)!!
        },
    )
}

private fun Hodina.jsouStejne() = size > 1 && all {
    it.predmet == first().predmet && it.ucitel == first().ucitel && it.ucebna == first().ucebna && it.tridaSkupina.split(" ").last() == first().tridaSkupina.split(" ").last()
} || size % 2 == 0 && size > 1 && count { it.predmet.startsWith("S:") } == count() / 2 && count { it.predmet.startsWith("L:") } == count() / 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Rozvrh(
    tabulka: Tyden?,
    vjec: Vjec,
    vybratRozvrh: (Vjec) -> Unit,
    tridy: List<Vjec.TridaVjec>,
    reset: () -> Unit,
    download: () -> Unit,
    upload: () -> Unit,
    changeView: () -> Unit,
    new: Boolean,
    mistnosti: List<Vjec.MistnostVjec>,
    vyucujici: List<Vjec.VyucujiciVjec>,
    edit: (String, (AdvancedTyden) -> AdvancedTyden) -> Unit,
    ziskatRozvrh: (Vjec) -> Tyden,
) {
    var pamet by remember { mutableStateOf<AdresaBunky?>(null) }
    var vysledkyDialog by remember { mutableStateOf(emptyList<String>()) }
    Scaffold(
        topBar = {
            AppBar(
                reset = reset,
                download = download,
                upload = upload,
                changeView = changeView,
                new = new,
                pamet = pamet,
                zapomenout = {
                    pamet = null
                },
                najitProblemy = {
                    vysledkyDialog = najitProblemy(vyucujici, mistnosti, ziskatRozvrh)
                }
            )
        }
    ) { paddingValues ->
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

        if (tridy.isEmpty()) LinearProgressIndicator(
            Modifier
                .padding(paddingValues)
                .fillMaxWidth()
        )
        else Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Vybiratko(
                    value = if (vjec is Vjec.TridaVjec) vjec else null,
                    seznam = tridy.drop(1),
                    onClick = { i, _ -> vybratRozvrh(tridy[i + 1]) },
                    Modifier
                        .weight(1F)
                        .padding(horizontal = 4.dp),
                    label = "Třídy",
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Vybiratko(
                    value = if (vjec is Vjec.MistnostVjec) vjec else null,
                    seznam = mistnosti.drop(1),
                    onClick = { i, _ -> vybratRozvrh(mistnosti[i + 1]) },
                    Modifier
                        .weight(1F)
                        .padding(horizontal = 4.dp),
                    label = "Místnosti",
                )

                Vybiratko(
                    value = if (vjec is Vjec.VyucujiciVjec) vjec else null,
                    seznam = vyucujici.drop(1),
                    onClick = { i, _ -> vybratRozvrh(vyucujici[i + 1]) },
                    Modifier
                        .weight(1F)
                        .padding(horizontal = 4.dp),
                    label = "Vyučující",
                )
            }

            if (tabulka == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            else Tabulka(
                vjec = vjec,
                tabulka = tabulka,
                kliklNaNeco = { vjec ->
                    vybratRozvrh(vjec)
                },
                tridy = tridy,
                mistnosti = mistnosti,
                vyucujici = vyucujici,
                edit = { mutate ->
                    if (vjec !is Vjec.TridaVjec) return@Tabulka
                    edit(vjec.zkratka) {
                        mutate(it)
                    }
                },
                pamet = pamet,
                zapamatovat = {
                    pamet = it
                },
                ziskatRozvrh = ziskatRozvrh,
                vybratRozvrh = vybratRozvrh,
            )
        }
    }
}

fun najitProblemy(
    vyucujici: List<Vjec.VyucujiciVjec>,
    mistnosti: List<Vjec.MistnostVjec>,
    ziskatRozvrh: (Vjec) -> Tyden,
): List<String> {
    val p1 = vyucujici.drop(1).flatMap { ucitel ->
        val rozvrh = ziskatRozvrh(ucitel)
        rozvrh.drop(1).flatMapIndexed { iDne: Int, den: Den ->
            den.drop(1).mapIndexed { iHodiny: Int, hodina: Hodina ->
                if (hodina.size > 1 && !hodina.jsouStejne()) "V ${ucitel.zkratka} ${Seznamy.dny1Pad[iDne]} ${Seznamy.hodiny1Pad[iHodiny]}"
                else null
            }
        }
    }.filterNotNull()
    println(p1)
    val p2 = mistnosti.drop(1).flatMap { trida ->
        val rozvrh = ziskatRozvrh(trida)
        rozvrh.drop(1).flatMapIndexed { iDne: Int, den: Den ->
            den.drop(1).mapIndexed { iHodiny: Int, hodina: Hodina ->
                if (hodina.size > 1 && !hodina.jsouStejne()) "M ${trida.zkratka} ${Seznamy.dny1Pad[iDne]} ${Seznamy.hodiny1Pad[iHodiny]}"
                else null
            }
        }
    }.filterNotNull()
    println(p2)
    return p1 + p2
}

