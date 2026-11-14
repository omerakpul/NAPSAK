package com.napsak.app.ui.screens.lobby

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import com.napsak.app.ui.components.NapsakTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.napsak.app.domain.model.Suggestion

/** Lets a participant propose an option to the host while the room is still in the lobby. */
@Composable
fun SuggestionForm(
    mine: List<Suggestion>,
    onSubmit: (name: String, details: String, onResult: (Boolean) -> Unit) -> Unit,
    onRemove: (Suggestion) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val limitReached = mine.size >= 5

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bir seçenek öner", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text(
                "Önerin host'a iletilir, host listeye ekleyebilir (en fazla 5).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            NapsakTextField(
                value = name,
                onValueChange = { name = it.take(100); failed = false },
                label = "Seçenek adı",
                leadingIcon = Icons.Default.Edit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !limitReached
            )
            NapsakTextField(
                value = details,
                onValueChange = { details = it.take(500) },
                label = "Açıklama (isteğe bağlı)",
                leadingIcon = Icons.Default.Info,
                modifier = Modifier.fillMaxWidth(),
                enabled = !limitReached
            )
            Button(
                onClick = {
                    sending = true
                    onSubmit(name.trim(), details.trim()) { ok ->
                        sending = false
                        failed = !ok
                        if (ok) {
                            name = ""
                            details = ""
                        }
                    }
                },
                enabled = name.isNotBlank() && !sending && !limitReached,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (limitReached) "Öneri limitine ulaştın" else "Öneriyi Gönder") }
            if (failed) {
                Text(
                    "Öneri gönderilemedi. Oylama başlamış olabilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            mine.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { onRemove(s) }) {
                        Icon(Icons.Default.Close, contentDescription = "Önerimi geri çek")
                    }
                }
            }
        }
    }
}
