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
- **Offline rejim.** Har bir surat avval telefon xotirasiga
  (`ScreenshotQueue`) yoziladi, keyin yuklashga urinadi. Internet
  (Wi-Fi yoki mobil) yo'q bo'lsa, suratlar navbatda qolaveradi (taxminan
  24 soatlik, keyin eng eskilari o'chiriladi). `ConnectivityManager`
  orqali internet qaytganini ushlab, navbatdagi barcha suratlarni
  ketma-ket, olingan tartibida serverga yuklaydi.

## Monitoring server + web panel

Shu repoda, [`server/`](server/) papkasida: skrinshotlarni qabul qiladi,
saqlaydi (saqlash muddati avtomatik tozalanadi) va barcha qurilmalarni
ko'rsatadigan web panelni beradi (login bilan himoyalangan). Ishga tushirish
va API tafsilotlari uchun [`server/README.md`](server/README.md)ga qarang.

`POST {server_url}/api/devices/{device_id}/screenshots` — multipart form,
`X-Upload-Token` header bilan:
- `device_id` — qurilma identifikatori
- `timestamp` — millisekundlarda
- `screenshot` — JPEG fayl

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

## Qabul qilingan qarorlar

- **Saqlash muddati: 30 kun** (`server/.env` dagi `RETENTION_DAYS` bilan
  o'zgartiriladi, standart shu).
- **Wi-Fi ham, mobil internet ham qo'llab-quvvatlanadi** — qaysi biri
  ishlatilishidan qat'i nazar, offline navbat (`ScreenshotQueue`) va
  internet qaytganda avtomatik yuklash ikkisida ham bir xil ishlaydi.
  JPEG 60% sifat hozircha ikkisiga ham mos (kichik fayl, ~10-40 KB/surat).
- Monitoring serverining haqiqiy manzili va upload-tokeni hali
  placeholder (`ApiConfig.DEFAULT_SERVER_URL` / `DEFAULT_UPLOAD_TOKEN`) —
  server joylashtirilgandan keyin shu ikkisini real qiymatlarga
  almashtirib, APK qayta build qilinadi.

## Keyingi qadamlar

- Haqiqiy kassir (sotish) interfeysi — hozir faqat status matni
  ko'rsatiladi; mahsulot/narx/chek bosib chiqarish talablari
  aniqlanmagani uchun hali yozilmagan.
- QR-kod orqali ommaviy provisioning skripti (300 telefonni birma-bir
  `adb` orqali sozlash o'rniga).
