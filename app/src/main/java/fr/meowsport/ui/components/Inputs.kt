package fr.meowsport.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.meowsport.data.formatWeight

private fun parseNumber(s: String): Double? = s.replace(',', '.').trim().toDoubleOrNull()

/** "05" becomes "5", so typing after a leftover 0 replaces it. "0,5" stays as is. */
private fun dropLeadingZeros(s: String): String {
    var t = s
    while (t.length > 1 && t[0] == '0' && t[1].isDigit()) t = t.substring(1)
    return t
}

/** Text field for a weight in kg. Accepts both "26,5" and "26.5". */
@Composable
fun WeightField(
    value: Double,
    onValue: (Double) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    var text by remember { mutableStateOf(formatWeight(value)) }
    LaunchedEffect(value) {
        // An empty field stands for 0 while the user is typing: don't put the 0 back.
        if (text.isEmpty() && value == 0.0) return@LaunchedEffect
        if (parseNumber(text) != value) text = formatWeight(value)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { t ->
            val clean = dropLeadingZeros(t.filter { it.isDigit() || it == ',' || it == '.' }).take(6)
            text = clean
            val v = parseNumber(clean)
            if (v != null) onValue(v) else if (clean.isEmpty()) onValue(0.0)
        },
        modifier = modifier,
        label = label?.let { l -> @Composable { Text(l) } },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
    )
}

/** Text field for a whole number such as reps. */
@Composable
fun IntField(
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        if (text.isEmpty() && value == 0) return@LaunchedEffect
        if (text.toIntOrNull() != value) text = value.toString()
    }
    OutlinedTextField(
        value = text,
        onValueChange = { t ->
            val clean = dropLeadingZeros(t.filter { it.isDigit() }).take(3)
            text = clean
            onValue(clean.toIntOrNull() ?: 0)
        },
        modifier = modifier,
        label = label?.let { l -> @Composable { Text(l) } },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
    )
}

/** − value + control. */
@Composable
fun Stepper(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Moins", modifier = Modifier.size(18.dp))
            }
            Text(
                valueText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 44.dp),
            )
            FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(18.dp))
            }
        }
    }
}
