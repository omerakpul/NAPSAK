package com.napsak.app.ui.screens.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import com.napsak.app.domain.model.Choice
import com.napsak.app.ui.screens.home.HomeViewModel
import com.napsak.app.ui.theme.CoralPrimary
import com.napsak.app.ui.theme.AmberSecondary
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListScreen(
    listId: String,
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val savedLists by viewModel.savedLists.collectAsState()
    val existingList = remember(listId, savedLists) {
        if (listId.isNotBlank()) savedLists.find { it.id == listId } else null
    }

    var listNameInput by remember { mutableStateOf("") }
    var listCategoryInput by remember { mutableStateOf("") }
    var listImageUrl by remember { mutableStateOf("") }
    var isUploadingListImage by remember { mutableStateOf(false) }
    val editingChoices = remember { mutableStateListOf<Choice>() }
    var newChoiceName by remember { mutableStateOf("") }
    var newChoiceDetails by remember { mutableStateOf("") }
    var newChoiceImageUrl by remember { mutableStateOf("") }

    var activeUploadChoiceId by remember { mutableStateOf<String?>(null) }
    val uploadingChoiceIds = remember { mutableStateListOf<String>() }
    var isUploadingImage by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val listImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingListImage = true
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("lists/${UUID.randomUUID()}.jpg")
            imageRef.putFile(it)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        listImageUrl = downloadUri.toString()
                        isUploadingListImage = false
                    }.addOnFailureListener { e ->
                        isUploadingListImage = false
                        Toast.makeText(context, "Url alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    isUploadingListImage = false
                    Toast.makeText(context, "Yükleme başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingImage = true
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("choices/${UUID.randomUUID()}.jpg")
            imageRef.putFile(it)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        newChoiceImageUrl = downloadUri.toString()
                        isUploadingImage = false
                    }.addOnFailureListener { e ->
                        isUploadingImage = false
                        Toast.makeText(context, "Url alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    isUploadingImage = false
                    Toast.makeText(context, "Yükleme başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val listChoiceImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val choiceId = activeUploadChoiceId
        if (uri != null && choiceId != null) {
            uploadingChoiceIds.add(choiceId)
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("choices/${UUID.randomUUID()}.jpg")
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val index = editingChoices.indexOfFirst { it.id == choiceId }
                        if (index != -1) {
                            editingChoices[index] = editingChoices[index].copy(imageUrl = downloadUri.toString())
                        }
                        uploadingChoiceIds.remove(choiceId)
                    }.addOnFailureListener {
                        uploadingChoiceIds.remove(choiceId)
                    }
                }
                .addOnFailureListener {
                    uploadingChoiceIds.remove(choiceId)
                }
        }
        activeUploadChoiceId = null
    }

    // Initialize list data once when loaded
    LaunchedEffect(existingList) {
        if (existingList != null && listNameInput.isEmpty() && editingChoices.isEmpty()) {
            listNameInput = existingList.name
            listCategoryInput = existingList.category
            listImageUrl = existingList.imageUrl ?: ""
            editingChoices.addAll(existingList.choices)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (listId.isBlank()) "Yeni Liste Oluştur" else "Listeyi Düzenle",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri Dön"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp) // Leave space for Save button
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = listNameInput,
                        onValueChange = { listNameInput = it },
                        label = { Text("Liste Adı (Örn: Kahve, Yemek)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CoralPrimary,
                            focusedLabelColor = CoralPrimary,
                            cursorColor = CoralPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = listCategoryInput,
                        onValueChange = { listCategoryInput = it },
                        label = { Text("Liste Kategorisi (Örn: Yemek, Aktivite, Film)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CoralPrimary,
                            focusedLabelColor = CoralPrimary,
                            cursorColor = CoralPrimary
                        )
                    )

                }

                item {
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
                            Text(
                                text = "Yeni Seçenek Ekle",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CoralPrimary
                            )

                            OutlinedTextField(
                                value = newChoiceName,
                                onValueChange = { newChoiceName = it },
                                label = { Text("Seçenek Adı") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CoralPrimary,
                                    focusedLabelColor = CoralPrimary,
                                    cursorColor = CoralPrimary
                                )
                            )

                            OutlinedTextField(
                                value = newChoiceDetails,
                                onValueChange = { newChoiceDetails = it },
                                label = { Text("Açıklama (İsteğe bağlı)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CoralPrimary,
                                    focusedLabelColor = CoralPrimary,
                                    cursorColor = CoralPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    if (newChoiceName.isNotBlank()) {
                                        editingChoices.add(
                                            Choice(
                                                id = java.util.UUID.randomUUID().toString(),
                                                name = newChoiceName.trim(),
                                                details = newChoiceDetails.trim(),
                                                imageUrl = null,
                                                category = listCategoryInput.trim()
                                            )
                                        )
                                        newChoiceName = ""
                                        newChoiceDetails = ""
                                        newChoiceImageUrl = ""
                                    }
                                },
                                enabled = newChoiceName.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Seçeneği Listeye Ekle", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seçenekler",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Badge(
                            containerColor = AmberSecondary.copy(alpha = 0.15f),
                            contentColor = AmberSecondary
                        ) {
                            Text(
                                text = "${editingChoices.size} Öğe",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (editingChoices.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Lütfen listenize en az 1 seçenek ekleyin.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                items(editingChoices) { choice ->
                    val isUploadingThisChoice = uploadingChoiceIds.contains(choice.id)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Category emoji Box (Replacer for custom photo)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CoralPrimary.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val emoji = when (listCategoryInput.lowercase()) {
                                    "yemek" -> "🍔"
                                    "aktivite" -> "🎬"
                                    "film" -> "🍿"
                                    "eğlence" -> "🎮"
                                    "kahve" -> "☕"
                                    else -> "📝"
                                }
                                Text(
                                    text = emoji,
                                    fontSize = 22.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = choice.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (choice.details.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = choice.details,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                            }

                            IconButton(
                                onClick = { editingChoices.remove(choice) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Sil",
                                    tint = Color(0xFFE57373),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Save Button (Fixed at Bottom)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp)
            ) {
                Button(
                    onClick = {
                        if (listNameInput.isNotBlank() && editingChoices.isNotEmpty()) {
                            if (listId.isBlank()) {
                                viewModel.saveChoiceList(listNameInput, listCategoryInput, editingChoices.toList(), listImageUrl.takeIf { it.isNotBlank() })
                            } else {
                                viewModel.updateChoiceList(listId, listNameInput, listCategoryInput, editingChoices.toList(), listImageUrl.takeIf { it.isNotBlank() })
                            }
                            onNavigateBack()
                        }
                    },
                    enabled = listNameInput.isNotBlank() && editingChoices.isNotEmpty() && !isUploadingListImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary)
                ) {
                    Text("Kaydet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
