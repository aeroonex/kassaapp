# Kubernetes'ga joylashtirish

Bu manifestlar serverni mavjud klasterga **alohida `kassaapp` namespace**
ichida, boshqa loyihalarga tegmay qo'shadi.

## 1. Image tayyorlash

`main` branchga `server/` papkasidagi o'zgarish push qilinganda,
`.github/workflows/server-image.yml` imageni avtomatik build qilib
`ghcr.io/aeroonex/kassaapp-server:latest` ga yuklaydi (GitHub Container
Registry, repo bilan birga keladi — alohida akkaunt/registry kerak
emas). Klaster image'ni GHCR'dan tortib ola olishi kerak; agar GHCR
package public bo'lmasa, klasterga `imagePullSecrets` qo'shish kerak
bo'ladi — shuni ayting, shu qismni qo'shib beraman.

Agar GitHub Actions ishlatmaslik kerak bo'lsa, image'ni qo'lda ham
build qilib boshqa registry'ga yuklash mumkin:

```bash
cd server
docker build -t <registry>/kassaapp-server:latest .
docker push <registry>/kassaapp-server:latest
```

va `deployment.yaml` dagi `image:` qatorini shunga moslang.

## 2. Sirlar (secrets)

```bash
cp secret.example.yaml secret.yaml
# secret.yaml ichidagi CHANGE-ME qiymatlarni almashtiring
kubectl apply -f secret.yaml
```

## 3. Qolganini joylashtirish

```bash
kubectl apply -f namespace.yaml
kubectl apply -f pvc.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f ingress.yaml   # avval host va cert-manager issuer'ni moslang
```

`ingress.yaml` nginx ingress controller + cert-manager bor deb hisoblab
yozilgan (eng ko'p tarqalgan sozlash). Klasterda boshqacha bo'lsa
(masalan Traefik, boshqa cert-manager issuer nomi, yoki ingress
umuman yo'q — shu holda `service.yaml`ni `LoadBalancer` qilish kerak
bo'ladi), ayting — shu faylni moslashtiraman.

## 4. Tekshirish

```bash
kubectl -n kassaapp get pods,svc,ingress
kubectl -n kassaapp logs deploy/kassaapp-server
```

Panel: `https://kassa.<domeningiz>.uz/login.html`

## Android ilovaga ta'siri

Ingress'dagi host (masalan `kassa.aeroonex.uz`) tayin bo'lgach, shu
manzil `ApiConfig.DEFAULT_SERVER_URL`ga va `secret.yaml`dagi
`UPLOAD_TOKEN` `ApiConfig.DEFAULT_UPLOAD_TOKEN`ga yoziladi — shundan
keyin APK qaysi telefonga o'rnatilsa ham, qo'lda sozlashsiz, avtomatik
shu serverga ulanadi.
