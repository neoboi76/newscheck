# NewsCheck

**Real-Time Android News Aggregator using Java Spring Boot and Apache Kafka for event-driven data orchestration.**

NewsCheck is a mobile news platform that lets users follow the topics they care about and receive
breaking-news push notifications the moment stories are published — without ever pulling to refresh.
A scheduled backend aggregates articles from public news APIs once, fans them out through Apache Kafka
topics by category, and pushes personalized alerts to Android devices via Firebase Cloud Messaging.

---

## Why NewsCheck

Traditional mobile news apps rely on REST polling: every device repeatedly asks the server for updates,
wasting battery and mobile data, straining public-API rate limits, and still leaving users to manually
refresh for breaking news. NewsCheck replaces that model with an **event-driven architecture**: the
backend polls each news source once, publishes new articles to Kafka, and delivers them to interested
users in real time and asynchronously. This scales cleanly during breaking-news surges and keeps
per-user data usage minimal.

## Architecture

```
        NewsAPI.org            The Guardian API
              \                     /
               v                   v
        ┌───────────────────────────────────┐
        │             aggregator             │   Spring Boot · :8081
        │   scheduled fetch → dedup → map     │
        └──────────────────┬─────────────────┘
                           │  publishes ArticleEvent
                           v
                   ┌────────────────┐
                   │  Apache Kafka   │   topics: news.<category>
                   └────────┬───────┘
                           │  consumes ArticleEvent
                           v
        ┌───────────────────────────────────┐        ┌──────────────────┐
        │            news-server             │ ─────► │  Firebase Cloud   │ ─► Android devices
        │  REST API · JWT auth · FCM push     │        │   Messaging (FCM) │
        └──────────────────┬─────────────────┘        └──────────────────┘
                           │  REST + JWT
                           v
                   ┌────────────────┐
                   │  Android app    │   Kotlin
                   └────────────────┘

        PostgreSQL persists articles, users, subscriptions, and read-state.
```

- **aggregator** polls the news APIs on a schedule, removes duplicates, classifies each article into a
  category, and publishes an `ArticleEvent` to the matching Kafka topic.
- **news-server** consumes those events, stores articles, exposes the mobile REST API (auth, feed,
  subscriptions, read-state), and triggers FCM push notifications to subscribed users.
- **app** is the Kotlin Android client: users register, pick categories, browse a personalized feed, and
  receive breaking-news notifications.

## Features

- Topic/category subscriptions (technology, sports, business, entertainment, health, science, politics,
  general, and breaking).
- Personalized news feed with headlines, images, sources, and summaries.
- Real-time breaking-news **push notifications** via Firebase Cloud Messaging — no polling on the device.
- Aggregation from multiple public sources (**NewsAPI.org** and **The Guardian API**) with deduplication.
- Event distribution through **Apache Kafka** topics per category, designed for breaking-news surges.
- JWT-authenticated REST API with per-endpoint **rate limiting** and request **correlation IDs**.
- Read/unread tracking per user and API-key-protected internal ingestion endpoints.

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile client | Kotlin, Android (compileSdk 36, minSdk 26), Retrofit |
| Backend services | Java 21, Spring Boot, Spring Web, Spring Data JPA, Spring Security |
| Messaging | Apache Kafka (Confluent 7.5) with ZooKeeper |
| Database | PostgreSQL 15 |
| Push notifications | Firebase Cloud Messaging (FCM) |
| Auth | JWT (access tokens), API-key auth for internal endpoints |
| Build & runtime | Maven (services), Gradle (Android), Docker Compose |

## Repository Structure

```
newscheck/
├── aggregator/        Spring Boot service: fetch news APIs, dedup, publish to Kafka (:8081)
├── news-server/       Spring Boot service: consume Kafka, REST API, JWT auth, FCM push (:8080)
├── app/               Android client (Kotlin)
├── docker-compose.yml PostgreSQL, ZooKeeper, Kafka, Kafka UI, and both services
├── init.sql           Database schema bootstrap
├── .env.example       Required environment variables (copy to .env)
├── FCM_SETUP.md       Firebase Cloud Messaging setup guide
└── build.gradle / settings.gradle   Android build
```

### Modules

| Module | Responsibility |
|---|---|
| `aggregator` | `NewsFetcherScheduler` polls NewsAPI + Guardian; `NewsApiClient` / `GuardianApiClient` behind a `NewsSourceClient` interface; `ArticleService` dedups; `ArticlePublisher` / `ArticleEventProducer` publish to Kafka. Internal endpoints guarded by `ApiKeyAuthFilter`. |
| `news-server` | `ArticleEventConsumer` ingests Kafka events; `ArticleController` / `AuthController` / `UserController` serve the mobile API; JWT (`JwtAuthFilter`, `JwtUtils`, `SecurityConfig`), `RateLimitFilter`, `CorrelationIdFilter`; `NotificationService` sends FCM push; entities `User`, `Subscription`, `Article`, `ReadArticle`. |
| `app` | `MainActivity`, Retrofit `NewsCheckApi` with `AuthInterceptor`, data models, and `ArticleRepository`; category selection, feed, and FCM notifications. |

## News Categories and Kafka Topics

Each category maps to a Kafka topic named `news.<category>`:

`news.general` · `news.technology` · `news.sports` · `news.business` · `news.entertainment` ·
`news.health` · `news.science` · `news.politics` · `news.breaking`

## Getting Started

### Prerequisites

- **Docker** and **Docker Compose**
- **JDK 21** and **Maven** (to build/run the services outside Docker)
- **Android Studio** / Android SDK (to build the `app`)
- Free API keys: [NewsAPI.org](https://newsapi.org/) and
  [The Guardian Open Platform](https://open-platform.theguardian.com/access/)
- A Firebase project for push notifications (see [`FCM_SETUP.md`](FCM_SETUP.md))

### 1. Configure environment

Create a `.env` file in the repository root (copy the template) and fill in your own values:

```bash
cp .env.example .env
```

| Variable | Description |
|---|---|
| `NEWS_API_KEY` | API key for NewsAPI.org. |
| `GUARDIAN_API_KEY` | API key for The Guardian Open Platform. |
| `FCM_SERVER_KEY` | Firebase Cloud Messaging server key (for push notifications). |
| `JWT_SECRET` | Secret for signing JWT access tokens (use a long random string, 32+ chars). |

The `.env` file is git-ignored — never commit real keys or secrets.

### 2. Run the backend

```bash
docker compose up --build
```

This starts PostgreSQL, ZooKeeper, Kafka, Kafka UI, and both Spring Boot services.

| Service | URL |
|---|---|
| news-server (mobile REST API) | http://localhost:8080 |
| aggregator (internal ingestion) | http://localhost:8081 |
| Kafka UI | http://localhost:8090 |
| PostgreSQL | localhost:5432 (db `newscheck`) |
| Kafka broker (host) | localhost:29092 |

The aggregator polls the news APIs on a schedule (default: every 15 minutes) and publishes new articles
to Kafka; the news-server consumes them and serves the API.

### 3. Build and run the Android app

Open the project in Android Studio, add your Firebase `google-services.json` as described in
[`FCM_SETUP.md`](FCM_SETUP.md), point the app at the news-server base URL, then build and run on an
emulator or device (minSdk 26).

## Push Notifications

Push delivery uses Firebase Cloud Messaging. Firebase configuration is required for the Android app to
build and receive notifications — follow [`FCM_SETUP.md`](FCM_SETUP.md).

## Documentation

Project documentation (proposal, diagrams, and supporting PDFs) is available in the shared Google Drive
folder:

[NewsCheck documentation (Google Drive)](https://drive.google.com/drive/folders/1s_mYbEeQdm5gU128jY7dq7BhT8SJHs_a?usp=sharing)

## Authors

Developed as an academic capstone project by:

- Del Rosario, Jhun Lawrence
- Pajarito, Amar Jacob
- Sampoleo, Jose Carlos
- Sergio, Bernardo Casey
- Timbol, Dino Alfred

Submitted to **Prof. Leopoldo Gabriel**.
