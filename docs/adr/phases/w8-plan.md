# W8 — IAM Token Exchange + Airport-Service Bootstrap

## Karar (üç AI'ın ortak sonucu — DeepSeek, ChatGPT, Claude)

K4.5 ADR'si korunuyor ("downstream servisler local validate eder, her
request'te iam-service'e sormaz") ama netleştiriliyor:

**Downstream servisler Keycloak'ın ham token'ını değil, iam-service'in
kendi imzaladığı bir "IAM Application Token"ı local validate edecek.**

Elenen alternatifler ve gerekçesi:
- **airport-service'in `iam` schema'sına doğrudan (read-only bile olsa)
  bağlanması:** Reddedildi. "Servisler birbirinin tablosuna bakmaz"
  prensibini ihlal eder, IAM şemasındaki her değişiklik airport-service'i
  kırılgan hale getirir, service boundary öğrenme hedefini boşa çıkarır.
- **airport-service'in her request'te iam-service'e HTTP ile sorması:**
  K4.5'te zaten reddedilmişti (single point of failure, latency,
  mikroservis bağımsızlığının azalması). Bu endişeler W8'in küçük
  ölçeğinde de geçerli, "önemsiz" sayılmayacak.

Token Exchange'in sanıldığı kadar büyük bir iş olmadığı üç AI tarafından
da teyit edildi: Keycloak JWKS zaten var, `TenantContextResolver` zaten
IAM DB sorgusunu yapıyor, Spring Security + Nimbus JOSE JWT zaten
projede. Yapılacak iş yeni bir altyapı değil, mevcut parçaların
birleştirilmesi.

## Faz Bölünmesi

W8 ikiye ayrılıyor, her biri bağımsız test edilebilir:

```
W8A — IAM Token Exchange Altyapısı (iam-service içinde)
W8B — Airport-Service Bootstrap + İlk Korumalı Endpoint
```

W8B, W8A tamamlanmadan başlamaz — bu bir bağımlılık, sıra önemli.

---

# W8A — IAM Token Exchange Altyapısı

## Amaç

iam-service, Keycloak token'ını alıp kendi imzaladığı, tenant context'i
(organizationId, roles, permissions) içeren bir JWT üretsin ve bunu JWKS
ile yayınlasın. Bu iş sadece iam-service içinde kalıyor, airport-service'e
hiç dokunmuyor.

## Backend Değişiklikleri

**1. RSA keypair üretimi**

Bir kerelik, local dev için: `keytool` veya bir init script ile RSA
keypair oluştur. Private key iam-service'in config'inde (env variable
veya local dosya, repo'ya asla commit edilmez), public key JWKS
endpoint'inden yayınlanacak.

```
app.iam-token.private-key-path=${IAM_TOKEN_PRIVATE_KEY_PATH:}
app.iam-token.key-id=${IAM_TOKEN_KEY_ID:iam-key-1}
```

**2. IAM JWT claim yapısı**

```json
{
  "iss": "airport-ops-iam",
  "sub": "<iam-user-id>",
  "keycloakSub": "<keycloak-subject>",
  "email": "user@example.com",
  "workspace": "TENANT",
  "organizationId": "<uuid>",
  "organizationStatus": "ACTIVE",
  "roles": ["AIRLINE_ADMIN"],
  "permissions": ["station:create", "station:read", "..."],
  "tokenScope": "TENANT_APP",
  "aud": ["airport-service"],
  "iat": ...,
  "exp": ...
}
```

Platform admin için `workspace: "PLATFORM"`, `organizationId: null`,
platform permission'ları — aynı token yapısı, farklı içerik (mevcut
`/auth/me` response'undaki ayrımla tutarlı).

**3. Token üretim endpoint'i**

```
POST /auth/iam-token
Header: Authorization: Bearer <keycloak-access-token>
```

Akış:
- Keycloak token'ı doğrulanır (zaten mevcut Resource Server config'i kullanır)
- email/sub üzerinden `TenantContextResolver` ile tenant context çekilir
- Yukarıdaki claim yapısıyla IAM JWT üretilir, private key ile imzalanır
- Response: `{ "iamAccessToken": "...", "expiresIn": 900 }`

Token ömrü kısa tutulmalı (örn. 15 dakika) — rol/permission değişikliği
sonrası eski token'ın uzun süre geçerli kalmaması için (K4.5'te tartışılan
"stale permission" riskinin mitigasyonu).

**4. JWKS endpoint**

```
GET /.well-known/jwks.json
```

Public key'i JWK formatında yayınlar. airport-service (ve ileride
flight-service, report-service, audit-service) bu endpoint'i kullanarak
token doğrulayacak.

**5. Frontend'e ne değişiyor?**

Şimdilik hiçbir şey. Frontend hâlâ session cookie ile çalışıyor,
`/auth/me` hâlâ aynı şekilde çağrılıyor. IAM JWT, servisler-arası
(service-to-service context taşıma) bir mekanizma — frontend'in bundan
haberi olmayacak. (İleride frontend'in bu token'ı airport-service'e
giden isteklerde kullanıp kullanmayacağı ayrı bir karar — W8B'de
netleşecek.)

## W8A Kapsamı Dışında Kalanlar

- Key rotation (tek key ile başla, rotation ayrı bir faz)
- Token revocation/blacklist
- Refresh token akışına IAM JWT entegrasyonu (şimdilik ayrı, kısa ömürlü
  token yeterli)
- airport-service veya başka bir servisin bu token'ı kullanması (W8B'nin işi)

## W8A Test Planı

```
- Geçerli Keycloak token ile POST /auth/iam-token → 200, IAM JWT döner
- Dönen IAM JWT jwt.io'da (veya test kodunda) decode edilince doğru
  claim'ler var mı (organizationId, roles, permissions doğru)
- Platform admin için workspace=PLATFORM, organizationId=null doğru mu
- GET /.well-known/jwks.json → public key JWK formatında dönüyor
- Dönen IAM JWT, JWKS'teki public key ile signature doğrulanabiliyor mu
  (test kodunda Nimbus ile manuel doğrulama)
- Geçersiz/expired Keycloak token ile /auth/iam-token → 401
- Rol/permission değişikliği sonrası yeni alınan IAM JWT güncel bilgiyi
  yansıtıyor mu (eski token'a dokunulmuyor, sadece yeni token doğru)
```

---

# W8B — Airport-Service Bootstrap

## Amaç

airport-service'i gerçek bir Spring Boot modülü olarak hayata geçirmek,
W8A'da üretilen IAM JWT'yi kullanarak ilk korumalı endpoint'i (station
create) çalıştırmak.

## Backend — Yeni Modül

```
airport-service/
├── build.gradle
├── src/main/java/.../airport_service/
│   ├── config/
│   │   └── SecurityConfig.java   -- IAM JWKS ile Resource Server
│   ├── station/
│   │   ├── StationController.java
│   │   ├── StationService.java
│   │   ├── StationEntity.java
│   │   └── StationRepository.java
│   └── security/
│       └── IamJwtAuthenticationConverter.java
└── src/main/resources/
    └── db/migration/
        └── V1__create_stations_and_gates.sql  (airport schema)
```

**SecurityConfig:**
```java
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=
  http://iam-service:8081/.well-known/jwks.json
```

**IamJwtAuthenticationConverter:** IAM JWT'sindeki `organizationId`,
`roles`, `permissions` claim'lerini okuyup Spring Security
`GrantedAuthority`'lerine çevirir — iam-service'teki mevcut pattern'in
(K4'te kurulan) aynısı, farklı bir servise taşınmış hali.

**Docker Compose:** yeni bir servis girişi (`airport-service`, ayrı port,
`airport` schema'sına bağlantı, `IAM_JWKS_URI` env variable ile
iam-service'e işaret eder). airport-service frontend'e hiç açılmaz
(sadece Docker network içinde iam-service'ten erişilebilir).

## iam-service'te Proxy Katmanı (yeni parça)

```
GET/POST/PUT  /app/stations/**
```

iam-service'te yeni bir controller/filter — session cookie'yi doğrular,
IAM JWT üretir (W8A'daki mekanizma), isteği airport-service'e
`Authorization: Bearer <IAM-JWT>` ile forward eder, cevabı aynen döner.
Bu katman **sadece** proxy — hiçbir business logic içermez.

## İlk Domain Endpoint — Station Create (airport-service içinde)

```
POST /organizations/{orgId}/stations
```

Bu endpoint airport-service'te yaşıyor, frontend'in gördüğü şey
`/app/stations` (iam-service proxy'si). airport-service bu path'i
sadece iam-service'ten (forward edilmiş haliyle) görür.

Aynı iki katmanlı kontrol (W7'deki `TenantMemberAccessGuard` pattern'i
tekrar kullanılacak, ama bu sefer IAM JWT claim'lerinden okuyarak):

1. IAM JWT'den organizationId + roles çıkar
2. `station:create` permission var mı kontrol et
3. Path'teki `{orgId}` ile token'daki organizationId eşleşiyor mu
   (TENANT_MISMATCH kontrolü — W7'de test ettiğimiz pattern'in aynısı)

Request:
```json
{ "stationName": "SAW Station", "airportCode": "SAW", "gateCount": 8 }
```

`organizationId` request body'sinde YOK.

## Frontend Köprüsü Kararı (üç AI'ın ortak sonucu — kesinleşti)

**Yaklaşım A: iam-service, browser-facing BFF/proxy rolünü minimal
şekilde üstlenir.**

Frontend hiçbir zaman IAM JWT'yi görmez, tutmaz, airport-service'in
varlığından haberdar olmaz. Akış:

```
Frontend → (session cookie ile, her zamanki gibi) → iam-service
iam-service → session'dan kullanıcıyı tanır
iam-service → kendi IAM JWT'sini üretir (W8A'daki mekanizma)
iam-service → isteği airport-service'e Authorization: Bearer <IAM-JWT>
              ile forward eder
airport-service → token'ı local validate eder, permission/tenant
                   kararını KENDİSİ verir
iam-service → airport-service'in cevabını frontend'e aynen döner
```

**Neden bu karar doğru (üç AI'ın gerekçesi):**

- W2A'dan beri korunan "frontend JWT görmez/tutmaz/decode etmez"
  prensibini sıfır tavizle korur. Yaklaşım B (frontend'in IAM JWT'yi
  memory'de taşıması) bu prensibi kısmen yumuşatır — XSS senaryosunda
  token'ın JS'den erişilebilir hale gelmesi riski, "sadece memory'de"
  dense bile gerçek bir güvenlik downgrade'idir.
- K4.5'te reddedilen "her request'te iam-service'e sor" pattern'iyle
  KARIŞTIRILMAMALI: K4.5'te reddedilen şey, airport-service'in
  authorization KARARINI iam-service'e outsource etmesiydi. Burada
  iam-service authorization kararı vermiyor, sadece bir token'ı
  proxy'liyor — karar hâlâ airport-service'in kendi JWT validation'ında
  kalıyor. Bu ayrım kritik ve üç AI tarafından da teyit edildi.
- Yaklaşım C (ayrı API Gateway) mimari olarak en temiz uzun vadeli hedef
  ama roadmap'te zaten "MVP'de yapılmayacaklar" listesinde — şimdi
  kurmak scope'u gereksiz büyütür. Yaklaşım A'dan C'ye geçiş ileride
  (3-4 servis birikince) büyük bir refactor gerektirmeyecek şekilde
  tasarlanabilir.

**Sınır çizgisi (ChatGPT'nin vurguladığı, kritik):**

iam-service'in proxy rolü SADECE şunları yapacak:
```
- session cookie doğrula
- tenant context çıkar (TenantContextResolver ile, zaten var)
- IAM JWT üret (W8A'daki endpoint/mekanizma)
- airport-service'e forward et
- response'u olduğu gibi frontend'e dön
```

iam-service'in KESİNLİKLE yapmayacağı şeyler:
```
- station validation
- station ownership logic
- airport domain kuralları
- herhangi bir business logic
```

Bu çizgi bozulursa iam-service fiilen bir "business gateway"e dönüşür,
ki bu hem sorumluluk sızıntısı hem de service boundary'nin ikinci kez
ihlali olur (ilkini Seçenek 1'de -direkt DB okuma- reddetmiştik).

**Ölçekleme notu:** flight-service, report-service, audit-service
eklendiğinde (W10, W13, W14) aynı proxy pattern'i iam-service içinde
genişleyecek (`/app/flights/**`, `/app/reports/**` gibi yeni route'lar).
Eğer bu proxy katmanı büyür ve iam-service'in ana sorumluluğunu
gölgelemeye başlarsa, o noktada (muhtemelen W12+ civarı) ayrı bir
`bff-service` veya gateway'e çıkarılması değerlendirilecek — bu şimdiden
bir ADR notu olarak düşülüyor, W8'de yapılmayacak.

## W8B Kapsamı Dışında Kalanlar

- Gate management (station'dan sonra ayrı bir alt-faz, W9'a bırakıldı
  zaten roadmap'te)
- flight-service'in airport-service'i çağırması (bu, W10'da gündeme
  gelecek, o zaman service-to-service IAM JWT kullanımı ikinci kez
  test edilmiş olacak)
- Frontend'in station create formu (backend önce, frontend sonra —
  istenirse ayrı bir alt-adım)

## W8B Test Planı

```
- IAM JWT olmadan (sadece Keycloak token ile) airport-service'e istek
  → 401 (airport-service Keycloak token'ı tanımıyor, sadece IAM JWT'yi
  tanıyor olmalı — bu ayrımı test et)
- Geçerli IAM JWT + station:create permission → 201, station oluşuyor
- Geçerli IAM JWT ama station:create permission yok (örn. VIEWER) → 403
- IAM JWT'deki organizationId ile path'teki orgId uyuşmuyor → 403
  TENANT_MISMATCH (W7'deki aynı pattern)
- organizationId request body'de gönderilirse yok sayılıyor mu (mass
  assignment koruması, W7'deki gibi)
- Cross-tenant: Org A'nın IAM JWT'si ile Org B'nin path'ine istek → 403
```

---

## Genel Sıra

```
1. W8A implementasyonu (iam-service içinde, izole, mevcut testleri
   bozmadan) — IAM JWT üretimi + JWKS endpoint
2. W8A test + smoke (JWKS + IAM JWT üretimi manuel Postman ile doğrulanır)
3. W8B implementasyonu (yeni airport-service modülü, IAM JWKS ile
   Resource Server, station create endpoint)
4. iam-service'te proxy katmanı (/app/stations/** → airport-service,
   Yaklaşım A) — bu katman SADECE forward yapar, business logic içermez
5. W8B test + smoke (station create uçtan uca, cross-tenant dahil,
   frontend'in hiçbir zaman IAM JWT görmediği doğrulanır)
```

Bu plan tamamlandığında, flight-service/report-service/audit-service için
(W10, W13, W14) aynı pattern (IAM JWT + JWKS + local validation) tekrar
kullanılacak — W8, sonraki tüm servisler için şablon oluşturuyor.