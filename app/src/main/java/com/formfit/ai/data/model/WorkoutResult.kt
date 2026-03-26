package com.formfit.ai.data.model

data class WorkoutResult(
    val antrenmanID: Int,
    val kullaniciID: Int,
    val hareketID: Int,
    val tekrarSayisi: Int,
    val skorPuani: Int,     // 0-100
    val sure: Long,         // milisaniye
    val tarih: Long,        // epoch milisaniye (System.currentTimeMillis())
    val omurgaDurusu: Int,  // yüzde 0-100
    val kolPozisyonu: Int,  // yüzde 0-100
    val denge: Int          // yüzde 0-100
)
