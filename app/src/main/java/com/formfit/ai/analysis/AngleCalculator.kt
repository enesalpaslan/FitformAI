package com.formfit.ai.analysis

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Eklem açısı ve postür hesaplamaları.
 * Tüm landmark koordinatları [0,1] aralığında normalize edilmiştir.
 */
object AngleCalculator {

    /**
     * Üç nokta arasındaki açıyı derece cinsinden hesaplar.
     *
     * @param pointA  İlk nokta (örn. kalça)
     * @param pointB  Merkez / pivot nokta (örn. diz)
     * @param pointC  Son nokta (örn. ayak bileği)
     * @return 0°–180° arasında açı
     */
    fun calculateAngle(
        pointA: NormalizedLandmark,
        pointB: NormalizedLandmark,
        pointC: NormalizedLandmark
    ): Double {
        // B merkezli vektörler
        val ax = (pointA.x() - pointB.x()).toDouble()
        val ay = (pointA.y() - pointB.y()).toDouble()
        val cx = (pointC.x() - pointB.x()).toDouble()
        val cy = (pointC.y() - pointB.y()).toDouble()

        val angleA = atan2(ay, ax)
        val angleC = atan2(cy, cx)

        var angle = Math.toDegrees(angleA - angleC)
        // [0, 360] → [0, 180]
        if (angle < 0) angle += 360.0
        if (angle > 180.0) angle = 360.0 - angle

        return angle
    }

    /**
     * Diz açısı: kalça → diz → ayak bileği.
     * İdeal squat alt noktasında: 70°–100°
     */
    fun calculateKneeAngle(
        hip: NormalizedLandmark,
        knee: NormalizedLandmark,
        ankle: NormalizedLandmark
    ): Double = calculateAngle(hip, knee, ankle)

    /**
     * Kalça açısı: omuz → kalça → diz.
     * Gövdenin öne eğilme miktarını gösterir.
     */
    fun calculateHipAngle(
        shoulder: NormalizedLandmark,
        hip: NormalizedLandmark,
        knee: NormalizedLandmark
    ): Double = calculateAngle(shoulder, hip, knee)

    /**
     * Gövde eğimi: omuz–kalça hattının dikey eksenle yaptığı açı.
     * 0° = tamamen dik, 90° = yatay.
     * Squat için ideal aralık: 45°–80°
     */
    fun calculateTrunkLean(
        shoulder: NormalizedLandmark,
        hip: NormalizedLandmark
    ): Double {
        val dx = abs((shoulder.x() - hip.x()).toDouble())
        val dy = abs((shoulder.y() - hip.y()).toDouble())
        // Dikey eksenle (dy) olan açı → küçük dx = dik duruş
        return Math.toDegrees(atan2(dx, dy))
    }

    /**
     * İki landmark arasındaki 2D Öklid mesafesi (normalize koordinatlarda).
     */
    fun distance(a: NormalizedLandmark, b: NormalizedLandmark): Double {
        val dx = (a.x() - b.x()).toDouble()
        val dy = (a.y() - b.y()).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Simetri farkı: sağ ve sol diz açıları arasındaki mutlak fark.
     * < 5° mükemmel, > 15° simetri sorunu.
     */
    fun symmetryDifference(leftKneeAngle: Double, rightKneeAngle: Double): Double =
        abs(leftKneeAngle - rightKneeAngle)

    /**
     * Dizin ayak ucunu geçip geçmediğini kontrol eder.
     * Normalize x koordinatına göre karşılaştırılır.
     * Ayna görüntüsü (ön kamera) dikkate alınmıştır.
     *
     * @param toleranceFraction  Kabul edilebilir geçiş miktarı (varsayılan %5)
     */
    fun isKneeOverToe(
        knee: NormalizedLandmark,
        ankle: NormalizedLandmark,
        toeFront: NormalizedLandmark,
        toleranceFraction: Float = 0.05f
    ): Boolean {
        // Ayak parmak ucu ile diz arasındaki x farkı
        val diff = abs(knee.x() - ankle.x())
        val toeExtension = abs(toeFront.x() - ankle.x())
        return diff > toeExtension + toleranceFraction
    }
}
