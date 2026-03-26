package com.formfit.ai.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

// İskelet bağlantıları — (landmark A indisi, landmark B indisi)
private val POSE_CONNECTIONS = listOf(
    // Gövde
    PoseDetectorHelper.LEFT_SHOULDER  to PoseDetectorHelper.RIGHT_SHOULDER,
    PoseDetectorHelper.LEFT_SHOULDER  to PoseDetectorHelper.LEFT_HIP,
    PoseDetectorHelper.RIGHT_SHOULDER to PoseDetectorHelper.RIGHT_HIP,
    PoseDetectorHelper.LEFT_HIP       to PoseDetectorHelper.RIGHT_HIP,
    // Sol kol
    PoseDetectorHelper.LEFT_SHOULDER  to PoseDetectorHelper.LEFT_ELBOW,
    PoseDetectorHelper.LEFT_ELBOW     to PoseDetectorHelper.LEFT_WRIST,
    // Sağ kol
    PoseDetectorHelper.RIGHT_SHOULDER to PoseDetectorHelper.RIGHT_ELBOW,
    PoseDetectorHelper.RIGHT_ELBOW    to PoseDetectorHelper.RIGHT_WRIST,
    // Sol bacak
    PoseDetectorHelper.LEFT_HIP       to PoseDetectorHelper.LEFT_KNEE,
    PoseDetectorHelper.LEFT_KNEE      to PoseDetectorHelper.LEFT_ANKLE,
    PoseDetectorHelper.LEFT_ANKLE     to PoseDetectorHelper.LEFT_HEEL,
    PoseDetectorHelper.LEFT_ANKLE     to PoseDetectorHelper.LEFT_FOOT_INDEX,
    // Sağ bacak
    PoseDetectorHelper.RIGHT_HIP      to PoseDetectorHelper.RIGHT_KNEE,
    PoseDetectorHelper.RIGHT_KNEE     to PoseDetectorHelper.RIGHT_ANKLE,
    PoseDetectorHelper.RIGHT_ANKLE    to PoseDetectorHelper.RIGHT_HEEL,
    PoseDetectorHelper.RIGHT_ANKLE    to PoseDetectorHelper.RIGHT_FOOT_INDEX,
)

// Squat için kritik eklem indisleri — hatalı ise kırmızı gösterilir
private val SQUAT_CRITICAL_JOINTS = setOf(
    PoseDetectorHelper.LEFT_HIP,
    PoseDetectorHelper.RIGHT_HIP,
    PoseDetectorHelper.LEFT_KNEE,
    PoseDetectorHelper.RIGHT_KNEE,
    PoseDetectorHelper.LEFT_ANKLE,
    PoseDetectorHelper.RIGHT_ANKLE,
)

private val COLOR_GOOD    = Color(0xFF2DD4A8)  // Turkuaz — doğru pozisyon
private val COLOR_BAD     = Color(0xFFFF6B6B)  // Kırmızı — hatalı pozisyon
private val COLOR_NEUTRAL = Color(0xFF8B949E)  // Gri — üst vücut / aktif değil
private val COLOR_DOT     = Color(0xFFFFFFFF)  // Beyaz — eklem noktası

/**
 * MediaPipe pose landmark sonuçlarını kamera ön izlemesinin üzerine çizen Canvas composable.
 *
 * @param result            PoseLandmarker çıktısı
 * @param liveMetrics       SquatAnalyzer'dan gelen canlı metrikler (renk kararı için)
 * @param isFrontCamera     Ön kamera kullanılıyorsa x ekseni yansıtılır
 * @param modifier          Compose modifier
 */
@Composable
fun PoseOverlay(
    result: PoseLandmarkerResult?,
    liveMetrics: LiveMetrics,
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = true
) {
    Canvas(modifier = modifier) {
        if (result == null || result.landmarks().isEmpty()) return@Canvas

        val canvasW = size.width
        val canvasH = size.height

        val landmarks = result.landmarks()[0]
        if (landmarks.size < 33) return@Canvas

        // Normalize koordinatı (0-1) → canvas koordinatına dönüştür
        fun toOffset(index: Int): Offset {
            val lm = landmarks[index]
            val normX = if (isFrontCamera) 1f - lm.x() else lm.x()
            return Offset(
                x = normX * canvasW,
                y = lm.y() * canvasH
            )
        }

        // Eklem bağlantı rengi: bacak eklemleri → form durumuna göre, üst vücut → nötr
        fun connectionColor(a: Int, b: Int): Color {
            val involvesCritical = a in SQUAT_CRITICAL_JOINTS && b in SQUAT_CRITICAL_JOINTS
            if (!involvesCritical) return COLOR_NEUTRAL

            val isGoodForm = liveMetrics.leftKneeAngle > 0 &&
                             !liveMetrics.kneeOverToe &&
                             liveMetrics.symmetryDiff < 15.0 &&
                             liveMetrics.trunkLean in 30.0..85.0
            return if (isGoodForm) COLOR_GOOD else COLOR_BAD
        }

        // ── Bağlantı çizgileri (kemik) ───────────────────────────────────────
        POSE_CONNECTIONS.forEach { (a, b) ->
            if (a < landmarks.size && b < landmarks.size) {
                val lmA = landmarks[a]
                val lmB = landmarks[b]
                // Görünürlük filtresi: çok düşük güven skoru olan noktaları atla
                val visA = lmA.visibility().orElse(0f)
                val visB = lmB.visibility().orElse(0f)
                if (visA < MIN_VISIBILITY || visB < MIN_VISIBILITY) return@forEach

                drawLine(
                    color       = connectionColor(a, b),
                    start       = toOffset(a),
                    end         = toOffset(b),
                    strokeWidth = LINE_WIDTH,
                    cap         = StrokeCap.Round
                )
            }
        }

        // ── Eklem noktaları (dot) ─────────────────────────────────────────────
        landmarks.forEachIndexed { index, lm ->
            val visibility = lm.visibility().orElse(0f)
            if (visibility < MIN_VISIBILITY) return@forEachIndexed

            val dotColor = when (index) {
                in SQUAT_CRITICAL_JOINTS -> if (liveMetrics.kneeOverToe) COLOR_BAD else COLOR_GOOD
                else -> COLOR_NEUTRAL
            }

            val center = toOffset(index)

            // Dış halka
            drawCircle(color = dotColor.copy(alpha = 0.4f), radius = DOT_OUTER_RADIUS, center = center)
            // İç nokta
            drawCircle(color = COLOR_DOT, radius = DOT_INNER_RADIUS, center = center)
        }
    }
}

private const val MIN_VISIBILITY  = 0.4f
private const val LINE_WIDTH      = 5f
private const val DOT_OUTER_RADIUS = 10f
private const val DOT_INNER_RADIUS = 5f
