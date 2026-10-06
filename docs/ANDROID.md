# Android Implementation Guide

## MediaStore Permissions & Scoped Storage

- **Android 13+ (API 33+)**: Uses `android.permission.READ_MEDIA_IMAGES`.
- **Android 12 & Below (API <= 32)**: Uses `android.permission.READ_EXTERNAL_STORAGE`.
- **No Unrestricted Filesystem Access**: Operates entirely with scoped storage and standard `ContentResolver` queries.

## App Architecture

- **Jetpack Compose UI**: Modern, declarative UI with Material 3 components.
- **StateFlow & ViewModel**: Unidirectional Data Flow (UDF) architecture with lifecycle-aware collection.
- **Room SQLite Persistence**: Thread-safe background database interactions using Kotlin Coroutines and Flow.
