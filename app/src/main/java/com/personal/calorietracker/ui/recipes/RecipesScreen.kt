package com.personal.calorietracker.ui.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.calorietracker.data.fmt
import com.personal.calorietracker.data.perServing
import com.personal.calorietracker.ui.common.EmptyState
import com.personal.calorietracker.ui.common.SearchField
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
    vm: RecipesViewModel = viewModel(),
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val recipes by vm.recipes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("My recipes") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = "New recipe")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                query, { vm.query.value = it }, "Search recipes",
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (recipes.isEmpty()) {
                EmptyState(
                    if (query.isBlank()) "No recipes yet.\nTap + to create your first one."
                    else "No recipe matches \"$query\"."
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(recipes, key = { it.recipe.id }) { r ->
                        val per = r.perServing()
                        ListItem(
                            headlineContent = { Text(r.recipe.name) },
                            supportingContent = {
                                Text(
                                    "${per.kcal.roundToInt()} kcal per serving · " +
                                        "P ${per.protein.fmt()} · C ${per.carbs.fmt()} · F ${per.fat.fmt()} · " +
                                        "${r.recipe.servings} serving(s), ${r.ingredients.size} ingredient(s)"
                                )
                            },
                            modifier = Modifier.clickable { onOpen(r.recipe.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
