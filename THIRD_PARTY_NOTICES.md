# Third-party notices

The card text recognition integration uses these open-source components:

- [Tesseract4Android 4.9.0](https://github.com/adaptech-cz/Tesseract4Android), licensed under Apache-2.0.
- The Simplified Chinese `chi_sim` model from [tesseract-ocr/tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast), licensed under Apache-2.0. It is bundled in the APK and copied into private app storage when OCR is first used.
- [OpenCV 4.12.0](https://opencv.org/), licensed under Apache-2.0, for card boundary detection and perspective correction.
- [AndroidX CameraX](https://developer.android.com/jetpack/androidx/releases/camera), for the in-app camera preview and capture flow.
- Tesseract OCR and native dependencies bundled by Tesseract4Android retain their upstream licenses; see the dependency list in the [Tesseract4Android README](https://github.com/adaptech-cz/Tesseract4Android#dependencies).

The card image and OCR model are processed on-device. The app does not send either to a server.
