package com.personal.calorietracker.data

import java.util.Locale

data class Macros(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
) {
    operator fun plus(o: Macros) =
        Macros(kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat)

    operator fun times(f: Double) = Macros(kcal * f, protein * f, carbs * f, fat * f)
}

fun Product.macrosFor(grams: Double): Macros = Macros(
    kcal = kcalPer100 * grams / 100.0,
    protein = proteinPer100 * grams / 100.0,
    carbs = carbsPer100 * grams / 100.0,
    fat = fatPer100 * grams / 100.0,
)

fun RecipeWithIngredients.totalMacros(): Macros =
    ingredients.fold(Macros()) { acc, i -> acc + i.product.macrosFor(i.ingredient.grams) }

fun RecipeWithIngredients.perServing(): Macros =
    totalMacros() * (1.0 / recipe.servings.coerceAtLeast(1))

fun List<LogEntry>.total(): Macros =
    fold(Macros()) { acc, e -> acc + Macros(e.kcal, e.protein, e.carbs, e.fat) }

/** 12.0 -> "12", 12.345 -> "12.3" */
fun Double.fmt(): String {
    val r = Math.round(this * 10) / 10.0
    return if (r % 1.0 == 0.0) r.toLong().toString() else String.format(Locale.US, "%.1f", r)
}

/** Accepts both "1.5" and "1,5". */
fun String.parseNumber(): Double? = trim().replace(',', '.').toDoubleOrNull()
