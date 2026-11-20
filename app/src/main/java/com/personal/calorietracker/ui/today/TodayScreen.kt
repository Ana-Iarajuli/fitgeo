package com.personal.calorietracker.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.calorietracker.data.fmt
import com.personal.calorietracker.data.total
import com.personal.calorietracker.ui.common.EmptyState
import com.personal.calorietracker.ui.common.MacroSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val dateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

private fun dayLabel(epochDay: Long): String {
    val today = LocalDate.now().toEpochDay()
    return when (epochDay) {
        today -> "Today"
        today - 1 -> "Yesterday"
        today + 1 -> "Tomorrow"
        else -> LocalDate.ofEpochDay(epochDay).format(dateFormat)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onAddFood: (day: Long) -> Unit,
    vm: TodayViewModel = viewModel(),
) {
    val day by vm.day.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val totals = entries.total()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Calorie Tracker") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddFood(day) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add food") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { vm.shiftDay(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                }
                Text(
                    dayLabel(day),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable { vm.goToToday() },
                )
                IconButton(onClick = { vm.shiftDay(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                }
            }
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${totals.kcal.roundToInt()} kcal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    MacroSummary(totals,Modifier.padding(top = 8.dp))
                }
            }
            if (entries.isEmpty()) {
                EmptyState("Nothing logged for this day.\nTap \"Add food\" to search or scan a product.")
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                    items(entries, key = { it.id }) { e ->
                        ListItem(
                            headlineContent = { Text(e.name) },
                            supportingContent = {
                                Text("${e.amountLabel} · P ${e.protein.fmt()} · C ${e.carbs.fmt()} · F ${e.fat.fmt()}")
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${e.kcal.roundToInt()} kcal")
                                    IconButton(onClick = { vm.delete(e) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove entry")
                                    }
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
