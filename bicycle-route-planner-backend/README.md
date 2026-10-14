# ⚙️ Bicycle Route Planner — Backend API

[![Backend CI](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/backend.yml/badge.svg)](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/backend.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL / PostGIS](https://img.shields.io/badge/PostGIS-15--3.4-blue?logo=postgresql)](https://postgis.net/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-PostGIS-informational?logo=docker)](https://testcontainers.com/)

Warstwa serwerowa systemu **Bicycle Route Planner**, zrealizowana w technologii **Java 21** oraz **Spring Boot 4.1.0**. Moduł udostępnia bezpieczne REST API, realizuje zaawansowane operacje przestrzenne w relacyjnej bazie **PostGIS** (JTS Topology Suite) oraz integruje się z silnikami nawigacyjnymi (**OpenRouteService**, **Photon**).

---

## 🏛️ Architektura Warstwowa

Backend został zaprojektowany zgodnie ze standardem wielowarstwowej aplikacji Spring Boot:

```text
com.bogusz.bicyclerouteplannerbackend
├── config/              # Konfiguracja Spring Security, JWT, CORS, OpenAPI Swagger
├── controller/          # Kontrolery REST API (weryfikacja DTO, mapowanie HTTP, dokumentacja)
├── dto/                 # Obiekty transferu danych (Request/Response DTOs z walidacją Bean Validation)
├── exception/           # Dedykowane wyjątki domenowe oraz GlobalExceptionHandler (RFC 7807 ProblemDetail)
├── mapper/              # Komponenty mapujące encje na DTO i odwrotnie
├── model/               # Encje JPA z polami przestrzennymi Hibernate Spatial / JTS
├── repository/          # Repozytoria Spring Data JPA z zapytaniami przestrzennymi (Spatial Queries)
└── service/             # Logika biznesowa, algorytmy generowania pętli, klienci zewnętrznych API
```

### Kluczowe standardy implementacyjne:
- **Strictly Java 21:** Wykorzystanie nowoczesnych konstrukcji języka (Pattern Matching, rekordy, instrukcje sterujące).
- **Logowanie wyłącznie mutacji (CUD):** Zgodnie z zasadą minimalizacji szumu w logach, na poziomie `INFO` rejestrowane są wyłącznie operacje trwale zmieniające stan (Save, Update, Delete). Zapytania odczytu (`GET`) nie obciążają loggera.
- **Ochrona Danych Osobowych (RODO / PII Compliance):** W logach nigdy nie pojawiają się adresy e-mail ani dane wrażliwe — operacje są identyfikowane wyłącznie poprzez anonimowe identyfikatory encji (`userId`, `routeId`, `folderId`).
- **Standard Obsługi Błędów (RFC 7807):** Dedykowane wyjątki domenowe (np. `RouteNotFoundException`, `FolderNotFoundException`, `BadCredentialsException`) są przechwytywane globalnie przez `GlobalExceptionHandler` i zwracane jako ustrukturyzowany format `ProblemDetail`.
- **Wstrzykiwanie zależności:** Wyłącznie przez konstruktor z adnotacją Lombok `@RequiredArgsConstructor`.

---

## 🗺️ Model Danych Przestrzennych (PostGIS & JTS)

Baza danych PostgreSQL 15 z rozszerzeniem **PostGIS 3.4** realizuje natywne składowanie geometrii w układzie współrzędnych **WGS 84 (SRID 4326)**:

### 1. Trójwymiarowa Geometria Tras (`routes`)
- **Kolumna `route_geom`:** Typ przestrzenny `GEOMETRY(LineStringZ, 4326)`. Każdy węzeł ścieżki przechowuje długość, szerokość oraz rzędną wysokości n.p.m. (X, Y, Z).
- **Kolumna `start_point`:** Typ `GEOMETRY(Point, 4326)` wyodrębniony dla błyskawicznego wyszukiwania tras w zadanym promieniu.
- **Indeksowanie:** Przestrzenne indeksy **GiST** (`idx_routes_geom`, `idx_routes_start_point`).

### 2. Wielokąty Stref Omijanych (`avoidance_zones`)
- **Kolumna `zone_geom`:** Typ przestrzenny `GEOMETRY(Polygon, 4326)`.
- **Indeksowanie:** Przestrzenny indeks **GiST** (`idx_avoidance_geom`).

Wersjonowanie struktury bazy danych i zakładanie indeksów jest zautomatyzowane przy użyciu migracji **Flyway** (`src/main/resources/db/migration`), podczas gdy mechanizm Hibernate zredukowano do roli walidatora (`spring.jpa.hibernate.ddl-auto=validate`).

---

## 🌐 Integracje Zewnętrzne

1. **OpenRouteService (ORS API):**
   - Obliczanie geometrii ścieżki dla profilu rowerowego (`cycling-regular`).
   - Przekazywanie współrzędnych poligonów w celu omijania zdefiniowanych barier przestrzennych (`avoid_polygons`).
2. **Photon Geocoding API (Komoot):**
   - Odpytywanie silnika geokodowania w celu zamiany ciągów tekstowych na współrzędne geograficzne.

---

## 📋 Endpointy REST API (Przegląd Kontrolerów)

| Kontroler | Ścieżka bazowa | Odpowiedzialność |
|---|---|---|
| `AuthController` | `/api/auth` | Rejestracja, logowanie, odświeżanie tokenów JWT |
| `RouteController` | `/api/routes` | Zapis, odczyt i usuwanie tras, edycja metadanych (nazwa, przypisanie do folderu), eksport .gpx |
| `RoutingController` | `/api/routing` | Wyznaczanie ścieżek punkt-do-punktu oraz generowanie tras w formie pętli |
| `AvoidanceZoneController` | `/api/avoidance-zones` | Swobodne kreślenie, aktywacja i usuwanie poligonów omijanych |
| `FolderController` | `/api/folders` | Zarządzanie jednopoziomową strukturą kolekcji/folderów użytkownika |
| `GeocodingController` | `/api/geocoding` | Przeszukiwanie adresów i obiektów geograficznych (proxy do Photona) |
| `ClientTelemetryController`| `/api/telemetry` | Przyjmowanie i strukturyzowane logowanie błędów klienta |

*Pełna, interaktywna specyfikacja OpenAPI dostępna jest pod adresem:* `http://localhost:8080/swagger-ui.html`

---

## ⚙️ Wymagania i Konfiguracja Środowiska

### Wymagania:
- **Java Development Kit (JDK):** Wersja **21** (rekomendowana dystrybucja Eclipse Temurin).
- **Silnik Bazy Danych:** PostgreSQL 15+ z włączonym rozszerzeniem PostGIS 3.4+.
- **Klucz API:** [OpenRouteService API Key](https://openrouteservice.org/dev/#/signup) (darmowy).

### Zmienne Środowiskowe:
Aplikacja ładuje zmienne z pliku `.env` znajdującego się w katalogu głównym projektu lub bezpośrednio ze zmiennych środowiskowych systemu operacyjnego:

| Zmienna | Opis | Wartość domyślna / Przykład |
|---|---|---|
| `DB_USER` | Użytkownik bazy PostgreSQL | `postgres` |
| `DB_PASSWORD` | Hasło użytkownika bazy | `your_secure_database_password` |
| `DB_NAME` | Nazwa bazy danych | `bicycle_route_planner` |
| `JWT_SECRET` | 256-bitowy klucz HMAC-SHA do podpisywania tokenów | *Min. 32-znakowy ciąg* |
| `ORS_API_KEY` | Klucz autoryzacyjny do OpenRouteService | `5b3ce359785a...` |
| `APP_FRONTEND_URL` | Adres URL frontendu na potrzeby konfiguracji CORS | `http://localhost:4200` |

---

## 🚀 Uruchomienie Lokalne (Developerskie)

Najwygodniejszym podejściem podczas pracy w IDE (IntelliJ IDEA / Eclipse / VS Code) jest uruchomienie samej bazy danych przez Docker Compose, a backendu natywnie:

### 1. Uruchomienie bazy PostGIS
```bash
# Z poziomu katalogu głównego projektu:
docker compose up postgis_db -d
```

### 2. Uruchomienie aplikacji Spring Boot
```bash
# Z poziomu katalogu bicycle-route-planner-backend/backend:
# Dla systemów Linux / macOS:
./mvnw clean spring-boot:run

# Dla systemu Windows:
mvnw clean spring-boot:run
```

Aplikacja wystartuje na porcie `8080`.
- Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 🧪 Testy Automatyczne

Projekt pokryty jest **123 zautomatyzowanymi testami**, podzielonymi na trzy warstwy izolacji:

1. **Testy Integracyjne (`Testcontainers`):** Całościowa weryfikacja przepływów biznesowych (od kontrolera REST po bazę danych) oraz poprawności zapisu/odczytu na rzeczywistym kontenerze `postgis/postgis:15-3.4` powoływanym w locie.
2. **Testy Kontrolerów (`@WebMvcTest`):** Izolowana weryfikacja endpointów REST, statusów HTTP, walidacji DTO oraz obsługi wyjątków (bez pełnego kontekstu Springa).
3. **Testy Jednostkowe:** Weryfikacja logiki domenowej, algorytmów i transformacji danych (GPX) z użyciem JUnit 5 oraz Mockito.

### Uruchomienie zestawu testów:
```bash
# Dla systemów Linux / macOS:
./mvnw clean verify

# Dla systemu Windows:
mvnw clean verify
```

*(Profil testowy korzysta z `application-test.properties`, co gwarantuje pełną hermetyczność testów bez konieczności definiowania prawdziwych kluczy API).*

---

## 🐳 Konteneryzacja (Docker)

Moduł backendu posiada wieloetapowy plik `Dockerfile` zoptymalizowany pod kątem bezpieczeństwa i wydajności:
- **Multi-stage build:** Etap budowania na obrazie `eclipse-temurin:21-jdk-alpine` z buforowaniem zależności Maven.
- **Minimalny runtime:** Obraz docelowy oparty o lekki `eclipse-temurin:21-jre-alpine`.
- **Zabezpieczenie Non-root:** Aplikacja uruchamia się jako dedykowany użytkownik systemowy bez uprawnień roota (`USER spring:spring`).

```bash
# Budowanie obrazu:
docker build -t bicycle-route-planner-backend .

# Uruchomienie kontenera:
docker run -d -p 8080:8080 --name bicycle_backend bicycle-route-planner-backend
```
