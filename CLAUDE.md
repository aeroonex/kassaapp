# Kassaapp

300 dona Poco C85 Android telefonni doimiy "kassa" rejimiga o'tkazadigan
va ularning ekranini markazlashtirilgan tarzda monitoring qiladigan
tizim. Ikki qism: `app/` (Android ilova) va `server/` (monitoring
server + web panel).

## Maqsad va asosiy qarorlar

- **Flashing/custom firmware yo'q.** Android Device Owner provisioning +
  lock task (kiosk) rejimi orqali telefon faqat shu ilovani ishlatadi.
- **Kassir ekranining o'zi ilova ichida.** Hozircha haqiqiy
  sotish/POS interfeysi yozilmagan — mahsulot/narx/chek talablari hali
  berilmagan (o'ylab topib yozilmagan, user'dan kelishini kutamiz).
- **Monitoring = ilovaning o'z ekranini skrinshot qilish.** Boshqa
  ilovalarni emas — faqat shu kassir ilovasining o'z oynasini
  (`PixelCopy`) suratga oladi, shu sababli `MediaProjection` ruxsat
  oynasi chiqmaydi. Kassirlar bu haqida xabardor, doimiy bildirishnoma
  bilan ko'rsatiladi.
- **Interval: 5 soniya, JPEG 60% sifat.**
- **Offline-safe.** Har bir surat avval telefon xotirasiga
  (`ScreenshotQueue`) yoziladi, keyin yuklanadi. Internet (Wi-Fi yoki
  mobil — ikkisi ham qo'llab-quvvatlanadi) yo'q bo'lsa navbatda qolaveradi
  (~24 soatlik zaxira), `ConnectivityManager` orqali internet qaytganda
  avtomatik, ketma-ket yuklanadi.
- **Saqlash muddati: 30 kun** (serverda avtomatik tozalanadi,
  `RETENTION_DAYS` bilan sozlanadi).
- **Xavfsizlik:** telefon-server ulanishi umumiy maxfiy token
  (`X-Upload-Token`) bilan himoyalangan; web panel login/parol bilan
  himoyalangan; HTTPS majburiy (`network_security_config.xml` faqat
  `localhost`/`10.0.2.2` uchun cleartext'ga ruxsat beradi — ya'ni haqiqiy
  server manzili albatta HTTPS bo'lishi kerak).

## Repo tuzilishi

```
app/                      Android ilova (Kotlin)
  MyDeviceAdminReceiver    Device Owner + kiosk siyosati
  BootReceiver             qayta yoqilganda avtomatik ishga tushirish
  MainActivity             kiosk'ga pin qilingan asosiy ekran (hozir placeholder)
  ScreenshotUploadService  har 5s skrinshot + yuklash
  ScreenshotQueue          offline navbat (diskka yozish)
  ApiConfig                server URL + upload token (SharedPreferences,
                           default qiymatlar placeholder — haqiqiy
                           server manzili va tokenidan keyin almashtiriladi)

server/                   Monitoring server + web panel (Node/Express)
  src/index.js             asosiy server
  src/routes/              auth, devices, screenshots API
  src/cleanup.js           30-kunlik avtomatik tozalash
  public/                  web panel (login, qurilmalar ro'yxati, tarix)
  k8s/                     Kubernetes manifestlari (alohida `kassaapp`
                           namespace, mavjud boshqa loyihalarga tegmaydi)
  Dockerfile, README.md    deploy qo'llanmasi

.github/workflows/server-image.yml   server/ o'zgarganda GHCR'ga Docker
                                      image avtomatik build+push qiladi
```

## Build/ishga tushirish

- Android: Gradle loyihasi, `minSdk 26`, Kotlin. Bu cloud sessiyada
  Android SDK yo'q — APK build qilish uchun Android Studio yoki SDK'li
  muhit kerak.
- Server: `cd server && npm install && npm start` (avval `.env.example`
  dan `.env` yaratib qiymatlarni to'ldirish kerak). Docker va
  Kubernetes orqali deploy qilish — `server/README.md` va
  `server/k8s/README.md`.

## Git

- Repo: `aeroonex/kassaapp`. Asosiy branch: `main`. Rivojlanish branchi:
  `claude/project-thread-3xglf7` — bu ikkisi hozircha bir xil holatda
  (repo boshida butunlay bo'sh edi, ikkisi ham shu ishdan boshlangan).
- Commit qilgandan keyin har doim ikkala branchga ham push qiling
  (`claude/project-thread-3xglf7` va `main`), toki ular sinxron tursin —
  bu repo hali boshqa branch strategiyasiga o'tmagan.

## Hali ochiq / qasddan qilinmagan

- Haqiqiy kassir (sotish) UI — mahsulot/narx/chek bosib chiqarish
  talablari berilmagan.
- 300 telefonni QR-kod orqali ommaviy Device Owner provisioning (hozir
  faqat bitta-bitta `adb shell dpm set-device-owner` yo'riqnomasi bor).
- Haqiqiy server domeni va upload-token — `ApiConfig.DEFAULT_SERVER_URL`
  / `DEFAULT_UPLOAD_TOKEN` hali placeholder, real qiymat
  ma'lum bo'lgach shu ikkisi almashtirilib APK qayta build qilinadi.
