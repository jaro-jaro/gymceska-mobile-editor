package cz.jaro.rozvrhmanual

import android.content.ContentResolver
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import cz.jaro.rozvrhmanual.rozvrh.TvorbaRozvrhu.vytvoritRozvrhPodleJinych
import cz.jaro.rozvrhmanual.rozvrh.Tyden
import cz.jaro.rozvrhmanual.rozvrh.Vjec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream

@Single
class Repository(
    private val sharedPreferences: SharedPreferences,
    private val contentResolver: ContentResolver,
    private val filesDir: Path,
) {

    private val rozvrhy = MutableStateFlow(null as Map<String, Tyden>?)

    private val _uri = MutableStateFlow(sharedPreferences.getString("uri", ""))
    val uri = _uri.asStateFlow()

    val tridy = rozvrhy.filterNotNull().map { it.keys }.map {
        setOf(" ") + it
    }

    fun rozvrh(trida: String) = rozvrhy.value!![trida]

    fun mistnosti() = rozvrhy.value!!.flatMap { tyden ->
        tyden.value.flatMap { den ->
            den.flatMap { hodina ->
                hodina.map { bunka ->
                    bunka.ucebna
                }
            }
        }
    }.distinct()

    fun vyucujici() = rozvrhy.value!!.flatMap { tyden ->
        tyden.value.flatMap { den ->
            den.flatMap { hodina ->
                hodina.map { bunka ->
                    bunka.ucitel
                }
            }
        }
    }.distinct()

    suspend fun vyucujici2() = vyucujici().filter {
        vytvoritRozvrhPodleJinych(Vjec.VyucujiciVjec(it, it), this, tridy.first().drop(1).map { Vjec.TridaVjec(it) }).all { den ->
            den.isEmpty() || den.all { hodina ->
                hodina.isEmpty() || hodina.all { bunka ->
                    bunka.predmet.isBlank() || !bunka.predmet.startsWith("ST")
                }
            }
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun loadFile(uri: Uri) {
        val tempFile = createTempFile(filesDir)
        contentResolver.openInputStream(uri)?.use { `is` ->
            tempFile.outputStream().use { os ->
                `is`.transferTo(os)
            }
        }
        setUri(tempFile.toFile().toUri().toString())
    }

    fun loadData() {
        Uri.parse(uri.value).toFile().inputStream().use {
            val data = it.readBytes().decodeToString()
            if (data.isBlank()) return
            rozvrhy.value = json.decodeFromString<Map<String, Tyden>>(data).mapValues { (_, tyden) ->
                tyden.map { den ->
                    den.take(10)
                }
            }
        }
    }

    fun setUri(uri: String?) {
        if (uri == null && _uri.value != null) {
            Uri.parse(_uri.value).toFile().delete()
        }
        sharedPreferences.edit().putString("uri", uri).apply()
        _uri.value = uri
    }
}