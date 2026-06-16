package com.napsak.app.ui.screens.createchoices

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
import com.napsak.app.ui.theme.AmberSecondary
import com.napsak.app.ui.theme.CoralPrimary
import com.napsak.app.ui.theme.CoralPrimaryDark
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChoicesScreen(
    roomId: String,
    onNavigateToVoting: (String, List<Choice>) -> Unit,
    viewModel: CreateChoicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            coroutineScope.launch {
                when (val result = com.napsak.app.data.util.ImgbbUploader.uploadImage(context, it)) {
                    is com.napsak.app.data.util.ImgbbUploader.UploadResult.Success -> {
                        viewModel.onImageUrlChange(result.url)
                    }
                    com.napsak.app.data.util.ImgbbUploader.UploadResult.FileTooLarge -> {
                        Toast.makeText(context, "Seçilen dosya çok büyük (Maksimum 10 MB)", Toast.LENGTH_LONG).show()
                    }
                    com.napsak.app.data.util.ImgbbUploader.UploadResult.Failure -> {
                        Toast.makeText(context, "Yükleme başarısız", Toast.LENGTH_SHORT).show()
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
            coroutineScope.launch {
                when (val result = com.napsak.app.data.util.ImgbbUploader.uploadImage(context, uri)) {
                    is com.napsak.app.data.util.ImgbbUploader.UploadResult.Success -> {
                        viewModel.updateChoiceImageUrl(choiceId, result.url)
                    }
                    com.napsak.app.data.util.ImgbbUploader.UploadResult.FileTooLarge -> {
                        Toast.makeText(context, "Seçilen dosya çok büyük (Maksimum 10 MB)", Toast.LENGTH_LONG).show()
                    }
                    com.napsak.app.data.util.ImgbbUploader.UploadResult.Failure -> {
                        Toast.makeText(context, "Yükleme başarısız", Toast.LENGTH_SHORT).show()
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
            coroutineScope.launch {
                when (val result = com.napsak.app.data.util.ImgbbUploader.uploadImage(context, it)) {
                    is com.napsak.app.data.util.ImgbbUploader.UploadResult.Success -> {
                        savedListImageUrl = result.url
                    }
                    com.napsak.app.data.util.ImgbbUploader.UploadResult.FileTooLarge -> {
                        Toast.makeText(context, "Seçilen dosya çok büyük (Maksimum 10 MB)", Toast.LENGTH_LONG).show()
                    }
                    com.napsak.app.data.util.ImgbbUploader.UploadResult.Failure -> {
                        Toast.makeText(context, "Yükleme başarısız", Toast.LENGTH_SHORT).show()
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
        val allLists = presets + uiState.savedLists
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
                text = "SEÇENEKLERİ OLUŞTUR",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Oylanacak seçenekleri ekle",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Şablon Kategoriler
            Text(
                text = "Şablon Kategoriler",
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
                            text = cat,
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
                                        text = "${template.choices.size} Se\u00e7enek",
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
                    OutlinedTextField(
                        value = uiState.currentName,
                        onValueChange = { viewModel.onNameChange(it) },
                        label = { Text("Seçenek adı") },
                        placeholder = { Text("ör: Starbucks, Sinema, Bowling...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CoralPrimary,
                            focusedLabelColor = CoralPrimary,
                            cursorColor = CoralPrimary
                        )
                    )

                    OutlinedTextField(
                        value = uiState.currentDetails,
                        onValueChange = { viewModel.onDetailsChange(it) },
                        label = { Text("Açıklama (isteğe bağlı)") },
                        placeholder = { Text("Kısa bir not ekle...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CoralPrimary,
                            focusedLabelColor = CoralPrimary,
                            cursorColor = CoralPrimary
                        )
                    )

                    OutlinedTextField(
                        value = uiState.currentCategory,
                        onValueChange = { viewModel.onCategoryChange(it) },
                        label = { Text("Kategori (isteğe bağlı)") },
                        placeholder = { Text("ör: Yemek, Aktivite, Tatlılar...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CoralPrimary,
                            focusedLabelColor = CoralPrimary,
                            cursorColor = CoralPrimary
                        )
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
                                        text = cat,
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
                                Text("İptal", fontWeight = FontWeight.Bold)
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
                                text = if (uiState.isEditing) "Kaydet" else "Seçenek Ekle",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

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
                        text = "Seçilenler",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Clear All Button
                        if (uiState.choices.isNotEmpty()) {
                            Text(
                                text = "Temizle",
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
                        contentColor = CoralPrimary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Text("Listeyi Kaydet", fontWeight = FontWeight.Bold)
                }

                // Listelerim Butonu
                TextButton(
                    onClick = { showLoadDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Listelerim", fontWeight = FontWeight.Bold)
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
                            text = "Henüz seçenek eklenmedi",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                  fontWeight = FontWeight.SemiBold,
                                  color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Yukarıdan şablon yükle veya kendin ekle",
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
                            text = if (category.isBlank()) "GENEL" else category.uppercase(),
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

            Spacer(modifier = Modifier.height(20.dp))

            // Start Voting Button
            Button(
                onClick = {
                    viewModel.startVoting(roomId) {
                        onNavigateToVoting(roomId, viewModel.getChoices())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState.choices.size >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (uiState.choices.size >= 2) gradient
                            else Brush.horizontalGradient(
                                listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Oylamaya Geç",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "İleri",
                            tint = Color.White
                        )
                    }
                }
            }

            if (uiState.choices.size < 2) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Oylamaya başlamak için en az 2 seçenek ekle",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
            title = { Text("Listeyi Şablon Olarak Kaydet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = listNameInput,
                        onValueChange = { listNameInput = it },
                        label = { Text("Liste Adı") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = listCategoryInput,
                        onValueChange = { listCategoryInput = it },
                        label = { Text("Kategori") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
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
                                    color = CoralPrimary,
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
                                    contentDescription = "Resim Ekle",
                                    tint = CoralPrimary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text("Liste Kapak Resmi", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Galeriden resim seç (isteğe bağlı)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
                    Text("Kaydet")
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
                    Text("İptal")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Listelerim Dialog
    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            title = { Text("Kayıtlı Listelerim") },
            text = {
                if (uiState.savedLists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Henüz kayıtlı bir listeniz yok.",
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
                                            text = "${savedList.choices.size} seçenek içeriyor",
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
                                            contentDescription = "Sil",
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
                    Text("Kapat")
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
                    .size(44.dp)
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
                        color = CoralPrimary,
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
                    val emoji = when (choice.category.lowercase()) {
                        "yemek" -> "🍔"
                        "aktivite" -> "🎬"
                        "film" -> "🍿"
                        "eğlence" -> "🎮"
                        "kahve" -> "☕"
                        else -> null
                    }
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
                            contentDescription = "Fotoğraf Ekle",
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
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberSecondary.copy(alpha = 0.10f))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "D\u00fczenle",
                        tint = AmberSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Remove button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE57373).copy(alpha = 0.10f))
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kald\u0131r",
                        tint = Color(0xFFE57373),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
