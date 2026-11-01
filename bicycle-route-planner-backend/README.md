# ⚙️ Bicycle Route Planner — Backend API

[![Backend CI](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/backend.yml/badge.svg)](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/backend.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL / PostGIS](https://img.shields.io/badge/PostGIS-15--3.4-blue?logo=postgresql)](https://postgis.net/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-PostGIS-informational?logo=docker)](https://testcontainers.com/)

Warstwa serwerowa systemu **Bicycle Route Planner** w technologii **Java 21** i **Spring Boot 4.1.0**. Udostępnia REST API zabezpieczone tokenami JWT, przechowuje i przetwarza geometrie w bazie **PostGIS**, waliduje geometrie WKT biblioteką **JTS Topology Suite** i pośredniczy w komunikacji z usługami zewnętrznymi (**OpenRouteService**, **Photon**). Szczegółowy opis klas: [`BACKEND_DOCUMENTATION.md`](BACKEND_DOCUMENTATION.md).

---

## 🏛️ Architektura Warstwowa

```text
com.bogusz.bicyclerouteplannerbackend
├── config/              # Konfiguracja: Spring Security, CORS, JWT, OpenAPI, klienci HTTP (ORS, Photon)
├── controller/          # Kontrolery REST (walidacja DTO, mapowanie HTTP, opis OpenAPI)
├── dto/                 # Rekordy żądań i odpowiedzi
├── exception/           # Wyjątki domenowe i GlobalExceptionHandler (ProblemDetail)
├── mapper/              # Konwersja encji ↔ DTO (WKT ↔ JTS)
├── model/               # Encje JPA z polami przestrzennymi (Hibernate Spatial / JTS)
├── repository/          # Repozytoria Spring Data JPA, w tym zapytania natywne PostGIS
├── security/            # UserPrincipal i filtr JWT
├── service/             # Logika biznesowa, limity stref, próbkowanie profilu wysokości, eksport GPX
└── validation/          # Własne adnotacje i walidatory (geometria WKT, zapytania do ORS)
```

### Przyjęte zasady:
- **DTO jako rekordy Javy**, zależności wstrzykiwane przez konstruktor (`@RequiredArgsConstructor`).
- **Walidacja na granicy systemu:** dane wejściowe sprawdzane są adnotacjami Bean Validation w DTO; dalej kod ufa typom i nie powtarza sprawdzeń.
- **Logowanie:** na poziomie `INFO` rejestrowane są rejestracja i logowanie użytkownika oraz utworzenie i usunięcie trasy, folderu i strefy; odczyty nie są logowane.
- **Minimalizacja danych w logach (RODO):** logi nie zawierają adresów e-mail – zdarzenia identyfikowane są numerami (`userId`, `routeId`, `folderId`).
- **Format błędów:** wyjątki są przechwytywane przez `GlobalExceptionHandler` i zwracane jako `application/problem+json` (`ProblemDetail`, RFC 9457) z typem błędu `urn:problem-type:...` i – przy walidacji – mapą `validationErrors`.

---

## 🗺️ Model Danych Przestrzennych (PostGIS & JTS)

Baza PostgreSQL 15 z rozszerzeniem **PostGIS 3.4**, układ współrzędnych **WGS 84 (SRID 4326)**. Schemat tworzą migracje **Flyway** (`src/main/resources/db/migration`), a Hibernate tylko go weryfikuje (`spring.jpa.hibernate.ddl-auto=validate`).

### 1. Trasy (`routes`)
- **`route_geom`:** `GEOMETRY(LineStringZ, 4326)` – każdy punkt trasy ma długość, szerokość i wysokość n.p.m. (X, Y, Z).
- **`start_point`:** `GEOMETRY(Point, 4326)` – punkt startowy trasy; przygotowanie pod wyszukiwanie tras w pobliżu (obecnie nie jest używany w zapytaniach).
- Dystans i przewyższenia zapisywane są z odpowiedzi OpenRouteService.

### 2. Strefy omijane (`avoidance_zones`)
- **`zone_geom`:** `GEOMETRY(Polygon, 4326)`.

### 3. Indeksy
- **GiST:** `idx_routes_geom`, `idx_routes_start_point`, `idx_avoidance_geom`.
- **B-tree** na kluczach obcych (`user_id`, `folder_id`).
- Obecne zapytania filtrują po `id` / `user_id`, więc korzystają z indeksów B-tree. Indeksy GiST zostałyby użyte przez zapytania z predykatem przestrzennym (np. `ST_DWithin`, `ST_Intersects`).

### 4. Podział obliczeń przestrzennych: PostGIS i JTS
- **PostGIS (zapytania natywne):**
  - profil wysokości trasy z odległościami geodezyjnymi na elipsoidzie WGS84 (`ST_DumpPoints`, `ST_Distance` na `geography`, funkcje okna),
  - eksport trasy do GeoJSON (`ST_AsGeoJSON`),
  - limity stref omijanych: pole (`ST_Area`) i rozpiętość (`ST_Distance`) na `geography`.
- **JTS w pamięci JVM:**
  - walidacja geometrii WKT (`ValidWktGeometry`, `WktGeometryValidator`),
  - odczyt współrzędnych do pliku GPX (`GpxExportService`, biblioteka JPX).

---

## 🌐 Integracje Zewnętrzne

1. **OpenRouteService (wyznaczanie tras):**
   - **Profil:** `cycling-road`, endpoint `/v2/directions/cycling-road/geojson`.
   - **Klucz API po stronie serwera:** `ORS_API_KEY` dołączany jest przez backend i nie trafia do przeglądarki.
   - **Zapytanie budowane we frontendzie:** współrzędne punktów, opcja pętli (`round_trip`: długość i ziarno losowania `seed`) oraz aktywne strefy (`avoid_polygons`).
   - **Walidacja przed wysłaniem:** zakresy i liczba współrzędnych (maks. 50 punktów, pętla dokładnie z 1 punktem), długość pętli do 100 km; `options` przyjmuje tylko `round_trip` i `avoid_polygons` (pozostałe pola są pomijane). Błędne zapytanie kończy się `400` bez wywołania ORS.
   - **Limity stref** (rozpiętość do 20 km, łączne pole aktywnych stref do 200 km²) sprawdzane są w PostGIS przy zapisie i włączaniu strefy.

2. **Photon (Komoot) – geokodowanie:** wyszukiwanie adresów i miejsc po tekście.

---

## 📋 Endpointy REST API (Przegląd Kontrolerów)

| Kontroler | Ścieżka bazowa | Odpowiedzialność |
|---|---|---|
| `AuthController` | `/api/auth` | Rejestracja, logowanie, odświeżanie tokenów JWT |
| `RouteController` | `/api/routes` | Zapis, odczyt i usuwanie tras, zmiana nazwy i folderu, profil wysokości (`GET /{id}/elevation`), eksport GPX i GeoJSON (`GET /{id}/export/gpx`, `/{id}/export/geojson`) |
| `RoutingController` | `/api/routing` | Pośrednik do OpenRouteService (`POST /directions`) – trasy punkt-do-punktu i pętle |
| `AvoidanceZoneController` | `/api/avoidance-zones` | Zapis, włączanie/wyłączanie i usuwanie stref omijanych |
| `FolderController` | `/api/folders` | Foldery użytkownika (jeden poziom: użytkownik → folder → trasa) |
| `GeocodingController` | `/api/geocoding` | Wyszukiwanie adresów (pośrednik do Photon) |
| `ClientTelemetryController`| `/api/telemetry` | Odbiór logów błędów z frontendu (publiczny, z limitem w nginx) |

*Specyfikacja OpenAPI:* `http://localhost:8080/swagger-ui.html`

---

## ⚙️ Wymagania i Konfiguracja Środowiska

### Wymagania:
- **JDK 21** (np. Eclipse Temurin).
- **PostgreSQL 15+ z PostGIS 3.4+** (najprościej przez Docker Compose z katalogu głównego).
- **Klucz API** [OpenRouteService](https://openrouteservice.org/dev/#/signup) (darmowy).

### Zmienne Środowiskowe:
Aplikacja odczytuje zmienne z pliku `.env` w katalogu głównym projektu lub ze zmiennych środowiskowych systemu:

| Zmienna | Opis | Przykład |
|---|---|---|
| `DB_USER` | Użytkownik bazy PostgreSQL | `postgres` |
| `DB_PASSWORD` | Hasło użytkownika bazy | `your_secure_database_password` |
| `DB_NAME` | Nazwa bazy danych | `bicycle_route_planner` |
| `JWT_SECRET` | Klucz HMAC do podpisywania tokenów | *min. 32 znaki* |
| `ORS_API_KEY` | Klucz do OpenRouteService | `5b3ce359785a...` |
| `APP_FRONTEND_URL` | Adres frontendu dopuszczony w CORS | `http://localhost:4200` |

---

## 🚀 Uruchomienie Lokalne

### Sposób 1: Pełne środowisko w Dockerze
```bash
# Z katalogu głównego projektu:
docker compose up -d --build
```

### Sposób 2: Baza w Dockerze + backend z IDE / Maven
```bash
# 1. Baza PostGIS (z katalogu głównego projektu):
docker compose up -d postgis_db

# 2. Backend (z katalogu bicycle-route-planner-backend/backend):
./mvnw clean spring-boot:run
```

Backend startuje na porcie `8080` (Swagger UI: `http://localhost:8080/swagger-ui.html`). Przy sposobie 2 kontener `backend` z Compose nie może działać jednocześnie, bo zajmuje ten sam port.

---

## 🧪 Testy Automatyczne

235 metod testowych JUnit 5 (stan: październik 2026):

1. **Testy jednostkowe (131):** serwisy (m.in. limity stref, próbkowanie profilu wysokości, eksport GPX), walidatory Bean Validation i klasy bezpieczeństwa; zależności zastępuje Mockito.
2. **Testy warstwy webowej (57, `@WebMvcTest`):** endpointy REST, statusy HTTP, walidacja DTO i format błędów, bez pełnego kontekstu Springa.
3. **Testy integracyjne (47):** przepływy od żądania HTTP do bazy na kontenerze `postgis/postgis:15-3.4` uruchamianym przez **Testcontainers** (z migracjami Flyway, więc testują też zapytania natywne PostGIS); OpenRouteService i Photon zastępuje **WireMock**.

```bash
# Z katalogu bicycle-route-planner-backend/backend (wymaga działającego Dockera):
./mvnw clean verify
```

Profil testowy (`application-test.properties`) używa sztucznych kluczy, więc testy nie wymagają prawdziwego klucza ORS.

---

## 🐳 Konteneryzacja (Docker)

`Dockerfile` jest wieloetapowy:
- **Budowanie:** obraz `eclipse-temurin:21-jdk-alpine` z buforowaniem zależności Maven.
- **Uruchomienie:** obraz `eclipse-temurin:21-jre-alpine`, aplikacja działa jako użytkownik bez uprawnień roota (`USER spring:spring`).

Backend wymaga bazy i zmiennych środowiskowych, dlatego uruchamia się go przez Docker Compose z katalogu głównego projektu:
```bash
docker compose up -d --build
```
