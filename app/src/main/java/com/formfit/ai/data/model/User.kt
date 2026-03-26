package com.formfit.ai.data.model

data class User(
    val kullaniciID: Int,
    val kullaniciAdi: String,
    val email: String,
    val profilEmoji: String,
    val kilo: Float?,
    val boy: Float?,
    val cinsiyet: String?
)
