package com.napsak.app.ui.screens.voting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.napsak.app.domain.model.Choice
import com.napsak.app.ui.OptionEmoji

@Composable
private fun ChoiceThumb(choice: Choice) {
    if (!choice.imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = choice.imageUrl,
            contentDescription = choice.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
        )
    } else {
        OptionEmoji(choice.name, choice.category, size = 52.dp)
    }
}

@Composable
private fun ChoiceTexts(choice: Choice, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(choice.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (choice.details.isNotBlank()) {
            Text(
                choice.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** SINGLE mode: tap one option, then send. */
@Composable
fun SingleChoiceVoting(choices: List<Choice>, onSubmit: (Choice) -> Unit) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(choices, key = { it.id }) { choice ->
                val selected = choice.id == selectedId
                Card(
                    // the whole row is the single control (selectable radio), so a screen reader reads name, details and state together
                    modifier = Modifier.fillMaxWidth().selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { selectedId = choice.id }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChoiceThumb(choice)
                        Spacer(Modifier.width(12.dp))
                        ChoiceTexts(choice, Modifier.weight(1f))
                        RadioButton(selected = selected, onClick = null)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { choices.firstOrNull { it.id == selectedId }?.let(onSubmit) },
            enabled = selectedId != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Oyumu Gönder", fontWeight = FontWeight.Bold) }
    }
}

/** RANK mode: move options up/down, best on top, then send. */
@Composable
fun RankingVoting(choices: List<Choice>, onSubmit: (List<Choice>) -> Unit) {
    val order = remember(choices) { mutableStateListOf<Choice>().apply { addAll(choices) } }
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(order, key = { _, c -> c.id }) { index, choice ->
                Card(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        ChoiceThumb(choice)
                        Spacer(Modifier.width(10.dp))
                        ChoiceTexts(choice, Modifier.weight(1f))
                        Column {
                            IconButton(
                                onClick = { order.add(index - 1, order.removeAt(index)) },
                                enabled = index > 0,
                                modifier = Modifier.size(48.dp)
                            ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Yukarı al") }
                            IconButton(
                                onClick = { order.add(index + 1, order.removeAt(index)) },
                                enabled = index < order.lastIndex,
                                modifier = Modifier.size(48.dp)
                            ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Aşağı al") }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onSubmit(order.toList()) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Sıralamayı Gönder", fontWeight = FontWeight.Bold) }
    }
}
