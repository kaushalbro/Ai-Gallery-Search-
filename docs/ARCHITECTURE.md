# LensVault Architecture

LensVault is architected around an **offline-first, zero-cloud data pipeline** optimized for mobile device constraints (battery life, thermal envelope, memory bounds, and storage limits).

```text
+-------------------------------------------------------------------------+
|                              LensVault UI                               |
|        (Jetpack Compose M3 • StateFlow • Reactive UI Collections)       |
+--------------------+-------------------------------+--------------------+
                     |                               |
                     v                               v
             SearchEngine                    MainViewModel
                     |                               |
                     +---------------+---------------+
                                     |
                                     v
                           GalleryRepository
                                     |
    +--------------------------------+--------------------------------+
    |                                |                                |
    v                                v                                v
AppDatabase                   IndexingCoordinator                 OcrEngine
(Room + SQLite FTS5)         (Durable Priority Queue)      (PP-OCRv6 Tiny Pipeline)
  - assets                     - Priority dispatch             - Preprocessor
  - ocr_documents              - Battery/Thermal supervisor    - Bounding polygon boxes
  - ocr_blocks                 - Concurrency pool (1-4)        - Normalized coordinates
  - ocr_search_fts             - Deduplication bypass          - Entity extractor
```

---

## 1. Key Architectural Principles

1. **Local-First & Zero-Cloud**: No API calls, no remote telemetry, no external network dependencies.
2. **Asynchronous & Non-Blocking**: Heavy operations (EXIF rotation, bitmap decoding, hash calculation, neural inference, FTS writing) happen strictly on background `Dispatchers.IO` / `Dispatchers.Default`. The UI thread remains locked at 60+ FPS.
3. **Bounded Memory Execution**: Bitmaps are downsampled to a max dimension of 1600px prior to inference, processed in small worker batches, and immediately recycled using `bitmap.recycle()`.
4. **Durable State Machine**: Indexing jobs and asset statuses are persisted in SQLite, surviving app termination, crashes, low memory kills, and device restarts.
5. **Incremental Scanning**: Scans only newly added or modified images via MediaStore timestamps; never re-indexes unchanged assets.
