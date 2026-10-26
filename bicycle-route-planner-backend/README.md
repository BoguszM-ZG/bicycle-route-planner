# ⚙️ Bicycle Route Planner — Backend API

[![Backend CI](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/backend.yml/badge.svg)](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/backend.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL / PostGIS](https://img.shields.io/badge/PostGIS-15--3.4-blue?logo=postgresql)](https://postgis.net/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-PostGIS-informational?logo=docker)](https://testcontainers.com/)

Warstwa serwerowa systemu **Bicycle Route Planner**, zrealizowana w technologii **Java 21** oraz **Spring Boot 4.1.0**. Moduł udostępnia bezpieczne REST API, realizuje składowanie geometrii w relacyjnej bazie **PostGIS** z indeksami GiST, wykonuje przetwarzanie topologiczne w pamięci (**JTS Topology Suite**) oraz pełni rolę bezpiecznego proxy dla silników zewnętrznych (**OpenRouteService**, **Photon**).

---

## 🏛️ Architektura Warstwowa

Backend został zaprojektowany zgodnie ze standardem wielowarstwowej aplikacji Spring Boot:

```text
com.bogusz.bicyclerouteplannerbackend
├── config/              # Konfiguracja Spring Security, JWT, CORS, OpenAPI Swagger, RestClient
├── controller/          # Kontrolery REST API (weryfikacja DTO, mapowanie HTTP, dokumentacja)
├── dto/                 # Obiekty transferu danych (Request/Response DTOs)
├── exception/           # Dedykowane wyjątki domenowe oraz GlobalExceptionHandler (RFC 7807 ProblemDetail)
├── mapper/              # Komponenty mapujące encje na DTO i odwrotnie
├── model/               # Encje JPA z polami przestrzennymi Hibernate Spatial / JTS
├── repository/          # Repozytoria Spring Data JPA (trwały magazyn danych, operacje CRUD)
├── security/            # Tożsamość użytkownika (UserPrincipal implementujący UserDetails) i kontekst autoryzacji
├── service/             # Logika biznesowa, redukcja profilu wysokościowego, eksport GPX, klienci API
└── validation/          # Dedykowane adnotacje i walidatory Bean Validation (WKT, reguły routingowe)
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
- **Kolumna `start_point`:** Typ `GEOMETRY(Point, 4326)` wyodrębniony dla natychmiastowej lokalizacji punktu startowego trasy.
- **Indeksowanie:** Przestrzenne indeksy **GiST** (`idx_routes_geom`, `idx_routes_start_point`).

### 2. Wielokąty Stref Omijanych (`avoidance_zones`)
- **Kolumna `zone_geom`:** Typ przestrzenny `GEOMETRY(Polygon, 4326)`.
- **Indeksowanie:** Przestrzenny indeks **GiST** (`idx_avoidance_geom`).

### 3. Rozdział Magazynu i Przetwarzania Topologicznego
Architektura systemu świadomie rozdziela trwałe składowanie od obliczeń przestrzennych:
- **PostGIS jako magazyn geometrii z indeksami GiST (`idx_routes_geom`, `idx_routes_start_point`, `idx_avoidance_geom`):** Baza danych odpowiada za integralność i persystencję struktur przestrzennych. Repozytoria Spring Data JPA nie realizują ciężkich zapytań przestrzennych w SQL — pobierają dane do pamięci na poziomie relacji JPA.
- **Przetwarzanie topologiczne w pamięci (JTS Topology Suite):** Wszystkie operacje na geometrii wykonywane są w pamięci operacyjnej JVM:
  - Walidacja struktur geometrycznych WKT (`ValidWktGeometry`, `WktGeometryValidator`).
  - Precyzyjne obliczenia odległości pomiędzy węzłami formułą Haversine'a.
  - Transformacje współrzędnych oraz serializacja do formatu GPX (`GpxExportService`).
Wersjonowanie struktury bazy danych i zakładanie indeksów jest zautomatyzowane przy użyciu migracji **Flyway** (`src/main/resources/db/migration`), podczas gdy mechanizm Hibernate zredukowano do roli walidatora (`spring.jpa.hibernate.ddl-auto=validate`).

---

## 🌐 Integracje Zewnętrzne i Bezpieczne Proxy (OpenRouteService)

1. **Bezpieczne Proxy dla OpenRouteService (ORS API):**
   - **Profil rowerowy:** Trasy kalkulowane są dla profilu szosowego (`cycling-road`).
   - **Ochrona poświadczeń:** Backend działa jako bezpieczny pośrednik (reverse proxy) — poufny klucz `ORS_API_KEY` jest wstrzykiwany po stronie serwera i nie trafia do klienta HTTP.
   - **Komponowanie po stronie klienta:** Parametry zapytania routingowego — zarówno wyznaczanie pętli (`round_trip` z zadaną długością i kątem nasiona), jak i definicje stref omijanych (`avoid_polygons`) — są dynamicznie komponowane w przeglądarce klienta (Angular).
   - **Walidacja i przekazywanie (Relay):** Backend weryfikuje poprawność żądania (`@ValidOrsRouteRequest` / `OrsRouteRequestValidator`: limity współrzędnych, brak wartości NaN/Infinity, wymagana liczba punktów) i bezpiecznie przekazuje je do endpointu `/v2/directions/cycling-road/geojson`.

2. **Photon Geocoding API (Komoot):**
   - Odpytywanie silnika geokodowania w celu zamiany ciągów tekstowych na współrzędne geograficzne.

---

## 📋 Endpointy REST API (Przegląd Kontrolerów)

| Kontroler | Ścieżka bazowa | Odpowiedzialność |
|---|---|---|
| `AuthController` | `/api/auth` | Rejestracja, logowanie, odświeżanie tokenów JWT |
| `RouteController` | `/api/routes` | Zapis, odczyt i usuwanie tras, edycja metadanych (nazwa, przypisanie do folderu), profil wysokościowy (`GET /api/routes/{id}/elevation`), eksport .gpx |
| `RoutingController` | `/api/routing` | Bezpieczne proxy dla OpenRouteService (`POST /api/routing/directions`) — walidacja i kalkulacja tras punkt-do-punktu oraz pętli (`round_trip`, `avoid_polygons`) |
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

Najwygodniejszym podejściem podczas pracy w IDE (IntelliJ IDEA / Eclipse / VS Code) jest uruchomienie całego stosu za pomocą Docker Compose lub uruchomienie samej bazy danych i aplikacji natywnie:

### Sposób 1: Pełne środowisko w Dockerze (rekomendowane)
```bash
# Z poziomu katalogu głównego projektu:
docker compose up -d
```

### Sposób 2: Baza danych w Dockerze + natywny Spring Boot
```bash
# 1. Start bazy PostGIS (z katalogu głównego projektu lub bicycle-route-planner-backend):
docker compose up postgis_db -d

# 2. Uruchomienie aplikacji Spring Boot (z poziomu katalogu bicycle-route-planner-backend/backend):
./mvnw clean spring-boot:run
```

Aplikacja wystartuje na porcie `8080`.
- Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 🧪 Testy Automatyczne

Projekt pokryty jest **226 zautomatyzowanymi testami**, podzielonymi na trzy warstwy piramidy testowej:

1. **Testy Jednostkowe (135 testów):** Weryfikacja logiki domenowej, algorytmu redukcji profilu wysokościowego (`ElevationProfileDownsampler`), walidatorów Bean Validation oraz transformacji danych (GPX) z użyciem JUnit 5 oraz Mockito.
2. **Testy Kontrolerów (50 testów `@WebMvcTest`):** Izolowana weryfikacja endpointów REST, statusów HTTP, autoryzacji tokenów JWT, walidacji DTO oraz obsługi wyjątków (bez pełnego kontekstu Springa).
3. **Testy Integracyjne (41 testów):** Całościowa weryfikacja przepływów biznesowych (od kontrolera REST po bazę danych) oraz poprawności zapisu/odczytu na rzeczywistym kontenerze `postgis/postgis:15-3.4` powoływanym w locie (`Testcontainers`) oraz z mockowaniem zewnętrznych integracji (`WireMock`).

### Uruchomienie zestawu testów:
```bash
# Z poziomu katalogu bicycle-route-planner-backend/backend:
./mvnw clean verify
```

*(Profil testowy korzysta z `application-test.properties`, co gwarantuje pełną hermetyczność testów bez konieczności definiowania prawdziwych kluczy API).*

---

## 🐳 Konteneryzacja (Docker)

Moduł backendu posiada wieloetapowy plik `Dockerfile` zoptymalizowany pod kątem bezpieczeństwa i wydajności:
- **Multi-stage build:** Etap budowania na obrazie `eclipse-temurin:21-jdk-alpine` z buforowaniem zależności Maven.
- **Minimalny runtime:** Obraz docelowy oparty o lekki `eclipse-temurin:21-jre-alpine`.
- **Zabezpieczenie Non-root:** Aplikacja uruchamia się jako dedykowany użytkownik systemowy bez uprawnień roota (`USER spring:spring`).

### Uruchomienie przez Docker Compose (rekomendowane):
```bash
# Uruchomienie całego stosu usług w tle (z poziomu katalogu głównego):
docker compose up -d
```

### Samodzielne budowanie i uruchomienie obrazu:
W przypadku manualnego budowania obrazu należy wskazać poprawny kontekst `./backend`, w którym znajduje się plik `Dockerfile` oraz kod źródłowy:

```bash
# Budowanie obrazu (z poziomu katalogu bicycle-route-planner-backend):
docker build -t bicycle-route-planner-backend ./backend

# Uruchomienie kontenera:
docker run -d -p 8080:8080 --name bicycle_backend bicycle-route-planner-backend
```
