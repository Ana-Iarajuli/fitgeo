package com.personal.calorietracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    /** Matches name or brand anywhere (case-insensitive), or an exact barcode. */
    @Query(
        """
        SELECT * FROM products
        WHERE name LIKE '%' || :q || '%' OR brand LIKE '%' || :q || '%' OR barcode = :q
        ORDER BY name COLLATE NOCASE
        """
    )
    fun search(q: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :code LIMIT 1")
    suspend fun findByBarcode(code: String): Product?

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun get(id: Long): Product?

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT COUNT(DISTINCT recipeId) FROM recipe_ingredients WHERE productId = :id")
    suspend fun recipeUsageCount(id: Long): Int
}

@Dao
abstract class RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE")
    abstract fun all(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract fun observe(id: Long): Flow<RecipeWithIngredients?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract suspend fun get(id: Long): RecipeWithIngredients?

    @Insert
    abstract suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    abstract suspend fun updateRecipe(recipe: Recipe)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    abstract suspend fun clearIngredients(recipeId: Long)

    @Insert
    abstract suspend fun insertIngredients(items: List<RecipeIngredient>)

    @Query("DELETE FROM recipes WHERE id = :id")
    abstract suspend fun delete(id: Long)

    /** Inserts or updates the recipe and replaces its ingredient list. Returns the recipe id. */
    @Transaction
    open suspend fun save(recipe: Recipe, ingredients: List<RecipeIngredient>): Long {
        val id = if (recipe.id == 0L) insertRecipe(recipe) else {
            updateRecipe(recipe)
            recipe.id
        }
        clearIngredients(id)
        insertIngredients(ingredients.map { it.copy(id = 0, recipeId = id) })
        return id
    }
}

@Dao
abstract class WishlistDao {
    @Transaction
    @Query("SELECT * FROM wishlist_items ORDER BY tested ASC, createdAt DESC")
    abstract fun all(): Flow<List<WishlistItemWithPhotos>>

    @Transaction
    @Query("SELECT * FROM wishlist_items WHERE id = :id")
    abstract suspend fun get(id: Long): WishlistItemWithPhotos?

    @Insert
    abstract suspend fun insertItem(item: WishlistItem): Long

    @Update
    abstract suspend fun updateItem(item: WishlistItem)

    @Query("DELETE FROM wishlist_photos WHERE itemId = :itemId")
    abstract suspend fun clearPhotos(itemId: Long)

    @Insert
    abstract suspend fun insertPhotos(photos: List<WishlistPhoto>)

    @Query("DELETE FROM wishlist_items WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Transaction
    open suspend fun save(item: WishlistItem, photoPaths: List<String>): Long {
        val id = if (item.id == 0L) insertItem(item) else {
            updateItem(item)
            item.id
        }
        clearPhotos(id)
        insertPhotos(photoPaths.map { WishlistPhoto(itemId = id, path = it) })
        return id
    }
}

@Dao
interface LogDao {
    @Query("SELECT * FROM log_entries WHERE day = :day ORDER BY createdAt")
    fun forDay(day: Long): Flow<List<LogEntry>>

    @Insert
    suspend fun insert(entry: LogEntry)

    @Delete
    suspend fun delete(entry: LogEntry)
}
