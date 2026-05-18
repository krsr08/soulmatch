# SoulMatch Architecture Foundation

This document explains the current SoulMatch architecture and the target extensible design. It is written for future developers so they can understand where each responsibility belongs before changing code.

## Cost-Safe Architecture Rule

SoulMatch is still in development. Keep monthly Azure cost under the agreed development budget by default.

Do not add these resources without explicit approval:

- Extra VM.
- Load balancer.
- Azure Application Gateway / paid WAF.
- Managed PostgreSQL.
- Managed Redis.
- OpenSearch / Elastic / Azure AI Search.
- High-volume Log Analytics ingestion.

Approved low-cost direction:

- Keep the current single VM Docker deployment.
- Keep Nginx as the public edge.
- Add extension placeholders in code/docs first.
- Move to paid managed services only when moving toward live public launch.

## Current Production Architecture

Current deployed commit: tracked in `/home/azureuser/soulmatch/.soulmatch-deployed-version.json` on the VM.

```mermaid
flowchart TB
  Android["Android App"] --> Edge["Nginx reverse proxy"]
  AdminWeb["Admin Web - React"] --> Edge

  Edge --> Auth["Auth Service - Node.js"]
  Edge --> Profile["Profile Service - Node.js"]
  Edge --> Match["Matching Service - Python FastAPI"]
  Edge --> Search["Search Service - Node.js"]
  Edge --> Chat["Chat Service - Node.js / Socket.IO"]
  Edge --> Notify["Notification Service - Node.js"]
  Edge --> Pay["Payment Service - Node.js"]
  Edge --> AdminApi["Admin Service - Node.js"]
  Edge --> AdminWebRuntime["Admin Web Container - Nginx"]

  Auth --> Postgres["PostgreSQL"]
  Profile --> Postgres
  Match --> Postgres
  Search --> Postgres
  Pay --> Postgres
  AdminApi --> Postgres

  Chat --> Mongo["MongoDB - chat messages"]
  Auth --> Redis["Redis - OTP/session/counters"]
  Match --> Redis
  Chat --> Redis
  Profile --> Redis

  Profile --> Uploads["Profile uploads volume"]
  Notify --> FCM["Firebase FCM"]
  Auth --> Twilio["Twilio OTP"]
  Pay --> Razorpay["Razorpay"]
```

## Target Extensible Architecture

This is the direction, not an immediate infrastructure change.

```mermaid
flowchart TB
  subgraph Clients
    Android["Android"]
    AdminWeb["Admin Web"]
  end

  subgraph Edge
    Nginx["Nginx + TLS + optional WAF rules"]
    BFF["Optional Mobile BFF - future"]
  end

  subgraph AppTier["App tier - current single VM, future scalable"]
    Services["Stateless services: Auth, Profile, Match, Search, Chat, Pay, Notify, Admin"]
  end

  subgraph Async
    Queue["Redis Streams / BullMQ - future"]
    Workers["Workers: notify, index, trust, webhooks - future"]
  end

  subgraph Data
    PG["PostgreSQL - current primary store"]
    OS["OpenSearch - optional future search"]
    MG["MongoDB - chat"]
    RD["Redis"]
    Blob["Blob storage - future photos/docs"]
  end

  subgraph External
    FCM["FCM"]
    Twilio["Twilio"]
    Razorpay["Razorpay"]
  end

  Android --> Nginx
  AdminWeb --> Nginx
  Nginx --> BFF
  Nginx --> Services
  BFF --> Services
  Services --> PG
  Services --> MG
  Services --> RD
  Services --> Blob
  Services --> Queue
  Queue --> Workers
  Workers --> PG
  Workers --> OS
  Workers --> FCM
  Services --> Twilio
  Services --> Razorpay
```

## Implemented Now vs Future Placeholder

| Capability | Current Implementation | Future Extension Point | Status |
| --- | --- | --- | --- |
| Edge routing | Nginx on single VM | TLS, stricter WAF rules, domain routing | Partially implemented |
| Mobile API | Android calls backend services via Nginx route paths | `backend/mobile-bff/` can aggregate mobile-specific calls | Placeholder only |
| Auth | `backend/auth-service` | Add device risk scoring and stronger duplicate detection | Implemented with room to extend |
| Profiles | `backend/profile-service` | Move media/documents to Blob adapter | Implemented with room to extend |
| Matching | `backend/matching-service` | Worker-generated rankings and async recalculation | Implemented with room to extend |
| Search | PostgreSQL filters/search | OpenSearch adapter behind `search-service` | Placeholder only |
| Chat | Node service + Mongo + Socket.IO | Redis adapter already prepared; future moderation worker | Partially implemented |
| Notifications | Notification service + FCM | Outbox worker, queue-backed retries | Partially implemented |
| Payments | Razorpay order/verify/webhook | Async webhook worker and invoice pipeline | Partially implemented |
| Admin | React admin + admin API | SSO/MFA enforcement and stronger admin audit workflows | Partially implemented |
| Observability | `/metrics`, docs, dashboard JSON | Run Prometheus/Grafana/alerts in production | Placeholder/ready |
| Backups | Backup/restore scripts | Automated scheduled backup + restore drill | Placeholder/ready |
| Operational config | Private `operations` config and `architectureFlags` helper | Adapter activation gates for BFF, workers, blob, search, WAF, observability | Placeholder/ready |

## Request Flow

### Member app flow

```mermaid
sequenceDiagram
  participant App as Android App
  participant Edge as Nginx
  participant Auth as Auth Service
  participant Profile as Profile Service
  participant Match as Matching Service
  participant DB as PostgreSQL

  App->>Edge: OTP login / role / profile request
  Edge->>Auth: /api/v1/auth/*
  Auth->>DB: user/session/consent records
  Auth-->>App: access + refresh token
  App->>Edge: profile or matches request with JWT
  Edge->>Profile: /api/v1/profile/*
  Profile->>DB: profile/privacy/documents/preferences
  App->>Edge: best matches
  Edge->>Match: /api/v1/matches/*
  Match->>DB: profile + preference reads
  Match-->>App: filtered recommendations
```

### Admin flow

```mermaid
sequenceDiagram
  participant Admin as Admin Web
  participant Edge as Nginx
  participant API as Admin Service
  participant DB as PostgreSQL
  participant Services as Backend Services

  Admin->>Edge: login
  Edge->>API: /api/v1/admin/login
  API->>DB: admin user/session/audit
  API-->>Admin: httpOnly session + csrf token
  Admin->>Edge: verify / moderate / configure
  Edge->>API: admin routes
  API->>DB: write audit + update records
  API->>Services: service health/config reads as needed
```

## Async Flow Placeholder

Current code already has some outbox/worker foundation, but do not move all request-path logic into queues until each feature is tested.

Future worker location:

```text
backend/
  workers/
    notification-worker/
    trust-score-worker/
    search-index-worker/
    payment-webhook-worker/
```

Future flow:

```mermaid
flowchart LR
  API["Service receives request"] --> DB["Write primary transaction"]
  DB --> Outbox["Create outbox event"]
  Outbox --> Queue["Redis Streams / BullMQ"]
  Queue --> Worker["Worker processes event"]
  Worker --> SideEffect["FCM / OpenSearch / trust score / webhook retry"]
  Worker --> Audit["Write status/audit"]
```

## Phase Roadmap

| Phase | Goal | Infrastructure Cost |
| --- | --- | --- |
| Phase 0 | Developer architecture docs and boundaries | 0 |
| Phase 1 | Domain + HTTPS on current VM | Very low |
| Phase 2 | Backup/restore discipline and restore drill | Very low |
| Phase 3 | Workers using current Redis/VM | Very low |
| Phase 4 | Blob storage adapter for uploads | Low |
| Phase 5 | Lightweight monitoring activation | Low |
| Future | OpenSearch only if PostgreSQL search becomes slow | Medium/high |
| Future | Extra VM/load balancer | Ignored for now |
| Future | Managed DB/Redis/WAF | Later live-production stage |
