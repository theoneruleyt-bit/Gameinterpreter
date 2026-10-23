# Gameınterpreter Android V0.4 – development package

Android Java / CameraX / ML Kit OCR + EN→TR translation / Android SpeechRecognizer / TTS / Google Play Billing.

### Improvements from V0.3
- Price copy corrected to $5.99 monthly, $69.99 yearly, $99.99 lifetime.
- Trial starts at first mode launch, rather than at initial application startup. **Local demo only; not secure**.
- Four-entry in-session translation history, stale translation guard, recognition retry backoff and simple repetition filtering.
- Updated Android version code and GitHub Actions artifact name.

### Build
Extract into your GitHub repository, then `git add . && git commit -m "Gameinterpreter V0.4" && git push origin main`. GitHub Actions builds a DEBUG APK.

### Important
This is a development/test APK source package. No server has been provisioned; premium purchases intentionally DO NOT unlock paid functionality. This cannot be sold as a protected paid product until the steps in `SECURITY_AND_RELEASE.md` are implemented and tested.
