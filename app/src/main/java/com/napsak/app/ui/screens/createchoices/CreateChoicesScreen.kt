package com.napsak.app.ui.screens.createchoices

import androidx.compose.material.icons.filled.Info
import com.napsak.app.ui.components.NapsakTextField
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.VoteMode
import com.napsak.app.ui.theme.AmberSecondary
import com.napsak.app.ui.theme.CoralPrimary
import com.napsak.app.ui.theme.CoralPrimaryDark
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.ui.res.stringResource
import com.napsak.app.R
import com.napsak.app.ui.categoryLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateChoicesScreen(
    roomId: String,
    onNavigateToVoting: (String, List<Choice>, VoteMode) -> Unit,
    viewModel: CreateChoicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val compactHeight = LocalConfiguration.current.screenHeightDp < 500

    LaunchedEffect(roomId) { viewModel.observeSuggestions(roomId) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var listNameInput by remember { mutableStateOf("") }
    var listCategoryInput by remember { mutableStateOf("") }
    var showLoadDialog by remember { mutableStateOf(false) }
    var activeUploadChoiceId by remember { mutableStateOf<String?>(null) }
    val uploadingChoiceIds = remember { mutableStateListOf<String>() }
    var isUploadingImage by remember { mutableStateOf(false) }
    var savedListImageUrl by remember { mutableStateOf("") }
    var isUploadingSavedListImage by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingImage = true
            viewModel.uploadImage(context, it) { result ->
                when (result) {
                    is com.napsak.app.domain.model.UploadResult.Success -> {
                        viewModel.onImageUrlChange(result.url)
                    }
                    com.napsak.app.domain.model.UploadResult.FileTooLarge -> {
                        Toast.makeText(context, context.getString(R.string.file_too_big), Toast.LENGTH_LONG).show()
                    }
                    com.napsak.app.domain.model.UploadResult.Failure -> {
                        Toast.makeText(context, context.getString(R.string.upload_failed), Toast.LENGTH_SHORT).show()
                    }
                }
                isUploadingImage = false
            }
        }
    }

    val listChoiceImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val choiceId = activeUploadChoiceId
        if (uri != null && choiceId != null) {
            uploadingChoiceIds.add(choiceId)
            viewModel.uploadImage(context, uri) { result ->
                when (result) {
                    is com.napsak.app.domain.model.UploadResult.Success -> {
                        viewModel.updateChoiceImageUrl(choiceId, result.url)
                    }
                    com.napsak.app.domain.model.UploadResult.FileTooLarge -> {
                        Toast.makeText(context, context.getString(R.string.file_too_big), Toast.LENGTH_LONG).show()
                    }
                    com.napsak.app.domain.model.UploadResult.Failure -> {
                        Toast.makeText(context, context.getString(R.string.upload_failed), Toast.LENGTH_SHORT).show()
                    }
                }
                uploadingChoiceIds.remove(choiceId)
            }
        }
        activeUploadChoiceId = null
    }

    val savedListImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingSavedListImage = true
            viewModel.uploadImage(context, it) { result ->
                when (result) {
                    is com.napsak.app.domain.model.UploadResult.Success -> {
                        savedListImageUrl = result.url
                    }
                    com.napsak.app.domain.model.UploadResult.FileTooLarge -> {
                        Toast.makeText(context, context.getString(R.string.file_too_big), Toast.LENGTH_LONG).show()
                    }
                    com.napsak.app.domain.model.UploadResult.Failure -> {
                        Toast.makeText(context, context.getString(R.string.upload_failed), Toast.LENGTH_SHORT).show()
                    }
                }
                isUploadingSavedListImage = false
            }
        }
    }

    var selectedCategoryFilter by remember { mutableStateOf("Tümü") }

    val gradient = Brush.horizontalGradient(
        colors = listOf(CoralPrimary, CoralPrimaryDark)
    )

    val scrollState = rememberScrollState()

    val categoryOptions = remember(uiState.savedLists) {
        val base = listOf("Tümü", "Yemek", "Aktivite", "Film", "Eğlence")
        val custom = uiState.savedLists.map { it.category }.filter { it.isNotBlank() && it !in base }
        (base + custom).distinct()
    }

    val matchingTemplates = remember(selectedCategoryFilter, uiState.savedLists) {
        val presets = viewModel.getDefaultLists()
        // saved lists start out as a copy of the presets, so the same template must not show twice
        val allLists = (presets + uiState.savedLists).distinctBy { it.id }
        if (selectedCategoryFilter == "Tümü") {
            allLists
        } else {
            allLists.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
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
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Text(
                text = stringResource(R.string.create_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.fillMaxWidth().semantics { heading() },
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.create_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Şablon Kategoriler
            Text(
                text = stringResource(R.string.create_templates),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryOptions) { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) CoralPrimary else CoralPrimary.copy(alpha = 0.08f)
                            )
                            .clickable { selectedCategoryFilter = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = categoryLabel(cat),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else CoralPrimary
                            )
                        )
                    }
                }
            }

            // Matching Templates Horizontal Scroll
            if (matchingTemplates.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(matchingTemplates) { template ->
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { viewModel.loadChoiceList(template) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column {
                                // Image/Icon Header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .background(CoralPrimary.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!template.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = template.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        val emoji = when (template.category.lowercase()) {
                                            "yemek" -> "\uD83C\uDF54"
                                            "aktivite" -> "\uD83C\uDFAC"
                                            "film" -> "\uD83C\uDF7F"
                                            "e\u011flence" -> "\uD83C\uDFAE"
                                            "kahve" -> "\u2615"
                                            else -> null
                                        }
                                        if (emoji != null) {
                                            Text(
                                                text = emoji,
                                                fontSize = 26.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = CoralPrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = template.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.create_tpl_count, template.choices.size),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SuggestionsCard(
                suggestions = uiState.suggestions,
                onAccept = { viewModel.acceptSuggestion(roomId, it) },
                onDismiss = { viewModel.dismissSuggestion(roomId, it) }
            )
            if (uiState.suggestions.isNotEmpty()) Spacer(modifier = Modifier.height(12.dp))

            PlaceSearchCard(
                query = uiState.placeQuery,
                onQueryChange = viewModel::onPlaceQueryChange,
                onSearch = viewModel::searchPlaces,
                results = uiState.placeResults,
                isSearching = uiState.isSearchingPlaces,
                failed = uiState.placeSearchFailed,
                onSelect = viewModel::selectPlace
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Input Card
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NapsakTextField(
                        value = uiState.currentName,
                        onValueChange = { viewModel.onNameChange(it) },
                        label = stringResource(R.string.create_name_label),
                        leadingIcon = Icons.Default.Edit,
                        placeholder = stringResource(R.string.create_name_ph),
                        modifier = Modifier.fillMaxWidth()
                    )

                    NapsakTextField(
                        value = uiState.currentDetails,
                        onValueChange = { viewModel.onDetailsChange(it) },
                        label = stringResource(R.string.create_details_label),
                        leadingIcon = Icons.Default.Info,
                        placeholder = stringResource(R.string.create_details_ph),
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    NapsakTextField(
                        value = uiState.currentCategory,
                        onValueChange = { viewModel.onCategoryChange(it) },
                        label = stringResource(R.string.create_category_label),
                        leadingIcon = Icons.Default.Label,
                        placeholder = stringResource(R.string.create_category_ph),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Show existing categories in this room as suggestion chips
                    val existingCategories = uiState.choices.map { it.category }.filter { it.isNotBlank() }.distinct()
                    if (existingCategories.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(existingCategories) { cat ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (uiState.currentCategory == cat) CoralPrimary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { viewModel.onCategoryChange(cat) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = categoryLabel(cat),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (uiState.currentCategory == cat) CoralPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Add / Save Edit Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.isEditing) {
                            OutlinedButton(
                                onClick = { viewModel.cancelEdit() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                            ) {
                                Text(stringResource(R.string.cancel_edit), fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                if (uiState.isEditing) {
                                    viewModel.saveEdit()
                                } else {
                                    viewModel.addChoice()
                                }
                            },
                            modifier = Modifier.weight(2f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = uiState.currentName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isEditing) AmberSecondary else CoralPrimary
                            )
                        ) {
                            Icon(
                                imageVector = if (uiState.isEditing) Icons.Default.Edit else Icons.Default.Add,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(if (uiState.isEditing) R.string.save else R.string.create_add_option),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            QuickEntryCard(
                text = uiState.quickEntryText,
                onTextChange = viewModel::onQuickEntryChange,
                onAdd = viewModel::addQuickEntries
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Choice List Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.create_selected),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Clear All Button
                        if (uiState.choices.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.clear),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE57373)
                                ),
                                modifier = Modifier.clickable { viewModel.clearAllChoices() }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Listeyi Kaydet Butonu
                TextButton(
                    onClick = { showSaveDialog = true },
                    enabled = uiState.choices.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Text(stringResource(R.string.create_save_list), fontWeight = FontWeight.Bold)
                }

                // Listelerim Butonu
                TextButton(
                    onClick = { showLoadDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.create_my_lists), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Choice List
            if (uiState.choices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🗳️",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.create_empty),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                  fontWeight = FontWeight.SemiBold,
                                  color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.create_empty_hint),
                            style = MaterialTheme.typography.bodySmall.copy(
                                  color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            } else {
                val groupedChoices = uiState.choices.groupBy { it.category }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedChoices.forEach { (category, choicesInCategory) ->
                        Text(
                            text = if (category.isBlank()) stringResource(R.string.cat_general) else category.uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (category.isBlank()) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f) else CoralPrimary,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                        choicesInCategory.forEachIndexed { idx, choice ->
                            ChoiceListItem(
                                index = idx + 1,
                                choice = choice,
                                isUploadingImage = uploadingChoiceIds.contains(choice.id),
                                onEdit = { viewModel.startEditing(choice) },
                                onRemove = { viewModel.removeChoice(choice.id) },
                                onAddPhotoClick = {
                                    activeUploadChoiceId = choice.id
                                    listChoiceImagePickerLauncher.launch("image/*")
                                }
                            )
                        }
                    }
                }
            }

            // room for the sticky start bar
            Spacer(modifier = Modifier.height(if (compactHeight) 84.dp else 190.dp))
        }

        // hidden while typing so the keyboard and the bar do not squeeze the form
        if (!WindowInsets.isImeVisible) StickyStartBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            choiceCount = uiState.choices.size,
            mode = uiState.voteMode,
            compact = compactHeight,
            onModeChange = viewModel::onVoteModeChange,
            onStart = {
                viewModel.startVoting(
                    roomId,
                    onFailure = {
                        Toast.makeText(context, context.getString(R.string.create_start_failed), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    onNavigateToVoting(roomId, viewModel.getChoices(), uiState.voteMode)
                }
            }
        )
    }

    // Save List Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSaveDialog = false
                listNameInput = ""
                listCategoryInput = ""
                savedListImageUrl = ""
            },
            title = { Text(stringResource(R.string.save_list_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NapsakTextField(
                        value = listNameInput,
                        onValueChange = { listNameInput = it },
                        label = stringResource(R.string.save_list_name),
                        leadingIcon = Icons.Default.Edit
                    )
                    NapsakTextField(
                        value = listCategoryInput,
                        onValueChange = { listCategoryInput = it },
                        label = stringResource(R.string.save_list_category),
                        leadingIcon = Icons.Default.Label
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = !isUploadingSavedListImage) {
                                    savedListImagePickerLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingSavedListImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else if (savedListImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = savedListImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_image),
                                    tint = CoralPrimary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(stringResource(R.string.list_cover), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(stringResource(R.string.save_list_cover_hint), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (listNameInput.isNotBlank()) {
                            viewModel.saveChoiceList(listNameInput, listCategoryInput, savedListImageUrl.takeIf { it.isNotBlank() })
                            showSaveDialog = false
                            listNameInput = ""
                            listCategoryInput = ""
                            savedListImageUrl = ""
                        }
                    },
                    enabled = listNameInput.isNotBlank() && !isUploadingSavedListImage
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        listNameInput = ""
                        listCategoryInput = ""
                        savedListImageUrl = ""
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Listelerim Dialog
    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            title = { Text(stringResource(R.string.saved_lists_title)) },
            text = {
                if (uiState.savedLists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.lists_empty),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.savedLists) { savedList ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.loadChoiceList(savedList)
                                            showLoadDialog = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // List Cover thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CoralPrimary.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!savedList.imageUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = savedList.imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            val emoji = when (savedList.category.lowercase()) {
                                                "yemek" -> "\uD83C\uDF54"
                                                "aktivite" -> "\uD83C\uDFAC"
                                                "film" -> "\uD83C\uDF7F"
                                                "eğlence" -> "\uD83C\uDFAE"
                                                "kahve" -> "\u2615"
                                                else -> null
                                            }
                                            if (emoji != null) {
                                                Text(
                                                    text = emoji,
                                                    fontSize = 18.sp,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = CoralPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = savedList.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.saved_list_contains, savedList.choices.size),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteChoiceList(savedList.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = Color(0xFFE57373)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoadDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceListItem(
    index: Int,
    choice: Choice,
    isUploadingImage: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onAddPhotoClick: () -> Unit
) {
    val hasImage = !choice.imageUrl.isNullOrBlank()
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo thumbnail or category emoji placeholder (Clickable to upload photo)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (hasImage) MaterialTheme.colorScheme.surfaceVariant
                        else CoralPrimary.copy(alpha = 0.08f)
                    )
                    .clickable(enabled = !isUploadingImage) { onAddPhotoClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isUploadingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else if (hasImage) {
                    AsyncImage(
                        model = choice.imageUrl,
                        contentDescription = choice.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    val emoji: String? = com.napsak.app.ui.CategoryStyle.emoji(choice.name, choice.category)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (emoji != null) {
                            Text(
                                text = emoji,
                                fontSize = 20.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = null,
                                tint = CoralPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_photo),
                            tint = CoralPrimary,
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .background(Color.White, shape = CircleShape)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))

            // Choice info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = choice.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (choice.details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = choice.details,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action buttons with spacing
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberSecondary.copy(alpha = 0.10f))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = AmberSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Remove button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE57373).copy(alpha = 0.10f))
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove),
                        tint = Color(0xFFE57373),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
