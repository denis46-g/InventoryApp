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
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.room.PrimaryKey
import com.example.inventory.data.Item
import com.example.inventory.data.ItemsRepository
import com.example.inventory.data.Source
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import java.security.KeyStore
import java.text.NumberFormat
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * ViewModel to validate and insert items in the Room database.
 */
class ItemEntryViewModel(private val itemsRepository: ItemsRepository) : ViewModel() {

    /**
     * Holds current item ui state
     */
    var itemUiState by mutableStateOf(ItemUiState())
        private set

    /**
     * Updates the [itemUiState] with the value provided in the argument. This method also triggers
     * a validation for input values.
     */
    fun updateUiState(itemDetails: ItemDetails) {
        itemUiState =
            ItemUiState(itemDetails = itemDetails, isEntryValid = validateInput(itemDetails))
    }

    private fun validateInput(uiState: ItemDetails = itemUiState.itemDetails): Boolean {
        return with(uiState) {
            name.isNotBlank() && isCorrectPrice(price) && isCorrectQuantity(quantity)
                    && isCorrectProviderName(providerName)
                    && isCorrectProviderEmail(providerEmail)
                    && isCorrectProviderPhoneNumber(providerPhoneNumber)
        }
    }

    suspend fun saveItem() {
        if (validateInput()) {
            itemsRepository.insertItem(itemUiState.itemDetails.toItem())
        }
    }

    // Функция для загрузки файла
    suspend fun loadFromFile(context: Context, uri: Uri) {
        try {
            val encryptedData = context.contentResolver.openInputStream(uri)?.readBytes() ?: throw Exception("Не удалось прочитать файл.")
            val secretKey = getSecretKey(uri) // Получение секретного ключа
            val decryptedData = decryptData(encryptedData, secretKey) // Расшифровка данных
            val json = String(decryptedData, Charsets.UTF_8)
            var itemDetails = Gson().fromJson(json, ItemDetails::class.java)

            itemDetails.createdBy = Source.FILE
            //val it: Item = itemDetails.toItem()
            val it = itemsRepository.getAllItemsStream().first()
            //if(itemsRepository.getItemStream(itemDetails.id) != null)
                //itemDetails.id *= 1
            var maxId = 1
            for(i in it) {
                if(i.id > maxId)
                    maxId = i.id
            }
            itemDetails.id = maxId + 1

            // Сохранение itemDetails в вашей базе данных
            updateUiState(itemDetails)
            itemsRepository.insertItem(itemUiState.itemDetails.toItem())
        } catch (e: Exception) {
            e.printStackTrace()
            // Обработка ошибок
        }
    }

    // Метод для получения секретного ключа
    private fun getSecretKey(uri: Uri): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        //val keyStore = KeyStore.getInstance("AES")
        val secretKey = keyStore.getKey("my_secret_key_$uri", null) as SecretKey

        return secretKey
    }

    // Метод для расшифровки данных
    private fun decryptData(encryptedData: ByteArray, secretKey: SecretKey): ByteArray {
        val iv = ByteArray(16)
        System.arraycopy(encryptedData, 0, iv, 0, iv.size)

        val cipherText = ByteArray(encryptedData.size - iv.size)
        System.arraycopy(encryptedData, iv.size, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

        return cipher.doFinal(cipherText)
    }
}

/**
 * Represents Ui State for an Item.
 */
data class ItemUiState(
    val itemDetails: ItemDetails = ItemDetails(),
    val isEntryValid: Boolean = false
)

data class ItemDetails(
    var id: Int = 0,
    val name: String = "",
    val price: String = "",
    val quantity: String = "",
    val providerName: String = "",
    val providerEmail: String = "",
    val providerPhoneNumber: String = "",
    var createdBy: Source = Source.MANUAL
)

/**
 * Extension function to convert [ItemDetails] to [Item]. If the value of [ItemDetails.price] is
 * not a valid [Double], then the price will be set to 0.0. Similarly if the value of
 * [ItemDetails.quantity] is not a valid [Int], then the quantity will be set to 0
 */
fun ItemDetails.toItem(): Item = Item(
    id = id,
    name = name,
    price = price.toDoubleOrNull() ?: 0.0,
    quantity = quantity.toIntOrNull() ?: 0,
    providerName = providerName,
    providerEmail = providerEmail,
    providerPhoneNumber = providerPhoneNumber,
    createdBy = createdBy
)

fun Item.formatedPrice(): String {
    return NumberFormat.getCurrencyInstance().format(price)
}

/**
 * Extension function to convert [Item] to [ItemUiState]
 */
fun Item.toItemUiState(isEntryValid: Boolean = false): ItemUiState = ItemUiState(
    itemDetails = this.toItemDetails(),
    isEntryValid = isEntryValid
)

/**
 * Extension function to convert [Item] to [ItemDetails]
 */
fun Item.toItemDetails(): ItemDetails = ItemDetails(
    id = id,
    name = name,
    price = price.toString(),
    quantity = quantity.toString(),
    providerName = providerName,
    providerEmail = providerEmail,
    providerPhoneNumber = providerPhoneNumber,
    createdBy = createdBy
)
