# iOS Implementation & Porting Reference

When deploying or maintaining the iOS target:
- **Photo Library**: Use `PhotoKit` (`PHPhotoLibrary`, `PHAsset`) with limited access support.
- **OCR Engine**: ONNX Runtime with CoreML Execution Provider.
- **Background Tasks**: `BGAppRefreshTask` / `BGProcessingTask` with power/charging constraints.
- **SQLite Engine**: SQLite3 with FTS5 virtual table support compiled with `unicode61`.
