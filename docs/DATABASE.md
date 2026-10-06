# LensVault Database Architecture & Schema

LensVault uses **SQLite in WAL (Write-Ahead Logging) mode** managed via Android Room.

## Tables Overview

### 1. `assets`
Stores discovered media files from MediaStore or imported libraries.

| Column | Type | Description |
|---|---|---|
| `id` | INTEGER PK | Auto-increment primary key |
| `platform_asset_id` | TEXT UNIQUE | MediaStore content ID or unique path hash |
| `uri` | TEXT | `content://` or `file://` URI string |
| `filename` | TEXT | Image file name (e.g. `Screenshot_20261005.png`) |
| `album_name` | TEXT | Bucket / folder display name |
| `mime_type` | TEXT | MIME type (`image/jpeg`, `image/png`, etc.) |
| `width` / `height` | INTEGER | Original pixel dimensions |
| `file_size` | INTEGER | Size in bytes |
| `created_at` / `modified_at` | INTEGER | Epoch timestamp in milliseconds |
| `is_screenshot` | INTEGER | Boolean flag (1 = screenshot, 0 = photo) |
| `exact_hash` | TEXT | xxHash3 / SHA-256 64-bit content hash |
| `perceptual_hash` | TEXT | 64-bit DCT binary perceptual hash string |
| `canonical_document_id` | INTEGER | Reference to canonical OCR document if duplicate |
| `indexing_status` | TEXT | `DISCOVERED`, `QUEUED`, `HASHING`, `OCR_RUNNING`, `INDEXED`, `SKIPPED_DUPLICATE`, `FAILED_RETRYABLE` |

### 2. `ocr_documents`
Persists the recognized text document corresponding to an asset.

| Column | Type | Description |
|---|---|---|
| `id` | INTEGER PK | Document ID |
| `asset_id` | INTEGER FK | Foreign key to `assets.id` (CASCADE delete) |
| `normalized_text` | TEXT | Normalized lowercase text for fast matching |
| `raw_text` | TEXT | Exact multiline recognized text |
| `model_name` | TEXT | OCR Model name (e.g. `PP-OCRv6-Tiny`) |
| `model_version` | TEXT | Model build version |
| `runtime_provider` | TEXT | Provider (`CPU`, `XNNPACK`, `NNAPI`) |
| `inference_ms` | INTEGER | Latency in milliseconds |
| `block_count` / `word_count` | INTEGER | Counts for ranking & diagnostics |

### 3. `ocr_blocks`
Stores individual recognized lines / words with 4-corner polygon coordinates.

| Column | Type | Description |
|---|---|---|
| `id` | INTEGER PK | Block ID |
| `document_id` | INTEGER FK | Foreign key to `ocr_documents.id` |
| `asset_id` | INTEGER | Reference asset ID |
| `block_index` | INTEGER | Order of reading appearance |
| `text` | TEXT | Recognized text segment |
| `confidence` | REAL | Confidence rating `[0.0 - 1.0]` |
| `x1`, `y1`, `x2`, `y2`, `x3`, `y3`, `x4`, `y4` | REAL | Normalized polygon bounding coordinates `[0.0 - 1.0]` |

### 4. `ocr_search_fts`
SQLite Full-Text Search virtual table using `@Fts4` for sub-millisecond keyword lookup.
Matches against `text`, `filename`, and `album_name`.
