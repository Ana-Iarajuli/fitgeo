package com.personal.calorietracker.ui.products

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.calorietracker.ui.common.EmptyState
import com.personal.calorietracker.ui.common.ProductRow
import com.personal.calorietracker.ui.common.SearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    scannedBarcode: String?,
    onScannedHandled: () -> Unit,
    onScan: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onNewProduct: (barcode: String, name: String) -> Unit,
    vm: ProductsViewModel = viewModel(),
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()

    // A code came back from the scanner: open the saved product, or start a new one with that code.
    LaunchedEffect(scannedBarcode) {
        val code = scannedBarcode ?: return@LaunchedEffect
        onScannedHandled()
        vm.lookupBarcode(code) { p ->
            if (p != null) onOpenProduct(p.id) else onNewProduct(code, "")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Products") },
                actions = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan barcode")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNewProduct("", query.trim()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add product")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                query = query,
                onQueryChange = { vm.query.value = it },
                placeholder = "Search by name, brand or barcode",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (products.isEmpty()) {
                if (query.isBlank()) {
                    EmptyState("No products yet.\nAdd one with + or scan a barcode. Once saved, it will be found by name or by scanning.")
                } else {
                    EmptyState("No product matches \"$query\".") {
                        Button(onClick = { onNewProduct("", query.trim()) }) {
                            Text("Add \"${query.trim()}\"")
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(products, key = { it.id }) { p ->
                        ProductRow(p) { onOpenProduct(p.id) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
