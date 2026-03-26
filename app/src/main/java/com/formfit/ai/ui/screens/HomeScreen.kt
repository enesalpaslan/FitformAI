package com.formfit.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formfit.ai.data.FakeDataSource
import com.formfit.ai.data.model.Exercise
import com.formfit.ai.data.model.User
import com.formfit.ai.ui.theme.BackgroundDark
import com.formfit.ai.ui.theme.DividerColor
import com.formfit.ai.ui.theme.FitFormAITheme
import com.formfit.ai.ui.theme.Primary
import com.formfit.ai.ui.theme.ScoreGradientEnd
import com.formfit.ai.ui.theme.ScoreGradientStart
import com.formfit.ai.ui.theme.SurfaceDark
import com.formfit.ai.ui.theme.SurfaceVariantDark
import com.formfit.ai.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    user: User? = null,
    onStartWorkout: (Int) -> Unit
) {
    val context = LocalContext.current
    val user = user ?: FakeDataSource.getUserById(1)
    val exercises = FakeDataSource.exercises

    // Squat (ID=1) varsayılan seçili
    var selectedExerciseId by remember { mutableIntStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Kaydırılabilir içerik
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                // Alta sabit buton için boşluk bırak
                .padding(bottom = 96.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // ── 1. Üst başlık ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hoş geldin 👋",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "FitForm AI",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    )
                }

                // Profil emoji dairesi
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                        .border(2.dp, Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.profilEmoji ?: "😊",
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 2. Motivasyon kartı ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Merhaba, bugün antrenman\nyapmaya hazır mısın?",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "😊🏋️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "1.234 kişi şu an antrenman yapıyor",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 3. Egzersiz seçim listesi ────────────────────────────────────
            Text(
                text = "Egzersiz Seç",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            exercises.forEach { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    isSelected = selectedExerciseId == exercise.hareketID,
                    onSelect = {
                        if (exercise.aktif) {
                            selectedExerciseId = exercise.hareketID
                        } else {
                            Toast.makeText(
                                context,
                                "Bu egzersiz yakında eklenecek!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // ── 4. Alt sabit buton ───────────────────────────────────────────────
        val isActive = selectedExerciseId == 1
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(BackgroundDark)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            GradientButton(
                text = "Antrenmana Başla",
                enabled = isActive,
                onClick = { onStartWorkout(selectedExerciseId) }
            )
        }
    }
}

// ── Egzersiz kartı ────────────────────────────────────────────────────────────

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = when {
        isSelected -> Primary
        else -> DividerColor
    }
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (exercise.aktif) 1f else 0.5f)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.08f) else SurfaceDark
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol ikon kutusu
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (exercise.aktif) Primary.copy(alpha = 0.15f)
                        else SurfaceVariantDark
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (exercise.hareketID) {
                        1 -> "🏋️"
                        2 -> "🚶"
                        3 -> "💪"
                        else -> "🏃"
                    },
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Orta: isim + kas grubu
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.hareketIsmi,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = exercise.hedefKaslar,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            // Sağ: ok ikonu veya "Yakında" badge
            if (exercise.aktif) {
                Text(
                    text = "›",
                    fontSize = 22.sp,
                    color = if (isSelected) Primary else TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariantDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Yakında",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

// ── Gradient buton ────────────────────────────────────────────────────────────

@Composable
private fun GradientButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = if (enabled) {
            listOf(ScoreGradientStart, ScoreGradientEnd)
        } else {
            listOf(SurfaceVariantDark, SurfaceVariantDark)
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (enabled) Color(0xFF0D1117) else TextSecondary
            )
        )
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
private fun HomeScreenPreview() {
    FitFormAITheme {
        HomeScreen(onStartWorkout = { _ -> })
    }
}
