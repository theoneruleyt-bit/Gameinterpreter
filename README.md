# GameInterpreter Android V0.5 — Test source

Android phone companion for console gaming: camera OCR and English speech recognition, English-to-Turkish ML Kit translation, optional Turkish TTS. Console-independent in microphone/camera mode.

## V0.5 changes
- Corrected BillingClient 7.1.1 queryProductDetailsAsync callback (`List<ProductDetails>`).
- Compact Premium entry with separate dialog and visible high-contrast purchase buttons.
- TTS completion callback instead of approximate word-count timer to resume speech recognition.
- Reduced immediate duplicate recognized phrases; ignored partial results when mode is inactive.
- Stop active camera/microphone when app goes to background.
- Kept V0.4 local trial state; old trials are not silently renewed.
- Incremented versionCode/versionName and artifact label.

## Build
Push these files to `main`. GitHub Actions runs `gradle --no-daemon --stacktrace assembleDebug` and uploads a debug APK under Artifacts. This package has NOT been locally compiled or device-tested. Confirm CI is green before installing.

## Limitations
- This is a test build, not a Google Play release or a functional paid-license product.
- Purchases cannot grant Premium without a secure backend to verify Play tokens and entitlements. See SECURITY_AND_RELEASE.md.
- A local 3-minute trial is not secure and remains expired if it was started in V0.4.
- Direct console HDMI audio and PC capture streaming are not included; only Android microphone/camera inputs currently work.
- Speech recognition uses Android SpeechRecognizer, which is not guaranteed continuous or low-latency for every game.
