package com.personal.calorietracker.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.calorietracker.ui.common.BackButton
import com.personal.calorietracker.ui.common.ConfirmDialog
import com.personal.calorietracker.ui.common.NumberField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    scannedBarcode: String?,
    onScannedHandled: () -> Unit,
    onScan: () -> Unit,
    onBack: () -> Unit,
    vm: ProductEditViewModel = viewModel(),
) {
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(scannedBarcode) {
        val code = scannedBarcode ?: return@LaunchedEffect
        vm.barcode = code
        onScannedHandled()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "New product" else "Edit product") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (!vm.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                    IconButton(onClick = { vm.save(onBack) }) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
                    }
                },
            )
        },
    ) { padding ->
        if (!vm.loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (vm.cameFromUnknownScan) {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "This code isn't saved yet. Enter the macros once and the product will be found next time you scan it.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            OutlinedTextField(
                value = vm.name, onValueChange = { vm.name = it; vm.error = null },
                label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.brand, onValueChange = { vm.brand = it },
                label = { Text("Brand") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.barcode, onValueChange = { vm.barcode = it; vm.error = null },
                label = { Text("Barcode / QR") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan")
                    }
                },
            )

            Text("Nutrition per 100 g", style = MaterialTheme.typography.titleMedium)
            NumberField(
                vm.kcal, { vm.kcal = it; vm.error = null }, "Calories",
                Modifier.fillMaxWidth(), suffix = "kcal",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(vm.protein, { vm.protein = it }, "Protein", Modifier.weight(1f), "g")
                NumberField(vm.carbs, { vm.carbs = it }, "Carbs", Modifier.weight(1f), "g")
                NumberField(vm.fat, { vm.fat = it }, "Fat", Modifier.weight(1f), "g")
            }
            Text(
                "Leave calories empty to calculate them from the macros.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NumberField(
                vm.serving, { vm.serving = it }, "Typical portion (optional)",
                Modifier.fillMaxWidth(), suffix = "g",
            )

            vm.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmDelete) {
        val usage = vm.usageCount
        ConfirmDialog(
            title = "Delete product?",
            text = if (usage > 0)
                "\"${vm.name}\" is used in $usage recipe(s). It will be removed from them too."
            else "\"${vm.name}\" will be removed from your products. Past diary entries are kept.",
            onConfirm = { confirmDelete = false; vm.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}
