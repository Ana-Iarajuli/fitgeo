package com.personal.calorietracker.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.calorietracker.data.fmt
import com.personal.calorietracker.data.macrosFor
import com.personal.calorietracker.data.perServing
import com.personal.calorietracker.data.totalMacros
import com.personal.calorietracker.ui.common.BackButton
import com.personal.calorietracker.ui.common.ConfirmDialog
import com.personal.calorietracker.ui.common.MacroSummary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    vm: RecipeDetailViewModel = viewModel(),
) {
    val data by vm.recipe.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(data?.recipe?.name ?: "Recipe") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    data?.let { r ->
                        IconButton(onClick = { onEdit(r.recipe.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val r = data
        if (r == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Per serving", style = MaterialTheme.typography.titleSmall)
                        MacroSummary(r.perServing(), Modifier.padding(top = 8.dp))
                        Text(
                            "Whole recipe (${r.recipe.servings} serving(s)): " +
                                "${r.totalMacros().kcal.roundToInt()} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        vm.logToToday(1.0) {
                            scope.launch { snackbar.showSnackbar("1 serving added to today") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Log 1 serving to today") }
            }
            item { Text("Ingredients", style = MaterialTheme.typography.titleMedium) }
            if (r.ingredients.isEmpty()) {
                item { Text("No ingredients yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(r.ingredients, key = { it.ingredient.id }) { i ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${i.product.name} · ${i.ingredient.grams.fmt()} g", Modifier.weight(1f))
                    Text("${i.product.macrosFor(i.ingredient.grams).kcal.roundToInt()} kcal")
                }
            }
            if (r.recipe.instructions.isNotBlank()) {
                item { Text("Instructions", style = MaterialTheme.typography.titleMedium) }
                item { Text(r.recipe.instructions) }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete recipe?",
            text = "\"${data?.recipe?.name.orEmpty()}\" will be permanently deleted.",
            onConfirm = { confirmDelete = false; vm.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}
