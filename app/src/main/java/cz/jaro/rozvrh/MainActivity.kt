package cz.jaro.rozvrh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.jaro.rozvrh.rozvrh.RozvrhScreen
import cz.jaro.rozvrh.rozvrh.Vjec
import cz.jaro.rozvrh.rozvrh.rememberResultLauncher
import cz.jaro.rozvrh.ui.theme.GymceskaTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val repo by inject<Repository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val tridy by repo.puvodniTridy.collectAsStateWithLifecycle(setOf())
            val uri by repo.uri.collectAsStateWithLifecycle()

            val launcher = rememberResultLauncher(ActivityResultContracts.OpenDocument())
            LaunchedEffect(uri) {
                if (uri.isNotNullNorBlank()) repo.loadData()
                else launcher.launch(
                    input = arrayOf("application/json")
                ) { uri ->
                    if (uri == null) return@launch
                    repo.loadFiles(uri)
                    repo.loadData()
                }
            }

            if (tridy.isNotEmpty()) GymceskaTheme(
                useDarkTheme = isSystemInDarkTheme(),
                useDynamicColor = true,
            ) {
                Surface {
                    RozvrhScreen(tridy.map { Vjec.TridaVjec(it) })
                }
            }
        }
    }
}

private fun String?.isNotNullNorBlank() = !isNullOrBlank()
