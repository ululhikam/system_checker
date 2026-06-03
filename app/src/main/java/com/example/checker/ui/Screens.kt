package com.example.checker.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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
    val scope = rememberCoroutineScope()
    val historyList by repository.historyList.collectAsState()
    
    var isShieldActive by remember { mutableStateOf(NotificationMonitorService.isServiceRunning) }
    var activeHistoryDetail by remember { mutableStateOf<HistoryItem?>(null) }

    // Refresh data setiap kali user masuk ke screen ini atau aplikasi di-resume
    LaunchedEffect(Unit) {
        while(true) {
            repository.fetchHistory()
            delay(5000) // Refresh otomatis setiap 5 detik saat dashboard terbuka
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
                                .background(if (isShieldActive) NeonGreen.copy(alpha = 0.2f) else CardBorder)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isShieldActive) "ACTIVE" else "SECURE",
                                color = if (isShieldActive) NeonGreen else TextSteel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Anti-Hoax & Cyber Guard",
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Pemantau notifikasi background melindungi ponsel Anda dari penyebaran hoax dan phising secara otomatis.",
                        color = TextSteel,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Notification Service Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBg.copy(alpha = 0.6f))
                            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable {
                                if (isShieldActive) {
                                    NotificationMonitorService.stopService(context)
                                    isShieldActive = false
                                } else {
                                    NotificationMonitorService.startService(context)
                                    isShieldActive = NotificationMonitorService.isNotificationServiceEnabled(context)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isShieldActive) Icons.Default.Shield else Icons.Default.ShieldMoon,
                                contentDescription = "Shield",
                                tint = if (isShieldActive) NeonGreen else NeonRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Auto Notification Scanner",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = isShieldActive,
                            onCheckedChange = { active ->
                                if (active) {
                                    NotificationMonitorService.startService(context)
                                    isShieldActive = NotificationMonitorService.isNotificationServiceEnabled(context)
                                } else {
                                    NotificationMonitorService.stopService(context)
                                    isShieldActive = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonGreen,
                                uncheckedThumbColor = TextSteel,
                                uncheckedTrackColor = ObsidianBg
                            ),
                            modifier = Modifier.scale(0.8f)
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
                        Text(text = "AMAN ✅", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        Text(text = "WASPADA ⚠️", color = NeonGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        Text(text = "BAHAYA 🚨", color = NeonRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        activeHistoryDetail = item
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (activeHistoryDetail != null) {
        var dialogTitle by remember { mutableStateOf("") }
        var dialogType by remember { mutableStateOf("") }
        var dialogScore by remember { mutableStateOf(0) }
        var dialogVerdict by remember { mutableStateOf("") }
        var dialogAdvice by remember { mutableStateOf("") }
        var dialogStatus by remember { mutableStateOf("") }

        showHistoryDetailDialog(activeHistoryDetail!!) { title, type, score, verdict, advice, status ->
            dialogTitle = title
            dialogType = type
            dialogScore = score
            dialogVerdict = verdict
            dialogAdvice = advice
            dialogStatus = status
        }

        if (dialogTitle.isNotEmpty()) {
            ShareCardDialog(
                title = dialogTitle.take(30),
                type = dialogType,
                score = dialogScore,
                verdict = dialogVerdict,
                advice = dialogAdvice,
                status = dialogStatus,
                onDismiss = { activeHistoryDetail = null }
            )
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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri.toString()
            Toast.makeText(context, "Mengekstrak teks dari gambar...", Toast.LENGTH_SHORT).show()
            try {
                val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                    com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
                )
                val image = com.google.mlkit.vision.common.InputImage.fromFilePath(context, uri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val extractedText = visionText.text
                        if (extractedText.isNotEmpty()) {
                            queryText = extractedText
                            Toast.makeText(context, "Teks berhasil diekstrak!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Gambar terbaca, tapi tidak ditemukan teks di dalamnya.", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Gagal memproses gambar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Error OCR: ${e.message}", Toast.LENGTH_LONG).show()
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
                                photoPickerLauncher.launch("image/*")
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
                            Text(text = "DeepSeek V3", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

                // Corrected Fact (Fakta Sebenarnya) Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonGreen.copy(alpha = 0.05f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FAKTA SEBENARNYA",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result.correctedFact,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp
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
                            Text(text = "Cacat Logika Terdeteksi:", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

                // Google Fact Check Matches Section
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "VERIFIKASI FAKTA GLOBAL",
                    color = TextSteel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (result.googleFactChecks.isNotEmpty()) {
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    result.googleFactChecks.forEach { check ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardCarbon)
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (check.url.isNotEmpty()) {
                                        try {
                                            uriHandler.openUri(check.url)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Gagal membuka link: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
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
                                Text(text = "Oleh: ${check.claimant}", color = TextSteel, fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardCarbon)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = NeonBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Google Fact Check Database",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tidak ditemukan artikel sanggahan instan di database global Google untuk kalimat ini. Analisis kebenaran sepenuhnya dikerjakan secara komprehensif oleh Context Engine AI (Gemini/DeepSeek).",
                                color = TextSteel,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
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
            status = showShareDialog!!.status,
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
    
    var activeTab by remember { mutableStateOf(0) } // 0 = URL, 1 = FILE
    var targetUrl by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    
    var isScanning by remember { mutableStateOf(false) }
    var scanLogs = remember { mutableStateListOf<String>() }
    var activeResult by remember { mutableStateOf<ScamResponse?>(null) }
    var showShareDialog by remember { mutableStateOf<ScamResponse?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "file_pindai"
                val contentResolver = context.contentResolver
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
                
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val tempFile = File(context.cacheDir, fileName)
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    selectedFile = tempFile
                    Toast.makeText(context, "File $fileName berhasil dimuat!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal membaca file terpilih.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                                val res = repository.scanUrl(targetUrl)
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
                                text = if (selectedFile != null) selectedFile!!.name else "Sentuh Untuk Memilih File",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val fileSizeText = if (selectedFile != null) {
                                val lengthInKb = selectedFile!!.length() / 1024.0
                                if (lengthInKb > 1024.0) {
                                    String.format("Ukuran: %.1f MB", lengthInKb / 1024.0)
                                } else {
                                    String.format("Ukuran: %.1f KB", lengthInKb)
                                }
                            } else {
                                "Format: PDF, APK, EXE, DOCX"
                            }
                            Text(
                                text = fileSizeText,
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
                val isVerdictDangerous = result.threatLevel == "dangerous"
                val isVerdictWarning = result.threatLevel == "warning"
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
                            Text(text = "${result.maliciousCount} / ${result.totalEngines} antivirus", color = verdictAccentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        result.detections.forEach { engine ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = engine.engine, color = TextWhite, fontSize = 13.sp)
                                val badgeColor = when (engine.result) {
                                    "clean" -> NeonGreen
                                    "phishing", "malware" -> NeonRed
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
            verdict = when (showShareDialog!!.threatLevel) {
                "dangerous" -> "🚨 TERDETEKSI BERBAHAYA (HIGH RISK)"
                "warning" -> "⚠️ WASPADA / MENCURIGAKAN (MEDIUM RISK)"
                else -> "✅ BERSIH / BEBAS ANCAMAN (SAFE)"
            },
            advice = showShareDialog!!.safetyAdvice,
            status = showShareDialog!!.threatLevel,
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
    var activeHistoryDetail by remember { mutableStateOf<HistoryItem?>(null) }

    LaunchedEffect(Unit) {
        repository.fetchHistory()
    }

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
                            activeHistoryDetail = item
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (activeHistoryDetail != null) {
        var dialogTitle by remember { mutableStateOf("") }
        var dialogType by remember { mutableStateOf("") }
        var dialogScore by remember { mutableStateOf(0) }
        var dialogVerdict by remember { mutableStateOf("") }
        var dialogAdvice by remember { mutableStateOf("") }
        var dialogStatus by remember { mutableStateOf("") }

        showHistoryDetailDialog(activeHistoryDetail!!) { title, type, score, verdict, advice, status ->
            dialogTitle = title
            dialogType = type
            dialogScore = score
            dialogVerdict = verdict
            dialogAdvice = advice
            dialogStatus = status
        }

        if (dialogTitle.isNotEmpty()) {
            ShareCardDialog(
                title = dialogTitle.take(30),
                type = dialogType,
                score = dialogScore,
                verdict = dialogVerdict,
                advice = dialogAdvice,
                status = dialogStatus,
                onDismiss = { activeHistoryDetail = null }
            )
        }
    }
}

fun showHistoryDetailDialog(
    item: HistoryItem,
    onShow: (title: String, type: String, score: Int, verdict: String, advice: String, status: String) -> Unit
) {
    val title = item.title
    val type = item.type
    val score = item.score
    val status = item.status
    
    // Default fallback values based on item status
    var advice = if (type == "hoax") {
        when (status) {
            "safe" -> "Berita ini terverifikasi aman dan kredibel. Aman untuk dibagikan."
            "neutral" -> "Waspada! Informasi ini kurang akurat atau membutuhkan klarifikasi lebih lanjut."
            else -> "Jangan sebarkan! Informasi ini terkonfirmasi hoaks. Hapus pesan segera."
        }
    } else {
        when (status) {
            "dangerous" -> "🚨 Bahaya! Tautan atau file ini terdeteksi sebagai ancaman aktif. Jangan dibuka!"
            "warning" -> "⚠️ Waspada! Tautan atau file ini dicurigai oleh beberapa vendor keamanan."
            else -> "✅ Bersih. Tautan atau file ini aman untuk diakses."
        }
    }
    
    var verdict = if (type == "hoax") {
        when (status) {
            "safe" -> "AMAN / KREDIBEL ✅"
            "neutral" -> "WASPADA KELIRU ⚠️"
            else -> "HOAKS TERKONFIRMASI 🚨"
        }
    } else {
        when (status) {
            "dangerous" -> "🚨 TERDETEKSI BERBAHAYA (HIGH RISK)"
            "warning" -> "⚠️ WASPADA / MENCURIGAKAN (MEDIUM RISK)"
            else -> "✅ BERSIH / BEBAS ANCAMAN (SAFE)"
        }
    }

    // Try to extract from resultDetails if present
    val details = item.resultDetails
    if (details != null) {
        try {
            if (details is Map<*, *>) {
                val adviceVal = details["safetyAdvice"] as? String 
                    ?: (details["aiInsights"] as? Map<*, *>)?.get("recommendations") as? String
                if (adviceVal != null) advice = adviceVal
                
                val verdictVal = details["verdictSummary"] as? String
                if (verdictVal != null) {
                    verdict = verdictVal
                } else if (type == "scam") {
                    val threatLevel = details["threatLevel"] as? String ?: status
                    verdict = when (threatLevel) {
                        "dangerous" -> "🚨 TERDETEKSI BERBAHAYA (HIGH RISK)"
                        "warning" -> "⚠️ WASPADA / MENCURIGAKAN (MEDIUM RISK)"
                        else -> "✅ BERSIH / BEBAS ANCAMAN (SAFE)"
                    }
                }
            } else if (details is HoaxResponse) {
                advice = details.aiInsights.recommendations
                verdict = details.verdictSummary
            } else if (details is ScamResponse) {
                advice = details.safetyAdvice
                verdict = when (details.threatLevel) {
                    "dangerous" -> "🚨 TERDETEKSI BERBAHAYA (HIGH RISK)"
                    "warning" -> "⚠️ WASPADA / MENCURIGAKAN (MEDIUM RISK)"
                    else -> "✅ BERSIH / BEBAS ANCAMAN (SAFE)"
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Screens", "Error parsing resultDetails: ${e.message}")
        }
    }

    onShow(title, type, score, verdict, advice, status)
}

/**
 * Shared History Row Row Item
 */
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
                    Text(
                        text = item.timestamp.substring(11, 16) + " • " + item.timestamp.substring(0, 10),
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
 * High-End Cyber-Security Social Sharing Card Dialog (Tampilan Premium)
 */
@Composable
fun ShareCardDialog(
    title: String,
    type: String,
    score: Int,
    verdict: String,
    advice: String,
    status: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isHoax = type == "hoax"
    
    val statusColor = when (status) {
        "safe" -> NeonGreen
        "warning", "neutral" -> NeonGold
        else -> NeonRed
    }

    val shareText = """
        [SHIELD SECURITY REPORT]
        
        Klaim: "$title"
        Verdict: $verdict
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
                                text = verdict,
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
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
                                setType("text/plain")
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

// Utility extension to scale switch
@Composable
private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(
            (placeable.width * scale).toInt(),
            (placeable.height * scale).toInt()
        ) {
            placeable.placeRelativeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
            }
        }
    }
)
