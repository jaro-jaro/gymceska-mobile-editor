package cz.jaro.rozvrh.rozvrh

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.jaro.rozvrh.Repository
import cz.jaro.rozvrh.rozvrh.TvorbaRozvrhu.vytvoritRozvrhPodleJinych
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class RozvrhViewModel(
    private val repo: Repository,
    @InjectedParam private val params: Parameters
) : ViewModel() {

    data class Parameters(
        val tridy: List<Vjec.TridaVjec>,
        val launcher: GenericActivityResultLauncher<Intent, ActivityResult>,
        val launcherOpen: GenericActivityResultLauncher<Array<String>, Uri?>,
    )

    private val tridy = params.tridy.drop(1)

    private val _vjec = MutableStateFlow<Vjec>(tridy.first())
    val vjec = _vjec.asStateFlow()

    private val _new = MutableStateFlow(true)
    val new = _new.asStateFlow()

    fun vybratRozvrh(vjec: Vjec) {
        _vjec.value = vjec
    }

    val mistnosti = repo.puvodniMistnosti().map {
        Vjec.MistnostVjec(it)
    }
    val vyucujici = repo.puvodniVvyucujici().map {
        Vjec.VyucujiciVjec(it, it)
    }

    val tabulka = combine(vjec, new, repo.update) { vjec, new, _ ->
        ziskatRozvrh(vjec, false)?.let { puvodniRozvrh ->
            if (new) ziskatRozvrh(vjec, true)?.mark(puvodniRozvrh)
            else puvodniRozvrh
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    private fun ziskatRozvrh(vjec: Vjec, new: Boolean) = when (vjec) {
        is Vjec.TridaVjec -> if (new) repo.upravenyRozvrh(vjec.jmeno) else repo.puvodniRozvrh(vjec.jmeno)

        is Vjec.VyucujiciVjec,
        is Vjec.MistnostVjec -> vytvoritRozvrhPodleJinych(
            vjec = vjec,
            repo = repo,
            tridy = tridy,
            new = new,
        )

        is Vjec.DenVjec,
        is Vjec.HodinaVjec -> TvorbaRozvrhu.vytvoritSpecialniRozvrh(
            vjec = vjec,
            repo = repo,
            tridy = tridy,
            new = new,
        )
    }

    fun ziskatRozvrh(vjec: Vjec) = ziskatRozvrh(vjec, new.value)

    fun reset() = repo.setUri(null)

    fun download() {
        params.launcher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "UPRAVENY_ROZVRH")
        }) {
            val uri = it.data?.data ?: return@launch
            with(repo) {
                uri.downloadData()
            }
        }
    }

    fun upload() {
        params.launcherOpen.launch(arrayOf("application/json")) { uri ->
            uri ?: return@launch
            repo.setNewUri(null)
            repo.loadNewFile(uri)
        }
    }

    fun changeView() {
        _new.value = !_new.value
    }

    val edit: (String, (AdvancedTyden) -> AdvancedTyden) -> Unit = repo::upravitRozvrh

    val changes = repo.changes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)
}

private fun Tyden.mark(puvodni: Tyden) = mapIndexed { iDne, den ->
    den.mapIndexed { iHodiny, hodina ->
        val odstranena = puvodni[iDne][iHodiny].find { b -> b.id !in hodina.map { it.id } }
        if (hodina.all { it.predmet == "ST" }) listOf(Bunka(id = hodina.first().id, predmet = "ST", ucebna = "mim", ucitel = "Tren", typ = TypBunky.Upravena))
        else (hodina.map { bunka ->
            val puvodniBunka = puvodni[iDne][iHodiny].find { it.id == bunka.id }
            if (puvodniBunka == null || puvodniBunka != bunka) bunka.copy(typ = TypBunky.Upravena) else bunka.copy(typ = TypBunky.Normalni)
        } + List(if (odstranena != null) 1 else 0) {
            Bunka.prazdna(odstranena!!.id.split("-").dropLast(1).plus("#").joinToString("-")).copy(typ = TypBunky.Upravena)
        }).let { h ->
            val i = h.indexOfFirst { it.id.last() == '#' }
            if (i != -1 && h.size > 1) h.dropAt(i) else h
        }
    }
}

private fun <T> List<T>.dropAt(i: Int) = subList(0, i) + subList(i + 1, size)
