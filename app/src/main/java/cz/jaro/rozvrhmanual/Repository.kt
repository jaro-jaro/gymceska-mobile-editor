package cz.jaro.rozvrhmanual

import android.content.ContentResolver
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import cz.jaro.rozvrhmanual.rozvrh.AdvancedTyden
import cz.jaro.rozvrhmanual.rozvrh.Bunka
import cz.jaro.rozvrhmanual.rozvrh.Tyden
import cz.jaro.rozvrhmanual.rozvrh.Tyden2
import cz.jaro.rozvrhmanual.rozvrh.TypBunky
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.createTempFile

@Single
class Repository(
    private val sharedPreferences: SharedPreferences,
    private val contentResolver: ContentResolver,
    private val filesDir: Path,
) {

    private val upraveneRozvrhy = MutableStateFlow(null as Map<String, AdvancedTyden>?)
    private val puvodniRozvrhy = MutableStateFlow(null as Map<String, Tyden>?)

    val update = upraveneRozvrhy.map {}

    private val _uri = MutableStateFlow(sharedPreferences.getString("uri", ""))
    val uri = _uri.asStateFlow()

    private val newUri = MutableStateFlow(sharedPreferences.getString("newUri", ""))

    val puvodniTridy = puvodniRozvrhy.filterNotNull().map { it.keys }.map {
        setOf(" ") + it
    }

    fun puvodniRozvrh(trida: String) = puvodniRozvrhy.value!![trida]
    fun upravenyRozvrh(trida: String) = upraveneRozvrhy.value!![trida]?.dny

    fun puvodniMistnosti() = puvodniRozvrhy.value!!.flatMap { tyden ->
        tyden.value.flatMap { den ->
            den.flatMap { hodina ->
                hodina.map { bunka ->
                    bunka.ucebna
                }
            }
        }
    }.distinct().sorted() - listOf("mim", "ST")

    fun puvodniVvyucujici() = puvodniRozvrhy.value!!.flatMap { tyden ->
        tyden.value.flatMap { den ->
            den.flatMap { hodina ->
                hodina.map { bunka ->
                    bunka.ucitel
                }
            }
        }
    }.distinct().filter {
        ':' !in it
    }.sorted()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        coerceInputValues = true
    }

    fun loadFiles(uri: Uri) {
        val tempFileUri1 = createTempFile(filesDir).toFile().toUri()
        val tempFileUri2 = createTempFile(filesDir).toFile().toUri()
        contentResolver.openInputStream(uri)?.use { `is` ->
            tempFileUri1.toFile().outputStream().use { os ->
                `is`.transferTo(os)
            }
        }
        contentResolver.openInputStream(uri)?.use { `is` ->
            tempFileUri2.toFile().outputStream().use { os ->
                `is`.transferTo(os)
            }
        }

        val data = tempFileUri2.toFile().readText()
        upraveneRozvrhy.value = json.decodeFromString<Map<String, Tyden2>>(data).addIds().toAdvancedTyden().also {
            tempFileUri2.toFile().writeText(json.encodeToString(it))
        }

        setUri(tempFileUri1.toString())
        setNewUri(tempFileUri2.toString())
    }

    fun loadNewFile(uri: Uri) {
        val tempFileUri = createTempFile(filesDir).toFile().toUri()
        contentResolver.openInputStream(uri)?.use { `is` ->
            tempFileUri.toFile().outputStream().use { os ->
                `is`.transferTo(os)
            }
        }

        setNewUri(tempFileUri.toString())
        loadData()
    }

    fun loadData() {
        Uri.parse(uri.value).toFile().inputStream().use {
            val data = it.readBytes().decodeToString()
            if (data.isBlank()) return
            puvodniRozvrhy.value = json.decodeFromString<Map<String, Tyden2>>(data).addIds()
        }
        Uri.parse(newUri.value).toFile().inputStream().use {
            val data = it.readBytes().decodeToString()
            if (data.isBlank()) return
            upraveneRozvrhy.value = json.decodeFromString<Map<String, AdvancedTyden>>(data)
        }
    }

    fun setUri(uri: String?) {
        if (uri == null && _uri.value != null) {
            Uri.parse(_uri.value).toFile().delete()
            setNewUri(null)
        }
        sharedPreferences.edit().putString("uri", uri).apply()
        _uri.value = uri
    }

    fun setNewUri(uri: String?) {
        if (uri == null && newUri.value != null) {
            Uri.parse(newUri.value).toFile().delete()
        }
        sharedPreferences.edit().putString("newUri", uri).apply()
        newUri.value = uri
    }

    fun upravitRozvrh(trida: String, mutate: (AdvancedTyden) -> AdvancedTyden) {
        upraveneRozvrhy.value = upraveneRozvrhy.value
            ?.toMutableMap()
            ?.apply {
                this[trida] = mutate(this[trida] ?: return)
                this.normalizovat()
            }
            ?.also {
                Uri.parse(newUri.value).toFile().writeText(json.encodeToString(it))
            }
    }

//    init {
//        CoroutineScope(Dispatchers.IO).launch {
//            delay(1000)
//            upravitRozvrh("1.A") {
//                println(this[1][1][0])
//                this.toMutableList().also {
//                    it[1] = it[1].toMutableList().also {
//                        it[1] = it[1].toMutableList().also {
//                            it[0] = Bunka.prazdna(id = "1.A-1-1-0")
//                        }
//                    }
//                }.also {
//                    println(it[1][1][0])
//                }
//            }
//        }
//    }

    private fun OutputStream.downloadData() {
        val text = json.encodeToString(upraveneRozvrhy.value?.normalizovat())
        write(text.encodeToByteArray())
    }

    fun Uri.downloadData() {
        contentResolver.openOutputStream(this)?.use {
            it.downloadData()
        }
    }
}

//private fun Map<String, AdvancedTyden>.removeIds() = mapValues { (_, tyden) -> tyden.removeIds() }
private fun Map<String, AdvancedTyden>.normalizovat() = mapValues { (_, tyden) -> tyden.normalizovat() }
private fun AdvancedTyden.normalizovat() = zmenitBunky { bunka ->
    bunka.copy(typ = TypBunky.Normalni)
}

//private fun AdvancedTyden.removeIds(): Tyden2 = filterBunky {
//    !(it.predmet.isEmpty() && it.typ == TypBunky.Upravena)
//}.mapBunky { bunka ->
//    Bunka2(
//        ucebna = bunka.ucebna,
//        predmet = bunka.predmet,
//        ucitel = bunka.ucitel,
//        tridaSkupina = bunka.tridaSkupina,
//    )
//}

private fun Map<String, Tyden2>.addIds(): Map<String, Tyden> = mapValues { (trida, tyden) ->
    tyden.mapIndexed { iDne, den ->
        den.mapIndexed { iHodiny, hodina ->
            hodina.mapIndexed { iBunky, bunka ->
                val i = if (bunka.predmet.isEmpty()) "#" else "$iBunky"
                Bunka(
                    ucebna = bunka.ucebna,
                    predmet = bunka.predmet,
                    ucitel = bunka.ucitel,
                    tridaSkupina = bunka.tridaSkupina,
                    id = "$trida-$iDne-$iHodiny-$i",
                )
            }
        }
    }
}

fun Tyden.toAdvancedTyden() = AdvancedTyden(this)
fun Map<String, Tyden>.toAdvancedTyden() = mapValues { (_, tyden) -> tyden.toAdvancedTyden() }