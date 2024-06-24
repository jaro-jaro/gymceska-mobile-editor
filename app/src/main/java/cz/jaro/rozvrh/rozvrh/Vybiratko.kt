package cz.jaro.rozvrh.rozvrh

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager

@ExperimentalMaterial3Api
@Composable
fun Vybiratko(
    value: Vjec?,
    seznam: List<Vjec>,
    onClick: (Int, Vjec) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    trailingIcon: (@Composable (hide: () -> Unit) -> Unit)? = null,
    colors: TextFieldColors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
        unfocusedTextColor = /*if (value == null) ExposedDropdownMenuDefaults.outlinedTextFieldColors().unfocusedPlaceholderColor else*/ MaterialTheme.colorScheme.primary,
        focusedTextColor = /*if (value == null) ExposedDropdownMenuDefaults.outlinedTextFieldColors().focusedPlaceholderColor else*/ MaterialTheme.colorScheme.primary,
    ),
) = Vybiratko(
    value = value?.jmeno ?: "",
    seznam = seznam.map { it.jmeno },
    onClick = { i, _ -> onClick(i, seznam[i]) },
    modifier = modifier,
    label = label,
    trailingIcon = trailingIcon,
    zaskrtavatko = { false },
    colors = colors,
)

@Composable
@ExperimentalMaterial3Api
fun Vybiratko(
    index: Int,
    seznam: List<String>,
    onClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    zaskrtavatko: (String) -> Boolean = { it == seznam[index] },
    trailingIcon: (@Composable (hide: () -> Unit) -> Unit)? = null,
    colors: TextFieldColors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
) = Vybiratko(
    value = seznam[index],
    seznam = seznam,
    onClick = onClick,
    modifier = modifier,
    label = label,
    trailingIcon = trailingIcon,
    zaskrtavatko = zaskrtavatko,
    colors = colors,
)

@Composable
@ExperimentalMaterial3Api
fun Vybiratko(
    value: String,
    seznam: List<String>,
    onClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    zaskrtavatko: (String) -> Boolean = { it == value },
    trailingIcon: (@Composable (hide: () -> Unit) -> Unit)? = null,
    zavirat: Boolean = true,
    colors: TextFieldColors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            value = value,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    trailingIcon?.invoke {
                        expanded = false
                        focusManager.clearFocus()
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            colors = colors,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                focusManager.clearFocus()
            },
        ) {
            val zaskrtavatka = seznam.map(zaskrtavatko)
            seznam.forEachIndexed { i, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onClick(i, option)
                        if (zavirat) {
                            expanded = false
                            focusManager.clearFocus()
                        }
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    leadingIcon = if (zaskrtavatka.any { it }) (@Composable {
                        if (zaskrtavatka[i]) Icon(Icons.Default.Check, null)
                    }) else null
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun Vybiratko(
    seznam: List<String>,
    aktualIndex: Int,
    modifier: Modifier = Modifier,
    label: String = "",
    poklik: (i: Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            value = seznam[aktualIndex],
            onValueChange = {},
            label = { Text(label) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                focusManager.clearFocus()
            },
        ) {
            seznam.forEachIndexed { i, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        poklik(i)
                        expanded = false
                        focusManager.clearFocus()
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}