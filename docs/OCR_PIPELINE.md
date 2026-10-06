# LensVault On-Device OCR Pipeline

The OCR pipeline in LensVault executes completely offline on the user's mobile device without network calls.

## Pipeline Lifecycle

```text
1. MediaStore / Storage Uri
       |
2. ImagePreprocessor
   - Decodes bounds with inJustDecodeBounds
   - Applies inSampleSize (max 1600px dimension)
   - Reads EXIF orientation and rotates bitmap
       |
3. Deduplication Check (HashEngine)
   - Computes exact SHA-256 / xxHash3
   - If exact hash exists in DB -> Reuses canonical document and skips OCR!
   - Computes 64-bit DCT perceptual hash (pHash)
       |
4. PP-OCRv6 Tiny Neural Inference
   - Text detection: identifies horizontal text line polygons
   - Text recognition: transcribes text characters
   - Confidence scoring: evaluates token likelihood [0.0 - 1.0]
   - Filters out blocks below user confidence threshold
       |
5. Entity Extraction
   - Regex engines extract URLs, Emails, Phone Numbers, IP addresses, Invoices, Totals, Credentials
       |
6. Database & FTS Sync
   - Inserts OcrDocument and OcrBlocks inside a single transaction
   - Updates FTS search virtual table
```

## Bounding Box Coordinate System

All OCR bounding boxes are persisted as **normalized floating point coordinates `[0.0 - 1.0]`**:
- `x1, y1`: Top-Left corner
- `x2, y2`: Top-Right corner
- `x3, y3`: Bottom-Right corner
- `x4, y4`: Bottom-Left corner

This makes rendering resolution-independent across variable screen densities, aspect ratios, zoom levels, and tablet screens.
