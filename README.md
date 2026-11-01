# 🚴‍♂️ Bicycle Route Planner

[![Backend CI](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/backend.yml/badge.svg)](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/backend.yml)
[![Frontend CI](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/frontend.yml/badge.svg)](https://github.com/BoguszM-ZG/bicycle-route-planner/actions/workflows/frontend.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-22-red?logo=angular)](https://angular.dev/)
[![PostgreSQL / PostGIS](https://img.shields.io/badge/PostGIS-15--3.4-blue?logo=postgresql)](https://postgis.net/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)](https://www.docker.com/)

---

## 🎓 Metryka Pracy Inżynierskiej

- **Temat pracy:** *Planowanie i analiza przestrzenna tras rowerowych.*
- **Cel pracy:** Zaprojektowanie i implementacja aplikacji webowej do planowania i analizy tras rowerowych, korzystającej z relacyjnej bazy danych przestrzennych. Aplikacja udostępnia konta użytkowników, interaktywną mapę do wyznaczania i edycji tras, automatyczne generowanie pętli, strefy omijane oraz eksport tras do plików.

### Zakres pracy:
1. **Baza danych przestrzennych:** PostgreSQL z PostGIS – geometrie `LineStringZ` (trasa z wysokością), `Polygon` (strefy) i `Point` w układzie WGS84 (SRID 4326), indeksy GiST, migracje Flyway. Część obliczeń wykonuje baza: profil wysokości z odległościami geodezyjnymi, eksport GeoJSON, pole i rozpiętość stref.
2. **Backend REST API:** Java 21 i Spring Boot 4.1.0, pośredniczący w komunikacji z usługą wyznaczania tras OpenRouteService (klucz API pozostaje na serwerze, zapytania są walidowane pod kątem limitów usługi).
3. **Aplikacja kliencka:** Angular 22 (komponenty standalone, sygnały) z mapą Leaflet.
4. **Strefy omijane:** rysowanie wielokątów na mapie; aktywne strefy są uwzględniane przy wyznaczaniu trasy, a ich rozmiar sprawdzany w bazie względem limitów ORS.
5. **Eksport danych:** zapis trasy do formatów GPX (`.gpx`) i GeoJSON (`.geojson`).
6. **Generator pętli:** wyznaczanie tras okrężnych z punktu startowego o zadanej długości (5–100 km).
7. **Konta i foldery:** uwierzytelnianie tokenami JWT; trasy użytkownika można grupować w folderach (hierarchia użytkownik → folder → trasa, jeden poziom folderów).
8. **Funkcje dodatkowe:** wyszukiwanie adresów przez usługę Photon (Komoot), geolokalizacja przeglądarki, wykres profilu wysokości, interfejs w języku polskim i angielskim.

---

## 🏗️ Stos Technologiczny

| Warstwa | Technologia | Zastosowanie |
|---|---|---|
| **Backend** | Java 21, Spring Boot 4.1.0 | REST API, Spring Security (JWT), Spring Data JPA, Hibernate Spatial |
| **Baza Danych** | PostgreSQL 15, PostGIS 3.4, Flyway | Składowanie i przetwarzanie geometrii, wersjonowanie schematu |
| **Frontend** | TypeScript, Angular 22 | Interfejs użytkownika, stan w sygnałach Angulara, RxJS do komunikacji HTTP |
| **Moduł Mapowy** | Leaflet 1.9, Leaflet.draw, Chart.js | Mapa OSM, warstwy wektorowe, rysowanie stref, wykres wysokości |
| **Usługi zewnętrzne** | OpenRouteService, Photon | Wyznaczanie tras i pętli z omijaniem stref; geokodowanie adresów |
| **Testy** | JUnit 5, Mockito, Testcontainers, WireMock, Vitest | Testy backendu (w tym na prawdziwej bazie PostGIS) i frontendu |
| **Infrastruktura** | Docker, Docker Compose, nginx | Uruchomienie całości; obrazy wieloetapowe, procesy bez uprawnień roota |

---

## 🚀 Szybki Start (Docker Compose)

Cały stos (PostGIS, backend, frontend) można uruchomić jedną komendą.

### Wymagania:
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (lub Docker Engine z Docker Compose).

### Uruchomienie:
```bash
# 1. Klonowanie repozytorium
git clone https://github.com/BoguszM-ZG/bicycle-route-planner.git
cd bicycle-route-planner

# 2. Utworzenie pliku ze zmiennymi środowiskowymi
cp .env.example .env

# UWAGA: Przed uruchomieniem edytuj plik .env i ustaw:
# - ORS_API_KEY (darmowy klucz: https://openrouteservice.org/dev/#/signup)
# - własny JWT_SECRET oraz dane dostępowe do bazy danych

# 3. Uruchomienie kontenerów w tle
docker compose up -d --build
```

Dostęp do usług:
- 🌐 **Aplikacja:** [http://localhost:4200](http://localhost:4200) (nginx serwuje frontend i przekazuje `/api` do backendu)
- ⚙️ **Backend REST API:** [http://localhost:8080](http://localhost:8080)
- 📖 **Swagger / OpenAPI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- 🗄️ **Baza PostGIS:** `localhost:5432`

Porty backendu i bazy są dostępne tylko z tego komputera (`127.0.0.1`); z sieci widoczny jest wyłącznie frontend na porcie 4200.

---

## 🛠️ Uruchomienie Deweloperskie (Lokalne)

Przy pracy w IDE w Dockerze uruchamia się tylko bazę danych (zmienne są pobierane z pliku `.env`).

### 1. Baza danych PostGIS
```bash
docker compose up -d postgis_db
```

Dane bazy są przechowywane w wolumenie Dockera (`postgis_data`), więc zostają po `docker compose stop` / `down` i restarcie komputera. Usuwa je dopiero `docker compose down -v`.

Jeśli wcześniej działał pełny stos, przed startem backendu w IDE zatrzymaj kontenery aplikacji (`docker compose stop backend frontend`), bo zajmują porty 8080 i 4200.

### 2. Backend (Spring Boot 4.1.0)
Wymagane: **JDK 21**.
```cmd
cd bicycle-route-planner-backend/backend
mvnw clean spring-boot:run
```

### 3. Frontend (Angular 22)
Wymagane: **Node.js 24**.
```cmd
cd bicycle-route-planner-frontend
npm ci
npm start
```

---

## 🧪 Testy Automatyczne

Testy uruchamia GitHub Actions przy każdym `push` i pull requeście zmieniającym kod danego modułu (osobne potoki dla backendu i frontendu).

**Backend** (JUnit 5) – 235 metod testowych (stan: październik 2026):
*   **Testy jednostkowe (131):** serwisy, walidatory i klasy bezpieczeństwa z użyciem Mockito.
*   **Testy warstwy webowej (57, `@WebMvcTest`):** endpointy REST, walidacja wejścia (DTO) i format błędów `ProblemDetail`, bez pełnego kontekstu Springa.
*   **Testy integracyjne (47):** pełne przepływy od żądania HTTP do bazy na kontenerze `postgis/postgis:15-3.4` uruchamianym przez **Testcontainers** (z migracjami Flyway); usługi zewnętrzne zastępuje **WireMock**.

**Frontend** (Vitest przez `ng test`) – testy komponentów, serwisów stanu, serwisów mapy, strażników i interceptorów.

### Uruchomienie testów:
```cmd
cd bicycle-route-planner-backend/backend
mvnw clean verify

cd bicycle-route-planner-frontend
npm test
```

---

## 📂 Struktura Projektu

```text
├── .github/workflows/                 # CI backendu i frontendu, publikacja obrazów Docker (GHCR) z gałęzi master
├── bicycle-route-planner-backend/
│   ├── BACKEND_DOCUMENTATION.md       # Szczegółowa dokumentacja klas backendu
│   └── backend/
│       ├── src/main/java/             # Warstwy: controller, service, repository, model, dto, security, validation
│       ├── src/main/resources/db/migration/   # Migracje Flyway (schemat bazy)
│       └── src/test/java/             # Testy jednostkowe, webowe i integracyjne
├── bicycle-route-planner-frontend/
│   ├── nginx.conf                     # Serwer frontendu w Dockerze (proxy /api, CSP, limit telemetrii)
│   └── src/app/
│       ├── components/                # Komponenty (mapa, panel boczny, modale, logowanie)
│       └── services/                  # Serwisy HTTP, stanu (*StateService) i mapy (Map*Service)
├── docker-compose.yml                 # Baza, backend i frontend
├── .env.example                       # Szablon zmiennych środowiskowych
└── README.md
```

---

## 📄 Licencja

Projekt zrealizowany w ramach pracy inżynierskiej. Wszelkie prawa zastrzeżone © 2026.
