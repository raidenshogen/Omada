# Omada Production Deployment Guide
# Zero production infra cost — everything runs locally via Docker Compose + Nginx SSL.

## Prerequisites
- Docker + Docker Compose
- Java 17 (for local `mvn spring-boot:run`)
- Node 20+ (for frontend dev)
- `openssl` (already on most systems; used for self-signed certs)

## 1. Generate SSL certificates (self-signed for local demo)
```bash
cd nginx/certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout omada.key -out omada.crt -subj "/CN=localhost"
```
For production, replace `omada.crt`/`omada.key` with Let's Encrypt certificates for your domain.

## 2. Configure environment
```bash
cp .env.example .env
# Edit .env: set JWT_SECRET to a strong 256-bit value (e.g. `openssl rand -base64 32`)
```

## 3. Start the production stack
```bash
# From the Omada repo root
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Services:
- `https://localhost` — Nginx SSL reverse proxy (port 443, redirects HTTP→HTTPS)
- `http://localhost:8080/api` — Spring Boot backend directly (port 8080)
- `http://localhost:27017` — MongoDB
- `http://localhost:6379` — Redis
- `http://localhost:9092` — Kafka

## 4. Frontend (dev mode, proxies to Nginx)
```bash
cd frontend
npm install
npm run dev   # Vite dev server proxies /api and /ws to Nginx
```
The Vite dev server forwards `/api` and `/ws` to `http://localhost:5173` → Nginx → backend.

## 5. Production hardening checklist
- [ ] Replace self-signed certs with Let's Encrypt (certbot) for your domain
- [ ] Set `JWT_SECRET` to a strong random value (never commit it)
- [ ] Set `SSL_KEYSTORE_PASSWORD` if you convert certs to PKCS12
- [ ] Lock CORS `allowed-origins` to your production domain (currently `localhost:5173`)
- [ ] Enable OWASP Dependency-Check and SonarQube in CI (see `.github/workflows/ci.yml`)

## 6. CI
GitHub Actions (`.github/workflows/ci.yml`) builds backend + frontend, runs tests,
runs OWASP Dependency-Check (fail on CVSS ≥ 7), and runs SonarQube scan
(requires `SONAR_TOKEN` + `SONAR_HOST_URL` repo secrets).

## Ports
| Service | Port | Purpose |
|---------|------|---------|
| Nginx HTTPS | 443 | SSL API + WebSocket entrypoint |
| Nginx HTTP | 80 | Redirects to HTTPS |
| Backend | 8080 | Spring Boot (direct, behind Nginx) |
| MongoDB | 27017 | Database |
| Redis | 6379 | Presence + rate-limit store |
| Kafka | 9092 | Activity event bus |
