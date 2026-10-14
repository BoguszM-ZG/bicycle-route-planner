# 🅰️ Bicycle Route Planner — Frontend Client

[![Frontend CI](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/frontend.yml/badge.svg)](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/frontend.yml)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-blue?logo=typescript)](https://www.typescriptlang.org/)
[![Angular](https://img.shields.io/badge/Angular-22-red?logo=angular)](https://angular.dev/)
[![Leaflet](https://img.shields.io/badge/Leaflet-1.9.4-green?logo=leaflet)](https://leafletjs.com/)
[![Nginx](https://img.shields.io/badge/Nginx-Non--Root-2496ED?logo=nginx)](https://nginx.org/)

Kliencka aplikacja SPA (Single Page Application) systemu **Bicycle Route Planner**, zrealizowana w oparciu o framework **Angular 22** oraz język **TypeScript**. Moduł zapewnia responsywny, nowoczesny interfejs użytkownika z interaktywną mapą wektorową (**Leaflet**, **Leaflet-Draw**), reaktywnym zarządzaniem stanem (**Angular Signals**), dynamicznym profilem wysokościowym (**Chart.js**) oraz bezpieczną komunikacją z REST API za pośrednictwem tokenów JWT.

---

## 🏛️ Architektura Warstwowa i Wzorce Projektowe

Frontend został zaprojektowany zgodnie z zasadami **Clean Code** i nowoczesnymi standardami ekosystemu Angulara:

```text
src
├── environments/        # Konfiguracja środowiskowa (adres API backendu, poziomy logowania)
│   ├── environment.development.ts # Środowisko lokalne / deweloperskie
│   └── environment.ts             # Środowisko produkcyjne
└── app/
    ├── components/      # Komponenty interfejsu (Standalone Components, nowa składnia Control Flow)
    │   ├── login/           # Uwierzytelnianie użytkownika
    │   ├── register/        # Rejestracja nowych kont
    │   ├── map/             # Główny kontener mapy Leaflet oraz panel wyboru trybów (MapModePanel)
    │   ├── map-controls/    # Pływające kontrolki mapy (zoom, centrowanie, geolokalizacja GPS)
    │   ├── map-search/      # Wyszukiwarka adresów (autouzupełnianie, geokodowanie Photon)
    │   ├── sidebar/         # Panel boczny: trasy, foldery, strefy omijane, wykres przewyższeń
    │   ├── save-route-modal/# Modal zapisu trasy i przypisania do folderu
    │   ├── save-zone-modal/ # Modal definiowania nowej strefy omijanej
    │   └── toast/           # Reaktywny system powiadomień (NotificationService)
    ├── core/            # Globalna obsługa błędów (GlobalErrorHandler zintegrowany z telemetrią)
    ├── guards/          # Strażnicy routingu (AuthGuard chroniący widok mapy)
    ├── interceptors/    # Interceptory HTTP (AuthInterceptor dołączający nagłówek Bearer JWT)
    ├── models/          # Ścisłe interfejsy domenowe TypeScript (Route, Folder, AvoidanceZone itp.)
    ├── constants/       # Stałe konfiguracyjne (geokodowanie, kafelki mapy, ograniczenia)
    └── services/        # Architektura serwisów: HTTP, zarządzanie stanem (*StateService), obsługa mapy
```

### Kluczowe standardy implementacyjne:
- **Modern Angular 22 API:** 100% Standalone Components (całkowity brak modułów `NgModule`), reaktywne zarządzanie stanem w oparciu o **Signals** (`signal()`, `computed()`) oraz nowa składnia szablonów **Control Flow** (`@if`, `@for`).
- **Separation of Concerns (Podział Odpowiedzialności Serwisów):**
  - **Serwisy HTTP (`*Service`):** Odpowiadają wyłącznie za bezpośrednią komunikację z backendem (`RouteService`, `FolderService`, `AvoidanceZoneService`).
  - **Serwisy Stanu (`*StateService`):** Utrzymują niemutowalny stan aplikacji w sygnałach, udostępniając komponentom właściwości read-only (`routes`, `folders`, `avoidanceZones`).
- **Ochrona Serwera Telemetrii (Logging Standards):**
  - Błędy walidacji użytkownika (400, 401, 404, 409, odmowa GPS) są logowane lokalnie w konsoli przeglądarki na poziomie `debug()`, aby nie obciążać sieci i nie zaśmiecać logów serwera.
  - Na backend do endpointu `/api/telemetry/logs` wysyłane są wyłącznie faktyczne awarie systemu (`error`: 5xx, brak połączenia z siecią, nieobsłużone wyjątki aplikacji przechwytywane przez `GlobalErrorHandler`).
  - Ustrukturyzowane logi z prefiksami domenowymi (np. `[Route]`, `[Folder]`, `[Auth]`).
- **Optymalizacja Zapytań Sieciowych (Request Guarding):** Blokowanie nadmiarowych requestów HTTP (np. weryfikacja braku zmian nazwy przed wysłaniem zapytania rename, blokada zapisu pustych tras 0 m).

---

## 🗺️ Moduł Mapowy i Integracja Przestrzenna (Leaflet)

Interaktywna obsługa danych przestrzennych została podzielona na wyspecjalizowane serwisy mapowe:

### 1. Wizualizacja i Trasowanie (`MapRenderService`, `MapRoutingService`)
- Wyświetlanie kafelków OpenStreetMap z płynnym skalowaniem.
- Interaktywne dodawanie punktów trasy (waypointów) z obsługą przeciągania (drag-and-drop markerów) i automatycznym przeliczaniem geometrii w czasie rzeczywistym.
- Rysowanie geometrii tras w formacie wektorowym `L.polyline` z podziałem na kolory i podświetlenia.

### 2. Kreślenie Stref Omijanych (`MapDrawService`)
- Integracja z wtyczką **Leaflet-Draw** umożliwiająca swobodne rysowanie dowolnych wielokątów (`Polygon`) na podkładzie mapowym.
- Konwersja geometrii warstw Leafleta do formatu OGC WKT (`POLYGON(...)`) i natychmiastowe przesyłanie ich do silnika nawigacyjnego w celu omijania przeszkód.

### 3. Profil Wysokościowy (`ElevationChartComponent`)
- Renderowanie wykresu profilu wysokościowego trasy w oparciu o bibliotekę **Chart.js** (`ng2-charts`).
- Ekstrakcja trzeciego wymiaru ($Z$ - wysokość n.p.m.) z geometrii `LineString Z` i synchronizacja kursora na wykresie z pozycją na mapie.

### 4. Narzędzia Pomocnicze
- **Wyszukiwarka adresowa (`MapSearchService`):** Wyszukiwanie adresów za pośrednictwem API Photon z mechanizmem *debounce* zapobiegającym zalewaniu serwera zapytaniami.
- **Geolokalizacja (`MapControlsComponent`):** Lokalizacja aktualnej pozycji użytkownika przy użyciu przeglądarkowego API Geolocation.

---

## 📋 Przegląd Serwisów Aplikacji

| Serwis | Kategoria | Odpowiedzialność |
|---|---|---|
| `RouteStateService` | Stan (Signals) | Centralny stan załadowanych tras, aktywnej trasy i parametrów kalkulacji |
| `FolderStateService` | Stan (Signals) | Zarządzanie listą folderów, filtrowaniem i relacjami tras |
| `AvoidanceZoneStateService` | Stan (Signals) | Stan zdefiniowanych poligonów unikania i ich aktywności |
| `MapRoutingService` | Mapa | Zarządzanie markerami trasy, nasłuch zdarzeń przeciągania, rysowanie geometrii |
| `MapDrawService` | Mapa | Obsługa narzędzi rysowania poligonów stref omijanych (Leaflet-Draw) |
| `MapRenderService` | Mapa | Inicjalizacja instancji mapy, warstwy kafelkowe, centrowanie i GPS |
| `MapSearchService` | Mapa / API | Autouzupełnianie i geokodowanie zapytań adresowych |
| `RouteCalculationService` | Logika GIS |Formatowanie geometrii WKT (LineString Z), uzupełnianie brakujących wysokości i kalkulacja sumy przewyższeń |
| `AuthService` | Bezpieczeństwo | Logowanie, rejestracja, zarządzanie tokenami JWT w `localStorage` |
| `LoggerService` | Telemetria | Scentralizowany logger z asynchronicznym wysyłaniem błędów na backend |
| `NotificationService` | UI | Emisja powiadomień typu Toast (sukces, błąd, ostrzeżenie) |

---

## ⚙️ Wymagania i Konfiguracja Środowiska

### Wymagania:
- **Node.js:** Wersja **22+** (LTS rekomendowana).
- **Menedżer pakietów:** **npm 10+**.
- Działający backend REST API pod adresem `http://localhost:8080`.

### Konfiguracja Adresu API i Poziomu Logowania:
Adres bazowy backendu REST API oraz domyślna czułość loggera zdefiniowane są w plikach środowiskowych Angulara:
- **Środowisko deweloperskie (`src/environments/environment.development.ts`):**
  - `apiUrl: 'http://localhost:8080/api'`
  - `logLevel: LogLevel.DEBUG` *(szczegółowe logi diagnostyczne w konsoli przeglądarki)*
- **Środowisko produkcyjne (`src/environments/environment.ts`):**
  - `apiUrl: 'http://localhost:8080/api'`
  - `logLevel: LogLevel.ERROR` *(minimalizacja logowania na produkcji, raportowanie wyłącznie awarii do telemetrii)*

W przypadku uruchomienia backendu na innym porcie lub zdalnym serwerze (np. na serwerze produkcyjnym VPS), wystarczy zmodyfikować wartość `apiUrl` w odpowiednim pliku konfiguracyjnym.

---

## 🚀 Uruchomienie Lokalne (Developerskie)

Aplikacja wykorzystuje nowoczesny system budowania Angulara zintegrowany z silnikiem **Vite** jako serwerem deweloperskim:

### 1. Instalacja zależności
```cmd
# Z poziomu katalogu bicycle-route-planner-frontend:
npm ci
```

### 2. Uruchomienie serwera deweloperskiego
```cmd
npm start
```

Aplikacja wystartuje pod adresem:
👉 **[http://localhost:4200/](http://localhost:4200/)**
*(Zmiany w kodzie źródłowym są automatycznie aplikowane w przeglądarce dzięki mechanizmowi Hot Module Replacement).*

### 3. Weryfikacja kompilacji produkcyjnej
```cmd
npm run build
```

---

## 🐳 Konteneryzacja (Docker & Nginx)

Moduł frontendu posiada produkcyjny, dwuetapowy plik `Dockerfile` zoptymalizowany pod kątem bezpieczeństwa i wydajności:
- **Stage 1 (Builder):** Kompilacja aplikacji produkcyjnej (`ng build`) w kontenerze `node:22-alpine`.
- **Stage 2 (Runner):** Serwowanie statycznych plików SPA przez zoptymalizowany serwer **Nginx 1.27 Alpine**.
- **Bezpieczeństwo Non-root:** Nginx został skonfigurowany do działania jako użytkownik nieuprzywilejowany (`USER nginx`) na porcie `8080`.

### Konfiguracja Nginx (`nginx.conf`):
- Obsługa routingu SPA (`try_files $uri $uri/ /index.html`).
- Włączona kompresja Gzip dla zasobów tekstowych i JSON.
- Wymuszone nagłówki bezpieczeństwa (`X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`, `X-XSS-Protection`).
- Agresywne buforowanie zasobów statycznych (hashe w nazwach plików JS/CSS).

```bash
# Budowanie obrazu:
docker build -t bicycle-route-planner-frontend .

# Uruchomienie kontenera (mapowanie portu 4200 na wewnętrzny 8080):
docker run -d -p 4200:8080 --name bicycle_frontend bicycle-route-planner-frontend
```
