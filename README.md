# URL Shortener on Kubernetes
A hands-on K8s learning project built with **Java + Spring Boot**, covering Pods, Deployments, StatefulSets, Services, Ingress, ConfigMaps, Secrets, Probes, and HPA.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 15 (StatefulSet) |
| Cache / Queue | Redis 7 |
| Container Runtime | Docker |
| Orchestration | Kubernetes (Minikube) |
| API Docs | Swagger UI (auto-generated at `/docs`) |

## Architecture
```
User → Ingress (short.local) → shortener-api (3 pods) → Redis (cache)
                                        ↓
                                   Postgres (StatefulSet)
                                        ↑
                              analytics-worker (1 pod) ← Redis click_events queue
```

## Folder Structure
```
url-shortener/
├── shortener-api/                          ← Spring Boot REST API
│   ├── src/
│   │   ├── main/java/com/shortener/
│   │   │   ├── ShortenerApplication.java   ← entry point
│   │   │   ├── controller/
│   │   │   │   └── UrlController.java      ← /health, /shorten, /r/{code}
│   │   │   ├── service/
│   │   │   │   ├── UrlService.java         ← business logic
│   │   │   │   └── AnalyticsService.java   ← pushes to Redis queue
│   │   │   ├── repository/
│   │   │   │   └── UrlRepository.java      ← JPA DB queries
│   │   │   ├── model/
│   │   │   │   └── Url.java                ← urls table entity
│   │   │   ├── dto/
│   │   │   │   ├── ShortenRequest.java     ← request body with validation
│   │   │   │   └── ShortenResponse.java    ← response body
│   │   │   └── config/
│   │   │       └── RedisConfig.java        ← Redis bean setup
│   │   ├── test/java/com/shortener/
│   │   │   ├── controller/
│   │   │   │   └── UrlControllerTest.java  ← endpoint tests (MockMvc)
│   │   │   └── service/
│   │   │       └── UrlServiceTest.java     ← unit tests (Mockito)
│   │   └── resources/
│   │       └── application.properties
│   ├── pom.xml
│   └── Dockerfile
├── analytics-worker/                       ← Spring Boot background worker
│   ├── src/
│   │   ├── main/java/com/analytics/
│   │   │   ├── AnalyticsWorkerApplication.java
│   │   │   ├── worker/
│   │   │   │   └── ClickEventWorker.java   ← Redis poll → saves clicks to DB
│   │   │   ├── model/
│   │   │   │   └── Click.java              ← clicks table entity
│   │   │   └── repository/
│   │   │       └── ClickRepository.java
│   │   ├── test/java/com/analytics/
│   │   │   └── worker/
│   │   │       └── ClickEventWorkerTest.java
│   │   └── resources/
│   │       └── application.properties
│   ├── pom.xml
│   └── Dockerfile
└── k8s/
    ├── base/
    │   ├── namespace.yaml
    │   ├── configmap.yaml
    │   ├── secret.yaml
    │   ├── postgres.yaml
    │   ├── redis.yaml
    │   ├── shortener-api.yaml
    │   └── analytics-worker.yaml
    ├── ingress/
    │   └── ingress.yaml
    └── hpa/
        └── hpa.yaml
```

---

## Step 1 — Prerequisites

```bash
# Install minikube
brew install minikube        # macOS
# or: https://minikube.sigs.k8s.io/docs/start/

# Install kubectl
brew install kubectl

# Install k9s (optional but great for visualization)
brew install k9s

# Start your cluster
minikube start --driver=docker --cpus=4 --memory=4096
```

---

## Step 2 — Build Docker Images

```bash
# Build shortener-api (multi-stage: Maven build → JRE runtime)
cd shortener-api
docker build -t minikube/shortener-api:latest .

# Build analytics-worker
cd ../analytics-worker
docker build -t minikube/analytics-worker:latest .

# Load images into Minikube (required on macOS — Minikube has its own registry)
minikube image load minikube/shortener-api:latest
minikube image load minikube/analytics-worker:latest
```

> **Why `minikube image load`?** Docker Desktop and Minikube have separate image stores.
> Without loading, Kubernetes tries to pull from Docker Hub and fails with `ImagePullBackOff`.

---

## Step 3 — Deploy Everything

```bash
# Apply in order (namespace first, then config, then apps)
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/configmap.yaml
kubectl apply -f k8s/base/secret.yaml
kubectl apply -f k8s/base/postgres.yaml
kubectl apply -f k8s/base/redis.yaml
kubectl apply -f k8s/base/shortener-api.yaml
kubectl apply -f k8s/base/analytics-worker.yaml

# Watch everything come up
kubectl get pods -n url-shortener -w
```

---

## Step 4 — Enable Ingress

```bash
# Enable nginx ingress addon in minikube
minikube addons enable ingress

# Apply ingress manifest
kubectl apply -f k8s/ingress/ingress.yaml

# Add short.local to your /etc/hosts
echo "$(minikube ip) short.local" | sudo tee -a /etc/hosts
```

> **On macOS** you also need `minikube tunnel` (in a separate terminal) for traffic to reach the cluster.
> Alternatively, skip Ingress and use `minikube service shortener-api-service -n url-shortener --url` for a direct localhost URL.

---

## Step 5 — Test the API

```bash
# Get service URL (no tunnel needed)
minikube service shortener-api-service -n url-shortener --url
# e.g. http://127.0.0.1:51034

# Health check
curl http://127.0.0.1:51034/health

# Shorten a URL
curl -X POST http://127.0.0.1:51034/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://github.com"}'

# Response: {"shortCode": "aB3xYz", "shortUrl": "/r/aB3xYz"}

# Use the short URL (will redirect)
curl -L http://127.0.0.1:51034/r/aB3xYz
```

**Swagger UI** — open in browser:
```
http://127.0.0.1:51034/docs
```

---

## Step 6 — Simulate Real K8s Scenarios

### 6a. Kill a pod and watch K8s restart it
```bash
kubectl get pods -n url-shortener
kubectl delete pod <shortener-api-pod-name> -n url-shortener
kubectl get pods -n url-shortener -w
```
**What you learn:** ReplicaSet ensures desired state is always maintained.

---

### 6b. Manual scaling
```bash
kubectl scale deployment shortener-api --replicas=6 -n url-shortener
kubectl scale deployment shortener-api --replicas=3 -n url-shortener
kubectl get pods -n url-shortener -w
```
**What you learn:** Deployments manage the replica count declaratively.

---

### 6c. Rolling update (zero-downtime deploy)
```bash
# Rebuild with a new tag after making code changes
cd shortener-api
docker build -t minikube/shortener-api:v2 .
minikube image load minikube/shortener-api:v2

# Update the image in K8s
kubectl set image deployment/shortener-api \
  shortener-api=minikube/shortener-api:v2 \
  -n url-shortener

# Watch pods roll one by one
kubectl rollout status deployment/shortener-api -n url-shortener

# Rollback if something breaks
kubectl rollout undo deployment/shortener-api -n url-shortener
```
**What you learn:** RollingUpdate strategy + readiness probes ensure zero downtime.

---

### 6d. Crash a pod intentionally
```bash
kubectl delete pod -l app=postgres -n url-shortener
kubectl get pods -n url-shortener -w
kubectl describe pod <api-pod> -n url-shortener
```
**What you learn:** Liveness/readiness probes, CrashLoopBackOff, self-healing.

---

## Step 7 — Enable HPA (Auto-scaling)

```bash
minikube addons enable metrics-server
kubectl apply -f k8s/hpa/hpa.yaml
kubectl get hpa -n url-shortener -w

# Load test to trigger scale-up (install hey: brew install hey)
hey -z 60s -c 50 http://127.0.0.1:51034/health

# Watch pods scale automatically from 3 → up to 10
kubectl get pods -n url-shortener -w
```
**What you learn:** HPA watches CPU metrics and scales deployments automatically.

---

## Useful Commands

```bash
# See all resources in namespace
kubectl get all -n url-shortener

# Describe a pod (events, probes, restarts)
kubectl describe pod <pod-name> -n url-shortener

# Stream logs from all API pods
kubectl logs -l app=shortener-api -n url-shortener -f

# Exec into a running pod
kubectl exec -it <pod-name> -n url-shortener -- bash

# Check Postgres records
kubectl exec -it postgres-0 -n url-shortener -- psql -U postgres -d urlshortener -c "SELECT * FROM urls;"

# Check Redis keys
kubectl exec -it -n url-shortener deployment/redis -- redis-cli KEYS "*"

# See resource usage
kubectl top pods -n url-shortener

# Open k9s dashboard
k9s -n url-shortener
```

---

## K8s Concepts Covered

| Concept | Where |
|---------|-------|
| Namespace | Isolates all resources under `url-shortener` |
| Pod | Every running container instance |
| Deployment | shortener-api, redis, analytics-worker |
| StatefulSet | postgres (stable identity + persistent storage) |
| PVC | Postgres data survives pod restarts |
| Service (ClusterIP) | Internal DNS between components |
| Ingress | External HTTP routing via nginx |
| ConfigMap | DB host/port/name config |
| Secret | DB password (base64 encoded) |
| Readiness Probe | API only gets traffic when /health is 200 |
| Liveness Probe | Container restarted if it becomes unhealthy |
| Rolling Update | Zero-downtime deploys |
| HPA | Auto-scale API based on CPU load |
| Resource Requests/Limits | CPU/memory budgets per container |
