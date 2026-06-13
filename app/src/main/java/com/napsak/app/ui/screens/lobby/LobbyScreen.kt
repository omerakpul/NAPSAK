package com.napsak.app.ui.screens.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.napsak.app.domain.model.Participant
import com.napsak.app.ui.theme.AmberSecondary
import com.napsak.app.ui.theme.CoralPrimary

@Composable
fun LobbyScreen(
    roomId: String,
    onNavigateToVoting: (String) -> Unit
) {
    // Mock list of participants for UI design (will be connected to database flow in Step 2)
    val participants = remember {
        mutableStateListOf(
            Participant(id = "1", name = "Ahmet (Host)", isReady = true),
            Participant(id = "2", name = "Mehmet (Siz)", isReady = false),
            Participant(id = "3", name = "Zeynep", isReady = true),
            Participant(id = "4", name = "Buse", isReady = false)
        )
    }

    var isReady by remember { mutableStateOf(false) }
    val isHost = true // Mock host status

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Lobby Header
            Text(
                text = "BEKLEME LOBİSİ",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Room Code Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Oda Kodu",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = roomId,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 4.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { /* Handle share */ },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Paylaş")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kodu Arkadaşlarınla Paylaş")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Participant List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Katılımcılar (${participants.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Hazır: ${participants.count { it.isReady }}/${participants.size}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Participant List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(participants) { participant ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar placeholder
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = if (participant.isReady) CoralPrimary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = participant.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (participant.isReady) CoralPrimary else Color.Gray
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = participant.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // Ready indicator chip
                            val statusColor = if (participant.isReady) Color(0xFF4CAF50) else AmberSecondary
                            val statusText = if (participant.isReady) "Hazır" else "Bekliyor"
                            
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = statusColor.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Actions (Ready Toggle & Start Button)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Toggle Ready Button
                Button(
                    onClick = {
                        isReady = !isReady
                        // Update local participant state for UI testing
                        val myIndex = participants.indexOfFirst { it.id == "2" }
                        if (myIndex != -1) {
                            participants[myIndex] = participants[myIndex].copy(isReady = isReady)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReady) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Hazır Durumu"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isReady) "Hazırım!" else "Hazır Durumuna Geç",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Start Voting (Only for Host, active only when everyone is ready)
                if (isHost) {
                    val allReady = participants.all { it.isReady }
                    Button(
                        onClick = { onNavigateToVoting(roomId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Başlat")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Oylamayı Başlat",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    if (!allReady) {
                        Text(
                            text = "Oylamayı başlatmak için tüm katılımcıların hazır olması bekleniyor.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
