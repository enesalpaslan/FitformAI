package com.formfit.ai.analysis

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * MediaPipe PoseLandmarker sarmalayıcısı.
 *
 * Model: app/src/main/assets/pose_landmarker_lite.task
 * İndirme komutu:
 *   curl -o pose_landmarker_lite.task \
 *     "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task"
 */
class PoseDetectorHelper(
    private val context: Context,
    private val listener: OnPoseDetectionResult
) {

    interface OnPoseDetectionResult {
        fun onResults(
            result: PoseLandmarkerResult,
            inputImageWidth: Int,
            inputImageHeight: Int
        )
        fun onError(error: String)
    }

    private var poseLandmarker: PoseLandmarker? = null

    init {
        setup(useGpu = true)
    }

    private fun setup(useGpu: Boolean) {
        val delegate = if (useGpu) Delegate.GPU else Delegate.CPU

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_FILE)
            .setDelegate(delegate)
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(MAX_POSES)
            .setMinPoseDetectionConfidence(MIN_DETECTION_CONFIDENCE)
            .setMinPosePresenceConfidence(MIN_PRESENCE_CONFIDENCE)
            .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)
            .setResultListener { result, input ->
                listener.onResults(result, input.width, input.height)
            }
            .setErrorListener { error ->
                if (useGpu) {
                    // GPU başarısız → CPU fallback
                    poseLandmarker?.close()
                    setup(useGpu = false)
                } else {
                    listener.onError(error.message ?: "Bilinmeyen hata")
                }
            }
            .build()

        try {
            poseLandmarker?.close()
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            if (useGpu) {
                setup(useGpu = false)
            } else {
                listener.onError("Model başlatılamadı: ${e.message}")
            }
        }
    }

    /**
     * Canlı yayından gelen her frame için çağrılır.
     * detectAsync non-blocking'dir; sonuçlar [OnPoseDetectionResult.onResults]'a gelir.
     */
    fun detectLiveStream(bitmap: Bitmap) {
        val mpImage = BitmapImageBuilder(bitmap).build()
        poseLandmarker?.detectAsync(mpImage, SystemClock.uptimeMillis())
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
    }

    companion object {
        const val MODEL_FILE = "pose_landmarker_lite.task"

        private const val MAX_POSES = 1
        private const val MIN_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_PRESENCE_CONFIDENCE = 0.5f
        private const val MIN_TRACKING_CONFIDENCE = 0.5f

        // MediaPipe BlazePose 33 landmark indisleri
        const val NOSE = 0
        const val LEFT_EYE = 2
        const val RIGHT_EYE = 5
        const val LEFT_EAR = 7
        const val RIGHT_EAR = 8
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
        const val LEFT_ELBOW = 13
        const val RIGHT_ELBOW = 14
        const val LEFT_WRIST = 15
        const val RIGHT_WRIST = 16
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_KNEE = 25
        const val RIGHT_KNEE = 26
        const val LEFT_ANKLE = 27
        const val RIGHT_ANKLE = 28
        const val LEFT_HEEL = 29
        const val RIGHT_HEEL = 30
        const val LEFT_FOOT_INDEX = 31
        const val RIGHT_FOOT_INDEX = 32
    }
}
