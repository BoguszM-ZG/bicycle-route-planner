# 🅰️ Bicycle Route Planner — Frontend Client

[![Frontend CI](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/frontend.yml/badge.svg)](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/frontend.yml)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-blue?logo=typescript)](https://www.typescriptlang.org/)
[![Angular](https://img.shields.io/badge/Angular-22-red?logo=angular)](https://angular.dev/)
[![Leaflet](https://img.shields.io/badge/Leaflet-1.9.4-green?logo=leaflet)](https://leafletjs.com/)
[![Nginx](https://img.shields.io/badge/Nginx-Non--Root-2496ED?logo=nginx)](https://nginx.org/)

Aplikacja kliencka (SPA) systemu **Bicycle Route Planner** w **Angular 22** i **TypeScript**. Zawiera mapę **Leaflet** z rysowaniem stref (**Leaflet.draw**), stan aplikacji w sygnałach Angulara, wykres profilu wysokości (**Chart.js**), interfejs w języku polskim i angielskim (**ngx-translate**) oraz komunikację z REST API z uwierzytelnianiem JWT.

---

## 🏛️ Struktura i Wzorce

```text
src
├── environments/        # Adres API i poziom logowania (deweloperski / produkcyjny)
└── app/
    ├── components/
    │   ├── login/, register/    # Logowanie i rejestracja
    │   ├── map/                 # Kontener mapy Leaflet i panel wyboru trybu (trasa / pętla / strefa)
    │   ├── map-controls/        # Przyciski pod mapą: „Gdzie jestem?”, „Zapisz”, „Wyczyść”
    │   ├── map-search/          # Wyszukiwarka adresów z podpowiedziami
    │   ├── sidebar/             # Panel boczny: profil, trasy i foldery, szczegóły trasy, wykres, strefy
    │   ├── save-route-modal/    # Zapis trasy z wyborem folderu
    │   ├── save-zone-modal/     # Zapis narysowanej strefy omijanej
    │   └── toast/               # Powiadomienia (NotificationService)
    ├── core/errors/     # GlobalErrorHandler – przechwytuje nieobsłużone wyjątki i przekazuje je do telemetrii
    ├── guards/          # authGuard (mapa tylko dla zalogowanych), guestGuard (logowanie tylko dla niezalogowanych)
    ├── interceptors/    # Dołączanie tokenu JWT i odświeżanie go po odpowiedzi 401
    ├── models/          # Interfejsy danych (trasy, foldery, strefy, ORS)
    ├── constants/       # Stałe mapy (kafelki, ikony, wyszukiwarka)
    └── services/        # Serwisy HTTP, stanu (*StateService) i mapy (Map*Service)
```

### Przyjęte zasady:
- **Angular 22:** komponenty standalone, sygnały (`signal()`, `computed()`, `input()`, `output()`), składnia `@if` / `@for`, tryb bez Zone.js, `ChangeDetectionStrategy.OnPush`.
- **Podział serwisów:**
  - **serwisy HTTP** (`RouteService`, `FolderService`, `AvoidanceZoneService`, `OrsService`, `GeocodingService`) tylko komunikują się z backendem;
  - **serwisy stanu** (`*StateService`) trzymają dane w prywatnych sygnałach i udostępniają je komponentom tylko do odczytu (`asReadonly()`).
- **Logowanie i telemetria:** błędy po stronie użytkownika (np. 400, 404, 409) logowane są w konsoli na poziomie `debug`. Do backendu (`/api/telemetry/logs`) trafiają tylko logi poziomu `error` i tylko pola potrzebne do diagnozy (np. status HTTP i typ błędu).
- **Bez zbędnych żądań:** zmiana nazwy trasy lub folderu na identyczną nie wysyła zapytania; wyszukiwarka czeka 350 ms po ostatnim znaku i wymaga min. 3 znaków.

---

## 🗺️ Moduł Mapowy (Leaflet)

### 1. Mapa i trasy (`MapEngineService`, `MapRouteLayerService`, `RoutePlanningStateService`)
- Kafelki OpenStreetMap, przelot do wybranego miejsca, znacznik lokalizacji użytkownika.
- Dodawanie punktów trasy kliknięciem, przeciąganie i usuwanie ich (menu kontekstowe lub przycisk w dymku); trasa jest wyznaczana ponownie po każdej zmianie.
- Tryb pętli: kliknięcie wyznacza trasę okrężną o długości wybranej suwakiem (5–100 km).
- Wyświetlanie szkicu trasy, zapisanej trasy i pinezki z wyszukiwarki na osobnych warstwach.

### 2. Strefy omijane (`MapDrawService`, `AvoidanceZoneStateService`)
- Rysowanie wielokątów wtyczką **Leaflet.draw**; geometria zamieniana jest na WKT (`POLYGON(...)`) i po nadaniu nazwy zapisywana w backendzie.
- Aktywne strefy są dołączane do zapytania o trasę (`avoid_polygons`); trasa jest przeliczana po włączeniu lub wyłączeniu strefy.
- Punkt trasy wewnątrz aktywnej strefy jest wykrywany przed wysłaniem zapytania (`RouteGeometryService`).

### 3. Profil wysokości (`ElevationChartComponent`)
- Punkty profilu (odległość od startu, wysokość) pobierane są z backendu (`GET /api/routes/{id}/elevation`), gdzie liczy je PostGIS.
- Wykres liniowy **Chart.js** (`ng2-charts`).

### 4. Narzędzia pomocnicze
- **Wyszukiwarka adresów** (`MapSearchService`): zapytania przez backend do usługi Photon.
- **Geolokalizacja:** API Geolocation przeglądarki.
- **Eksport:** pobranie trasy jako GPX lub GeoJSON; nazwa pliku pochodzi z nagłówka `Content-Disposition`.

---

## 📋 Przegląd Serwisów

| Serwis | Kategoria | Odpowiedzialność |
|---|---|---|
| `RouteStateService` | Stan | Lista zapisanych tras, wybrana trasa, profil wysokości, zapis, zmiana nazwy i folderu, eksport |
| `RoutePlanningStateService` | Stan | Punkty trasy, tryb mapy, szkic trasy, zapytania do ORS (trasa i pętla) |
| `FolderStateService` | Stan | Lista folderów, tworzenie, zmiana nazwy, usuwanie (zdarzenie `folderDeleted$` dla tras) |
| `AvoidanceZoneStateService` | Stan | Strefy omijane, ich aktywność i szkic rysowanej strefy |
| `MapEngineService` | Mapa | Inicjalizacja mapy, warstwy (panes), kafelki, przelot, geolokalizacja |
| `MapRouteLayerService` | Mapa | Znaczniki punktów trasy, szkic trasy, zapisana trasa, pinezka wyszukiwania |
| `MapDrawService` | Mapa | Rysowanie i wyświetlanie stref omijanych (Leaflet.draw) |
| `MapSearchService` | Mapa / API | Wyszukiwanie adresów i zamiana wyników na podpowiedzi |
| `RouteGeometryService` | Geometria | Budowa WKT trasy (`LineString` z wysokością), odczyt wielokątów z WKT, sprawdzanie punktu w wielokącie |
| `AuthService` | Bezpieczeństwo | Logowanie, rejestracja, odświeżanie i przechowywanie tokenów JWT (`localStorage`) |
| `LoggerService` | Telemetria | Logowanie w konsoli i wysyłanie błędów do backendu |
| `NotificationService` | UI | Powiadomienia (sukces, informacja, ostrzeżenie, błąd) |

---

## ⚙️ Wymagania i Konfiguracja

### Wymagania:
- **Node.js 24** (z dołączonym npm).
- Backend REST API pod adresem `http://localhost:8080` (tryb deweloperski).

### Adres API i poziom logowania:
- **Deweloperski (`src/environments/environment.development.ts`):** `apiUrl: 'http://localhost:8080/api'`, `logLevel: LogLevel.DEBUG`.
- **Produkcyjny (`src/environments/environment.ts`):** `apiUrl: '/api'` (nginx przekazuje `/api` do backendu), `logLevel: LogLevel.ERROR`.

---

## 🚀 Uruchomienie Lokalne

```cmd
# Z katalogu bicycle-route-planner-frontend:
npm ci
npm start        # serwer deweloperski: http://localhost:4200
npm test         # testy (Vitest przez ng test)
npm run build    # build produkcyjny
```

---

## 🐳 Konteneryzacja (Docker i nginx)

`Dockerfile` jest dwuetapowy:
- **Budowanie:** `ng build` w obrazie `node:24-alpine`.
- **Serwowanie:** `nginx:1.27-alpine` jako użytkownik bez uprawnień roota (`USER nginx`) na porcie `8080`.

### Konfiguracja nginx (`nginx.conf`):
- Routing SPA (`try_files $uri $uri/ /index.html`) i przekazywanie `/api/` do kontenera `backend`.
- Limit żądań do `/api/telemetry/` (`limit_req`: 1 żądanie/s z jednego IP, zapas 30, nadmiar – `429`).
- Nagłówki bezpieczeństwa: `Content-Security-Policy` (m.in. `script-src 'self'`, kafelki tylko z `tile.osm.org`), `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`.
- Kompresja gzip i długie buforowanie plików statycznych (nazwy z hashem).
- Build produkcyjny ma wyłączone `inlineCritical` (`angular.json`), bo ładowanie stylów przez `onload` blokowałoby CSP.

Frontend przekazuje `/api` do kontenera `backend`, więc uruchamia się go razem z resztą przez Docker Compose z katalogu głównego projektu:
```bash
docker compose up -d --build
```
