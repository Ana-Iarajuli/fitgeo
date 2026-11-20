package com.personal.calorietracker.ui.products

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.personal.calorietracker.data.AppDatabase
import com.personal.calorietracker.data.Product
import com.personal.calorietracker.data.fmt
import com.personal.calorietracker.data.parseNumber
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).productDao()

    val query = MutableStateFlow("")

    val products: StateFlow<List<Product>> = query
        .flatMapLatest { dao.search(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun lookupBarcode(code: String, onResult: (Product?) -> Unit) {
        viewModelScope.launch { onResult(dao.findByBarcode(code)) }
    }
}

class ProductEditViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).productDao()

    val productId: Long = handle.get<Long>("id") ?: 0L
    val isNew get() = productId == 0L

    var name by mutableStateOf(handle.get<String>("name").orEmpty())
    var brand by mutableStateOf("")
    var barcode by mutableStateOf(handle.get<String>("barcode").orEmpty())
    var kcal by mutableStateOf("")
    var protein by mutableStateOf("")
    var carbs by mutableStateOf("")
    var fat by mutableStateOf("")
    var serving by mutableStateOf("")

    var error by mutableStateOf<String?>(null)
    var loaded by mutableStateOf(isNew)
        private set
    var usageCount by mutableIntStateOf(0)
        private set

    /** True when a scan opened this form because the code wasn't in the database yet. */
    val cameFromUnknownScan = isNew && barcode.isNotBlank()

    private var original: Product? = null

    init {
        if (!isNew) viewModelScope.launch {
            dao.get(productId)?.let { p ->
                original = p
                name = p.name
                brand = p.brand
                barcode = p.barcode.orEmpty()
                kcal = p.kcalPer100.fmt()
                protein = p.proteinPer100.fmt()
                carbs = p.carbsPer100.fmt()
                fat = p.fatPer100.fmt()
                serving = p.servingGrams?.fmt().orEmpty()
            }
            usageCount = dao.recipeUsageCount(productId)
            loaded = true
        }
    }

    fun save(onDone: () -> Unit) {
        val p = protein.parseNumber() ?: 0.0
        val c = carbs.parseNumber() ?: 0.0
        val f = fat.parseNumber() ?: 0.0
        // If calories are left empty, estimate them from the macros (4/4/9 kcal per gram).
        val k = kcal.parseNumber() ?: if (protein.isBlank() && carbs.isBlank() && fat.isBlank()) null
        else p * 4 + c * 4 + f * 9

        when {
            name.isBlank() -> { error = "Please enter a name."; return }
            k == null -> { error = "Please enter calories or macros per 100 g."; return }
        }

        val code = barcode.trim().ifEmpty { null }
        val product = Product(
            id = productId,
            name = name.trim(),
            brand = brand.trim(),
            barcode = code,
            kcalPer100 = k!!,
            proteinPer100 = p,
            carbsPer100 = c,
            fatPer100 = f,
            servingGrams = serving.parseNumber()?.takeIf { it > 0 },
        )
        viewModelScope.launch {
            if (code != null) {
                val existing = dao.findByBarcode(code)
                if (existing != null && existing.id != productId) {
                    error = "This barcode already belongs to \"${existing.name}\"."
                    return@launch
                }
            }
            if (isNew) dao.insert(product) else dao.update(product)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val p = original ?: return
        viewModelScope.launch {
            dao.delete(p)
            onDone()
        }
    }
}
