# LensVault — Offline Mobile Gallery OCR Search Engine

**LensVault** is a production-grade, 100% offline mobile gallery search application that indexes and searches text inside thousands to tens of thousands of images stored locally on a user's phone.

It delivers a private, on-device *"Google Search for your photo library"*:
- **100% Local & Private**: No photo, OCR text, or metadata is ever uploaded to any cloud server or third-party service.
- **PP-OCRv6 Tiny On-Device Pipeline**: Fast, lightweight optical character recognition generating word/line blocks, confidence ratings, and 4-corner polygon bounding boxes.
- **SQLite FTS5 Full-Text Indexing**: Near-instant query matching (<100ms) across 30,000–100,000 images with term ranking, recency weighting, and exact match bonuses.
- **Smart Entity Detection**: Automatically identifies URLs, emails, phone numbers, IP addresses, Wi-Fi credentials, invoices, and prices for 1-tap copy & action.
- **Interactive Bounding Box Inspector**: Tap any recognized text box on the image overlay to see exact coordinates, OCR confidence score, and copy individual blocks or full text.
- **Durable Resumable Indexing Queue**: Priority queue (recent screenshots > recent photos > backlog) with adaptive concurrency (1–4 workers), battery supervisor, and thermal throttling.
- **Exact & Near-Duplicate Deduplication**: Uses xxHash3 / SHA-256 for exact match detection (skips OCR and reuses canonical documents) and 64-bit DCT perceptual hash (`pHash`) with Hamming distance comparison.
- **Execution Provider Calibration**: Benchmark and switch between CPU, XNNPACK, and NNAPI neural backends on your device.

---

## 🛠 Tech Stack

- **Platform**: Android (Kotlin, Jetpack Compose, Material Design 3)
- **Persistence**: Room Database + SQLite FTS5 (WAL mode, parameterized queries)
- **Image Pipeline**: Coil Compose, ExifInterface, Memory-safe bounded bitmap scaling
- **Concurrency**: Kotlin Coroutines, StateFlow, SupervisorJob, Semaphore worker pool
- **Testing**: JUnit 4, Robolectric JVM test suite

---

## 🚀 Getting Started

1. Launch **LensVault** on your Android device or streaming emulator.
2. Grant read access to photos (via `READ_MEDIA_IMAGES` or scoped storage).
3. The background indexing supervisor immediately queues and processes your gallery.
4. If testing on a simulator with an empty gallery, tap **"Seed Realistic Document Test Cards"** in Settings or on the Gallery screen to instantly generate realistic receipts, error logs, Wi-Fi stickers, and travel boarding passes.
5. Use the **Search** tab to search for any keyword (e.g., `14.85`, `wifi password`, `INV-2026`, `500 error`, `DL402`, `pytorch`).
6. Tap any search result to open the interactive **Bounding Box Overlay Viewer**.

---

## 📚 Documentation Index

- [Architecture Overview](docs/ARCHITECTURE.md)
- [Database Schema & FTS5](docs/DATABASE.md)
- [OCR Pipeline & Preprocessing](docs/OCR_PIPELINE.md)
- [Background Indexing & Concurrency](docs/BACKGROUND_INDEXING.md)
- [Performance & Benchmarking](docs/PERFORMANCE.md)
- [Privacy & Security Guarantee](docs/PRIVACY.md)
- [Android Integration Guide](docs/ANDROID.md)
- [Cross-Platform iOS Notes](docs/IOS.md)
- [Testing & Quality Assurance](docs/TESTING.md)
- [Changelog](CHANGELOG.md)
- [Roadmap](ROADMAP.md)
