package com.example.inventory.ui.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey


lateinit var context: Context

class SettingsViewModel(): ViewModel() {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "settings_encrypted",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val checkboxStates = mutableStateListOf<Boolean>().apply {
        add(sharedPreferences.getBoolean("checkbox_1", false))
        add(sharedPreferences.getBoolean("checkbox_2", false))
        add(sharedPreferences.getBoolean("checkbox_3", false))
    }

    // Получение состояния чекбокса по индексу
    fun getCheckboxState(index: Int): Boolean {
        return sharedPreferences.getBoolean("checkbox_${index + 1}", false)
    }

    fun setCheckboxState(index: Int, isChecked: Boolean) {
        checkboxStates[index] = isChecked
        saveToPreferences(index, isChecked)
    }

    private fun saveToPreferences(index: Int, isChecked: Boolean) {
        sharedPreferences.edit().putBoolean("checkbox_${index + 1}", isChecked).apply()
    }

    var quantityState = sharedPreferences.getString("default quantity", "10")

    //var textFieldValue = mutableStateOf(quantityState.toString()) // Начальное значение пустое

    fun setQuantState(quantity: String){
        quantityState = quantity
        sharedPreferences.edit().putString("default quantity", quantity).apply()
    }
}