package com.formfit.ai.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formfit.ai.ui.theme.BackgroundDark
import com.formfit.ai.ui.theme.DividerColor
import com.formfit.ai.ui.theme.FitFormAITheme
import com.formfit.ai.ui.theme.Primary
import com.formfit.ai.ui.theme.SurfaceDark
import com.formfit.ai.ui.theme.SurfaceVariantDark
import com.formfit.ai.ui.theme.TextSecondary

private val emojiOptions = listOf("😊", "😎", "🤓", "🎮", "🤖")

@Composable
fun LoginScreen(onLoginSuccess: (kullaniciAdi: String, email: String, profilEmoji: String) -> Unit) {
    var ad by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var secilenEmoji by remember { mutableStateOf("😊") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // ── Logo & Başlık ────────────────────────────────────────────────
            Text(
                text = "🏋️",
                fontSize = 64.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "FitForm AI",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Kişisel Antrenman Asistanın",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Form Kartı ───────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Hesap Oluştur",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Ad Soyad
                    FormLabel("Adın Soyadın")
                    Spacer(modifier = Modifier.height(6.dp))
                    FitFormTextField(
                        value = ad,
                        onValueChange = { ad = it },
                        placeholder = "Örn: Ahmet Yılmaz",
                        keyboardType = KeyboardType.Text
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // E-posta
                    FormLabel("E-posta")
                    Spacer(modifier = Modifier.height(6.dp))
                    FitFormTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "ornek@email.com",
                        keyboardType = KeyboardType.Email
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Emoji seçimi
                    FormLabel("Profil Fotoğrafı (Emoji)")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        emojiOptions.forEach { emoji ->
                            EmojiPickerItem(
                                emoji = emoji,
                                isSelected = secilenEmoji == emoji,
                                onClick = { secilenEmoji = emoji }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Hesap Oluştur butonu
                    Button(
                        onClick = { onLoginSuccess(ad, email, secilenEmoji) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = Color(0xFF0D1117)
                        )
                    ) {
                        Text(
                            text = "Hesap Oluştur",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Alt link ─────────────────────────────────────────────────────
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = TextSecondary)) {
                        append("Zaten hesabın var mı? ")
                    }
                    withStyle(
                        SpanStyle(
                            color = Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("Giriş Yap")
                    }
                },
                fontSize = 14.sp,
                modifier = Modifier.clickable { onLoginSuccess(ad, email, secilenEmoji) }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ── Alt bileşenler ───────────────────────────────────────────────────────────

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            color = TextSecondary,
            fontSize = 12.sp
        )
    )
}

@Composable
private fun FitFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SurfaceVariantDark,
            unfocusedContainerColor = SurfaceVariantDark,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Primary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f),
            unfocusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f)
        )
    )
}

@Composable
private fun EmojiPickerItem(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (isSelected) Primary.copy(alpha = 0.15f) else SurfaceVariantDark)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Primary else DividerColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
private fun LoginScreenPreview() {
    FitFormAITheme {
        LoginScreen(onLoginSuccess = { _, _, _ -> })
    }
}
