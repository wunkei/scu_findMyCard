# Third-party notices

The card text recognition integration uses these open-source components:

- [Tesseract4Android 4.9.0](https://github.com/adaptech-cz/Tesseract4Android), licensed under Apache-2.0.
- The Simplified Chinese `chi_sim` model from [tesseract-ocr/tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast), licensed under Apache-2.0. The app downloads this model on first use and keeps it in private app storage.
- Tesseract OCR and native dependencies bundled by Tesseract4Android retain their upstream licenses; see the dependency list in the [Tesseract4Android README](https://github.com/adaptech-cz/Tesseract4Android#dependencies).

The card image is processed on-device and is not sent to the model host. The network request downloads only the OCR model.
