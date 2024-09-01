package cz.jaro.rozvrh.rozvrh

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


const val CELA_HODINA = -9

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    new: Boolean,
    reset: () -> Unit,
    download: () -> Unit,
    upload: () -> Unit,
    changeView: () -> Unit,
    pamet: AdresaBunky?,
    showFeatures: () -> Unit,
    zapomenout: () -> Unit,
    najitProblemy: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(text = "R Editor")
        },
        actions = {
            IconButton(
                onClick = {
                    showFeatures()
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Sort, "Sekce")
            }
            var c by remember { mutableIntStateOf(0) }
            IconButton(
                onClick = {
                    c = 0
                    najitProblemy()
                }
            ) {
                Icon(Icons.Default.PriorityHigh, "Najít problémy")
            }
            IconButton(
                onClick = {
                    if (c++ == 3) {
                        c = 0
                        reset()
                    }
                }
            ) {
                Icon(Icons.Default.RestartAlt, "Resetovat")
            }
            IconButton(
                onClick = {
                    c = 0
                    download()
                }
            ) {
                Icon(Icons.Default.Download, "Stáhnout")
            }
            IconButton(
                onClick = {
                    c = 0
                    upload()
                }
            ) {
                Icon(Icons.Default.Upload, "Nahrát")
            }
            IconButton(
                onClick = {
                    c = 0
                    changeView()
                }
            ) {
                Icon(if (new) Icons.Default.Visibility else Icons.Default.VisibilityOff, "Změnit zobrazení")
            }
        },
        navigationIcon = {
            if (pamet != null) IconButton(
                onClick = {
                    zapomenout()
                }
            ) {
                Icon(if (pamet.jeCelaHodina()) Icons.Default.FolderCopy else Icons.Default.FileCopy, "Zapomenout")
            }
        },
    )
}

