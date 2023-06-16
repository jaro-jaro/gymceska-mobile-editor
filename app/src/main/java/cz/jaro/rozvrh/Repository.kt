package cz.jaro.rozvrh

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.widget.Toast
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.get
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import cz.jaro.rozvrh.rozvrh.Stalost
import cz.jaro.rozvrh.rozvrh.TvorbaRozvrhu
import cz.jaro.rozvrh.rozvrh.Vjec
import cz.jaro.rozvrh.ukoly.Ukol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.koin.core.annotation.Single
import java.io.IOException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

@Single
class Repository(
    private val ctx: Context,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val preferences = PreferenceDataStoreFactory.create(migrations = listOf(SharedPreferencesMigration({
        ctx.getSharedPreferences("hm", Context.MODE_PRIVATE)!!
    }))) {
        ctx.preferencesDataStoreFile("Gymceska_JARO_datastore")
    }.also {
        scope.launch {
            if (it.data.first().contains(booleanPreferencesKey("first")).not()) {
                if (!isOnline()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(ctx, "Je potřeba připojení k internetu!", Toast.LENGTH_LONG).show()
                    }
                    exitProcess(-1)
                }
                it.edit {
                    it[booleanPreferencesKey("first")] = false
                }
            }
        }
    }

    object Keys {
        val NASTAVENI = stringPreferencesKey("nastaveni")
        fun rozvrh(trida: Vjec.TridaVjec, stalost: Stalost) = stringPreferencesKey("rozvrh_${trida.jmeno}_${stalost.nazev}")
        fun rozvrhPosledni(trida: Vjec.TridaVjec, stalost: Stalost) = stringPreferencesKey("rozvrh_${trida.jmeno}_${stalost.nazev}_posledni")
        val SKRTLE_UKOLY = stringSetPreferencesKey("skrtle_ukoly")
        val UKOLY = stringPreferencesKey("ukoly")
    }

    private val firebase = Firebase
    private val database = firebase.database("https://gymceska-b9b4c-default-rtdb.europe-west1.firebasedatabase.app/")
    private val remoteConfig = Firebase.remoteConfig

    private val configActive = flow {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        emit(remoteConfig.fetchAndActivate().await())
    }

    val tridy = configActive.map {
        listOf(Vjec.TridaVjec("Třídy")) + remoteConfig["tridy"].asString().fromJson<List<Vjec.TridaVjec>>()
    }.stateIn(scope, SharingStarted.Eagerly, listOf(Vjec.TridaVjec("Třídy")))
    val mistnosti = configActive.map {
        listOf(Vjec.MistnostVjec("Místnosti")) + remoteConfig["mistnosti"].asString().fromJson<List<Vjec.MistnostVjec>>()
    }.stateIn(scope, SharingStarted.Eagerly, listOf(Vjec.MistnostVjec("Místnosti")))
    val vyucujici = configActive.map {
        listOf(Vjec.VyucujiciVjec("Vyučující", "")) + remoteConfig["vyucujici"].asString().fromJson<List<Vjec.VyucujiciVjec>>()
    }.stateIn(scope, SharingStarted.Eagerly, listOf(Vjec.VyucujiciVjec("Vyučující", "")))

    val nastaveni = preferences.data.combine(tridy) { it, tridy ->
        it[Keys.NASTAVENI]?.let { it1 -> Json.decodeFromString<Nastaveni>(it1) } ?: Nastaveni(mojeTrida = tridy.getOrElse(1) { tridy.first() })
    }

    suspend fun zmenitNastaveni(edit: (Nastaveni) -> Nastaveni) {
        preferences.edit {
            it[Keys.NASTAVENI] =
                Json.encodeToString(edit(it[Keys.NASTAVENI]?.let { it1 -> Json.decodeFromString<Nastaveni>(it1) } ?: Nastaveni(mojeTrida = tridy.value[1])))
        }
    }

    private val ukolyRef = database.getReference("ukoly")

    init {
        ukolyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ukoly = snapshot.getValue(object : GenericTypeIndicator<List<Map<String, String>>?>() {})
                val noveUkoly = ukoly?.mapNotNull {
                    Ukol(
                        datum = it["datum"] ?: return@mapNotNull null,
                        nazev = it["nazev"] ?: return@mapNotNull null,
                        predmet = it["predmet"] ?: return@mapNotNull null,
                        id = it["id"]?.let { id -> UUID.fromString(id) } ?: UUID.randomUUID(),
                    )
                }
                onlineUkoly.value = noveUkoly

                scope.launch {
                    preferences.edit {
                        it[Keys.UKOLY] = Json.encodeToString(noveUkoly)
                    }

                    upravitSkrtleUkoly { skrtle ->
                        skrtle.filter { uuid ->
                            uuid in (noveUkoly?.map { it.id } ?: emptyList())
                        }.toSet()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }

    suspend fun stahnoutVse(update: (String) -> Unit, finish: () -> Unit) {
        if (!isOnline()) return
        withContext(Dispatchers.IO) {
            tridy.value.drop(1).forEach { trida ->
                Stalost.values().forEach { stalost ->
                    update("Stahování:\n${trida.jmeno} – ${stalost.nazev}")

                    val doc = Jsoup.connect(trida.odkaz?.replace("###", stalost.odkaz) ?: run {
                        update("Něco se nepovedlo :(")
                        return@withContext
                    }).get()

                    preferences.edit {
                        it[Keys.rozvrh(trida, stalost)] = doc.toString()
                        it[Keys.rozvrhPosledni(trida, stalost)] = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).toString()
                    }
                }
            }
            finish()
        }
    }

    suspend fun ziskatSkupiny(trida: Vjec.TridaVjec): Sequence<String> {
        val result = ziskatDocument(trida, Stalost.Staly)

        if (result !is Uspech) return emptySequence()

        return TvorbaRozvrhu.vytvoritTabulku(result.document)
            .asSequence()
            .flatten()
            .filter { it.size > 1 }
            .flatten()
            .map { it.tridaSkupina }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    private suspend fun pouzitOfflineRozvrh(trida: Vjec.TridaVjec, stalost: Stalost): Boolean {

        val posledni = preferences.data.first()[Keys.rozvrhPosledni(trida, stalost)]?.let { LocalDateTime.parse(it) } ?: return false
        val staryHodin = posledni.until(LocalDateTime.now(), ChronoUnit.HOURS)
        return staryHodin < 1
    }

    suspend fun ziskatDocument(trida: Vjec.TridaVjec, stalost: Stalost): Result = withContext(Dispatchers.IO) {
        if (trida.odkaz == null) return@withContext TridaNeexistuje

        if (isOnline() && !pouzitOfflineRozvrh(trida, stalost)) {
            try {
                val doc = Jsoup.connect(trida.odkaz.replace("###", stalost.odkaz)).get().also { doc ->
                    preferences.edit {
                        it[Keys.rozvrh(trida, stalost)] = doc.toString()
                        it[Keys.rozvrhPosledni(trida, stalost)] = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).toString()
                    }
                }

                return@withContext Uspech(doc, Online)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        val kdy = preferences.data.first()[Keys.rozvrhPosledni(trida, stalost)]?.let { LocalDateTime.parse(it) }
            ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, R.string.neni_stazeno, Toast.LENGTH_LONG).show()
                }
                return@withContext ZadnaData
            }

        val html = preferences.data.first()[Keys.rozvrh(trida, stalost)]
            ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, R.string.neni_stazeno, Toast.LENGTH_LONG).show()
                }
                return@withContext ZadnaData
            }

        Uspech(Jsoup.parse(html), Offline(kdy))
    }

    suspend fun ziskatDocument(stalost: Stalost): Result = ziskatDocument(nastaveni.first().mojeTrida, stalost)

    private fun isOnline(): Boolean = ctx.isOnline()

    companion object {
        fun Context.isOnline(): Boolean {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false

            return capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ) || capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) || capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_ETHERNET
            )
        }

        inline fun <reified T> String.fromJson(): T = Json.decodeFromString(this)
    }

    val skrtleUkoly = preferences.data.map {
        it[Keys.SKRTLE_UKOLY]?.map { id -> UUID.fromString(id) }?.toSet() ?: emptySet()
    }

    suspend fun upravitSkrtleUkoly(edit: (Set<UUID>) -> Set<UUID>) {
        preferences.edit {
            it[Keys.SKRTLE_UKOLY] = edit(
                it[Keys.SKRTLE_UKOLY]?.map { id -> UUID.fromString(id) }?.toSet() ?: emptySet()
            ).map { id -> id.toString() }.toSet()
        }
    }

    val isOnlineFlow = flow {
        while (currentCoroutineContext().isActive) {
            delay(5.seconds)
            emit(isOnline())
        }
    }

    private val onlineUkoly = MutableStateFlow(null as List<Ukol>?)

    private val offlineUkoly = preferences.data.map {
        it[Keys.UKOLY]?.let { it1 -> Json.decodeFromString<List<Ukol>>(it1) }
    }

    val ukoly = combine(isOnlineFlow, onlineUkoly, offlineUkoly) { isOnline, onlineUkoly, offlineUkoly ->
        if (isOnline) onlineUkoly else offlineUkoly
    }

    suspend fun upravitUkoly(ukoly: List<Ukol>) {
        ukolyRef.setValue(ukoly.map { mapOf("datum" to it.datum, "nazev" to it.nazev, "predmet" to it.predmet, "id" to it.id.toString()) }).await()
    }

    @SuppressLint("HardwareIds")
    fun jeZarizeniPovoleno(): Boolean {
        val povolene = remoteConfig["povolenaZarizeni"].asString().fromJson<List<String>>()

        val ja = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)

        return ja in povolene
    }

    val verzeNaRozbiti = remoteConfig["rozbitAplikaci"].asString().toIntOrNull() ?: -1
}
