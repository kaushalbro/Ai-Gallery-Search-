# LensVault Privacy & Security Architecture

## The 100% Offline Promise

LensVault is architected with strict on-device isolation principles:

1. **Zero Network Calls for OCR or Search**: All image decodes, neural inferences, hash calculations, and text searches execute strictly on local CPU/NPU hardware.
2. **Zero Telemetry with User Data**: No filenames, image URIs, OCR text excerpts, or personal queries are ever logged or sent off-device.
3. **Sandbox Storage**: All OCR documents and FTS indices reside inside the app's private SQLite sandbox (`/data/user/0/com.aistudio.lensvault.kxvocr/databases/`).
4. **Non-Destructive Index Maintenance**: Purging local OCR databases clears recognized text and bounding boxes without deleting or modifying the original media files on the device.
