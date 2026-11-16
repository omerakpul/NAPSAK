package com.napsak.app.ui.screens.lobby

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.activity.compose.BackHandler
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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import com.napsak.app.ui.qrBitmap
import com.napsak.app.ui.roomLink
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage

import com.napsak.app.ui.screens.shared.SharedSessionViewModel
import androidx.compose.ui.res.stringResource
import com.napsak.app.R

@Composable
fun LobbyScreen(
    roomId: String,
    sharedViewModel: SharedSessionViewModel,
    onNavigateToCreateChoices: (String) -> Unit,
    onNavigateToVoting: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LobbyViewModel = hiltViewModel()
) {
    val roomState by viewModel.room.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val context = LocalContext.current

    val roomClosed by viewModel.roomClosed.collectAsState()

    // Leaving the lobby (back) removes me from the room; the host leaving closes it.
    BackHandler {
        viewModel.leaveRoom(roomId)
        onNavigateBack()
    }

    // Room was closed by the host while I was waiting
    LaunchedEffect(roomClosed) {
        if (roomClosed) onNavigateBack()
    }

    // Start observing room updates when screen opens
    LaunchedEffect(roomId) {
        viewModel.observeRoom(roomId)
    }

    // Oylama resmi olarak başladığında tüm katılımcıları oylama ekranına yönlendir
    LaunchedEffect(roomState?.state) {
        if (roomState?.state == RoomState.VOTING) {
            val roomChoices = roomState?.choices?.values?.toList() ?: emptyList()
            sharedViewModel.setChoices(roomChoices)
            roomState?.let { sharedViewModel.setVoteMode(it.voteMode) }
            onNavigateToVoting(roomId)
        }
    }

    val room = roomState
    if (room == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val participants = room.participants.values.toList()
    val myParticipant = room.participants[currentUserId]
    val isReady = myParticipant?.isReady == true
    val isHost = room.hostId == currentUserId

    // phones in landscape and small split-screen windows
    val compactHeight = LocalConfiguration.current.screenHeightDp < 500

    // Bottom Actions (Ready Toggle & Start Button)
    val bottomActions: @Composable () -> Unit = {
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
                containerColor = if (isReady) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary,
                contentColor = if (isReady) Color.White else MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.lobby_ready_cd)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(if (isReady) R.string.lobby_im_ready else R.string.lobby_get_ready),
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
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = stringResource(R.string.lobby_start_cd))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.lobby_start),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            if (!allReady) {
                Text(
                    text = stringResource(R.string.lobby_start_hint),
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
            // Participant List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Lobby Header
            Text(
                modifier = Modifier.semantics { heading() },
                text = stringResource(R.string.lobby_title),
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
                        text = stringResource(R.string.lobby_room_code),
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
                        ),
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.lobby_room_code), roomId))
                            Toast.makeText(context, context.getString(R.string.lobby_code_copied), Toast.LENGTH_SHORT).show()
                        }
                    )
                    Text(
                        text = stringResource(R.string.lobby_tap_copy),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.lobby_share_text, roomLink(roomId), roomId))
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = stringResource(R.string.share_cd))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.lobby_share_code))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 1.dp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.lobby_or_qr),
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
                            val qr = remember(roomId) { qrBitmap(roomLink(roomId)).asImageBitmap() }
                            Image(
                                bitmap = qr,
                                contentDescription = stringResource(R.string.lobby_qr_cd),
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
                    text = stringResource(R.string.lobby_participants, participants.size),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = stringResource(R.string.lobby_ready_ratio, participants.count { it.isReady }, participants.size),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

                    }
                }

                items(participants) { participant ->
                    val isParticipantHost = room.hostId == participant.id
                    val displayName = if (isParticipantHost) stringResource(R.string.lobby_host_suffix, participant.name) else participant.name
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
                            val statusText = stringResource(if (participant.isReady) R.string.status_ready else R.string.status_waiting)
                            
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

                // Participants can propose options; the host sees them on the next screen
                if (!isHost) {
                    item {
                        SuggestionForm(
                            mine = room.suggestions.filter { it.userId == currentUserId },
                            onSubmit = { name, details, onResult -> viewModel.suggestChoice(roomId, name, details, onResult) },
                            onRemove = { viewModel.dismissSuggestion(roomId, it.id) }
                        )
                    }
                } else if (room.suggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.lobby_suggestions_in, room.suggestions.size),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
                if (compactHeight) {
                    item { bottomActions() }
                }
            }

            if (!compactHeight) {
                Spacer(modifier = Modifier.height(24.dp))
                bottomActions()
            }
        }
    }
}
