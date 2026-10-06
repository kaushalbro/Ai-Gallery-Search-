# LensVault Background Indexing & Queue Supervisor

LensVault implements a durable, prioritized background indexing queue designed to process up to 100,000 images seamlessly without battery drain or device overheating.

## Priority Ordering

Jobs are dispatched according to real-world utility:
1. **Priority 100 (Explicit)**: User taps an unindexed image to view immediately.
2. **Priority 80 (Screenshots)**: Screenshots are most likely to contain searchable text (receipts, Wi-Fi codes, error logs, flight passes).
3. **Priority 60 (Recent Photos)**: Photos taken in the past 7 days.
4. **Priority 20 (Backlog)**: Older photos in the gallery.

## Adaptive Concurrency Modes

| Mode | Max Workers | Policy |
|---|---|---|
| **Fast** | 4 Workers | Max CPU throughput for initial fast library scan. |
| **Balanced** | 2 Workers | Default balanced configuration for day-to-day use. |
| **Battery Saver** | 1 Worker | Serial single-threaded processing for low power draw. |
| **Charging Only** | 2-3 Workers | Pauses during battery discharge; resumes automatically when connected to AC power. |

## Thermal and Battery Supervision

- **Battery < 15%**: Automatic throttling to single worker or pause unless charging.
- **Thermal Status Tracking**: Reacts to device thermal broadcasts to prevent throttling.
- **Resumability**: Unfinished batches resume exactly where they left off on next app start.
