package com.formfit.ai.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formfit.ai.data.model.WorkoutResult
import com.formfit.ai.ui.theme.BackgroundDark
import com.formfit.ai.ui.theme.DividerColor
import com.formfit.ai.ui.theme.ErrorRed
import com.formfit.ai.ui.theme.Primary
import com.formfit.ai.ui.theme.SurfaceDark
import com.formfit.ai.ui.theme.SurfaceVariantDark
import com.formfit.ai.ui.theme.TextPrimary
import com.formfit.ai.ui.theme.TextSecondary
import com.formfit.ai.ui.theme.WarningYellow

@Composable
fun SummaryScreen(
    workoutResult: WorkoutResult,
    onBackToHome: () -> Unit
) {
    val score        = workoutResult.skorPuani
    val repCount     = workoutResult.tekrarSayisi
    val durationSecs = (workoutResult.sure / 1000).toInt()
    val spineScore   = workoutResult.omurgaDurusu
    val kneeScore    = workoutResult.kolPozisyonu
    val balanceScore = workoutResult.denge
    val calories     = repCount * 5 + durationSecs / 60 * 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // ── Üst Bar ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "Antrenman Özeti",
                color      = TextPrimary,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Skor Dairesi ─────────────────────────────────────────────────
            ScoreRing(score = score)

            Spacer(Modifier.height(24.dp))

            // ── İstatistik Satırı ─────────────────────────────────────────────
            StatRow(
                repCount     = repCount,
                durationSecs = durationSecs,
                calories     = calories
            )

            Spacer(Modifier.height(24.dp))

            // ── Detaylı Geri Bildirim ─────────────────────────────────────────
            Text(
                text       = "Detaylı Geri Bildirim",
                color      = TextPrimary,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            MetricRow(label = "Omurga Duruşu", percent = spineScore)
            Spacer(Modifier.height(10.dp))
            MetricRow(label = "Diz Açısı",     percent = kneeScore)
            Spacer(Modifier.height(10.dp))
            MetricRow(label = "Denge",          percent = balanceScore)

            Spacer(Modifier.height(20.dp))

            // ── İyileştirme Önerisi Kartı ─────────────────────────────────────
            SuggestionCard(
                spineScore   = spineScore,
                kneeScore    = kneeScore,
                balanceScore = balanceScore
            )

            Spacer(Modifier.height(24.dp))
        }

        // ── Alt Buton ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundDark)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Button(
                onClick  = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text       = "Ana Sayfaya Dön",
                    color      = Color(0xFF0D1117),
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Skor halkası ──────────────────────────────────────────────────────────────

@Composable
private fun ScoreRing(score: Int) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animatedProgress.animateTo(
            targetValue   = score / 100f,
            animationSpec = tween(durationMillis = 1200)
        )
    }

    val motivationText = when {
        score >= 80 -> "🎉 Harika İş!"
        score >= 60 -> "💪 İyi Gidiyorsun!"
        else        -> "📈 Gelişmeye Devam!"
    }

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeWidth = 16.dp.toPx()
            val inset   = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color      = SurfaceVariantDark,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color      = Primary,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.value,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = "$score",
                color      = TextPrimary,
                fontSize   = 48.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 48.sp
            )
            Text(
                text     = "Genel Skor",
                color    = TextSecondary,
                fontSize = 13.sp
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    Text(
        text       = motivationText,
        color      = Primary,
        fontSize   = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
}

// ── İstatistik satırı ─────────────────────────────────────────────────────────

@Composable
private fun StatRow(repCount: Int, durationSecs: Int, calories: Int) {
    val mins = durationSecs / 60
    val secs = durationSecs % 60
    val durationText = "%d:%02d".format(mins, secs)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell(value = "$repCount",    label = "Tekrar",  modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(DividerColor)
        )

        StatCell(value = durationText, label = "Süre",    modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(DividerColor)
        )

        StatCell(value = "$calories",    label = "Kalori",  modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = value,
            color      = TextPrimary,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text     = label,
            color    = TextSecondary,
            fontSize = 12.sp
        )
    }
}

// ── Metrik satırı ─────────────────────────────────────────────────────────────

@Composable
private fun MetricRow(label: String, percent: Int) {
    val barColor = when {
        percent >= 80 -> Primary
        percent >= 60 -> WarningYellow
        else          -> ErrorRed
    }
    val icon = when {
        percent >= 80 -> "✅"
        percent >= 60 -> "⚠️"
        else          -> "❌"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(text = label, color = TextPrimary, fontSize = 15.sp)
            }
            Text(
                text       = "%$percent",
                color      = barColor,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(SurfaceVariantDark, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent / 100f)
                    .height(4.dp)
                    .background(barColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

// ── İyileştirme önerisi kartı ─────────────────────────────────────────────────

@Composable
private fun SuggestionCard(spineScore: Int, kneeScore: Int, balanceScore: Int) {
    val suggestion = when {
        kneeScore < 80 ->
            "Squat yaparken dizlerin ayak parmaklarının önüne çıkmamasına dikkat et. " +
            "Ağırlığını topuklarına aktararak aşağı in."
        spineScore < 80 ->
            "Squat yaparken omurganı dik tut ve başını öne eğme. " +
            "Göğsünü açık tutarak hareketi kontrollü gerçekleştir."
        balanceScore < 80 ->
            "Dengeyi artırmak için ayaklarını omuz genişliğinde aç ve " +
            "hareketi yavaş, kontrollü bir tempoda yap."
        else ->
            "Harika form! Antrenmanını sürdür ve ağırlığı kademeli olarak artırmayı dene."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text       = "💡 İyileştirme Önerisi",
            color      = Primary,
            fontSize   = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text       = suggestion,
            color      = TextSecondary,
            fontSize   = 14.sp,
            textAlign  = TextAlign.Start,
            lineHeight = 20.sp
        )
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

private val previewResult = WorkoutResult(
    antrenmanID  = 1,
    kullaniciID  = 1,
    hareketID    = 1,
    tekrarSayisi = 10,
    skorPuani    = 85,
    sure         = 372_000L,
    tarih        = 0L,
    omurgaDurusu = 95,
    kolPozisyonu = 78,
    denge        = 90
)

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
private fun SummaryScreenPreview() {
    SummaryScreen(workoutResult = previewResult, onBackToHome = {})
}
