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
import androidx.hilt.navigation.compose.hiltViewModel
import com.napsak.app.domain.model.RoomState
import com.napsak.app.ui.theme.AmberSecondary
import com.napsak.app.ui.theme.CoralPrimary
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage

import com.napsak.app.ui.screens.shared.SharedSessionViewModel

@Composable
fun LobbyScreen(
    roomId: String,
    sharedViewModel: SharedSessionViewModel,
    onNavigateToCreateChoices: (String) -> Unit,
    onNavigateToVoting: (String) -> Unit,
    viewModel: LobbyViewModel = hiltViewModel()
) {
    val roomState by viewModel.room.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val context = LocalContext.current

    // Start observing room updates when screen opens
    LaunchedEffect(roomId) {
        viewModel.observeRoom(roomId)
    }

    // Oylama resmi olarak başladığında tüm katılımcıları oylama ekranına yönlendir
    LaunchedEffect(roomState?.state) {
        if (roomState?.state == RoomState.VOTING) {
            val roomChoices = roomState?.choices?.values?.toList() ?: emptyList()
            sharedViewModel.setChoices(roomChoices)
            onNavigateToVoting(roomId)
        }
    }

    val room = roomState
    if (room == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CoralPrimary)
        }
        return
    }

    val participants = room.participants.values.toList()
    val myParticipant = room.participants[currentUserId]
    val isReady = myParticipant?.isReady == true
    val isHost = room.hostId == currentUserId

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
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "N'APSAK? Odamıza katıl!\nLink: https://omerakpul.github.io/NAPSAK/?room=$roomId\nOda Kodu: $roomId")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Paylaş")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kodu Arkadaşlarınla Paylaş")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 1.dp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Veya QR Kodu Taratın",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        modifier = Modifier.size(140.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=https://omerakpul.github.io/NAPSAK/?room=$roomId",
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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
                    val isParticipantHost = room.hostId == participant.id
                    val displayName = if (isParticipantHost) "${participant.name} (Host)" else participant.name
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
                                text = displayName,
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
                        viewModel.toggleReady(roomId, !isReady)
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
                        onClick = {
                            onNavigateToCreateChoices(roomId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = allReady,
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
