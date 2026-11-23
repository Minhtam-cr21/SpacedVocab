# SpacedVocab API

A REST API for learning English vocabulary with flashcards.
Reviews are scheduled with the **SM-2 spaced repetition algorithm**: the better you remember a word, the longer until you see it again.

> 🚧 Work in progress — built milestone by milestone (see [Roadmap](#roadmap)).

## Tech stack

- **Java 21**, **Spring Boot 4.1** (Web MVC, Data JPA, Validation, Actuator)
- **PostgreSQL 17** + **Flyway** migrations
- **JUnit 5**, **Testcontainers**
- **Docker** / **Docker Compose**, **GitHub Actions** CI

## Getting started

Requirements: Docker Desktop (JDK 21 only needed if you run outside Docker).

```bash
cp .env.example .env          # then set DB_PASSWORD
docker compose up -d --build  # starts PostgreSQL + the API
```

| Service | URL |
|---|---|
| API health | http://localhost:8090/actuator/health |
| Adminer (DB UI, optional) | `docker compose --profile tools up -d` → http://localhost:8081 |

Useful commands:

```bash
docker compose logs -f app    # follow API logs
docker compose down           # stop (data is kept in the db-data volume)
docker compose down -v        # stop and wipe the database
./mvnw verify                 # run all tests (needs Docker for Testcontainers)
```

### Run the API from IntelliJ (debugging)

1. `docker compose up -d db` — start only the database.
2. Run `SpacedVocabApplication` with env var `DB_PASSWORD` set to the value in `.env`.

## Domain overview

| Actor | Main capabilities |
|---|---|
| **Guest** | Register, log in, browse public decks |
| **Learner** | Manage own decks & cards, import CSV, subscribe to public decks, daily review session, statistics & streak |
| **Admin** | Lock/unlock users, manage official decks, moderate reports |
| **System** | Run SM-2 after each review, nightly streak/statistics jobs |

Key design choice: **`Card`** (shared content) is separated from **`CardProgress`** (each learner's SM-2 state), so many learners can study the same deck without copying it.

## Roadmap

- [x] **M0** Project setup — Maven, Docker Compose, Flyway, CI
- [ ] **M1** Users & JWT authentication
- [ ] **M2** Deck & card CRUD with ownership rules
- [ ] **M3** SM-2 scheduling, due cards, review logs
- [ ] **M4** Public/official decks, subscriptions, CSV import, admin
- [ ] **M5** Statistics, streaks, Swagger docs

## Project structure

```
src/main/java/com/spacedvocab
├── controller   REST endpoints
├── service      business logic
├── repository   Spring Data JPA
├── entity       JPA entities
├── dto          request/response records
├── algorithm    spaced repetition (SM-2)
├── config       Spring configuration
└── exception    error types + global handler
```
