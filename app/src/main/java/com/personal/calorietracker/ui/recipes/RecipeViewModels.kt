package com.personal.calorietracker.ui.recipes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.personal.calorietracker.data.AppDatabase
import com.personal.calorietracker.data.LogEntry
import com.personal.calorietracker.data.Macros
import com.personal.calorietracker.data.Product
import com.personal.calorietracker.data.Recipe
import com.personal.calorietracker.data.RecipeIngredient
import com.personal.calorietracker.data.RecipeWithIngredients
import com.personal.calorietracker.data.fmt
import com.personal.calorietracker.data.macrosFor
import com.personal.calorietracker.data.parseNumber
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

class RecipesViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).recipeDao()

    val query = MutableStateFlow("")

    val recipes: StateFlow<List<RecipeWithIngredients>> =
        combine(dao.all(), query) { list, q ->
            val t = q.trim()
            if (t.isEmpty()) list else list.filter { it.recipe.name.contains(t, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class RecipeDetailViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val recipeId: Long = handle.get<Long>("id") ?: 0L

    val recipe: StateFlow<RecipeWithIngredients?> = db.recipeDao().observe(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logToToday(servings: Double, onDone: () -> Unit) {
        val r = recipe.value ?: return
        val m = r.perServing() * servings
        viewModelScope.launch {
            db.logDao().insert(
                LogEntry(
                    day = LocalDate.now().toEpochDay(),
                    name = r.recipe.name,
                    amountLabel = "${servings.fmt()} serving(s)",
                    kcal = m.kcal, protein = m.protein, carbs = m.carbs, fat = m.fat,
                )
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            db.recipeDao().delete(recipeId)
            onDone()
        }
    }
}

data class DraftIngredient(val product: Product, val grams: String)

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val recipeId: Long = handle.get<Long>("id") ?: 0L
    val isNew get() = recipeId == 0L

    var name by mutableStateOf("")
    var servings by mutableStateOf("1")
    var instructions by mutableStateOf("")
    val ingredients = mutableStateListOf<DraftIngredient>()
    var error by mutableStateOf<String?>(null)
    var loaded by mutableStateOf(isNew)
        private set

    private var createdAt = System.currentTimeMillis()

    // Product search for the "add ingredient" picker
    val productQuery = MutableStateFlow("")
    val productResults: StateFlow<List<Product>> = productQuery
        .flatMapLatest { db.productDao().search(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (!isNew) viewModelScope.launch {
            db.recipeDao().get(recipeId)?.let { r ->
                name = r.recipe.name
                servings = r.recipe.servings.toString()
                instructions = r.recipe.instructions
                createdAt = r.recipe.createdAt
                ingredients.addAll(r.ingredients.map { DraftIngredient(it.product, it.ingredient.grams.fmt()) })
            }
            loaded = true
        }
    }

    val totals: Macros
        get() = ingredients.fold(Macros()) { acc, d ->
            acc + d.product.macrosFor(d.grams.parseNumber() ?: 0.0)
        }

    val servingCount: Int get() = servings.toIntOrNull()?.coerceAtLeast(1) ?: 1

    fun addIngredient(p: Product) {
        ingredients.add(DraftIngredient(p, (p.servingGrams ?: 100.0).fmt()))
        productQuery.value = ""
    }

    fun setGrams(index: Int, grams: String) {
        ingredients[index] = ingredients[index].copy(grams = grams)
    }

    fun removeIngredient(index: Int) {
        ingredients.removeAt(index)
    }

    fun save(onDone: () -> Unit) {
        if (name.isBlank()) { error = "Please enter a recipe name."; return }
        val bad = ingredients.firstOrNull { (it.grams.parseNumber() ?: 0.0) <= 0.0 }
        if (bad != null) { error = "Enter an amount for \"${bad.product.name}\"."; return }

        val recipe = Recipe(
            id = recipeId,
            name = name.trim(),
            servings = servingCount,
            instructions = instructions.trim(),
            createdAt = createdAt,
        )
        val items = ingredients.map {
            RecipeIngredient(recipeId = recipeId, productId = it.product.id, grams = it.grams.parseNumber()!!)
        }
        viewModelScope.launch {
            db.recipeDao().save(recipe, items)
            onDone()
        }
    }
}
