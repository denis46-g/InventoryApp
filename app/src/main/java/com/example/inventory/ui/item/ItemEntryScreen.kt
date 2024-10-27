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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventory.InventoryTopAppBar
import com.example.inventory.MainActivity
import com.example.inventory.R
import com.example.inventory.ui.AppViewModelProvider
import com.example.inventory.ui.navigation.NavigationDestination
import com.example.inventory.ui.settings.SettingsViewModel
import com.example.inventory.ui.settings.context
import com.example.inventory.ui.theme.InventoryTheme
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale

object ItemEntryDestination : NavigationDestination {
    override val route = "item_entry"
    override val titleRes = R.string.item_entry_title
}

var addItemPage : Boolean = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEntryScreen(
    navigateBack: () -> Unit,
    onNavigateUp: () -> Unit,
    canNavigateBack: Boolean = true,
    viewModel: ItemEntryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val coroutineScope = rememberCoroutineScope()

    addItemPage = true

    val loadFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                viewModel.loadFromFile(context, uri)
                navigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            InventoryTopAppBar(
                title = stringResource(ItemEntryDestination.titleRes),
                canNavigateBack = canNavigateBack,
                navigateUp = onNavigateUp
            )
        }
    ) { innerPadding ->
        ItemEntryBody(
            itemUiState = viewModel.itemUiState,
            onItemValueChange = viewModel::updateUiState,
            onSaveClick = {
                coroutineScope.launch {
                    viewModel.saveItem()
                    navigateBack()
                }
            },
            onLoadFromFileClick = {
                //viewModel.loadFromFile(context, uri = Uri)
                loadFileLauncher.launch(arrayOf("application/octet-stream"))
            },
            modifier = Modifier
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                    top = innerPadding.calculateTopPadding()
                )
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        )
    }
}

@Composable
fun ItemEntryBody(
    itemUiState: ItemUiState,
    onItemValueChange: (ItemDetails) -> Unit,
    onSaveClick: () -> Unit,
    onLoadFromFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    // Launcher для работы с документами (выбор зашифрованного файла)
    /*val loadFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            onLoadFromFileClick
        }
    }*/

    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_large)),
        modifier = modifier.padding(dimensionResource(id = R.dimen.padding_medium))
        ) {
        ItemInputForm(
            itemDetails = itemUiState.itemDetails,
            onValueChange = onItemValueChange,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = onSaveClick,
            enabled = itemUiState.isEntryValid,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.save_action))
        }

        if(addItemPage){
            Button(
                //onClick = { loadFileLauncher.launch(arrayOf("application/octet-stream")) },
                onClick = onLoadFromFileClick,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.load_from_file))
            }
        }
    }
}

@Composable
fun ItemInputForm(
    itemDetails: ItemDetails,
    modifier: Modifier = Modifier,
    onValueChange: (ItemDetails) -> Unit = {},
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
    ) {
        val isItemValid = itemDetails.name.isNotBlank()
        val isPriceValid = isCorrectPrice(itemDetails.price)
        val isQuantityValid = isCorrectQuantity(itemDetails.quantity)
        val isProviderNameValid = isCorrectProviderName(itemDetails.providerName)
        val isProviderEmailValid = isCorrectProviderEmail(itemDetails.providerEmail)
        val isProviderPhoneNumberValid = isCorrectProviderPhoneNumber(itemDetails.providerPhoneNumber)
        OutlinedTextField(
            value = itemDetails.name,
            onValueChange = { onValueChange(itemDetails.copy(name = it)) },
            label = { Text(stringResource(R.string.item_name_req)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor =
                if (isItemValid) MaterialTheme.colorScheme.secondaryContainer
                else Color(0xFFFFC0CB),
                unfocusedContainerColor =
                if (isItemValid) MaterialTheme.colorScheme.secondaryContainer
                else Color(0xFFFFC0CB),
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true
        )
        OutlinedTextField(
            value = itemDetails.price,
            onValueChange = { onValueChange(itemDetails.copy(price = it)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            label = { Text(stringResource(R.string.item_price_req)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor =
                if (isPriceValid) MaterialTheme.colorScheme.secondaryContainer
                else Color(0xFFFFC0CB),
                unfocusedContainerColor =
                if (isPriceValid) MaterialTheme.colorScheme.secondaryContainer
                else Color(0xFFFFC0CB),
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            leadingIcon = { Text(Currency.getInstance(Locale.getDefault()).symbol) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true
        )

        val settings = SettingsViewModel()
        // Помещаем флаги и состояния в Compose, чтобы они автоматически менялись
        var flagDefaultQuantity by remember { mutableStateOf(settings.getCheckboxState(2)) }
        // Состояние для количества
        val defQuantity = settings.quantityState ?: ""

        // Порог для выбора текста
        val displayQuantity = if (flagDefaultQuantity) defQuantity else itemDetails.quantity

        OutlinedTextField(
            value = displayQuantity,
            onValueChange = {
                flagDefaultQuantity = false
                onValueChange(itemDetails.copy(quantity = it))
                            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(R.string.quantity_req)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor =
                if (flagDefaultQuantity || isQuantityValid) MaterialTheme.colorScheme.secondaryContainer
                else Color(0xFFFFC0CB),
                unfocusedContainerColor =
                if (flagDefaultQuantity || isQuantityValid) MaterialTheme.colorScheme.secondaryContainer
                else Color(0xFFFFC0CB),
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true
        )
        OutlinedTextField(
            value = itemDetails.providerName,
            onValueChange = { onValueChange(itemDetails.copy(providerName = it)) },
            label = { Text(stringResource(R.string.provider_name_req)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor =
                if (isProviderNameValid) MaterialTheme.colorScheme.secondaryContainer
                else Color(0xFFFFC0CB),
                unfocusedContainerColor =
                if (isProviderNameValid) MaterialTheme.colorScheme.secondaryContainer
                else Color(0xFFFFC0CB),
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true
        )
        OutlinedTextField(
            value = itemDetails.providerEmail,
            onValueChange = { onValueChange(itemDetails.copy(providerEmail = it)) },
            label = { Text(stringResource(R.string.provider_email_req)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor =
                if (isProviderEmailValid) MaterialTheme.colorScheme.secondaryContainer
                else Color(0xFFFFC0CB),
                unfocusedContainerColor =
                if (isProviderEmailValid) MaterialTheme.colorScheme.secondaryContainer
                else Color(0xFFFFC0CB),
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true
        )
        OutlinedTextField(
            value = itemDetails.providerPhoneNumber,
            onValueChange = { onValueChange(itemDetails.copy(providerPhoneNumber = it)) },
            label = { Text(stringResource(R.string.provider_phone_number_req)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor =
                if (isProviderPhoneNumberValid) MaterialTheme.colorScheme.secondaryContainer
                else Color(0xFFFFC0CB),
                unfocusedContainerColor =
                if (isProviderPhoneNumberValid) MaterialTheme.colorScheme.secondaryContainer
                else Color(0xFFFFC0CB),
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true
        )
        if (enabled) {
            Text(
                text = stringResource(R.string.required_fields),
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.padding_medium))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemEntryScreenPreview() {
    InventoryTheme {
        ItemEntryBody(itemUiState = ItemUiState(
            ItemDetails(
                name = "Item name", price = "10.00", quantity = "5"
            )
        ), onItemValueChange = {}, onSaveClick = {}, onLoadFromFileClick = {})
    }
}
