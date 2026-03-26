package com.formfit.ai.data.model

data class Exercise(
    val hareketID: Int,
    val hareketIsmi: String,
    val hedefKaslar: String,
    val iconResId: Int,         // R.drawable.*
    val aktif: Boolean = true,
    val annneni2: Boolean = false// false → UI'da kilitli / tıklanamaz
)
