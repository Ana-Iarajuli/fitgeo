package com.personal.calorietracker.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.personal.calorietracker.data.AppDatabase
import com.personal.calorietracker.data.LogEntry
import com.personal.calorietracker.data.Product
import com.personal.calorietracker.data.RecipeWithIngredients
import com.personal.calorietracker.data.fmt
import com.personal.calorietracker.data.macrosFor
import com.personal.calorietracker.data.perServing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).logDao()

    val day = MutableStateFlow(LocalDate.now().toEpochDay())

    val entries: StateFlow<List<LogEntry>> = day
        .flatMapLatest { dao.forDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun shiftDay(by: Long) {
        day.value += by
    }

    fun goToToday() {
        day.value = LocalDate.now().toEpochDay()
    }

    fun delete(e: LogEntry) {
        viewModelScope.launch { dao.delete(e) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddFoodViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val day: Long = handle.get<Long>("day") ?: LocalDate.now().toEpochDay()

    val query = MutableStateFlow("")

    val products: StateFlow<List<Product>> = query
        .flatMapLatest { db.productDao().search(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recipes: StateFlow<List<RecipeWithIngredients>> =
        combine(db.recipeDao().all(), query) { list, q ->
            val t = q.trim()
            if (t.isEmpty()) list else list.filter { it.recipe.name.contains(t, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun lookupBarcode(code: String, onResult: (Product?) -> Unit) {
        viewModelScope.launch { onResult(db.productDao().findByBarcode(code)) }
    }

    fun logProduct(p: Product, grams: Double, onDone: () -> Unit) {
        val m = p.macrosFor(grams)
        viewModelScope.launch {
            db.logDao().insert(
                LogEntry(
                    day = day, name = p.name, amountLabel = "${grams.fmt()} g",
                    kcal = m.kcal, protein = m.protein, carbs = m.carbs, fat = m.fat,
                )
            )
            onDone()
        }
    }

    fun logRecipe(r: RecipeWithIngredients, servings: Double, onDone: () -> Unit) {
        val m = r.perServing() * servings
        viewModelScope.launch {
            db.logDao().insert(
                LogEntry(
                    day = day, name = r.recipe.name, amountLabel = "${servings.fmt()} serving(s)",
                    kcal = m.kcal, protein = m.protein, carbs = m.carbs, fat = m.fat,
                )
            )
            onDone()
        }
    }
}
