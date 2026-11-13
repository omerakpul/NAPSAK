package com.napsak.app.ui.screens.result

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.material.icons.filled.Share
import com.napsak.app.domain.model.VoteMode
import com.napsak.app.ui.OptionEmoji
import com.napsak.app.ui.ShareCard
import com.napsak.app.domain.model.Choice
import com.napsak.app.ui.screens.shared.SharedSessionViewModel
import com.napsak.app.ui.theme.CoralPrimary
import com.napsak.app.ui.theme.CoralPrimaryDark

@Composable
fun ResultScreen(
    roomId: String,
    sharedViewModel: SharedSessionViewModel,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val winnerChoice by sharedViewModel.winnerChoice.collectAsState()
    val currentRoom by sharedViewModel.currentRoom.collectAsState()

    val unit = if (currentRoom?.voteMode == VoteMode.RANK) "Puan" else "Oy"
    val choices = currentRoom?.choices?.values?.toList() ?: emptyList()
    val maxVotes = choices.maxOfOrNull { it.voteCount } ?: 0
    val candidates = if (maxVotes > 0) {
        choices.filter { it.voteCount == maxVotes }
    } else {
        choices
    }
    val isTie = candidates.size > 1

    var isRaffleActive by remember { mutableStateOf(isTie) }
    var displayedChoice by remember { mutableStateOf<Choice?>(null) }

    LaunchedEffect(isTie, candidates, winnerChoice) {
        if (isTie && candidates.isNotEmpty() && winnerChoice != null) {
            isRaffleActive = true
            val animationSteps = listOf(
                50L, 50L, 60L, 60L, 70L, 80L, 90L, 100L, 120L, 140L, 170L, 200L, 240L, 300L, 380L, 480L, 600L
            )
            var lastIndex = -1
            for (delayTime in animationSteps) {
                var nextIndex = (0 until candidates.size).random()
                if (candidates.size > 1) {
                    while (nextIndex == lastIndex) {
                        nextIndex = (0 until candidates.size).random()
                    }
                }
                lastIndex = nextIndex
                displayedChoice = candidates[nextIndex]
                kotlinx.coroutines.delay(delayTime)
            }
            displayedChoice = winnerChoice
            kotlinx.coroutines.delay(1000L)
            isRaffleActive = false
        } else {
            displayedChoice = winnerChoice
            isRaffleActive = false
        }
    }

    // If no winner is set or raffle hasn't initialized displayedChoice, show fallback
    if (winnerChoice == null || (isTie && displayedChoice == null)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val winner = displayedChoice ?: winnerChoice!!

    // Keep a private on-device record of every finished decision
    LaunchedEffect(isRaffleActive, winnerChoice) {
        if (!isRaffleActive && winnerChoice != null) sharedViewModel.recordHistory(roomId)
    }

    val gradient = Brush.horizontalGradient(
        colors = listOf(CoralPrimary, CoralPrimaryDark)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Celebratory Header
                Text(
                    modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite },
                    text = if (isRaffleActive) "🎲 KURA ÇEKİLİYOR... 🎲" else "🎉 KAZANAN SEÇENEK 🎉",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isRaffleActive) "Eşitlik durumunda kura çekiliyor:" else "Grup kararını verdi! Bu akşam buradayız:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Winner Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.65f),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    if (!winner.imageUrl.isNullOrBlank()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Image
                            AsyncImage(
                                model = winner.imageUrl,
                                contentDescription = winner.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            )

                            // Info
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = winner.name,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 24.sp
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    SuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                text = if (isRaffleActive) "? $unit" else "${winner.voteCount} $unit",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (isRaffleActive) "Kura çekimi yapılıyor..." else winner.details,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    } else {
                        // Centered contents for no-image card
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            OptionEmoji(winner.name, winner.category, size = 120.dp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = winner.name,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    fontSize = 28.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = if (isRaffleActive) "? $unit" else "${winner.voteCount} $unit",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )

                            if (winner.details.isNotBlank() || isRaffleActive) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isRaffleActive) "Kura çekimi yapılıyor..." else winner.details,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Actions (Open Maps & Back Home)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Open in Maps Button (Only show if location is available and raffle finished)
                if (!isRaffleActive && winner.latitude != null && winner.longitude != null) {
                    Button(
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:${winner.latitude},${winner.longitude}?q=${Uri.encode(winner.name)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(mapIntent)
                            } else {
                                // Fallback to web browser search
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${winner.latitude},${winner.longitude}"))
                                context.startActivity(webIntent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(gradient, shape = RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "Maps",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Haritada Aç",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Share the result as an image card
                if (!isRaffleActive) {
                    OutlinedButton(
                        onClick = {
                            ShareCard.share(
                                context,
                                winner,
                                "${winner.voteCount} $unit",
                                currentRoom?.participants?.size ?: 1
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Paylaş")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sonucu Paylaş", fontWeight = FontWeight.Bold)
                    }
                }

                // Back to Home Button
                if (!isRaffleActive) {
                    Button(
                        onClick = {
                            val currentRoomVal = sharedViewModel.currentRoom.value
                            val isHost = currentRoomVal?.hostId == sharedViewModel.currentUserId.value
                            if (isHost) {
                                sharedViewModel.deleteRoom(roomId)
                            }
                            onNavigateToHome()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ana Sayfaya Dön",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
