package com.formfit.ai.data

import com.formfit.ai.R
import com.formfit.ai.data.model.Exercise
import com.formfit.ai.data.model.User
import com.formfit.ai.data.model.WorkoutResult

object FakeDataSource {

    // ── Kullanıcılar ────────────────────────────────────────────────────────────

    private val users = listOf(
        User(
            kullaniciID = 1,
            kullaniciAdi = "Ahmet Yılmaz",
            email = "ornek@email.com",
            profilEmoji = "😊",
            kilo = 75f,
            boy = 178f,
            cinsiyet = "Erkek"
        )
    )

    /**
     * Email ve isim (büyük/küçük harf duyarsız) eşleşirse kullanıcıyı döner,
     * eşleşmezse null döner.
     */
    fun login(email: String, kullaniciAdi: String): User? {
        return users.firstOrNull {
            it.email.equals(email.trim(), ignoreCase = true) &&
            it.kullaniciAdi.equals(kullaniciAdi.trim(), ignoreCase = true)
        }
    }

    fun getUserById(id: Int): User? = users.firstOrNull { it.kullaniciID == id }

    // ── Egzersizler ─────────────────────────────────────────────────────────────

    val exercises = listOf(
        Exercise(
            hareketID = 1,
            hareketIsmi = "Squat",
            hedefKaslar = "Bacak & Kalça",
            iconResId = R.drawable.ic_exercise_squat,
            aktif = true
        ),
        Exercise(
            hareketID = 2,
            hareketIsmi = "Lunge",
            hedefKaslar = "Bacak & Denge",
            iconResId = R.drawable.ic_exercise_lunge,
            aktif = false   // yakında
        ),
        Exercise(
            hareketID = 3,
            hareketIsmi = "Push-up",
            hedefKaslar = "Göğüs & Kol",
            iconResId = R.drawable.ic_exercise_pushup,
            aktif = false   // yakında
        )
    )

    fun getExerciseById(id: Int): Exercise? = exercises.firstOrNull { it.hareketID == id }

    // ── Antrenman Geçmişi (in-memory) ───────────────────────────────────────────

    private val workoutResults = mutableListOf<WorkoutResult>()
    private var nextAntrenmanID = 1

    fun saveWorkoutResult(result: WorkoutResult): WorkoutResult {
        val saved = result.copy(antrenmanID = nextAntrenmanID++)
        workoutResults.add(saved)
        return saved
    }

    fun getWorkoutsByUser(kullaniciID: Int): List<WorkoutResult> {
        return workoutResults
            .filter { it.kullaniciID == kullaniciID }
            .sortedByDescending { it.tarih }
    }

    fun getLastWorkout(kullaniciID: Int): WorkoutResult? {
        return getWorkoutsByUser(kullaniciID).firstOrNull()
    }
}
