/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.inventory.ui.item

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.Item
import com.example.inventory.data.ItemsRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * ViewModel to retrieve, update and delete an item from the [ItemsRepository]'s data source.
 */
class ItemDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val itemsRepository: ItemsRepository
) : ViewModel() {

    private val itemId: Int = checkNotNull(savedStateHandle[ItemDetailsDestination.itemIdArg])

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }

    val uiState: StateFlow<ItemDetailsUiState> =
        itemsRepository.getItemStream(itemId)
            .filterNotNull()
            .map {
                ItemDetailsUiState(outOfStock = it.quantity <= 0, itemDetails = it.toItemDetails())
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = ItemDetailsUiState()
            )

    fun reduceQuantityByOne() {
        viewModelScope.launch {
            val currentItem = uiState.value.itemDetails.toItem()
            if (currentItem.quantity > 0) {
                itemsRepository.updateItem(currentItem.copy(quantity = currentItem.quantity - 1))
            }
        }
    }

    suspend fun deleteItem() {
        itemsRepository.deleteItem(uiState.value.itemDetails.toItem())
    }

    fun saveProductToCache(context: Context, item: Item, uri: Uri): File {
        val gson = Gson()
        val jsonData = gson.toJson(item)

        // Шифруем данные
        val encryptedData = encryptData(jsonData, uri)

        // Сохранение зашифрованных данных в кэш
        val cacheDir = context.cacheDir
        val cacheFile = File(cacheDir, "product_cache.enc")

        FileOutputStream(cacheFile).use { outputStream ->
            outputStream.write(encryptedData)
        }
        return cacheFile
    }

    fun writeCacheToSharedStorage(context: Context, cacheFile: File, uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            // Чтение зашифрованного содержимого из файла кэша
            FileInputStream(cacheFile).use { inputStream ->
                copyStream(inputStream, outputStream)
            }
        }
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }
    }

    // Метод для шифрования данных
    private fun encryptData(data: String, uri: Uri): ByteArray {

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias("my_secret_key_$uri")) {
            keyStore.deleteEntry("my_secret_key_$uri")
        }

        // Генерация и получение мастер-ключа
        /*val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256) // Можете использовать 128 или 192
        val secretKey: SecretKey = keyGen.generateKey()*/

        // Проверяем, существует ли ключ
        if (!keyStore.containsAlias("my_secret_key_$uri")) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                "my_secret_key_$uri",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }

        val secretKey = keyStore.getKey("my_secret_key_$uri", null) as SecretKey

        // Создание шифра
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        // Генерация вектора инициализации
        val iv = cipher.iv

        // Шифрование данных
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        // Возвращаем вектор и зашифрованные данные
        return iv + encryptedData
    }

    fun shareItem(context: Context){
        val itemInformation =
            "Item: ${uiState.value.itemDetails.toItem().name}\n" +
                    "Price: ${uiState.value.itemDetails.toItem().price}\n" +
                    "Quantity: ${uiState.value.itemDetails.toItem().quantity}\n" +
                    "Provider name: ${uiState.value.itemDetails.toItem().providerName}\n" +
                    "Provider email: ${uiState.value.itemDetails.toItem().providerEmail}\n" +
                    "Provider phone: ${uiState.value.itemDetails.toItem().providerPhoneNumber}"

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, itemInformation)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }
}

/**
 * UI state for ItemDetailsScreen
 */
data class ItemDetailsUiState(
    val outOfStock: Boolean = true,
    val itemDetails: ItemDetails = ItemDetails()
)
