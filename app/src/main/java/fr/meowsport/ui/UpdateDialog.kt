package fr.meowsport.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.meowsport.UpdateState
import fr.meowsport.app

/** Shows the update prompt, the download progress and manual-check results. */
@Composable
fun UpdateDialog() {
    val updater = LocalContext.current.app.updater
    val state by updater.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = { updater.dismiss() },
            title = { Text("Nouvelle version disponible") },
            text = {
                Column {
                    Text("${s.release.name} est disponible (tu as la ${updater.currentName}).")
                    if (s.release.size > 0) {
                        Text(
                            "Taille : %.1f Mo".format(s.release.size / 1_000_000f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (s.release.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            s.release.notes,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Tes séances et modèles sont conservés.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = { updater.download(s.release) }) { Text("Mettre à jour") } },
            dismissButton = {
                Column {
                    TextButton(onClick = { updater.dismiss() }) { Text("Plus tard") }
                    TextButton(onClick = { updater.skip(s.release) }) { Text("Ignorer cette version") }
                }
            },
        )

        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Téléchargement…") },
            text = {
                Column {
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("${(s.progress * 100).toInt()} %")
                }
            },
            confirmButton = {},
        )

        is UpdateState.Failed -> AlertDialog(
            onDismissRequest = { updater.dismiss() },
            title = { Text("Téléchargement impossible") },
            text = { Text("Vérifie ta connexion et réessaie.") },
            confirmButton = { Button(onClick = { updater.download(s.release) }) { Text("Réessayer") } },
            dismissButton = { TextButton(onClick = { updater.dismiss() }) { Text("Annuler") } },
        )

        UpdateState.UpToDate -> AlertDialog(
            onDismissRequest = { updater.dismiss() },
            title = { Text("Déjà à jour") },
            text = { Text("Tu as la dernière version (${updater.currentName}).") },
            confirmButton = { TextButton(onClick = { updater.dismiss() }) { Text("OK") } },
        )

        UpdateState.Unreachable -> AlertDialog(
            onDismissRequest = { updater.dismiss() },
            title = { Text("GitHub injoignable") },
            text = { Text("Impossible de vérifier les mises à jour pour le moment.") },
            confirmButton = { TextButton(onClick = { updater.dismiss() }) { Text("OK") } },
        )

        UpdateState.Idle, UpdateState.Checking -> Unit
    }
}
