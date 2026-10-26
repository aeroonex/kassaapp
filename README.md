# Kassaapp — kassir telefonlari uchun kiosk + monitoring ilovasi

Poco C85 (300 dona) telefonlarni doimiy "kassa" rejimiga o'tkazadigan va
ekranini monitoring serveriga yuboradigan Android ilova.

## Qanday ishlaydi

- **Device Owner + kiosk (lock task) rejimi.** Flashing/custom firmware
  yo'q. Ilova Device Owner sifatida o'rnatiladi, keyin o'zini kiosk
  (lock task) rejimida pin qiladi — kassir boshqa ilovaga, home screen'ga
  yoki settings'ga chiqa olmaydi.
- **Qayta yuklanganda avtomatik ishga tushish.** `BootReceiver`
  `BOOT_COMPLETED` signalini ushlab, ilovani darhol qayta ochadi. Device
  Owner sifatida bu ilova doimiy home/launcher qilib belgilanadi
  (`MyDeviceAdminReceiver.applyKioskPolicy`).
- **O'z ekranini monitoring qilish.** `ScreenshotUploadService` har 5
  soniyada ilovaning o'z oynasini (`PixelCopy`) suratga oladi va serverga
  yuklaydi. Bu boshqa ilovalarni emas, faqat shu kassir ilovasining o'z
  ekranini suratga oladi — shu sababli `MediaProjection` ruxsat oynasi
  kerak emas. Doimiy bildirishnoma kassirga monitoring yoqilganini
  ko'rsatib turadi.

## Serverga yuklash formati

`POST {server_url}/api/devices/{device_id}/screenshots` — multipart form:
- `device_id` — qurilma identifikatori
- `timestamp` — millisekundlarda
- `screenshot` — JPEG fayl

Server va boshqaruv paneli (barcha 300 qurilmani ko'rish) alohida loyiha
sifatida qo'shiladi — bu repo hozircha faqat Android ilovani o'z ichiga
oladi.

## Device Owner sifatida o'rnatish (bitta telefon uchun)

Har bir telefon birinchi marta sozlanishda (factory reset holatda,
hech qanday Google akkaunt qo'shilmagan bo'lishi kerak):

```bash
adb install app-release.apk
adb shell dpm set-device-owner com.aeroonex.kassaapp/.MyDeviceAdminReceiver
```

300 telefonni qo'lda sozlash o'rniga quyidagilar tavsiya etiladi:
- **QR-kod provisioning** — `android.app.extra.PROVISIONING_*` extralar
  bilan QR kod tayyorlanadi, har bir telefon factory reset ekranida bir
  marta shu QR kodni skanerlaydi va o'zi Device Owner bo'lib, APK'ni
  yuklab, sozlanadi. Keyingi bosqichda shu QR generatorni qo'shamiz.
- Yoki `adb` orqali USB bilan ketma-ket (300 tasi uchun sekinroq, lekin
  oddiy).

## Hali ochiq savollar

1. **Skrinshotlarni qancha vaqt saqlash kerak** (server tomonida
   retention) — hali belgilanmagan.
2. **Wi-Fi yoki mobil internet** — bu tanlov upload chastotasi/siqish
   sifatiga ta'sir qiladi; hozircha JPEG 60% sifat bilan yuborilmoqda,
   kerak bo'lsa moslashtiramiz.
3. Monitoring serverining haqiqiy manzili (`ApiConfig.DEFAULT_SERVER_URL`
   hozircha placeholder).

## Keyingi qadamlar

- Haqiqiy kassir UI (hozir faqat status matni ko'rsatiladi).
- Monitoring server + web panel (boshqa repo/loyihada).
- QR-kod orqali ommaviy provisioning skripti.
