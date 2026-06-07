package com.example.checker.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import com.google.gson.Gson
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.checker.data.*
import com.example.checker.service.NotificationMonitorService
import com.example.checker.ui.components.*
import com.example.checker.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Custom Layout modifier to scale content and its bounds.
 */
fun Modifier.scaleLayout(scale: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(
        (placeable.width * scale).toInt(),
        (placeable.height * scale).toInt()
    ) {
        placeable.placeRelativeWithLayer(0, 0) {
            this.scaleX = scale
            this.scaleY = scale
        }
    }
}

/**
 * Screen wrapper for premium styling.
 */
@Composable
fun CyberScreenWrapper(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(NeonGreen, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

/**
 * Dashboard / Home Screen
 */
@Composable
fun DashboardScreen(
    navController: NavController,
    repository: CheckerRepository
) {
    val context = LocalContext.current
    val historyList by repository.historyList.collectAsState()
    
    var isAutoScanEnabled by remember { mutableStateOf(repository.isAutoScanEnabled()) }
    var isServiceActive by remember { mutableStateOf(false) }
    var isPermissionGranted by remember { mutableStateOf(false) }

    // Check service status and preference
    LaunchedEffect(Unit) {
        while(true) {
            isServiceActive = NotificationMonitorService.isServiceRunning
            isPermissionGranted = NotificationMonitorService.isEnabled(context)
            isAutoScanEnabled = repository.isAutoScanEnabled()
            delay(1000)
        }
    }

    val safeItemsCount = historyList.count { it.status == "safe" }
    val warningItemsCount = historyList.count { it.status == "warning" || it.status == "neutral" }
    val threatItemsCount = historyList.count { it.status == "dangerous" || it.status == "unsafe" }

    CyberScreenWrapper(title = "Security Dashboard") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Premium Header Cyber Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CyberPurple.copy(alpha = 0.2f), NeonBlue.copy(alpha = 0.1f))
                        )
                    )
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SHIELD SYSTEM v1.0",
                            color = NeonBlue,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isServiceActive) NeonGreen.copy(alpha = 0.2f) else CardBorder)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isServiceActive) "LISTENING" else "SECURE",
                                color = if (isServiceActive) NeonGreen else TextSteel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Notification Guard",
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Memantau notifikasi real-time untuk mendeteksi link phising dan berita hoaks secara otomatis.",
                        color = TextSteel,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Permission Indicator
                    if (!isPermissionGranted) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonRed.copy(alpha = 0.1f))
                                .border(1.dp, NeonRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable {
                                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, null, tint = NeonRed, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Izin Akses Notifikasi Diperlukan", color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Klik di sini untuk mengizinkan di pengaturan", color = NeonRed.copy(alpha = 0.8f), fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // Notification Service Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBg.copy(alpha = 0.6f))
                            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable {
                                val newState = !isAutoScanEnabled
                                repository.setAutoScanEnabled(newState)
                                isAutoScanEnabled = newState
                                Toast.makeText(context, if (newState) "Auto Scan Aktif" else "Auto Scan Dinonaktifkan", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAutoScanEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = "Shield",
                                tint = if (isAutoScanEnabled) NeonGreen else NeonRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Auto Notification Scan",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = isAutoScanEnabled,
                            onCheckedChange = { active ->
                                repository.setAutoScanEnabled(active)
                                isAutoScanEnabled = active
                                Toast.makeText(context, if (active) "Auto Scan Aktif" else "Auto Scan Dinonaktifkan", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonGreen,
                                uncheckedThumbColor = TextSteel,
                                uncheckedTrackColor = ObsidianBg
                            ),
                            modifier = Modifier.scaleLayout(0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bento Grid Stats
            Text(
                text = "STATISTIK PEMINDAIAN",
                color = TextSteel,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Safe bento
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                        Text(text = "AMAN", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "$safeItemsCount", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(text = "item aman", color = TextSteel, fontSize = 11.sp)
                    }
                }
                // Warning bento
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                        Text(text = "WASPADA", color = NeonGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "$warningItemsCount", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(text = "item netral", color = TextSteel, fontSize = 11.sp)
                    }
                }
                // Threat bento
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                        Text(text = "BAHAYA", color = NeonRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "$threatItemsCount", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(text = "ancaman diblok", color = TextSteel, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Portals navigation widgets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Hoax Scanner Portal Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clickable { navController.navigate("hoax") }
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Hoax Checker", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Cek kebenaran narasi, berita & tangkapan layar", color = TextSteel, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                }

                // Cyber Scam Scanner Portal Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clickable { navController.navigate("scam") }
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Scam Scanner", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Pindai tautan mencurigakan & file virus (VirusTotal)", color = TextSteel, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // History Quick List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AKTIVITAS TERBARU",
                    color = TextSteel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Lihat Semua",
                    color = NeonBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { navController.navigate("history") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Belum ada aktivitas keamanan.", color = TextSteel, fontSize = 13.sp)
                }
            } else {
                historyList.take(3).forEach { item ->
                    HistoryRowItem(item = item, onClick = {
                        navController.navigate("history")
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Hoax Checker Screen Layout
 */
@Composable
fun HoaxScreen(
    repository: CheckerRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var queryText by remember { mutableStateOf("") }
    var selectedEngine by remember { mutableStateOf("gemini") } // "gemini" or "deepseek"
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    
    var isScanning by remember { mutableStateOf(false) }
    var scanLogs = remember { mutableStateListOf<String>() }
    var activeResult by remember { mutableStateOf<HoaxResponse?>(null) }
    var showShareDialog by remember { mutableStateOf<HoaxResponse?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it.toString()
            // Proses OCR
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val image = InputImage.fromFilePath(context, it)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (visionText.text.isNotBlank()) {
                            queryText = visionText.text
                            Toast.makeText(context, "Teks Berhasil Diekstrak dari Gambar!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Tidak ada teks yang terdeteksi di gambar.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Gagal OCR: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memproses gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val ocrLogPool = listOf(
        "Mengekstrak file gambar...",
        "Menginisialisasi engine OCR cerdas...",
        "Menganalisis teks tangkapan layar...",
        "Teks berhasil diekstrak!",
        "Mengirim data teks ke BFF gateway...",
        "Menghubungkan ke database Google Fact Check...",
        "Menghubungkan ke multi-LLM engine untuk analisis narasi..."
    )

    val claimLogPool = listOf(
        "Mengambil teks klaim...",
        "Menghubungkan ke database Google Fact Check global...",
        "Memindai kecocokan klaim artikel...",
        "Menghubungkan ke model AI context analyzer...",
        "Menganalisis bias, falasi logika, dan anomali sintaksis...",
        "Selesai! Menyusun laporan keamanan..."
    )

    CyberScreenWrapper(title = "Hoax Checker") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (!isScanning && activeResult == null) {
                // Core Input Area
                Text(
                    text = "MASUKKAN NARASI BERITA / DOKUMEN",
                    color = TextSteel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // High tech Text Input Box
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = {
                        Text(
                            text = "Tempel berita, rumor WhatsApp, atau pernyataan di sini untuk dianalisis...",
                            color = TextSteel.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardCarbon),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = NeonBlue,
                        unfocusedLabelColor = TextSteel
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Image Upload Simulated Portal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardCarbon)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                imagePickerLauncher.launch("image/*")
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedImageUri != null) "Gambar Terunggah" else "Unggah Foto / Screenshot",
                                color = if (selectedImageUri != null) NeonGreen else TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (selectedImageUri != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonRed.copy(alpha = 0.1f))
                                .border(1.dp, NeonRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedImageUri = null
                                    queryText = ""
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = NeonRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // LLM Engine Selector Options
                Text(
                    text = "PILIH ENGINE KONTEKS AI (MULTI-LLM)",
                    color = TextSteel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Gemini Selector
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedEngine == "gemini") CyberPurple.copy(alpha = 0.2f) else CardCarbon)
                            .border(
                                1.dp, 
                                if (selectedEngine == "gemini") CyberPurple else CardBorder, 
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedEngine = "gemini" }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedEngine == "gemini",
                            onClick = { selectedEngine = "gemini" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = CyberPurple,
                                unselectedColor = TextSteel
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(text = "Gemini 2.5 Flash", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Analisis Cepat & Global", color = TextSteel, fontSize = 10.sp)
                        }
                    }

                    // DeepSeek Selector
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedEngine == "deepseek") NeonBlue.copy(alpha = 0.2f) else CardCarbon)
                            .border(
                                1.dp, 
                                if (selectedEngine == "deepseek") NeonBlue else CardBorder, 
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedEngine = "deepseek" }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedEngine == "deepseek",
                            onClick = { selectedEngine = "deepseek" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = NeonBlue,
                                unselectedColor = TextSteel
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(text = "DeepSeek V4 Flash", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Analisis Kritis & Konteks", color = TextSteel, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Scan trigger button
                Button(
                    onClick = {
                        if (queryText.trim().isEmpty()) {
                            return@Button
                        }
                        isScanning = true
                        scanLogs.clear()
                        
                        // Type logs simulator
                        scope.launch {
                            val activePool = if (selectedImageUri != null) ocrLogPool else claimLogPool
                            activePool.forEach { log ->
                                delay((300..600).random().toLong())
                                scanLogs.add(log)
                            }
                            
                            // Perform check
                            val res = repository.checkHoax(queryText, selectedImageUri, selectedEngine)
                            res.onSuccess {
                                activeResult = it
                                    isScanning = false
                            }
                            res.onFailure {
                                isScanning = false
                                Toast.makeText(context, "Gagal melakukan pemindaian: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = ObsidianBg)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PERIKSA KEBENARAN SEKARANG", 
                        color = ObsidianBg, 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // scanning state views
            if (isScanning) {
                Spacer(modifier = Modifier.height(20.dp))
                RadarSweepScanner(
                    scannerColor = NeonBlue,
                    label = if (selectedImageUri != null) "PEMINDAIAN OCR & FAKTA..." else "MENGANALISIS NARASI KONTEKS..."
                )
                Spacer(modifier = Modifier.height(24.dp))
                CyberpunkLogs(logs = scanLogs, terminalColor = NeonBlue)
            }

            // Results views
            if (!isScanning && activeResult != null) {
                val result = activeResult!!
                val isVerdictSafe = result.trustScore >= 75
                val isVerdictWarning = result.trustScore in 40..74
                val verdictAccentColor = when {
                    isVerdictSafe -> NeonGreen
                    isVerdictWarning -> NeonGold
                    else -> NeonRed
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAPORAN KEAMANAN FAKTA",
                        color = TextSteel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Reset Button
                    Text(
                        text = "Scan Ulang",
                        color = NeonBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            activeResult = null
                            queryText = ""
                            selectedImageUri = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Trust score dial indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GaugeChart(
                            percentage = result.trustScore / 100f,
                            gaugeColor = verdictAccentColor,
                            label = "Trust Score"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result.verdictSummary,
                            color = verdictAccentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breakdown explanation card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(text = "Rangkuman Analisis", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = result.explanation, color = TextSteel, fontSize = 13.sp, lineHeight = 18.sp)
                        
                        if (result.fallaciesDetected.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Terdeteksi:", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                result.fallaciesDetected.forEach { fallacy ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CardBorder)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = fallacy, color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // AI Insights Card (Multi-LLM Context Narrative)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(CyberPurple, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Konteks AI: ${result.aiInsights.engineUsed}", 
                                color = TextWhite, 
                                fontSize = 14.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Latar Belakang Narasi:", 
                            color = MutedText, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = result.aiInsights.contextNarrative, 
                            color = TextSteel, 
                            fontSize = 13.sp, 
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )
                        
                        Text(
                            text = "Analisis Kredibilitas:", 
                            color = MutedText, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = result.aiInsights.credibilityAnalysis, 
                            color = TextSteel, 
                            fontSize = 13.sp, 
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        Text(
                            text = "Saran Tindak Lanjut:", 
                            color = MutedText, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = result.aiInsights.recommendations, 
                            color = TextSteel, 
                            fontSize = 13.sp, 
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Google Fact Check Matches Section (Hanya tampilkan jika ada hasil verifikasi nyata)
                val validFactChecks = result.googleFactChecks.filter { 
                    it.verdict != "Rujukan Web" && it.verdict != "Belum terverifikasi" 
                }

                if (validFactChecks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "VERIFIKASI FAKTA GLOBAL",
                        color = TextSteel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    validFactChecks.forEach { check ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardCarbon)
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (check.url.isNotEmpty()) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(check.url))
                                        context.startActivity(intent)
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = check.publisher, 
                                        color = NeonBlue, 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeonRed.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = check.verdict, color = NeonRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Klaim: \"${check.claim}\"", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Oleh: ${check.claimant}", color = TextSteel, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom buttons: Share & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showShareDialog = result },
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                        border = BorderStroke(1.dp, NeonGreen)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "BAGIKAN HASIL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            activeResult = null
                            queryText = ""
                            selectedImageUri = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardBorder)
                    ) {
                        Text(text = "KEMBALI", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Share Card popup dialog
    if (showShareDialog != null) {
        ShareCardDialog(
            title = showShareDialog!!.query.take(30),
            type = "hoax",
            score = showShareDialog!!.trustScore,
            verdict = showShareDialog!!.verdictSummary,
            advice = showShareDialog!!.aiInsights.recommendations,
            onDismiss = { showShareDialog = null }
        )
    }
}

/**
 * Scam Scanner Screen Layout
 */
@Composable
fun ScamScreen(
    repository: CheckerRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var activeTab by remember { mutableIntStateOf(0) } // 0 = URL, 1 = FILE
    var targetUrl by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableStateOf("") }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Get file info
                val cursor = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
                    if (c.moveToFirst()) {
                        selectedFileName = c.getString(nameIndex)
                        val size = c.getLong(sizeIndex)
                        selectedFileSize = if (size > 1024 * 1024) {
                            "${String.format("%.1f", size / (1024f * 1024f))} MB"
                        } else {
                            "${size / 1024} KB"
                        }
                    }
                }

                // Create a temporary file to pass to repository
                val inputStream = context.contentResolver.openInputStream(it)
                val tempFile = File(context.cacheDir, selectedFileName)
                tempFile.outputStream().use { output ->
                    inputStream?.copyTo(output)
                }
                selectedFile = tempFile
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    var isScanning by remember { mutableStateOf(false) }
    var scanLogs = remember { mutableStateListOf<String>() }
    var activeResult by remember { mutableStateOf<ScamResponse?>(null) }
    var showShareDialog by remember { mutableStateOf<ScamResponse?>(null) }

    val urlLogPool = listOf(
        "Menginisialisasi engine scanner URL...",
        "Melacak DNS & alamat IP host...",
        "Memeriksa sertifikat keamanan SSL...",
        "Query VirusTotal API Gateway...",
        "Mengumpulkan feedback mesin deteksi anti-phising...",
        "Menghitung tingkat kerentanan siber...",
        "Selesai! Menampilkan data pertahanan..."
    )

    val fileLogPool = listOf(
        "Membaca representasi biner file...",
        "Mengekstrak ukuran & hash tanda tangan digital (SHA-256)...",
        "Menghubungkan ke API VirusTotal...",
        "Membandingkan signature dengan database virus global...",
        "Memindai backdoor, trojan, dan spyware payload...",
        "Menghitung rasio deteksi ancaman...",
        "Selesai! Membuat saran mitigasi siber..."
    )

    CyberScreenWrapper(title = "Scam Scanner") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (!isScanning && activeResult == null) {
                // Tab Header Selection
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = ObsidianBg,
                    contentColor = NeonRed,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = NeonRed
                        )
                    },
                    divider = { HorizontalDivider(color = CardBorder) }
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                text = "URL SCANNER", 
                                color = if (activeTab == 0) TextWhite else TextSteel, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Text(
                                text = "FILE SCANNER", 
                                color = if (activeTab == 1) TextWhite else TextSteel, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Jika tab URL aktif, tampilkan input URL, jika tab File aktif, tampilkan upload file area
                if (activeTab == 0) {
                    // URL Scanner Input Area
                    Text(
                        text = "ALAMAT URL TAUTAN",
                        color = TextSteel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = targetUrl,
                        onValueChange = { targetUrl = it },
                        placeholder = { Text(text = "https://bit.ly/undian-gratis", color = TextSteel.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardCarbon),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = NeonRed,
                            unfocusedBorderColor = CardBorder
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Scan button
                    Button(
                        onClick = {
                            if (targetUrl.trim().isEmpty()) return@Button
                            isScanning = true
                            scanLogs.clear()
                            scope.launch {
                                urlLogPool.forEach { log ->
                                    delay((250..550).random().toLong())
                                    scanLogs.add(log)
                                }
                                val finalUrl = if (!targetUrl.contains("://")) "https://$targetUrl" else targetUrl
                                val res = repository.scanUrl(finalUrl)
                                res.onSuccess {
                                    activeResult = it
                                    isScanning = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = ObsidianBg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PINDAI LINK PHISING", 
                            color = ObsidianBg, 
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    // File Scanner Upload Area
                    Text(
                        text = "UNGGAH DOKUMEN / FILE APK",
                        color = TextSteel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardCarbon)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (selectedFile != null) NeonGreen else CardBorder
                                ), RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                filePickerLauncher.launch("*/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (selectedFile != null) Icons.Default.FilePresent else Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = if (selectedFile != null) NeonGreen else TextSteel,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (selectedFile != null) selectedFileName else "Sentuh Untuk Memilih File",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (selectedFile != null) "Ukuran: $selectedFileSize" else "Format: PDF, APK, EXE, DOCX",
                                color = TextSteel,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (selectedFile != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        // Clear selected file option
                        OutlinedButton(
                            onClick = { selectedFile = null },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                            border = BorderStroke(1.dp, NeonRed)
                        ) {
                            Text(text = "Hapus File Terpilih", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Scan file button
                    Button(
                        onClick = {
                            if (selectedFile == null) return@Button
                            isScanning = true
                            scanLogs.clear()
                            scope.launch {
                                fileLogPool.forEach { log ->
                                    delay((250..550).random().toLong())
                                    scanLogs.add(log)
                                }
                                val res = repository.scanFile(selectedFile!!)
                                res.onSuccess {
                                    activeResult = it
                                    isScanning = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonRed,
                            disabledContainerColor = CardBorder
                        ),
                        enabled = selectedFile != null
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = ObsidianBg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PINDAI PAYLOAD FILE", 
                            color = ObsidianBg, 
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Scanning state views
            if (isScanning) {
                Spacer(modifier = Modifier.height(20.dp))
                RadarSweepScanner(
                    scannerColor = NeonRed,
                    label = if (activeTab == 0) "MEMINDAI ALAMAT LINK..." else "MEMINDAI KODE TANDA TANGAN FILE..."
                )
                Spacer(modifier = Modifier.height(24.dp))
                CyberpunkLogs(logs = scanLogs, terminalColor = NeonRed)
            }

            // Results views
            if (!isScanning && activeResult != null) {
                val result = activeResult!!
                val isVerdictDangerous = result.dangerScore >= 50
                val isVerdictWarning = result.dangerScore in 1..49
                val verdictAccentColor = when {
                    isVerdictDangerous -> NeonRed
                    isVerdictWarning -> NeonGold
                    else -> NeonGreen
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAPORAN ANCAMAN SIBER",
                        color = TextSteel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Reset Button
                    Text(
                        text = "Scan Ulang",
                        color = NeonBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            activeResult = null
                            targetUrl = ""
                            selectedFile = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Danger Score Dial Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GaugeChart(
                            percentage = result.dangerScore / 100f,
                            gaugeColor = verdictAccentColor,
                            label = "Danger Ratio"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val statusLabel = when {
                            isVerdictDangerous -> "🚨 TERDETEKSI BERBAHAYA (HIGH RISK)"
                            isVerdictWarning -> "⚠️ WASPADA / MENCURIGAKAN (MEDIUM RISK)"
                            else -> "✅ BERSIH / BEBAS ANCAMAN (SAFE)"
                        }
                        
                        Text(
                            text = statusLabel,
                            color = verdictAccentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detail Metrics Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(text = "Rangkuman Teknis", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Sumber Target:", color = TextSteel, fontSize = 12.sp)
                            Text(text = result.target, color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(180.dp), textAlign = TextAlign.End)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Rasio Deteksi:", color = TextSteel, fontSize = 12.sp)
                            Text(text = "${result.flaggedEngineCount} / ${result.totalEngines} antivirus", color = verdictAccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        if (result.ipAddress != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "IP Address Host:", color = TextSteel, fontSize = 12.sp)
                                Text(text = result.ipAddress, color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (result.hostCountry != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Negara Server:", color = TextSteel, fontSize = 12.sp)
                                Text(text = result.hostCountry, color = TextWhite, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Kredibilitas Domain:", color = TextSteel, fontSize = 12.sp)
                            Text(text = "${result.reputationPoints} / 100 poin", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 12.dp))
                        Text(text = "Mitigasi Keamanan:", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = result.safetyAdvice, color = TextSteel, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Engine List Detailed detections
                Text(
                    text = "LAPORAN MESIN DETEKSI",
                    color = TextSteel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val detectedEngines = result.detections.filter {
                            it.result != "clean" && it.result != "undetected"
                        }.sortedByDescending {
                            when (it.result) {
                                "phishing", "malware" -> 3
                                "suspicious" -> 2
                                else -> 1
                            }
                        }

                        val cleanEngines = result.detections.filter {
                            it.result == "clean" || it.result == "undetected"
                        }

                        val enginesToShow = if (detectedEngines.isNotEmpty()) detectedEngines else cleanEngines

                        // Label header
                        if (detectedEngines.isNotEmpty()) {
                            Text(
                                text = "🚨 ${detectedEngines.size} MESIN MENDETEKSI ANCAMAN",
                                color = NeonRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        } else {
                            Text(
                                text = "SEMUA MESIN MELAPORKAN BERSIH",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        enginesToShow.forEach { engine ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = engine.engine, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    if (engine.category.isNotEmpty() && engine.category != "harmless" && engine.category != "undetected") {
                                        Text(text = engine.category, color = TextSteel, fontSize = 10.sp)
                                    }
                                }
                                val badgeColor = when (engine.result) {
                                    "clean" -> NeonGreen
                                    "phishing", "malware" -> NeonRed
                                    "suspicious" -> NeonGold
                                    else -> NeonGold
                                }
                                Text(
                                    text = engine.result.uppercase(),
                                    color = badgeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom buttons: Share & Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showShareDialog = result },
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                        border = BorderStroke(1.dp, NeonGreen)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "BAGIKAN HASIL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            activeResult = null
                            targetUrl = ""
                            selectedFile = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardBorder)
                    ) {
                        Text(text = "KEMBALI", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Share Card popup dialog
    if (showShareDialog != null) {
        ShareCardDialog(
            title = showShareDialog!!.target.take(30),
            type = "scam",
            score = showShareDialog!!.dangerScore,
            verdict = if (showShareDialog!!.dangerScore > 35) "BAHAYA ANCAMAN SIBER DETECTED!" else "AMAN / BERSIH",
            advice = showShareDialog!!.safetyAdvice,
            onDismiss = { showShareDialog = null }
        )
    }
}

/**
 * Search History Screen Layout
 */
@Composable
fun HistoryScreen(
    repository: CheckerRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val historyList by repository.historyList.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = historyList.filter {
        it.title.lowercase().contains(searchQuery.lowercase())
    }

    var selectedHistoryItem by remember { mutableStateOf<HistoryItem?>(null) }

    CyberScreenWrapper(title = "Riwayat Pemindaian") {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(text = "Cari di riwayat...", color = TextSteel.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardCarbon),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = CardBorder
                ),
                singleLine = true,
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSteel) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOG AKTIVITAS (${filteredList.size})",
                    color = TextSteel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                // Clear button
                if (historyList.isNotEmpty()) {
                    Text(
                        text = "Bersihkan Semua",
                        color = NeonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            scope.launch { repository.clearHistory() }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = TextSteel, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Belum ada log keamanan.", color = TextSteel, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { item ->
                        HistoryRowItem(item = item, onClick = {
                            selectedHistoryItem = item
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
    
    if (selectedHistoryItem != null) {
        HistoryDetailDialog(item = selectedHistoryItem!!, onDismiss = { selectedHistoryItem = null })
    }
}

/**
 * Notification List Screen
 */
@Composable
fun NotificationLogsScreen(
    repository: CheckerRepository
) {
    val scope = rememberCoroutineScope()
    val logs by repository.notificationLogs.collectAsState()
    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }

    CyberScreenWrapper(title = "Daftar Notifikasi") {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MONITORING REAL-TIME",
                    color = TextSteel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (logs.isNotEmpty()) {
                    Text(
                        text = "Hapus Log",
                        color = NeonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            scope.launch { repository.clearNotificationLogs() }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (logs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "Belum ada notifikasi yang ditangkap.", color = TextSteel, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(logs) { item ->
                        NotificationRowItem(item = item, onClick = { selectedItem = item })
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (selectedItem != null) {
        HistoryDetailDialog(item = selectedItem!!, onDismiss = { selectedItem = null })
    }
}

@Composable
fun NotificationRowItem(item: HistoryItem, onClick: () -> Unit) {
    val statusColor = when (item.status) {
        "completed" -> {
            // Check if it's likely a hoax or scam result
            val isHoax = item.score >= 70 // Simple heuristic
            if (isHoax) NeonGreen else if (item.score > 40) NeonGold else NeonRed
        }
        "analyzing" -> NeonBlue
        "no_scan" -> TextSteel
        "failed" -> NeonRed
        else -> TextSteel
    }

    val time = try { item.timestamp.substring(11, 16) } catch (e: Exception) { "--:--" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardCarbon)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = item.appName ?: "Unknown App", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = time, color = TextSteel, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.originalContent ?: "", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when(item.status) {
                        "analyzing" -> "Sedang Dianalisis..."
                        "completed" -> "Selesai Dianalisis (${item.score}%)"
                        "no_scan" -> "Tidak Perlu Dipindai"
                        "failed" -> "Gagal Dianalisis"
                        else -> item.status.uppercase()
                    },
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
@Composable
fun HistoryRowItem(
    item: HistoryItem,
    onClick: () -> Unit
) {
    val isHoax = item.type == "hoax"
    val statusColor = when (item.status) {
        "safe" -> NeonGreen
        "warning", "neutral" -> NeonGold
        else -> NeonRed
    }

    val typeIcon = if (isHoax) Icons.Default.FactCheck else Icons.Default.Security

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardCarbon)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = typeIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.title,
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val timeAndDate = try {
                        item.timestamp.substring(11, 16) + " • " + item.timestamp.substring(0, 10)
                    } catch (e: Exception) {
                        item.timestamp
                    }
                    Text(
                        text = timeAndDate,
                        color = TextSteel,
                        fontSize = 11.sp
                    )
                }
            }
            
            // Score rating badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.score}%",
                    color = statusColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = if (isHoax) "Trust" else "Risk",
                    color = TextSteel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Detailed view for history items
 */
@Composable
fun HistoryDetailDialog(
    item: HistoryItem,
    onDismiss: () -> Unit
) {
    val gson = remember { Gson() }
    
    val hoaxRes = remember(item) {
        if (item.type == "hoax" || (item.type == "notification" && item.status == "completed")) {
            try {
                val json = gson.toJson(item.resultDetails)
                // Cek apakah ada field khas HoaxResponse
                if (json.contains("verdictSummary") || json.contains("trustScore")) {
                    gson.fromJson(json, HoaxResponse::class.java)
                } else null
            } catch (e: Exception) { null }
        } else null
    }
    
    val scamRes = remember(item) {
        if (item.type == "scam" || (item.type == "notification" && item.status == "completed")) {
            try {
                val json = gson.toJson(item.resultDetails)
                // Cek apakah ada field khas ScamResponse
                if (json.contains("dangerScore") || json.contains("totalEngines")) {
                    gson.fromJson(json, ScamResponse::class.java)
                } else null
            } catch (e: Exception) { null }
        } else null
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianBg)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DETAIL PEMINDAIAN",
                        color = TextSteel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TextSteel)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = item.title,
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cek ${item.type.uppercase()} • ${item.timestamp}",
                    color = TextSteel,
                    fontSize = 11.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val statusColor = when (item.status) {
                    "safe" -> NeonGreen
                    "completed" -> {
                        if (scamRes != null) {
                            if (scamRes.dangerScore > 50) NeonRed else if (scamRes.dangerScore > 0) NeonGold else NeonGreen
                        } else if (hoaxRes != null) {
                            if (hoaxRes.trustScore >= 75) NeonGreen else if (hoaxRes.trustScore >= 40) NeonGold else NeonRed
                        } else NeonGreen
                    }
                    "warning", "neutral" -> NeonGold
                    "analyzing" -> NeonBlue
                    "no_scan" -> TextSteel
                    else -> NeonRed
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardCarbon)
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (item.status == "no_scan") {
                            Text(text = "TIDAK DIPINDAI", color = statusColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        } else if (item.status == "analyzing") {
                            CircularProgressIndicator(color = NeonBlue, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "${item.score}%",
                                color = statusColor,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = if (item.type == "hoax") "TRUST SCORE" else if (item.type == "scam") "DANGER SCORE" else "HASIL ANALISIS",
                            color = TextSteel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (item.type == "hoax" || (item.type == "notification" && hoaxRes != null)) {
                    if (hoaxRes != null) {
                        Text(text = "Rangkuman Analisis", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = hoaxRes.explanation, color = TextSteel, fontSize = 13.sp, lineHeight = 18.sp)
                        
                        if (hoaxRes.correctedFact.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Fakta Sebenarnya", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = hoaxRes.correctedFact, color = TextWhite, fontSize = 13.sp, lineHeight = 18.sp)
                        }

                        if (hoaxRes.googleFactChecks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Hasil Google Fact Check", color = NeonBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            hoaxRes.googleFactChecks.take(2).forEach { check ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CardCarbon)
                                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(text = check.publisher, color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(text = check.verdict, color = NeonRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(text = check.claim, color = TextWhite, fontSize = 12.sp, maxLines = 2)
                                    }
                                }
                            }
                        }
                    } else if (item.status == "no_scan") {
                        Text(text = "Alasan:", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = item.resultDetails as? String ?: "Tidak mengandung berita atau tautan.", color = TextSteel, fontSize = 13.sp)
                    } else {
                        Text(text = "Gagal memuat detail mendalam.", color = NeonRed, fontSize = 12.sp)
                    }
                } else if (item.type == "scam" || (item.type == "notification" && scamRes != null)) {
                    if (scamRes != null) {
                        Text(text = "Mitigasi Keamanan", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = scamRes.safetyAdvice, color = TextSteel, fontSize = 13.sp, lineHeight = 18.sp)
                        
                        if (scamRes.ipAddress != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Informasi Teknis", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "IP: ${scamRes.ipAddress}", color = TextSteel, fontSize = 12.sp)
                            Text(text = "Server: ${scamRes.hostCountry}", color = TextSteel, fontSize = 12.sp)
                        }
                    } else {
                        Text(text = "Gagal memuat detail mendalam.", color = NeonRed, fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CardBorder)
                ) {
                    Text(text = "TUTUP")
                }
            }
        }
    }
}

/**
 * High-End Cyber-Security Social Sharing Card Dialog (Tampilan Premium)
 */
@Composable
fun ShareCardDialog(
    title: String,
    type: String,
    score: Int,
    verdict: String,
    advice: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isHoax = type == "hoax"
    
    val statusColor = if (isHoax) {
        if (score >= 75) NeonGreen else if (score >= 40) NeonGold else NeonRed
    } else {
        if (score >= 75) NeonRed else if (score >= 30) NeonGold else NeonGreen
    }

    val verdictBadge = if (isHoax) {
        if (score >= 75) "AMAN / KREDIBEL ✅" else if (score >= 40) "WASPADA KELIRU ⚠️" else "HOAKS TERKONFIRMASI 🚨"
    } else {
        if (score >= 75) "BAHAYA SIBER (RISK) 🚨" else if (score >= 30) "MENCURIGAKAN ⚠️" else "BERSIH / AMAN ✅"
    }

    val shareText = """
        [SHIELD SECURITY REPORT]
        
        Klaim: "$title"
        Verdict: $verdictBadge
        Score: $score% (${if (isHoax) "Akurasi" else "Risiko"})
        
        Analisis Mitigasi:
        $advice
        
        -- Dikirim via Cyber Threat Shield App --
    """.trimIndent()

    Dialog(onDismissRequest = { onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianBg)
                .border(2.dp, statusColor, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Header
                Text(
                    text = "PREVIEW KARTU SHARE",
                    color = TextSteel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                // Replicating Glowing Sharing Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardCarbon)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "CYBER GUARD REPORT", color = NeonBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(text = "2026", color = TextSteel, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Score Gauge
                        Text(
                            text = "$score%",
                            color = statusColor,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = if (isHoax) "TRUST SCORE" else "DANGER SCORE",
                            color = TextSteel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = verdictBadge,
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "\"$title\"",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Trigger actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy text
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(shareText))
                            Toast.makeText(context, "Laporan Keamanan Disalin!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Text(text = "SALIN TEKS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Direct Share WhatsApp / general intent
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                this.type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Bagikan Laporan Keamanan via"))
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text(text = "KIRIM WA / CHAT", color = ObsidianBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cancel Button
                Text(
                    text = "Batalkan",
                    color = TextSteel,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}