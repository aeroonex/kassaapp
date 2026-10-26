# Kassaapp monitoring server

300 kassir telefonidan kelgan skrinshotlarni qabul qiladi, saqlaydi va
web panel orqali ko'rsatadi.

## Ishga tushirish (lokal)

```bash
cd server
cp .env.example .env   # ADMIN_PASSWORD, SESSION_SECRET, UPLOAD_TOKEN'ni almashtiring
npm install
npm start
```

Panel: `http://localhost:3000/login.html`

## Docker bilan ishga tushirish

```bash
cd server
docker build -t kassaapp-server .
docker run -d --name kassaapp-server \
  -p 3000:3000 \
  --env-file .env \
  -v kassaapp_data:/app/data \
  kassaapp-server
```

## API (Android ilova shu bilan gaplashadi)

`POST /api/devices/:deviceId/screenshots`
- Header: `X-Upload-Token: <UPLOAD_TOKEN>`
- Multipart form: `device_id`, `timestamp` (millisekund), `screenshot` (JPEG)

Android tomonida bu manzil `ApiConfig.setServerUrl` / `ApiConfig.setUploadToken`
orqali sozlanadi — ikkisi ham `.env`dagi qiymatlarga mos kelishi kerak.

## Saqlash muddati

`.env` dagi `RETENTION_DAYS` (standart: 30 kun) dan eski skrinshotlar har
soatda avtomatik o'chiriladi (`src/cleanup.js`).

## Web panel

- `/index.html` — barcha qurilmalar ro'yxati, online/offline holati, oxirgi
  skrinshot, har 5 soniyada avtomatik yangilanadi.
- `/device.html?id=<deviceId>` — bitta qurilmaning jonli ekrani va tarixi.

Kirish uchun login/parol `.env` dagi `ADMIN_USER` / `ADMIN_PASSWORD`.
Hozircha bitta admin hisobi bor; ko'proq xodim kirishi kerak bo'lsa,
`devices` jadvaliga o'xshash `users` jadvali qo'shish kerak bo'ladi.
