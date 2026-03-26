package com.formfit.ai.analysis

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.roundToInt

// ── Faz ve geri bildirim tipleri ─────────────────────────────────────────────

enum class SquatPhase {
    STANDING,    // Başlangıç pozisyonu
    DESCENDING,  // İniş aşaması
    BOTTOM,      // Alt nokta (diz açısı < BOTTOM_THRESHOLD)
    ASCENDING,   // Çıkış aşaması
    COMPLETED    // Tekrar tamamlandı — bir sonraki frame'de STANDING'e geçer
}

enum class FeedbackType { SUCCESS, WARNING, ERROR }

data class RepFeedback(
    val mesaj: String,
    val detay: String,
    val tur: FeedbackType
)

/**
 * Tek bir tekrar (rep) boyunca toplanan ham metrikler.
 * Her frame'de güncellenir; tekrar bitince değerlendirmeye alınır.
 */
data class LiveMetrics(
    val leftKneeAngle: Double  = 180.0,
    val rightKneeAngle: Double = 180.0,
    val trunkLean: Double      = 0.0,
    val kneeOverToe: Boolean   = false,
    val symmetryDiff: Double   = 0.0
)

// ── Ana analizör ──────────────────────────────────────────────────────────────

class SquatAnalyzer {

    var currentPhase: SquatPhase = SquatPhase.STANDING
        private set

    var repCount: Int = 0
        private set

    var lastFeedback: RepFeedback? = null
        private set

    var liveMetrics: LiveMetrics = LiveMetrics()
        private set

    // Metrik bazlı puanlar (son tamamlanan tekrara ait)
    var omurgaDurusu: Int = 100
        private set
    var dizAcisi: Int = 100
        private set
    var denge: Int = 100
        private set

    val averageScore: Int
        get() = if (repScores.isEmpty()) 0 else repScores.average().roundToInt()

    private val repScores = mutableListOf<Int>()

    // Tekrar süresi boyunca izlenen worst-case değerler
    private var minKneeAngle       = 180.0
    private var maxTrunkLean       = 0.0
    private var symmetryAccum      = 0.0
    private var kneeOverToeFrames  = 0
    private var frameCount         = 0

    // ── Ana analiz fonksiyonu ─────────────────────────────────────────────────

    /**
     * Her kamera frame'inde çağrılır.
     * @return Mevcut [SquatPhase]
     */
    fun analyze(result: PoseLandmarkerResult): SquatPhase {
        val landmarkList = result.landmarks()
        if (landmarkList.isEmpty()) return currentPhase

        val lm = landmarkList[0]
        if (lm.size < 33) return currentPhase

        val lShoulder = lm[PoseDetectorHelper.LEFT_SHOULDER]
        val lHip      = lm[PoseDetectorHelper.LEFT_HIP]
        val rHip      = lm[PoseDetectorHelper.RIGHT_HIP]
        val lKnee     = lm[PoseDetectorHelper.LEFT_KNEE]
        val rKnee     = lm[PoseDetectorHelper.RIGHT_KNEE]
        val lAnkle    = lm[PoseDetectorHelper.LEFT_ANKLE]
        val rAnkle    = lm[PoseDetectorHelper.RIGHT_ANKLE]
        val lFoot     = lm[PoseDetectorHelper.LEFT_FOOT_INDEX]
        val rFoot     = lm[PoseDetectorHelper.RIGHT_FOOT_INDEX]

        // Açı hesaplamaları
        val leftKneeAngle  = AngleCalculator.calculateKneeAngle(lHip,  lKnee, lAnkle)
        val rightKneeAngle = AngleCalculator.calculateKneeAngle(rHip,  rKnee, rAnkle)
        val avgKneeAngle   = (leftKneeAngle + rightKneeAngle) / 2.0
        val trunkLean      = AngleCalculator.calculateTrunkLean(lShoulder, lHip)
        val symmetryDiff   = AngleCalculator.symmetryDifference(leftKneeAngle, rightKneeAngle)
        val lKneeOverToe   = AngleCalculator.isKneeOverToe(lKnee, lAnkle, lFoot)
        val rKneeOverToe   = AngleCalculator.isKneeOverToe(rKnee, rAnkle, rFoot)
        val kneeOverToe    = lKneeOverToe || rKneeOverToe

        // Canlı metriği güncelle (UI overlay için)
        liveMetrics = LiveMetrics(
            leftKneeAngle  = leftKneeAngle,
            rightKneeAngle = rightKneeAngle,
            trunkLean      = trunkLean,
            kneeOverToe    = kneeOverToe,
            symmetryDiff   = symmetryDiff
        )

        // Tekrar süresi boyunca en kötü değerleri biriktir
        if (currentPhase != SquatPhase.STANDING && currentPhase != SquatPhase.COMPLETED) {
            frameCount++
            if (avgKneeAngle < minKneeAngle) minKneeAngle = avgKneeAngle
            if (trunkLean > maxTrunkLean)    maxTrunkLean = trunkLean
            symmetryAccum += symmetryDiff
            if (kneeOverToe) kneeOverToeFrames++
        }

        // Faz geçiş mantığı
        val previousPhase = currentPhase
        currentPhase = nextPhase(currentPhase, avgKneeAngle)

        if (previousPhase == SquatPhase.ASCENDING && currentPhase == SquatPhase.COMPLETED) {
            evaluateRep()
        }
        if (previousPhase == SquatPhase.COMPLETED) {
            currentPhase = SquatPhase.STANDING
        }

        return currentPhase
    }

    // ── State machine ─────────────────────────────────────────────────────────

    private fun nextPhase(phase: SquatPhase, avgKneeAngle: Double): SquatPhase = when (phase) {
        SquatPhase.STANDING -> {
            if (avgKneeAngle < DESCEND_START) {
                resetRepAccumulators()
                SquatPhase.DESCENDING
            } else SquatPhase.STANDING
        }
        SquatPhase.DESCENDING -> when {
            avgKneeAngle <= BOTTOM_THRESHOLD  -> SquatPhase.BOTTOM
            avgKneeAngle > DESCEND_START + 10 -> SquatPhase.STANDING // hareketten vazgeçti
            else                              -> SquatPhase.DESCENDING
        }
        SquatPhase.BOTTOM -> {
            if (avgKneeAngle > ASCEND_START) SquatPhase.ASCENDING
            else SquatPhase.BOTTOM
        }
        SquatPhase.ASCENDING -> {
            if (avgKneeAngle >= STANDING_THRESHOLD) SquatPhase.COMPLETED
            else SquatPhase.ASCENDING
        }
        SquatPhase.COMPLETED -> SquatPhase.COMPLETED // evaluate() sonrasında dışarıda STANDING yapılır
    }

    // ── Tekrar değerlendirmesi ────────────────────────────────────────────────

    private fun evaluateRep() {
        repCount++

        val avgSymmetry     = if (frameCount > 0) symmetryAccum / frameCount else 0.0
        val kneeOverToeRate = if (frameCount > 0) kneeOverToeFrames.toFloat() / frameCount else 0f

        val kneeScore  = scoreKneeAngle(minKneeAngle)
        val trunkScore = scoreTrunkLean(maxTrunkLean)
        val symScore   = scoreSymmetry(avgSymmetry)
        val balScore   = scoreBalance(kneeOverToeRate)

        // Ağırlıklı ortalama: diz + gövde daha kritik
        val repScore = (kneeScore  * 0.35 +
                        trunkScore * 0.35 +
                        symScore   * 0.15 +
                        balScore   * 0.15).roundToInt().coerceIn(0, 100)

        repScores.add(repScore)

        // Son tekrara ait metrik puanları güncelle
        omurgaDurusu = trunkScore
        dizAcisi     = kneeScore
        denge        = ((symScore + balScore) / 2.0).roundToInt().coerceIn(0, 100)

        // Geri bildirim — en kötü sorunu öncelikle bildir
        lastFeedback = when {
            trunkScore < 60 -> RepFeedback(
                mesaj = "Omurgayı Dik Tut",
                detay  = "Sırtını dik ve göğsünü yukarı tutmaya çalış",
                tur    = FeedbackType.WARNING
            )
            kneeScore < 60 -> RepFeedback(
                mesaj = "Daha Derine İn",
                detay  = "Kalçanı diz hizasına kadar indir",
                tur    = FeedbackType.WARNING
            )
            balScore < 60 -> RepFeedback(
                mesaj = "Dengeyi Koru",
                detay  = "Ağırlığı topuklara ver",
                tur    = FeedbackType.WARNING
            )
            symScore < 60 -> RepFeedback(
                mesaj = "Simetriyi Düzelt",
                detay  = "Sağ ve sol bacağına eşit ağırlık ver",
                tur    = FeedbackType.WARNING
            )
            else -> RepFeedback(
                mesaj = "Harika Tempo!",
                detay  = "Bu hızda devam et",
                tur    = FeedbackType.SUCCESS
            )
        }
    }

    // ── Puanlama ─────────────────────────────────────────────────────────────

    /**
     * Diz açısı puanı.
     * İdeal: 70°–100° → 100 puan
     * Her 1° sapma için ceza uygulanır.
     */
    private fun scoreKneeAngle(minAngle: Double): Int = when {
        minAngle in 70.0..100.0 -> 100
        minAngle < 70.0         -> (100 - (70.0 - minAngle) * 2.0).roundToInt().coerceAtLeast(0)
        else                    -> (100 - (minAngle - 100.0) * 3.0).roundToInt().coerceAtLeast(0)
    }

    /**
     * Gövde eğimi puanı.
     * İdeal: 45°–80° → 100 puan
     */
    private fun scoreTrunkLean(maxLean: Double): Int = when {
        maxLean in 45.0..80.0 -> 100
        maxLean < 45.0        -> (100 - (45.0 - maxLean) * 2.5).roundToInt().coerceAtLeast(0)
        else                  -> (100 - (maxLean - 80.0) * 3.0).roundToInt().coerceAtLeast(0)
    }

    /**
     * Simetri puanı.
     * < 5° fark → 100 puan; her 1° için -5 puan.
     */
    private fun scoreSymmetry(avgDiff: Double): Int =
        (100.0 - avgDiff * 5.0).roundToInt().coerceIn(0, 100)

    /**
     * Denge puanı (diz–ayak ucu aşımı).
     * Hiç aşım yok → 100 puan.
     */
    private fun scoreBalance(overToeRate: Float): Int =
        (100f - overToeRate * 200f).roundToInt().coerceIn(0, 100)

    // ── Yardımcılar ──────────────────────────────────────────────────────────

    private fun resetRepAccumulators() {
        minKneeAngle      = 180.0
        maxTrunkLean      = 0.0
        symmetryAccum     = 0.0
        kneeOverToeFrames = 0
        frameCount        = 0
    }

    fun reset() {
        currentPhase = SquatPhase.STANDING
        repCount     = 0
        repScores.clear()
        lastFeedback  = null
        liveMetrics   = LiveMetrics()
        omurgaDurusu  = 100
        dizAcisi      = 100
        denge         = 100
        resetRepAccumulators()
    }

    // ── Eşik değerleri ───────────────────────────────────────────────────────

    companion object {
        /** İnişin başladığı diz açısı */
        private const val DESCEND_START      = 155.0
        /** Alt noktaya ulaşıldı sayılan açı */
        private const val BOTTOM_THRESHOLD   = 110.0
        /** Çıkışın başladığı açı */
        private const val ASCEND_START       = 120.0
        /** Ayakta (başlangıç) sayılan açı */
        private const val STANDING_THRESHOLD = 155.0
    }
}
