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
- **Cel pracy:** Zaprojektowanie i implementacja aplikacji webowej do zaawansowanego planowania oraz analizy przestrzennej tras rowerowych, zintegrowanej z relacyjną bazą danych geograficznych. Aplikacja oferuje użytkownikom system kont, interaktywną mapę do przeglądania i modyfikacji geometrii tras, jak również narzędzia do ich automatycznego planowania oraz eksportu.

### Zakres pracy:
1. **Relacyjna Baza Danych Przestrzennych:** Schemat bazy przystosowany do natywnego składowania, indeksowania przestrzennego (GIST) oraz przetwarzania obiektów geograficznych 3D (`LineString Z`, `Polygon` z SRID 4326).
2. **Warstwa Serwerowa REST API:** Implementacja backendu w oparciu o Java 21 i Spring Boot 4.1.0 integrującego silnik routingu wektorowego (OpenRouteService).
3. **Reaktywna Aplikacja Kliencka:** Opracowanie interfejsu użytkownika w Angular 22 opartego o Standalone Components, Signals oraz bibliotekę Leaflet.
4. **Strefy Omijane (Avoidance Zones):** Mechanizm swobodnego kreślenia poligonów na mapie z dynamicznym uwzględnianiem barier w kalkulacji trasy.
5. **Eksport Danych:** Transformacja geometrii przestrzennych do formatu GPS Exchange Format (`.gpx`).
6. **Generator Pętli (Loop Generator):** Automatyczne wyznaczanie tras okrężnych na podstawie punktu startowego i zadanego dystansu.
7. **Autoryzacja i Katalogowanie:** Bezpieczny system kont (JWT) z kategoryzacją tras w ramach płaskiej struktury kolekcji/folderów.
8. **Narzędzia Przestrzenne (Funkcje Dodatkowe):** Integracja z zewnętrznym API Photon (Komoot) w celu geokodowania i wyszukiwania adresów oraz obsługa geolokalizacji po stronie przeglądarki (GPS).

---



## 🏗️ Stos Technologiczny

| Warstwa | Technologia | Zastosowanie |
|---|---|---|
| **Backend** | Java 21, Spring Boot 4.1.x | Logika biznesowa REST API, Spring Security, Spring Data JPA |
| **Baza Danych** | PostgreSQL 15, PostGIS 3.4 | Składowanie geometrii `LineString Z` |
| **Frontend** | TypeScript, Angular 22 | Interfejs klienta, reaktywne zarządzanie stanem (RxJS, Signals) |
| **Moduł Mapowy** | Leaflet, Leaflet-Draw | Wizualizacja wektorowa, interakcja przestrzenna i obsługa zdarzeń mapy |
| **Nawigacja** | OpenRouteService API | Zewnętrzny silnik wyznaczania geometrii tras i omijania stref |
| **Testy** | JUnit 5, Testcontainers, Mockito | Izolowane testy bazy PostGIS (123 testy backendu) |
| **Infrastruktura** | Docker, Docker Compose | Środowisko uruchomieniowe (multi-stage builds, tryb unprivileged: `nginx` & `spring`) |

---

## 🚀 Szybki Start (Docker Compose)

Cały stos aplikacji (PostGIS, Backend, Frontend) można uruchomić w trybie produkcyjnym za pomocą jednej komendy:

### Wymagania:
- Zainstalowany [Docker Desktop](https://www.docker.com/products/docker-desktop/) (lub Docker Engine z Docker Compose).

### Uruchomienie:
```bash
# 1. Klonowanie repozytorium
git clone https://github.com/BoguszM-ZG/bicycle-route-planner.git
cd bicycle-route-planner

# 2. Utworzenie pliku ze zmiennymi środowiskowymi
cp .env.example .env

# UWAGA: Przed uruchomieniem edytuj plik .env i ustaw:
# - ORS_API_KEY (darmowy klucz uzyskasz na: https://openrouteservice.org/dev/#/signup)
# - Własny JWT_SECRET oraz dane dostępowe do bazy danych

# 3. Uruchomienie kontenerów w tle
docker compose up -d
```

Dostęp do usług:
- 🌐 **Frontend (Web UI):** [http://localhost:4200](http://localhost:4200)
- ⚙️ **Backend REST API:** [http://localhost:8080](http://localhost:8080)
- 📖 **Swagger / OpenAPI Documentation:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- 🗄️ **Baza PostGIS:** `localhost:5432`

---

## 🛠️ Uruchomienie Developerskie (Lokalne)

W przypadku pracy deweloperskiej w IDE, samą bazę danych uruchamia się bezpiecznie przez Compose (zmienne pobierane są automatycznie z pliku `.env`):

### 1. Uruchomienie samej bazy danych PostGIS
```bash
docker compose up postgis_db -d
```

### 2. Backend (Spring Boot 4.1.0)
Wymagane: **JDK 21**.
```bash
cd bicycle-route-planner-backend/backend
./mvnw clean spring-boot:run
```

### 3. Frontend (Angular 22)
Wymagane: **Node.js 22+**.
```bash
cd bicycle-route-planner-frontend
npm ci
npm start
```

---

## 🧪 Testy Automatyczne i Jakość Kodu

Wszystkie testy są automatycznie weryfikowane przez pipeline CI/CD na GitHub Actions przy każdym commicie. Architektura testów backendu (łącznie 123 scenariusze) została podzielona na trzy poziomy izolacji:

*   **Testy jednostkowe (Unit Tests):** Szybka weryfikacja logiki biznesowej w serwisach z wykorzystaniem JUnit 5 oraz Mockito.
*   **Testy wycinkowe (Slice Tests):** Izolowane testowanie warstwy webowej (`@WebMvcTest`) w celu weryfikacji endpointów REST, walidacji wejścia (DTO) oraz globalnej obsługi błędów (RFC 7807), bez podnoszenia całego kontekstu Springa.
*   **Testy integracyjne (Integration Tests):** Kompleksowa weryfikacja pełnych procesów biznesowych i persystencji danych przy użyciu biblioteki **Testcontainers**, podnoszącej na czas testów rzeczywisty kontener z bazą PostGIS.

### Uruchomienie zestawu testów:
```bash
cd bicycle-route-planner-backend/backend
./mvnw clean verify
  ```

---

## 📂 Struktura Projektu

```text
├── .github/workflows/                 # Pipeline'y CI/CD (Backend CI, Frontend CI, CD GHCR)
├── bicycle-route-planner-backend/     # Kod backendu (Java 21, Spring Boot 4.1.0)
│   └── backend/
│       ├── src/main/java/             # Architektura warstwowa (Controller, Service, Domain)
│       └── src/test/java/             # Testy jednostkowe oraz integracyjne Testcontainers
├── bicycle-route-planner-frontend/    # Kod frontendu (Angular 22, Signals, Leaflet)
│   └── src/app/
│       ├── components/                # Standalone Components (Map, Sidebar, Modals)
│       └── services/                  # Serwisy domenowe i stanowe (*StateService)
├── docker-compose.yml                 # Definicja kontenerów dla środowiska developerskiego
├── .env.example                       # Szablon zmiennych środowiskowych
└── README.md                          # Główna dokumentacja projektu
```

---

## 📄 Licencja

Projekt zrealizowany w ramach pracy inżynierskiej. Wszelkie prawa zastrzeżone © 2026.
