# LensVault Comprehensive Bug Audit & Stability Report (100+ Findings)

This audit analyzes stability hazards, OCR accuracy pitfalls, concurrency edge cases, memory leaks, database bottlenecks, and thermal issues across the **LensVault** on-device offline architecture.

---

## I. OCR Pipeline & Preprocessing Accuracy (Bugs 1–15)

1. **EXIF Orientation Coordinate Inversion**: When images with EXIF orientations 6 (90° CW) or 8 (270° CW) are rotated, mapped bounding polygon coordinates remain relative to raw orientation, causing bounding box overlays to appear mirrored or transposed.
2. **Extreme Aspect Ratio Distortion**: Panoramic receipts or 20,000-pixel chat screenshots downscaled to 1600px suffer severe vertical text compression, making small fonts unreadable.
3. **Low-Contrast Dark Mode Text Missing**: White text on dark grey or OLED black screenshots fails contrast thresholding without adaptive local thresholding (Otsu/Sauvola).
4. **Normalized Coordinate Rounding Drift**: On ultra-high-DPI screens (e.g., 1440p), single-precision float rounding errors cause 2–4 pixel bounding box boundary misalignments.
5. **Multi-Column Scrambling**: In restaurant bills with parallel price columns, horizontal line segmentation concatenates item names with prices from adjacent columns.
6. **Rotated / Skewed Document Misalignment**: Non-orthogonal handheld camera scans produce trapezoidal text blocks that rectangular bounding box estimators clip.
7. **Punctuation Stripping Deleting Currency and Math**: Over-aggressive normalization regex strips `$`, `€`, `.`, and `,`, converting `$14.85` into `14 85`.
8. **Accent & Non-Latin Character Stripping**: Lowercase regex `[^a-z0-9]` strips German umlauts, French accents, Spanish `ñ`, and Cyrillic characters.
9. **Line-Break Hyphenation Splitting**: Words split with a hyphen across lines (e.g., `infor-` / `mation`) are indexed as two separate disconnected words.
10. **False Positive Text Regions on Grid Textures**: Woven cloth, window blinds, or striped UI backgrounds trigger high Laplacian variance, generating ghost text blocks.
11. **Grayscale Color Channel Blindness**: Colored text (e.g. yellow on white, or red on black) can have identical luminescence to the background, disappearing during grayscale conversion.
12. **Overexposed Document Clipping**: Camera document photos taken under bright office lights suffer from blown-out highlights where text strokes are eroded.
13. **Out-of-Bounds Polygon Clamping**: Text line boxes near image margins can produce normalized coordinates `< 0.0` or `> 1.0`, corrupting Canvas draw operations.
14. **HEIC / WebP EXIF Tag Drop**: Standard ExifInterface on older Android builds fails to read orientation metadata from newer HEIF/AVIF containers.
15. **Tiny Font Degradation**: Font sizes under 8pt on 4K photos fall below the receptive field of tiny neural models when downsampled to 1600px.

---

## II. Database, Room & SQLite FTS5 Scaling (Bugs 16–30)

16. **FTS Special Character Crash**: User searches containing unescaped FTS syntax characters (e.g. `*`, `"`, `:`, `AND`, `NOT`, `-`) throw `SQLiteException: syntax error`.
17. **FTS Missing Wildcard Matching**: FTS query `MATCH :query` without trailing `*` fails to match partial words (e.g., typing "recei" does not match "receipt").
18. **Unindexed Canonical Document Lookup**: `assets.canonical_document_id` lacks an index, turning duplicate lookups into full table scans on 100K image libraries.
19. **WAL Checkpoint Starvation**: Long-running background write transactions prevent SQLite from checkpointing the WAL file, causing the `-wal` file to swell to gigabytes.
20. **SQLite Parameter Limit on Older Android**: Batch inserting thousands of blocks in a single query hits the 999 parameter limit on SQLite < 3.32.0.
21. **Foreign Key Cascade Race Condition**: Deleting an asset while a background worker is mid-insert on `ocr_blocks` can trigger `SQLiteConstraintException`.
22. **Destructive Migration Data Loss**: Database builder's `fallbackToDestructiveMigration()` wipes all OCR indices on any schema update.
23. **Case-Folding Mismatch in FTS**: Normalized text using Java lowercase might diverge from SQLite `unicode61` tokenizer case-folding rules for international scripts.
24. **Slow Album / Filename Filters**: Filtering on `album_name` or `filename` without indexes forces table scans during search queries.
25. **Cursor Drift on Search Pagination**: Offset-based pagination returns duplicate items or skips items when newly indexed assets are inserted between scrolls.
26. **Read Query Lockouts During Heavy Indexing**: Long-running write transactions during 50-asset batches block the UI thread from querying search results.
27. **Invalidated Content URIs**: When the user edits or deletes an image outside the app, the stored `content://` URI becomes a dangling reference that throws `FileNotFoundException`.
28. **Empty String Allocation Overhead**: Indexing empty documents generates empty text columns that waste disk space and inflate the FTS B-tree.
29. **Uncommitted FTS Sync on Block Failure**: If block insertion fails midway, the document might remain in FTS while blocks are missing from Room.
30. **Database Size Inflation from Raw Text**: Storing both `raw_text` and `normalized_text` doubles storage consumption for large documents.

---

## III. Concurrency, Queue & State Machine (Bugs 31–45)

31. **Backlog Starvation**: Assets with Priority 20 never get processed if the user continuously takes new screenshots (Priority 80) or photos (Priority 60).
32. **Semaphore Leak on Worker Exception**: If an uncaught `Throwable` or `OutOfMemoryError` occurs inside a worker block, the semaphore permit is never released, permanently locking the queue.
33. **Zombie Indexing Status**: Force-killing the app or OS process termination leaves assets stuck in `HASHING` or `OCR_RUNNING` states.
34. **Duplicate Worker Collision**: Two workers in parallel can pop the same pending job if the selection and claim are not executed inside a single immediate atomic transaction.
35. **Infinite Retry on Corrupt Images**: Images with corrupted headers or zero bytes are retried indefinitely if error classification fails to mark them `FAILED_PERMANENT`.
36. **Division by Zero in Speed EMA**: Calculating rolling images-per-second when elapsed time is 0 ms results in `Float.NaN` or `Float.POSITIVE_INFINITY`.
37. **Pause State Desynchronization**: Setting `isPaused = true` in UI while workers are in the middle of a 10-second batch does not cancel the in-flight batch.
38. **Unbounded Thread Dispatch**: Spawning multiple coroutines on `Dispatchers.Default` for neural inference starves UI layout animations.
39. **Unchecked Coroutine Scope Lifecycle**: ViewModel clearing during background processing can cancel ongoing database writes halfway.
40. **Job Completion Race**: Marking `indexing_jobs` completed before the database transaction commits can cause the job supervisor to re-dispatch the job.
41. **Missing Exponential Backoff**: Retryable failures (e.g. device storage busy) retry immediately, exacerbating I/O contention.
42. **Memory Leak in Queue Waiters**: Retaining job lambdas that reference Activity/Context causes memory leaks if activity recreates.
43. **No Worker Concurrency Adjustment on Thermals**: If device temperature spikes, the number of workers does not step down from 4 to 1 dynamically.
44. **Foreground Task Cancellation**: Background indexing pauses or terminates when user locks phone unless managed by WorkManager.
45. **Stale Progress Notifications**: Progress percentage doesn't update until a full batch of 10 images completes.

---

## IV. Image Memory Management & OOM Hazards (Bugs 46–60)

46. **108MP Camera Sensor OOM**: Decoding raw camera photos (12000x9000px) allocates ~430MB in ARGB_8888, crashing devices with 256MB JVM heaps.
47. **Lack of `inBitmap` Buffer Recycling**: Repeatedly allocating and discarding large bitmaps causes heavy garbage collection pauses (GC churn) and UI jank.
48. **Unbounded ByteArray Compression**: Compressing large bitmaps to JPEG byte arrays for hashing exhausts heap memory.
49. **Hardware Bitmap Incompatibility**: Passing `Bitmap.Config.HARDWARE` to software Canvas operations throws `IllegalArgumentException: Software rendering doesn't support hardware bitmaps`.
50. **Coil Cache Contention**: Caching full-resolution bitmaps in image loaders consumes memory needed by the OCR pipeline.
51. **Recycled Bitmap Crash**: Calling `bitmap.recycle()` on a bitmap that Jetpack Compose or Coil is concurrently rendering throws `Canvas: trying to use a recycled bitmap`.
52. **Cursor Window Overflow**: Querying 50,000 assets from MediaStore in one query exceeds the 2MB Android `CursorWindow` limit, throwing `CursorWindowAllocationException`.
53. **Large Search Result Memory Footprint**: Returning 1,000 `SearchResultItem` objects holding full text strings in memory slows down Compose rendering.
54. **Unclosed InputStreams**: Failure to close file streams inside catch blocks causes Linux file descriptor exhaustion (`EMFILE: Too many open files`).
55. **Double Buffer Allocation**: Creating an intermediate scaled bitmap without recycling the original input bitmap doubles peak memory usage.
56. **Canvas Path Memory Spike**: Allocating new `Path()` and `Stroke()` objects on every frame inside `Canvas` draw calls triggers continuous allocations.
57. **Animated GIF/WebP Frame Leaks**: Attempting to OCR animated files without frame extraction consumes excessive RAM.
58. **Bitmap Configuration Fallback**: Some PNG images fail to decode in `ARGB_8888` on older GPU chipsets without falling back to `RGB_565`.
59. **Deep Stack Traces in Coroutines**: Chained coroutine exceptions in worker pools retain large stack trace allocations.
60. **Non-Native Asset Memory Retention**: Converting bitmaps to base64 or heavy object models in memory instead of direct URI references.

---

## V. Search Querying, Ranking & Precision (Bugs 61–75)

61. **Substring False Positives**: Querying `"car"` matches `"card"`, `"scary"`, `"carpet"`, and `"carnival"` with identical score to an exact vehicle photo.
62. **Case Sensitivity Discrepancies**: Searches for uppercase acronyms (e.g. `"AWS"` or `"API"`) failing to match lowercase normalized text.
63. **Space & Newline Breakage**: Multi-word phrase search `"Total $14.85"` fails if OCR inserted a newline between `"Total"` and `"$14.85"`.
64. **Recency Dominance Distortion**: Recent photos outscoring exact phrase matches from older photos due to high recency multipliers.
65. **Stopword Dilution**: Queries like `"the hotel room"` dilute the relevance score because `"the"` matches thousands of unrelated images.
66. **No Stemming or Lemmatization**: Searching for `"invoices"` fails to match images containing `"invoice"`.
67. **URL Regex Truncation**: URLs containing port numbers (`:8080`) or query parameters (`?id=123&token=xyz`) get cut off at special characters.
68. **IP Address False Matches**: Regex `\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b` matches software version numbers (`v2.14.0.1`) and chapter markers.
69. **Currency Symbol Misclassification**: OCR engines frequently transcribe `$` as `S` or `8`, and `€` as `C` or `E`, breaking exact price filters.
70. **Wi-Fi Credential Boundary Loss**: Passwords containing spaces or special characters (`Password: My Wi-Fi 2026!`) get partially captured.
71. **Emoji & Multi-Byte Unicode Highlight Shift**: Substring indices calculated on UTF-16 characters shift position when emoji or non-BMP symbols appear before the match.
72. **Empty Snippet Generation**: Matching an asset solely by filename or album name displays a blank or generic snippet.
73. **Confidence Inversion**: High-confidence text on an irrelevant timestamp receives a higher rank bonus than low-confidence text on the primary search term.
74. **Lack of Fuzzy Search / Levenshtein Distance**: Search fails when OCR misreads a single letter (e.g., `"Passw0rd"` vs `"Password"`, `"L0g1n"` vs `"Login"`).
75. **Date Range Filter Timezone Shift**: Date filtering using UTC day boundaries excludes photos taken near midnight in local timezones.

---

## VI. Hardware, Thermal & Battery Management (Bugs 76–90)

76. **Stale Sticky Battery Intent**: Reading sticky `ACTION_BATTERY_CHANGED` can report cached battery values if the system has not updated the intent.
77. **Missing Thermal Listener API**: Relying on generic strings rather than Android 10+ `PowerManager.OnThermalStatusChangedListener` misses hardware thermal throttling alerts.
78. **Background Termination by Doze Mode**: When device enters Doze mode, CPU access is cut, pausing indexing indefinitely without `WorkManager` constraints.
79. **Slow USB / Wireless Charging Misdetection**: Flagging wireless or 500mA PC USB charging as "Charging" causes thermal buildup and battery drain during Fast mode.
80. **Lack of Battery Drain Metering**: App does not compute mAh or battery percent consumed per 1,000 images for user diagnostics.
81. **Haptic Feedback Crash on Vibrator-less Tablets**: Calling vibration without verifying `vibrator.hasVibrator()` crashes on certain Android tablets.
82. **Broken Vendor NNAPI Drivers**: Certain chipsets crash inside proprietary NNAPI drivers during quantized tensor inference.
83. **Thread Thrashing on big.LITTLE Cores**: Worker threads scheduled indiscriminately across efficiency and performance cores cause latency jitter.
84. **Screen-On Indexing Causing UI Stutter**: Running 4 workers while the user is actively scrolling the gallery induces noticeable UI jank.
85. **Battery Scale Assumptions**: Assuming `BatteryManager.EXTRA_SCALE` is always 100 leads to incorrect battery percentage calculations on custom Android skins.
86. **Rapid Battery Drop Under 15%**: Failure to immediately halt worker execution when battery drops to 14% can accelerate device shutdown.
87. **Lack of Network Unmetered Checks**: If models or auxiliary assets are downloaded, absence of unmetered network checks risks user cellular data fees.
88. **Continuous Polling Wakeups**: Polling the database for pending jobs every 2 seconds wastes power while the app is idle.
89. **Audio / Media Playback Interruption**: Heavy background CPU load causing audio stutters during concurrent music playback.
90. **Missing Process Death Survival**: Resuming app after Android kills the process fails to restore the user's active filter states.

---

## VII. Security, Privacy & Data Integrity (Bugs 91–105)

91. **Unencrypted SQLite Storage**: Extracted Wi-Fi passwords, tax IDs, credit card numbers, and medical notes are stored in plaintext SQLite on disk.
92. **Clipboard Exposure Without Auto-Clear**: Copied Wi-Fi passwords and sensitive data persist in the system clipboard and clipboard history indefinitely.
93. **Debug Logcat Exposure**: Logging full recognized text strings in logcat allows other apps or USB debugging to capture private gallery text.
94. **Insecure Sharesheet File Sharing**: Sharing an image without `FileProvider` or granting temporary `FLAG_GRANT_READ_URI_PERMISSION` fails or leaks file paths.
95. **Unencrypted Cloud Backup Inclusion**: Default `backup_rules.xml` may include the OCR database in automated Google Drive device backups.
96. **Lack of Biometric Gate**: Anyone with physical access to an unlocked phone can search through private photos, receipts, and sensitive documents.
97. **Permanent File Path Assumptions**: Caching absolute Linux file paths fails when scoped storage volumes are migrated or remounted.
98. **World-Readable Cache Storage**: Saving sample cards or temporary scaled bitmaps to external cache leaves them accessible to other apps.
99. **Runtime Permission Revocation Crash**: Revoking photo permission while the app is backgrounded causes a crash on foregrounding.
100. **Unchecked Disk Space Prior to Indexing**: Indexing 100K images without verifying free storage can exhaust device disk space, corrupting system apps.
101. **Exact Hash Collision on Weak Hashes**: Using a 32-bit hash or truncated key can falsely treat different images as identical, skipping OCR.
102. **Sensitive Album Exclusion Missing**: Inability to blacklist specific folders (e.g. WhatsApp Private, Secure Folder) from automated scanning.
103. **Missing Database Integrity Verification**: Sudden battery death during a write transaction can corrupt the SQLite database without auto-recovery.
104. **Raw SQLite Query Injection Risk**: Raw dynamic queries with unescaped user strings could allow SQLite injection.
105. **Dangling Bounding Boxes on Asset Replacement**: If an image is modified in-place by a photo editor, old OCR blocks and text remain attached until manual re-scan.
