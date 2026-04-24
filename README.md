# URL Shortener on Kubernetes

A production-grade URL shortener built with **Java 21 + Spring Boot 3**, deployed on **Kubernetes (Minikube)**.
Generates collision-free short codes using PostgreSQL sequences + Base62 encoding.

> Design docs: [HLD](docs/HLD.md) | [LLD](docs/LLD.md)

---

## Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 15 |
| Cache / Queue | Redis 7 |
| Orchestration | Kubernetes (Minikube) |
| API Docs | Swagger UI at `/docs` |

---

## Quick Start

### 1. Prerequisites
```bash
brew install minikube kubectl maven
# + Docker Desktop running
```

### 2. Start Minikube
```bash
minikube start --driver=docker --cpus=4 --memory=4096
```

### 3. Build & Load Images
```bash
docker build -t minikube/shortener-api:latest ./shortener-api
docker build -t minikube/analytics-worker:latest ./analytics-worker

minikube image load minikube/shortener-api:latest
minikube image load minikube/analytics-worker:latest
```

> `postgres:15` and `redis:7-alpine` are pulled automatically by Kubernetes.

### 4. Deploy
```bash
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/configmap.yaml
kubectl apply -f k8s/base/secret.yaml
kubectl apply -f k8s/base/postgres.yaml
kubectl apply -f k8s/base/redis.yaml
kubectl apply -f k8s/base/shortener-api.yaml
kubectl apply -f k8s/base/analytics-worker.yaml

kubectl get pods -n url-shortener -w
```

### 5. Get Service URL
```bash
# Keep this terminal open
minikube service shortener-api-service -n url-shortener --url
```

---

## Test the API

Replace `<PORT>` with the port from Step 5.

```bash
# Health check
curl http://127.0.0.1:<PORT>/health

# Shorten a URL
curl -X POST http://127.0.0.1:<PORT>/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://google.com"}'
# Response: {"shortCode":"1","shortUrl":"/r/1"}

# Redirect
curl -L http://127.0.0.1:<PORT>/r/1
```

Swagger UI: `http://127.0.0.1:<PORT>/docs`

---

## Cleanup

```bash
docker rmi minikube/shortener-api:latest minikube/analytics-worker:latest
minikube delete
```

To restart fresh:
```bash
minikube start --driver=docker --cpus=4 --memory=4096
docker build -t minikube/shortener-api:latest ./shortener-api
docker build -t minikube/analytics-worker:latest ./analytics-worker
minikube image load minikube/shortener-api:latest
minikube image load minikube/analytics-worker:latest
kubectl apply -f k8s/base/
```

---

## K8s Concepts Covered

| Concept | What it is | Where used |
|---------|-----------|------------|
| **Namespace** | Isolated workspace that groups all related resources together. Like a folder for your app. | `namespace.yaml` — all resources live under `url-shortener` |
| **Pod** | The smallest unit in K8s — one running container instance. K8s manages pods, not containers directly. | Every service runs as one or more pods |
| **Deployment** | Manages a set of identical pods. Ensures desired number are always running and handles updates. | `shortener-api`, `redis`, `analytics-worker` |
| **StatefulSet** | Like a Deployment but for stateful apps — gives each pod a stable identity and persistent storage. | `postgres` — needs stable storage across restarts |
| **PersistentVolumeClaim (PVC)** | Reserves disk storage that survives pod restarts. Without this, data is lost when a pod dies. | `postgres-pvc` — 1Gi reserved for database files |
| **Service (ClusterIP)** | Gives pods a stable internal DNS name so other pods can reach them by name, not IP. | `postgres-service`, `redis-service`, `shortener-api-service` |
| **Ingress** | External HTTP gateway. Routes incoming requests to the right internal service based on host/path. | `ingress.yaml` — routes `short.local` → API |
| **ConfigMap** | Stores non-sensitive config (hostnames, ports) that pods read as environment variables. | `configmap.yaml` — DB host, Redis host, ports |
| **Secret** | Same as ConfigMap but for sensitive values — stored base64 encoded. | `secret.yaml` — database password |
| **Readiness Probe** | K8s checks this before sending traffic to a pod. Pod only receives requests when probe passes. | API checks `/health`, Postgres checks `pg_isready` |
| **Liveness Probe** | K8s checks this to know if a pod is still healthy. Restarts the pod if it fails. | API checks `/health`, Redis checks `redis-cli ping` |
| **Rolling Update** | Replaces pods one at a time during a deploy — keeps the app running with zero downtime. | `shortener-api` deployment strategy |
| **HPA** | Horizontal Pod Autoscaler — automatically adds/removes pods based on CPU or memory usage. | Scales API from 3 → 10 pods at 50% CPU |
| **Resource Limits** | Sets CPU and memory budgets per container. Prevents one pod from starving others. | API: 100m–500m CPU, 128Mi–256Mi memory |
