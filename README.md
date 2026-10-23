# Gameınterpreter — Android V0.1

Android / PS5 / Xbox oyun çeviri yardımcısı. Bu ZIP **kaynak projedir**, APK değildir.

## İşlevler
- CameraX kamera önizlemesi + ML Kit İngilizce OCR + İngilizce→Türkçe cihaz içi çeviri
- Android SpeechRecognizer ile İngilizce konuşma tanıma (cihaz/servis koşullarına bağlı)
- Türkçe Android TextToSpeech, aç/kapat
- Kamera ve mikrofon izinleri çalışma anında istenir
- Dil modeli ilk çalıştırmada indirilir, internet gerekir

## Windows CMD ile GitHub'a yükleme
1. ZIP dosyasını açın; klasör içinde CMD açın.
2. GitHub'da **boş** bir repo oluşturun (README eklemeyin).
3. Şu komutları uygulayın:

```bat
git init
git branch -M main
git add .
git commit -m "Gameinterpreter V0.1 initial Android build"
git remote add origin https://github.com/KULLANICI_ADINIZ/Gameinterpreter.git
git push -u origin main
```

4. GitHub > Actions > Android APK > Run workflow (veya push sonrası otomatik çalışır).
5. Başarılı işlem sonrası Actions > ilgili run > Artifacts > Gameinterpreter-V0.1-debug-apk dosyasını indirin.

## Sınırlamalar
- Test sürümüdür; donanım üzerinde derlenip doğrulanmamıştır.
- Mikrofon modu sistem konuşma tanıyıcısının parça parça sonuçlarına dayanır, kesintisiz eşzamanlı tercüme değildir.
- Android konuşma tanıma servisi bazı cihazlarda ağ gerektirebilir; tamamen offline olduğu garanti edilmez.
- TTS oynatımı sırasında dinleme geçici duraklar; tekrar yakalamayı azaltır.
- OCR şu an İngilizce Latin yazısını hedefler; ekran parlaması, hareket ve yazı büyüklüğü etkilidir.
- Arka planda çalışma, Bluetooth doğrudan konsol sesi, konuşmacı ayrımı ve overlay henüz yoktur.
- Google Play sürümü için yayın öncesi testler, gizlilik politikası ve release imzalama gerekir.
