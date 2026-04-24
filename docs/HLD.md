# High Level Design — URL Shortener

## Architecture

```mermaid
graph TD
    User -->|HTTP| Ingress
    Ingress --> API[shortener-api\n3 pods]
    API -->|cache + queue| Redis
    API -->|store URLs| Postgres[(PostgreSQL\nStatefulSet)]
    Redis -->|click_events| Worker[analytics-worker]
    Worker -->|save clicks| Postgres
```

---

## Components

| Component | Role |
|-----------|------|
| **shortener-api** | REST API — shortens URLs, handles redirects, pushes click events |
| **analytics-worker** | Background worker — consumes Redis queue, saves clicks to DB |
| **PostgreSQL** | Primary store for URLs and clicks. StatefulSet with 1Gi PVC |
| **Redis** | URL cache (1hr TTL) + click_events queue |
| **Nginx Ingress** | Routes `short.local` → API |

---

## Data Flows

### Shorten a URL

```mermaid
sequenceDiagram
    Client->>API: POST /shorten {url}
    API->>PostgreSQL: SELECT nextval('url_code_seq')
    PostgreSQL-->>API: 42
    API->>API: Base62(42) = "G"
    API->>PostgreSQL: INSERT urls (code="G", url)
    API->>Redis: SET "G" url EX 3600
    API-->>Client: {shortCode: "G", shortUrl: "/r/G"}
```

### Redirect

```mermaid
sequenceDiagram
    Client->>API: GET /r/{code}
    API->>Redis: GET code
    alt cache hit
        Redis-->>API: original URL
    else cache miss
        API->>PostgreSQL: SELECT WHERE code=?
        PostgreSQL-->>API: original URL
        API->>Redis: SET code url EX 3600
    end
    API->>Redis: RPUSH click_events code
    API-->>Client: 302 Redirect
```

### Analytics

```mermaid
sequenceDiagram
    loop every 5s
        Worker->>Redis: BLPOP click_events
        Redis-->>Worker: code
        Worker->>PostgreSQL: INSERT clicks (code)
    end
```

---

## Collision-Free Code Generation

Random codes risk collisions. Instead we use a PostgreSQL atomic sequence + Base62 encoding — same approach as bit.ly.

```
nextval() → 1, 2, 3 ... 56 billion  (atomic, persistent across restarts)
Base62     → "1", "2", "3" ... "ZZzzzz"
```

**Capacity:** 6-char Base62 = 56 billion unique codes.

---

## Key Design Decisions

| Decision | Reason |
|----------|--------|
| PostgreSQL sequence for codes | Atomic across pods and threads — zero collision risk |
| Redis cache on redirect | Avoids DB hit on every click — sub-millisecond reads |
| Redis list for analytics | Decouples click recording from the redirect response |
| StatefulSet for Postgres | Stable identity + data survives pod restarts |
| 3 API replicas + HPA | High availability, auto-scales 3→10 on CPU load |
