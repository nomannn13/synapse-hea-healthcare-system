# Online Deployment

This project is ready for Docker-based hosting and Railway deployment. The Docker production setup runs:

- React frontend through Nginx
- Spring Boot backend
- MySQL 8.4
- Redis 7.4
- Persistent volumes for MySQL, Redis, and uploaded clinical documents

## Docker Production

Copy the production example:

```bash
cp .env.production.example .env
```

Edit `.env` and set strong values for:

- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `FRONTEND_URL`

For one-domain Docker hosting, set both URL values to your public HTTPS domain:

```env
CORS_ALLOWED_ORIGINS=https://hea.example.com
FRONTEND_URL=https://hea.example.com
```

Generate a strong JWT secret with:

```bash
openssl rand -base64 48
```

Start the app:

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Open:

- Web app: `http://your-server-ip` or your configured domain
- API docs: `http://your-server-ip/swagger-ui.html`
- Health check: `http://your-server-ip/actuator/health`

Add HTTPS with a reverse proxy such as Caddy, Nginx Proxy Manager, Traefik, or Cloudflare Tunnel.

## Railway Notes

Use separate Railway services for:

- Backend from `backend/Dockerfile`
- Frontend from `frontend/Dockerfile`
- MySQL
- Redis

Backend environment variables:

```env
DB_URL=jdbc:mysql://${MYSQLHOST}:${MYSQLPORT}/${MYSQLDATABASE}?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=${MYSQLUSER}
DB_PASSWORD=${MYSQLPASSWORD}
REDIS_HOST=${REDISHOST}
REDIS_PORT=${REDISPORT}
JWT_SECRET=<generated-secret>
SECURE_COOKIES=true
REFRESH_COOKIE_SAMESITE=None
CORS_ALLOWED_ORIGINS=https://<frontend-domain>
FRONTEND_URL=https://<frontend-domain>
DOCUMENT_STORAGE_ROOT=/app/data/uploads
MAIL_ENABLED=false
```

Frontend build variable:

```env
VITE_API_BASE_URL=https://<backend-domain>/api/v1
```

Attach persistent storage to the backend at:

```text
/app/data/uploads
```

## Demo accounts

The database migrations seed these demo accounts:

| Role | Email | Password |
| --- | --- | --- |
| Patient | `patient@synapse.local` | `Password123!` |
| Doctor | `doctor@synapse.local` | `Password123!` |
| Administrator | `admin@synapse.local` | `Password123!` |

Change or remove demo accounts before using the system with real data.

## Production cautions

This is educational hospital-management software. Before using real patient data, add professional review for privacy, security, backups, audit retention, consent, encrypted storage, and healthcare compliance.
