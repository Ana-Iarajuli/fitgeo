package com.personal.calorietracker.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.calorietracker.data.macrosFor
import com.personal.calorietracker.data.parseNumber
import com.personal.calorietracker.ui.common.BackButton
import com.personal.calorietracker.ui.common.MacroSummary
import com.personal.calorietracker.ui.common.NumberField
import com.personal.calorietracker.ui.common.ProductPickerDialog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    onBack: () -> Unit,
    vm: RecipeEditViewModel = viewModel(),
) {
    var showPicker by remember { mutableStateOf(false) }
    val productQuery by vm.productQuery.collectAsStateWithLifecycle()
    val productResults by vm.productResults.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "New recipe" else "Edit recipe") },
                navigationIcon = { BackButton(onBack) },
                actions = {
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
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = vm.name, onValueChange = { vm.name = it; vm.error = null },
                    label = { Text("Recipe name *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                NumberField(
                    vm.servings, { vm.servings = it }, "Servings",
                    Modifier.fillMaxWidth(), decimal = false,
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Per serving", style = MaterialTheme.typography.titleSmall)
                        MacroSummary(vm.totals * (1.0 / vm.servingCount), Modifier.padding(top = 8.dp))
                        Text(
                            "Whole recipe: ${vm.totals.kcal.roundToInt()} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
            item { Text("Ingredients", style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(vm.ingredients) { index, d ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(d.product.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${d.product.macrosFor(d.grams.parseNumber() ?: 0.0).kcal.roundToInt()} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    NumberField(
                        d.grams, { vm.setGrams(index, it) }, "Amount",
                        Modifier.width(120.dp), suffix = "g",
                    )
                    IconButton(onClick = { vm.removeIngredient(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ingredient")
                    }
                }
            }
            item {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add ingredient", Modifier.padding(start = 8.dp))
                }
            }
            item {
                OutlinedTextField(
                    value = vm.instructions, onValueChange = { vm.instructions = it },
                    label = { Text("Instructions / notes") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            vm.error?.let { msg ->
                item { Text(msg, color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (showPicker) {
        ProductPickerDialog(
            title = "Add ingredient",
            query = productQuery,
            onQueryChange = { vm.productQuery.value = it },
            products = productResults,
            onPick = { vm.addIngredient(it); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}
