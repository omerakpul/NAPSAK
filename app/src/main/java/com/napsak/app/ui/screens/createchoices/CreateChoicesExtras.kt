package com.napsak.app.ui.screens.createchoices

import androidx.compose.material.icons.filled.Place
import com.napsak.app.ui.components.NapsakTextField
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.napsak.app.domain.model.Place
import com.napsak.app.domain.model.Suggestion
import com.napsak.app.domain.model.VoteMode
import com.napsak.app.ui.theme.CoralPrimary
import com.napsak.app.ui.theme.CoralPrimaryDark

/** Friends' suggestions for the host: add to the list or dismiss. */
@Composable
fun SuggestionsCard(
    suggestions: List<Suggestion>,
    onAccept: (Suggestion) -> Unit,
    onDismiss: (Suggestion) -> Unit
) {
    if (suggestions.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Arkadaşlarından öneriler (${suggestions.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            suggestions.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (s.details.isNotBlank()) {
                            Text(
                                s.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = { onAccept(s) }) {
                        Icon(Icons.Default.Check, contentDescription = "Listeye ekle", tint = Color(0xFF2E9E5B))
                    }
                    IconButton(onClick = { onDismiss(s) }) {
                        Icon(Icons.Default.Close, contentDescription = "Reddet", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/** Paste a list: one option per line ("Ad - açıklama" optional). */
@Composable
fun QuickEntryCard(
    text: String,
    onTextChange: (String) -> Unit,
    onAdd: () -> Int
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Toplu Ekle", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Her satıra bir seçenek yaz veya yapıştır",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Kapat" else "Aç"
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NapsakTextField(
                        value = text,
                        onValueChange = onTextChange,
                        label = "Seçenekler (her satıra bir tane)",
                        placeholder = "Pizza\nBurger - gurme hamburger\nSushi",
                        singleLine = false,
                        minLines = 4,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val added = onAdd()
                            Toast.makeText(
                                context,
                                if (added > 0) "$added seçenek eklendi" else "Eklenecek yeni satır bulunamadı",
                                Toast.LENGTH_SHORT
                            ).show()
                            if (added > 0) expanded = false
                        },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Satırları Ekle")
                    }
                }
            }
        }
    }
}

/** Search a place on OpenStreetMap; picking one fills the option form (name, address, coordinates). */
@Composable
fun PlaceSearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    results: List<Place>,
    isSearching: Boolean,
    failed: Boolean,
    onSelect: (Place) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Haritadan Mekan Ara", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Mekanı bul, adı ve konumu otomatik dolsun",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Kapat" else "Aç"
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NapsakTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        label = "Mekan veya adres (ör: Kadıköy Starbucks)",
                        leadingIcon = Icons.Default.Place,
                        trailing = {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = onSearch, enabled = query.trim().length >= 2) {
                                    Icon(Icons.Default.Search, contentDescription = "Ara")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (failed) {
                        Text(
                            "Arama yapılamadı, bağlantını kontrol edip tekrar dene.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (results.isEmpty() && !isSearching && query.isNotBlank()) {
                        Text(
                            "Sonuçlar burada görünür. Aramak için büyüteç simgesine dokun.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    results.forEach { place ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelect(place)
                                    expanded = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(place.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (place.address.isNotBlank()) {
                                Text(
                                    place.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Text(
                        "Arama OpenStreetMap ile yapılır.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

private fun VoteMode.label() = when (this) {
    VoteMode.LIKE -> "Beğen"
    VoteMode.SINGLE -> "Tek seçim"
    VoteMode.RANK -> "Sırala"
}

private fun VoteMode.hint() = when (this) {
    VoteMode.LIKE -> "Herkes seçenekleri kaydırarak beğenir; en çok beğenilen kazanır."
    VoteMode.SINGLE -> "Herkes tek bir seçenek seçer; en çok oy alan kazanır."
    VoteMode.RANK -> "Herkes tüm seçenekleri sıralar; en yüksek toplam puan kazanır."
}

/** Always-visible bottom bar: vote mode chips and the start button. */
@Composable
fun StickyStartBar(
    modifier: Modifier = Modifier,
    choiceCount: Int,
    mode: VoteMode,
    onModeChange: (VoteMode) -> Unit,
    onStart: () -> Unit
) {
    val enabled = choiceCount >= 2
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                VoteMode.entries.forEachIndexed { index, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { onModeChange(m) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = VoteMode.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            activeContentColor = MaterialTheme.colorScheme.primary,
                            activeBorderColor = MaterialTheme.colorScheme.primary,
                            inactiveContainerColor = Color.Transparent,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                            inactiveBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                        ),
                        label = { Text(m.label()) }
                    )
                }
            }
            Text(
                text = mode.hint(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            brush = if (enabled) Brush.horizontalGradient(listOf(CoralPrimary, CoralPrimaryDark))
                            else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f))),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (enabled) "Oylamaya Geç ($choiceCount seçenek)" else "En az 2 seçenek ekle",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        if (enabled) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "İleri", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
