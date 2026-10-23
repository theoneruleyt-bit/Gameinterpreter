# V0.5 Phone QA

1. Install APK built by GitHub Actions; check startup and screen fit.
2. Verify two main mode buttons have visible text, Premium appears as a compact card.
3. Tap Premium > confirm three visible plans and restore; do not buy until backend verified.
4. Start Canlı Dinle with English game audio; verify EN recognition and Turkish translation.
5. Enable Türkçe seslendirme; verify microphone resumes after Turkish audio completes.
6. Switch to Kamera and verify English screen text is recognized and translated.
7. Press stop; verify camera/microphone stops. Background app; verify no recording continues.
8. Test denied permissions, airplane mode, no speech, rapid mode switching.
9. Confirm trial does not restart after force-stop/reopen. Note that trial is a local demo only.
10. Inspect logs for errors, and compare latency and repetitions to V0.4.
