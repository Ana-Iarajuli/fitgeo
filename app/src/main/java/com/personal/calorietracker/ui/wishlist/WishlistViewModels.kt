package com.personal.calorietracker.ui.wishlist

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.personal.calorietracker.data.AppDatabase
import com.personal.calorietracker.data.ImageStore
import com.personal.calorietracker.data.WishlistItem
import com.personal.calorietracker.data.WishlistItemWithPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class WishlistFilter(val label: String) { ALL("All"), TO_TRY("To try"), TESTED("Tested") }

class WishlistViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).wishlistDao()

    val filter = MutableStateFlow(WishlistFilter.ALL)
    val query = MutableStateFlow("")

    val items: StateFlow<List<WishlistItemWithPhotos>> =
        combine(dao.all(), filter, query) { list, f, q ->
            val t = q.trim()
            list.filter {
                when (f) {
                    WishlistFilter.ALL -> true
                    WishlistFilter.TO_TRY -> !it.item.tested
                    WishlistFilter.TESTED -> it.item.tested
                }
            }.filter {
                t.isEmpty() || it.item.name.contains(t, true) || it.item.store.contains(t, true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class WishlistEditViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).wishlistDao()
    private val itemId: Long = handle.get<Long>("id") ?: 0L
    val isNew get() = itemId == 0L

    var name by mutableStateOf("")
    var store by mutableStateOf("")
    var price by mutableStateOf("")
    var notes by mutableStateOf("")
    var tested by mutableStateOf(false)
    val photos = mutableStateListOf<String>()
    var error by mutableStateOf<String?>(null)
    var loaded by mutableStateOf(isNew)
        private set

    private var createdAt = System.currentTimeMillis()
    private val originalPhotos = mutableListOf<String>()
    /** Photos added in this session; deleted from disk if the user leaves without saving. */
    private val newPhotos = mutableListOf<String>()
    private var committed = false

    init {
        if (!isNew) viewModelScope.launch {
            dao.get(itemId)?.let { w ->
                name = w.item.name
                store = w.item.store
                price = w.item.price
                notes = w.item.notes
                tested = w.item.tested
                createdAt = w.item.createdAt
                photos.addAll(w.photos.map { it.path })
                originalPhotos.addAll(w.photos.map { it.path })
            }
            loaded = true
        }
    }

    fun addPhotoFile(path: String) {
        photos.add(path)
        newPhotos.add(path)
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { ImageStore.copyFromUri(getApplication(), uri) }
            if (file != null) addPhotoFile(file.absolutePath)
            else error = "Couldn't import that image."
        }
    }

    fun removePhoto(path: String) {
        photos.remove(path)
        if (newPhotos.remove(path)) ImageStore.delete(path)
    }

    fun save(onDone: () -> Unit) {
        if (name.isBlank()) { error = "Please enter a product name."; return }
        val item = WishlistItem(
            id = itemId,
            name = name.trim(),
            store = store.trim(),
            price = price.trim(),
            notes = notes.trim(),
            tested = tested,
            createdAt = createdAt,
        )
        val keep = photos.toList()
        viewModelScope.launch {
            dao.save(item, keep)
            committed = true
            // Remove files of photos the user deleted from this item.
            originalPhotos.filterNot { it in keep }.forEach(ImageStore::delete)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            dao.delete(itemId)
            committed = true
            (originalPhotos + newPhotos).forEach(ImageStore::delete)
            onDone()
        }
    }

    override fun onCleared() {
        if (!committed) newPhotos.forEach(ImageStore::delete)
    }
}
