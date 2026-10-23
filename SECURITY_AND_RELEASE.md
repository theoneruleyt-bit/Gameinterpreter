# Gameınterpreter V0.4 – Security and release checklist

## What is implemented
- Play Billing product query and purchase UI (no premium unlock without verification).
- 180-second *local demo* gate starts at first mode launch. It is deliberately **not** claimed secure: app data or reinstall can reset it.
- Play Billing purchase callbacks do not grant premium access.
- TLS-only Android network configuration; no HTTP cleartext.
- Recognition backoff, short/noise transcript filtering, stale translation result guard, and a small in-session history.

## REQUIRED BEFORE PUBLIC RELEASE
1. Deploy an authenticated backend and implement server-side trial accounting with trusted server time. Never trust a device-only timer.
2. Use Google Play Developer API to verify subscription and in-app tokens server-side. Check active state, expiry, refunds and acknowledgement.
3. Integrate Play Integrity with a server-issued nonce and validate the verdict on backend. Do not rely on client-only checks.
4. Configure Real-time Developer Notifications for renewals, cancellations and refunds.
5. Enforce Premium feature authorization from server entitlement and decide short-lived offline grants.
6. Create product IDs gameinterpreter_monthly, gameinterpreter_yearly (SUBS), gameinterpreter_lifetime (INAPP) in Play Console.
7. Configure $5.99/month, $69.99/year, $99.99 lifetime and verify localized prices in Console.
8. Generate signed AAB, enable Play App Signing, publish privacy policy and Data Safety form, run internal testing.
9. Add instrumentation and device testing, accessibility and microphone/camera privacy disclosures.

## Translation limitations
Android SpeechRecognizer is not guaranteed to stream continuously. This build is not a production-grade simultaneous interpreter. On-device ML Kit translation may mishandle slang and context. TTS may pause microphone recognition.
