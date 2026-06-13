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
import coil.compose.AsyncImage
import com.napsak.app.domain.model.Choice
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VotingScreen(
    roomId: String,
    onNavigateToResult: (String) -> Unit
) {
    // TODO: Receive choices from CreateChoicesScreen via shared ViewModel or navigation args
    val choices = remember { mutableStateListOf<Choice>() }

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentChoice = choices.getOrNull(currentIndex)

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

            // Progress Indicators
            Text(
                text = "SEÇENEKLERİ SWIPE'LA",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
            )

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
                            currentIndex++
                        },
                        onSwipeRight = {
                            currentIndex++
                        }
                    )
                } else {
                    // Voting Finished state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Harika! Oylamayı bitirdin.",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Diğer arkadaşların da oylamayı bitirdiğinde sonuçlar açıklanacak.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { onNavigateToResult(roomId) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Sonuçları Gör")
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

    val offsetX = remember { Animatable(0f) }
    val rotation = offsetX.value / screenWidth * 25f

    Card(
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .fillMaxWidth(0.95f)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer {
                rotationZ = rotation
            }
            .pointerInput(choice.id) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value > swipeThreshold) {
                                // Swipe Right
                                offsetX.animateTo(screenWidth, animationSpec = tween(300))
                                onSwipeRight()
                            } else if (offsetX.value < -swipeThreshold) {
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
        Column(modifier = Modifier.fillMaxSize()) {
            // Choice Image
            AsyncImage(
                model = choice.imageUrl,
                contentDescription = choice.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            )

            // Info details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
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
        }
    }
}
