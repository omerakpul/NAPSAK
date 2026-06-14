package com.napsak.app.ui.screens.voting

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.RoomState
import com.napsak.app.ui.screens.shared.SharedSessionViewModel
import com.napsak.app.ui.theme.CoralPrimary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VotingScreen(
    roomId: String,
    sharedViewModel: SharedSessionViewModel,
    onNavigateToResult: (String) -> Unit
) {
    BackHandler(enabled = true) {
        // Oylama esnasında geri tuşunu devre dışı bırak
    }

    val allChoices by sharedViewModel.choices.collectAsState()
    val choices = remember(allChoices) { allChoices.toList() }

    // Track liked choices
    val likedChoices = remember { mutableStateListOf<Choice>() }

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentChoice = choices.getOrNull(currentIndex)

    var isSelectingWinner by remember { mutableStateOf(false) }
    var animatingChoice by remember { mutableStateOf<Choice?>(null) }

    var votesSubmitted by remember { mutableStateOf(false) }

    val currentRoom by sharedViewModel.currentRoom.collectAsState()
    val winnerChoice by sharedViewModel.winnerChoice.collectAsState()

    // Countdown Timer State (5 seconds per choice)
    var timeLeft by remember(choices.size) { mutableIntStateOf(if (choices.isNotEmpty()) choices.size * 5 else 30) }

    // Oylama ekranı açıldığında oda güncellemelerini takip et
    LaunchedEffect(roomId) {
        sharedViewModel.observeRoom(roomId)
    }

    // Countdown Timer Effect
    LaunchedEffect(timeLeft, currentIndex, choices.size) {
        if (choices.isNotEmpty() && timeLeft > 0 && currentIndex < choices.size) {
            kotlinx.coroutines.delay(1000L)
            timeLeft--
        } else if (choices.isNotEmpty() && timeLeft == 0 && currentIndex < choices.size) {
            // Time's up! Force finish voting
            currentIndex = choices.size
        }
    }

    // Background Submit Votes Effect (When finished)
    LaunchedEffect(currentIndex, choices.size) {
        if (choices.isNotEmpty() && currentIndex >= choices.size && !votesSubmitted) {
            votesSubmitted = true
            sharedViewModel.submitVotes(roomId, likedChoices.map { it.id })
        }
    }

    // Oda durumu RESULT olduğunda ve kazanan belli olduğunda otomatik olarak sonuç ekranına yönlendir
    LaunchedEffect(currentRoom?.state, winnerChoice) {
        if (currentRoom?.state == RoomState.RESULT && winnerChoice != null) {
            val isHost = currentRoom?.hostId == sharedViewModel.currentUserId.value
            if (!isHost && !votesSubmitted) {
                votesSubmitted = true
                sharedViewModel.submitVotes(roomId, likedChoices.map { it.id })
            }
            onNavigateToResult(roomId)
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
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SEÇENEKLERİ SWIPE'LA",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                )
                if (currentIndex < choices.size) {
                    Text(
                        text = "Kalan Süre: ${timeLeft}s",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (timeLeft <= 5) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${currentIndex.coerceAtMost(choices.size)} / ${choices.size}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tinder Card Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (currentChoice != null) {
                    SwipeableCard(
                        choice = currentChoice,
                        onSwipeLeft = {
                            // Dislike - just skip
                            currentIndex++
                        },
                        onSwipeRight = {
                            // Like - add to liked list
                            if (!likedChoices.contains(currentChoice)) {
                                likedChoices.add(currentChoice)
                            }
                            currentIndex++
                        }
                    )
                } else if (choices.isNotEmpty()) {
                    // Voting Finished state
                    val room = currentRoom
                    val participantsList = room?.participants?.values?.toList() ?: emptyList()
                    val totalParticipants = participantsList.size
                    val finishedCount = participantsList.count { it.hasVoted }
                    val allVoted = participantsList.isNotEmpty() && participantsList.all { it.hasVoted }
                    val isHost = room?.hostId == sharedViewModel.currentUserId.value

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(24.dp)
                    ) {
                        Text(
                            text = "🎉",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Oylamayı bitirdin!",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${likedChoices.size} / ${choices.size} seçeneği beğendin.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        if (isHost) {
                            Text(
                                text = if (allVoted) "Herkes oylamayı tamamladı!" else "Diğer katılımcıların oylamayı bitirmesi bekleniyor...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (allVoted) CoralPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tamamlanan: $finishedCount / $totalParticipants",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            val coroutineScope = rememberCoroutineScope()
                            Button(
                                onClick = {
                                    val currentRoomVal = room ?: return@Button
                                    
                                    val dbChoices = currentRoomVal.choices.values.toList()
                                    val maxVotes = dbChoices.maxOfOrNull { it.voteCount } ?: 0
                                    
                                    val candidates = if (maxVotes > 0) {
                                        dbChoices.filter { it.voteCount == maxVotes }
                                    } else {
                                        dbChoices
                                    }
                                    
                                    val winner = candidates.random()
                                    val isTie = candidates.size > 1

                                    if (isTie) {
                                        isSelectingWinner = true
                                        coroutineScope.launch {
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
                                                animatingChoice = candidates[nextIndex]
                                                kotlinx.coroutines.delay(delayTime)
                                            }
                                            
                                            animatingChoice = winner
                                            kotlinx.coroutines.delay(1000L)
                                            
                                            sharedViewModel.declareWinner(roomId, winner) {
                                                isSelectingWinner = false
                                                onNavigateToResult(roomId)
                                            }
                                        }
                                    } else {
                                        sharedViewModel.declareWinner(roomId, winner) {
                                            onNavigateToResult(roomId)
                                        }
                                    }
                                },
                                enabled = allVoted,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Sonuçları Gör")
                            }
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = CoralPrimary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Diğer katılımcıların oylamayı tamamlaması bekleniyor...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tamamlanan: $finishedCount / $totalParticipants",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons (Cross & Heart)
            if (currentChoice != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dislike Button (Cross)
                    IconButton(
                        onClick = {
                            // Dislike - just skip
                            currentIndex++
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE57373).copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Pas geç",
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(48.dp))

                    // Like Button (Heart)
                    IconButton(
                        onClick = {
                            // Like - add to liked list
                            currentChoice?.let { choice ->
                                if (!likedChoices.contains(choice)) {
                                    likedChoices.add(choice)
                                }
                            }
                            currentIndex++
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF81C784).copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Beğen",
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Eşitlik Çözücü Animasyon Dialog
        if (isSelectingWinner && animatingChoice != null) {
            AlertDialog(
                onDismissRequest = {}, // Animasyon sırasında kapatılamasın
                title = {
                    Text(
                        text = "Eşitlik Çözülüyor...",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CoralPrimary,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .size(width = 220.dp, height = 280.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (!animatingChoice!!.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = animatingChoice!!.imageUrl,
                                        contentDescription = animatingChoice!!.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    )
                                    Text(
                                        text = animatingChoice!!.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(12.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = animatingChoice!!.name,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        CircularProgressIndicator(
                            color = CoralPrimary,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                },
                confirmButton = {},
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun SwipeableCard(
    choice: Choice,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val screenWidth = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    
    val swipeThreshold = screenWidth * 0.4f

    var isSwiped by remember(choice.id) { mutableStateOf(false) }
    val offsetX = remember(choice.id) { Animatable(0f) }
    val rotation = offsetX.value / screenWidth * 25f

    Box(
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .fillMaxWidth(0.95f),
        contentAlignment = Alignment.Center
    ) {
        // Sürükleme yönüne göre değişen arka plan (Yeşil/Kırmızı)
        val alpha = (kotlin.math.abs(offsetX.value) / swipeThreshold).coerceAtMost(1f) * 0.25f
        if (alpha > 0f) {
            val bgColor = if (offsetX.value > 0) {
                Color(0xFF4CAF50).copy(alpha = alpha) // Yeşil (Beğen)
            } else {
                Color(0xFFE57373).copy(alpha = alpha) // Kırmızı (Pas geç)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(bgColor)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .graphicsLayer {
                    rotationZ = rotation
                }
                .pointerInput(choice.id) {
                    detectDragGestures(
                        onDragEnd = {
                            if (isSwiped) return@detectDragGestures
                            coroutineScope.launch {
                                if (offsetX.value > swipeThreshold) {
                                    isSwiped = true
                                    // Swipe Right
                                    offsetX.animateTo(screenWidth, animationSpec = tween(300))
                                    onSwipeRight()
                                } else if (offsetX.value < -swipeThreshold) {
                                    isSwiped = true
                                    // Swipe Left
                                    offsetX.animateTo(-screenWidth, animationSpec = tween(300))
                                    onSwipeLeft()
                                } else {
                                    // Reset position
                                    offsetX.animateTo(0f, animationSpec = tween(200))
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (isSwiped) return@detectDragGestures
                            change.consume()
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = if (choice.imageUrl.isNullOrBlank()) Arrangement.Center else Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!choice.imageUrl.isNullOrBlank()) {
                    // Resim varsa
                    AsyncImage(
                        model = choice.imageUrl,
                        contentDescription = choice.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    )

                    // Alt detay alanı
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        if (choice.category.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CoralPrimary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = choice.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CoralPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = choice.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = choice.details,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // Resim yoksa ortalanmış içerik
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (choice.category.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CoralPrimary.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = choice.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CoralPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = choice.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                fontSize = 28.sp
                            )
                        )

                        if (choice.details.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = choice.details,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
