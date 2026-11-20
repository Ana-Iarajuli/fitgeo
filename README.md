# Calorie Tracker (Android)

A personal, offline calorie tracker built with Kotlin, Jetpack Compose and a local Room database.
All data stays on the phone.

## Features

| Tab | What it does |
| --- | --- |
| **Today** | Daily food diary. Add food by searching or scanning, see kcal / protein / carbs / fat totals, browse previous days. |
| **Products** | Your product database. Enter a product's macros (per 100 g) once; afterwards it is found by typing its name/brand or by scanning its barcode or QR code. Scanning an unknown code opens a new-product form with the code pre-filled. |
| **Recipes** | Create, read, edit and delete your own recipes made from saved products. Shows per-serving and total macros, and can log a serving to today. |
| **Wishlist** | Products you want to try or buy: photos (camera or gallery), name, store, price, notes, and a "tested" flag. Filter by To try / Tested. |

Scanning uses Google ML Kit on-device, so it works offline and reads both product barcodes (EAN/UPC) and QR codes.

## Build and install

You need **Android Studio** (Ladybug 2024.2 or newer). It bundles the JDK and Android SDK.

1. Open Android Studio and choose **File > Open**, then pick this folder.
2. Let Gradle sync finish. It downloads Gradle 8.11.1 and all libraries on first run.
3. On your phone, enable **Developer options** and **USB debugging**, then connect it by USB.
   (Alternatively use **Pair devices using Wi-Fi** in Android Studio's device manager.)
4. Select your phone in the device dropdown and press **Run**.

To get an installable APK instead: **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
The file appears in `app/build/outputs/apk/debug/app-debug.apk`; copy it to the phone and open it.

If you prefer the command line, run `gradle wrapper` once (with a local Gradle install) to generate
`gradlew`, then `./gradlew assembleDebug`.

## Project layout

```
app/src/main/java/com/personal/calorietracker/
  data/            Room entities, DAOs, database, macro math, photo storage
  ui/today/        Daily diary + "Add food" screen
  ui/products/     Product list/search and create/edit form
  ui/recipes/      Recipe list, detail and editor
  ui/wishlist/     Wishlist grid and editor with photos
  ui/scanner/      CameraX + ML Kit barcode/QR scanner
  ui/AppNav.kt     Navigation and bottom bar
```

## Notes

- Nutrition values are stored per 100 g. If calories are left empty, they are estimated from macros (4/4/9 kcal per g).
- Diary entries store a snapshot of the macros, so editing or deleting a product later doesn't change past days.
- Wishlist photos are copied into the app's private storage, so deleting them from the gallery doesn't break the wishlist.
- The database schema is version 1. If you change entities later, bump the version and add a Room migration to keep your data.
