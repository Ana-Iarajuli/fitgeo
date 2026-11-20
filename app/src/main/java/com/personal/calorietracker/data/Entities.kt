package com.personal.calorietracker.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** A product whose macros were entered once. All nutrition values are per 100 g. */
@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"], unique = true), Index(value = ["name"])]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String = "",
    /** Barcode or QR content. Null when the product has no code. */
    val barcode: String? = null,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    /** Optional typical portion in grams, used as the default amount. */
    val servingGrams: Double? = null,
)

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val servings: Int = 1,
    val instructions: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [Index("recipeId"), Index("productId")]
)
data class RecipeIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val productId: Long,
    val grams: Double,
)

data class IngredientWithProduct(
    @Embedded val ingredient: RecipeIngredient,
    @Relation(parentColumn = "productId", entityColumn = "id")
    val product: Product,
)

data class RecipeWithIngredients(
    @Embedded val recipe: Recipe,
    @Relation(
        entity = RecipeIngredient::class,
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val ingredients: List<IngredientWithProduct>,
)

@Entity(tableName = "wishlist_items")
data class WishlistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val store: String = "",
    val price: String = "",
    val notes: String = "",
    val tested: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "wishlist_photos",
    foreignKeys = [
        ForeignKey(
            entity = WishlistItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class WishlistPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    /** Absolute path of a JPEG inside the app's private storage. */
    val path: String,
)

data class WishlistItemWithPhotos(
    @Embedded val item: WishlistItem,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val photos: List<WishlistPhoto>,
)

/**
 * One eaten food on a given day. Macros are copied at log time so the diary
 * stays correct if a product or recipe is later edited or deleted.
 */
@Entity(tableName = "log_entries", indices = [Index("day")])
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** LocalDate.toEpochDay() */
    val day: Long,
    val name: String,
    val amountLabel: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val createdAt: Long = System.currentTimeMillis(),
)
