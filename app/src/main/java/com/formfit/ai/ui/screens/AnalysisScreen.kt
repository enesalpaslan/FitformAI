package com.formfit.ai.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.formfit.ai.analysis.FeedbackType
import com.formfit.ai.analysis.LiveMetrics
import com.formfit.ai.analysis.PoseDetectorHelper
import com.formfit.ai.analysis.PoseOverlay
import com.formfit.ai.analysis.RepFeedback
import com.formfit.ai.analysis.SquatAnalyzer
import com.formfit.ai.analysis.SquatPhase
import com.formfit.ai.data.model.WorkoutResult
import com.formfit.ai.ui.theme.BackgroundDark
import com.formfit.ai.ui.theme.DividerColor
import com.formfit.ai.ui.theme.ErrorRed
import com.formfit.ai.ui.theme.FitFormAITheme
import com.formfit.ai.ui.theme.Primary
import com.formfit.ai.ui.theme.ScoreGradientEnd
import com.formfit.ai.ui.theme.ScoreGradientStart
import com.formfit.ai.ui.theme.SurfaceDark
import com.formfit.ai.ui.theme.TextSecondary
import com.formfit.ai.ui.theme.WarningYellow
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

@Composable
fun AnalysisScreen(
    exerciseId: Int = 1,
    kullaniciID: Int = 1,
    onFinish: (WorkoutResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity

    // ── Ekranı açık tut ──────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ── Kamera izni ──────────────────────────────────────────────────────────
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) permissionDenied = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ── Analiz durumu ─────────────────────────────────────────────────────────
    val squatAnalyzer = remember { SquatAnalyzer() }
    var repCount by remember { mutableIntStateOf(0) }
    var currentScore by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<RepFeedback?>(null) }
    var liveMetrics by remember { mutableStateOf(LiveMetrics()) }
    var poseResult by remember { mutableStateOf<PoseLandmarkerResult?>(null) }
    var currentPhase by remember { mutableStateOf(SquatPhase.STANDING) }

    // ── Duraklat / Süre ───────────────────────────────────────────────────────
    var isPaused by remember { mutableStateOf(false) }
    val isPausedAtomic = remember { AtomicBoolean(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPaused) {
        isPausedAtomic.set(isPaused)
        if (!isPaused) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    // ── Kamera executor ───────────────────────────────────────────────────────
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isFinishing = remember { AtomicBoolean(false) }

    // ── PoseDetector ─────────────────────────────────────────────────────────
    val poseDetector = remember {
        PoseDetectorHelper(
            context = context,
            listener = object : PoseDetectorHelper.OnPoseDetectionResult {
                override fun onResults(
                    result: PoseLandmarkerResult,
                    inputImageWidth: Int,
                    inputImageHeight: Int
                ) {
                    if (isPausedAtomic.get() || isFinishing.get()) return
                    poseResult = result
                    val phase = squatAnalyzer.analyze(result)
                    currentPhase = phase
                    repCount = squatAnalyzer.repCount
                    currentScore = squatAnalyzer.averageScore
                    feedback = squatAnalyzer.lastFeedback
                    liveMetrics = squatAnalyzer.liveMetrics
                }
                override fun onError(error: String) { /* Hata sessizce loglanır */ }
            }
        )
    }

    // ── CameraX bağlama ───────────────────────────────────────────────────────
    val previewView = remember { PreviewView(context) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Tüm kaynakları temizle
    DisposableEffect(Unit) {
        onDispose {
            isFinishing.set(true)
            cameraProviderRef?.unbindAll()
            poseDetector.close()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProvider = suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (cont.isActive) cont.resume(future.get())
            }, ContextCompat.getMainExecutor(context))
        }
        cameraProviderRef = cameraProvider

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(480, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    imageProxy.use { proxy ->
                        if (!isPausedAtomic.get() && !isFinishing.get()) {
                            val originalBitmap = proxy.toBitmap()
                            val rotation = proxy.imageInfo.rotationDegrees
                            val bitmap = if (rotation != 0) {
                                val matrix = Matrix()
                                matrix.postRotate(rotation.toFloat())
                                Bitmap.createBitmap(
                                    originalBitmap, 0, 0,
                                    originalBitmap.width, originalBitmap.height,
                                    matrix, true
                                )
                            } else {
                                originalBitmap
                            }
                            poseDetector.detectLiveStream(bitmap)
                        }
                    }
                }
            }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            preview,
            imageAnalysis
        )
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Kamera önizlemesi
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            // İskelet overlay
            PoseOverlay(
                result = poseResult,
                liveMetrics = liveMetrics,
                modifier = Modifier.fillMaxSize()
            )
        } else if (permissionDenied) {
            PermissionDeniedContent()
        }

        // ── Üst bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveBadge()
            TimerText(elapsedSeconds)
            ScoreCircle(score = currentScore)
        }

        // ── Tekrar sayacı (sol alt) ───────────────────────────────────────────
        RepCounter(
            count = repCount,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 140.dp)
        )

        // ── Geri bildirim kartı (sağ, orta-alt) ──────────────────────────────
        feedback?.let { fb ->
            FeedbackCard(
                feedback = fb,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .width(180.dp)
            )
        }

        // ── Alt bar ───────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // İyileştirme önerisi
            ImprovementCard(feedback = feedback)

            Spacer(modifier = Modifier.height(10.dp))

            // Butonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Duraklat / Devam Et
                AnalysisButton(
                    text = if (isPaused) "Devam Et" else "Duraklat",
                    containerColor = SurfaceDark,
                    textColor = Color.White,
                    modifier = Modifier.weight(1f)
                ) { isPaused = !isPaused }

                // Bitir
                AnalysisButton(
                    text = "Bitir",
                    containerColor = Primary,
                    textColor = Color(0xFF0D1117),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        isFinishing.set(true)
                        isPausedAtomic.set(true)
                        cameraProviderRef?.unbindAll()
                        poseDetector.close()
                        onFinish(
                            WorkoutResult(
                                antrenmanID  = 0,
                                kullaniciID  = kullaniciID,
                                hareketID    = exerciseId,
                                tekrarSayisi = repCount,
                                skorPuani    = currentScore,
                                sure         = elapsedSeconds * 1000L,
                                tarih        = System.currentTimeMillis(),
                                omurgaDurusu = squatAnalyzer.omurgaDurusu,
                                kolPozisyonu = squatAnalyzer.dizAcisi,
                                denge        = squatAnalyzer.denge
                            )
                        )
                    }
                )
            }
        }

        // Duraklatma overlay
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⏸ Duraklatıldı",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ── Alt bileşenler ────────────────────────────────────────────────────────────

@Composable
private fun LiveBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Kırmızı dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ErrorRed)
        )
        Text(
            text = "CANLI",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TimerText(elapsedSeconds: Long) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val formatted = "%02d:%02d".format(minutes, seconds)
    Text(
        text = formatted,
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ScoreCircle(score: Int) {
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 5f, cap = StrokeCap.Round)
            val sweep = score / 100f * 360f

            // Arka halka
            drawArc(
                color = DividerColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                topLeft = Offset(stroke.width / 2, stroke.width / 2),
                size = ComposeSize(size.width - stroke.width, size.height - stroke.width)
            )
            // Dolgu halkası
            drawArc(
                color = ScoreGradientStart,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = stroke,
                topLeft = Offset(stroke.width / 2, stroke.width / 2),
                size = ComposeSize(size.width - stroke.width, size.height - stroke.width)
            )
        }

        Text(
            text = "$score",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RepCounter(count: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 40.sp
        )
        Text(
            text = "Tekrar",
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun FeedbackCard(feedback: RepFeedback, modifier: Modifier = Modifier) {
    val iconAndColor = when (feedback.tur) {
        FeedbackType.SUCCESS -> "✓" to Primary
        FeedbackType.WARNING -> "⚠" to WarningYellow
        FeedbackType.ERROR   -> "✕" to ErrorRed
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark.copy(alpha = 0.88f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = iconAndColor.first,
                color = iconAndColor.second,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = feedback.mesaj,
                color = iconAndColor.second,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = feedback.detay,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun ImprovementCard(feedback: RepFeedback?) {
    val text = when {
        feedback == null -> "Squat yapmaya başla, AI analiz edecek."
        feedback.tur == FeedbackType.SUCCESS -> "Form mükemmel! Aynı tempoda devam et."
        else -> feedback.detay
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark.copy(alpha = 0.80f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "💡", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "İyileştirme Önerisi",
                color = Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = text,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AnalysisButton(
    text: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PermissionDeniedContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📷", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Kamera İzni Gerekli",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pose analizi için kamera iznine\nihtiyaç duyulmaktadır.\nAyarlar > Uygulama İzinleri'nden etkinleştirin.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@ComposePreview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AnalysisScreenPreview() {
    FitFormAITheme {
        // Kamera gerçek cihaz gerektirdiğinden önizleme sadece UI bileşenlerini gösterir
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Üst bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiveBadge()
                TimerText(elapsedSeconds = 97L)
                ScoreCircle(score = 81)
            }

            RepCounter(
                count = 5,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 140.dp)
            )

            FeedbackCard(
                feedback = RepFeedback(
                    mesaj = "Harika Tempo!",
                    detay  = "Bu hızda devam et",
                    tur    = FeedbackType.SUCCESS
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .width(180.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                ImprovementCard(
                    feedback = RepFeedback(
                        "Harika Tempo!", "Bu hızda devam et", FeedbackType.SUCCESS
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnalysisButton(
                        text = "Duraklat",
                        containerColor = SurfaceDark,
                        textColor = Color.White,
                        modifier = Modifier.weight(1f)
                    ) {}
                    AnalysisButton(
                        text = "Bitir",
                        containerColor = Primary,
                        textColor = Color(0xFF0D1117),
                        modifier = Modifier.weight(1f)
                    ) {}
                }
            }
        }
    }
}
