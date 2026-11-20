package com.personal.calorietracker.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.calorietracker.data.Product
import com.personal.calorietracker.data.RecipeWithIngredients
import com.personal.calorietracker.data.fmt
import com.personal.calorietracker.data.macrosFor
import com.personal.calorietracker.data.parseNumber
import com.personal.calorietracker.data.perServing
import com.personal.calorietracker.ui.common.BackButton
import com.personal.calorietracker.ui.common.EmptyState
import com.personal.calorietracker.ui.common.NumberField
import com.personal.calorietracker.ui.common.ProductRow
import com.personal.calorietracker.ui.common.SearchField
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    scannedBarcode: String?,
    onScannedHandled: () -> Unit,
    onScan: () -> Unit,
    onBack: () -> Unit,
    onCreateProduct: (barcode: String, name: String) -> Unit,
    vm: AddFoodViewModel = viewModel(),
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var pickedProduct by remember { mutableStateOf<Product?>(null) }
    var pickedRecipe by remember { mutableStateOf<RecipeWithIngredients?>(null) }

    LaunchedEffect(scannedBarcode) {
        val code = scannedBarcode ?: return@LaunchedEffect
        onScannedHandled()
        vm.lookupBarcode(code) { p ->
            if (p != null) pickedProduct = p else onCreateProduct(code, "")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add food") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan barcode")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                query, { vm.query.value = it }, "Search products or recipes",
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Products") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Recipes") })
            }
            if (tab == 0) {
                if (products.isEmpty()) {
                    EmptyState(
                        if (query.isBlank()) "No products saved yet." else "No product matches \"$query\"."
                    ) {
                        Button(onClick = { onCreateProduct("", query.trim()) }) { Text("Create new product") }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(products, key = { it.id }) { p ->
                            ProductRow(p) { pickedProduct = p }
                            HorizontalDivider()
                        }
                    }
                }
            } else {
                if (recipes.isEmpty()) {
                    EmptyState(if (query.isBlank()) "No recipes yet." else "No recipe matches \"$query\".")
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(recipes, key = { it.recipe.id }) { r ->
                            ListItem(
                                headlineContent = { Text(r.recipe.name) },
                                supportingContent = {
                                    Text("${r.perServing().kcal.roundToInt()} kcal per serving")
                                },
                                modifier = Modifier.clickable { pickedRecipe = r },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    pickedProduct?.let { p ->
        AmountDialog(
            title = p.name,
            initial = (p.servingGrams ?: 100.0).fmt(),
            unit = "g",
            label = "Amount",
            kcalFor = { p.macrosFor(it).kcal },
            onConfirm = { grams -> vm.logProduct(p, grams) { pickedProduct = null; onBack() } },
            onDismiss = { pickedProduct = null },
        )
    }
    pickedRecipe?.let { r ->
        AmountDialog(
            title = r.recipe.name,
            initial = "1",
            unit = "serving(s)",
            label = "Servings",
            kcalFor = { r.perServing().kcal * it },
            onConfirm = { s -> vm.logRecipe(r, s) { pickedRecipe = null; onBack() } },
            onDismiss = { pickedRecipe = null },
        )
    }
}

@Composable
private fun AmountDialog(
    title: String,
    initial: String,
    unit: String,
    label: String,
    kcalFor: (Double) -> Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val amount = text.parseNumber()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                NumberField(text, { text = it }, label, Modifier.fillMaxWidth(), suffix = unit)
                Text(
                    "= ${amount?.let { kcalFor(it).roundToInt() } ?: 0} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount?.let(onConfirm) },
                enabled = amount != null && amount > 0,
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
