package com.napsak.app.ui.screens.home

import androidx.compose.material.icons.filled.Key
import com.napsak.app.ui.components.NapsakTextField
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.SavedChoiceList
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.napsak.app.ui.theme.CoralPrimary
import com.napsak.app.ui.theme.CoralPrimaryDark
import com.napsak.app.ui.theme.AmberSecondary
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import com.napsak.app.R
import com.napsak.app.domain.model.RoomError

@Composable
fun HomeScreen(
    initialRoomCode: String? = null,
    onNavigateToLobby: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val savedName by viewModel.savedUsername.collectAsState()
    var name by remember { mutableStateOf("") }

    val savedLists by viewModel.savedLists.collectAsState()
    var editingList by remember { mutableStateOf<SavedChoiceList?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(savedName) {
        if (savedName.isNotBlank() && name.isBlank()) {
            name = savedName
        }
    }

    var roomCode by remember(initialRoomCode) { mutableStateOf(initialRoomCode.orEmpty()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorIsForJoin by remember { mutableStateOf(false) }
    val resumableRoomId by viewModel.resumableRoomId.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshResumableRoom() }

    val errNotFound = stringResource(R.string.err_room_not_found)
    val errVotingStarted = stringResource(R.string.err_voting_started)
    val errFull = stringResource(R.string.err_room_full)
    val errUnreadable = stringResource(R.string.err_room_read)
    val errConnection = stringResource(R.string.err_connection)
    fun friendly(e: Throwable): String = when ((e as? RoomError)?.kind) {
        RoomError.Kind.NOT_FOUND -> errNotFound
        RoomError.Kind.VOTING_STARTED -> errVotingStarted
        RoomError.Kind.FULL -> errFull
        RoomError.Kind.UNREADABLE -> errUnreadable
        null -> errConnection
    }
    val scrollState = rememberScrollState()

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
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo & Header
            Text(
                text = "N'APSAK?",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Login & Actions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.home_sign_in),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NapsakTextField(
                        value = name,
                        onValueChange = { input ->
                            if (input.length <= 15) {
                                name = input.filter { it.isLetterOrDigit() || it.isWhitespace() }
                            }
                        },
                        label = stringResource(R.string.home_nickname),
                        maxLength = 15,
                        leadingIcon = Icons.Default.Person,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Resume an unfinished room (e.g. after the app was closed)
                    resumableRoomId?.let { roomId ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.home_resume, roomId),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // stacked so large font sizes cannot squeeze the labels
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Button(
                                        onClick = { onNavigateToLobby(roomId) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(stringResource(R.string.home_back_to_room)) }
                                    TextButton(
                                        onClick = { viewModel.leaveResumableRoom(roomId) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(stringResource(R.string.home_leave_room)) }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Host & Join actions (only visible when name is entered)
                    AnimatedVisibility(
                        visible = name.isNotBlank(),
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            // Create Room Button
                            Button(
                                onClick = {
                                    if (name.isNotBlank() && !isLoading) {
                                        isLoading = true
                                        errorMessage = null
                                        viewModel.createRoom(name) { result ->
                                            isLoading = false
                                            result
                                                .onSuccess { room -> onNavigateToLobby(room.id) }
                                                .onFailure { errorMessage = friendly(it); errorIsForJoin = false }
                                        }
                                    }
                                },
                                enabled = !isLoading,
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
                                            imageVector = Icons.Default.Add,
                                            contentDescription = stringResource(R.string.home_create_cd),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(if (isLoading) R.string.home_waiting else R.string.home_create),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = stringResource(R.string.home_join_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            NapsakTextField(
                                value = roomCode,
                                onValueChange = {
                                    roomCode = it.filter(Char::isDigit).take(6)
                                    errorMessage = null
                                },
                                label = stringResource(R.string.home_room_code),
                                helperText = stringResource(R.string.home_room_code_helper),
                                errorText = errorMessage.takeIf { errorIsForJoin },
                                leadingIcon = Icons.Default.Key,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (name.isNotBlank() && roomCode.length == 6 && !isLoading) {
                                        isLoading = true
                                        errorMessage = null
                                        viewModel.joinRoom(roomCode, name) { result ->
                                            isLoading = false
                                            result
                                                .onSuccess { room -> onNavigateToLobby(room.id) }
                                                .onFailure { errorMessage = friendly(it); errorIsForJoin = true }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = roomCode.length == 6 && !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.home_join),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = stringResource(R.string.home_join_cd)
                                    )
                                }
                            }

                            errorMessage?.takeIf { !errorIsForJoin }?.let { message ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
