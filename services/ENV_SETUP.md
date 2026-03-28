# Services Environment Setup Guide

## Quick Start with .env Files

### Option 1: Using .env.local (Recommended for Development)

The `.env.local` file contains example values for local development. You can use it as-is or customize it:

```bash
# 1. Use .env.local directly (no changes needed)
docker-compose --env-file .env.local up -d

# OR

# 2. Copy to .env and customize for your environment
cp .env.local .env
# Edit .env with your preferred values
docker-compose --env-file .env up -d

# OR

# 3. Use default docker-compose (uses hardcoded values in compose file)
docker-compose up -d
```

### Option 2: Use .env.example Template

Create your own `.env` file from the template:

```bash
# Copy template
cp .env.example .env

# Edit with your values
nano .env

# Start services
docker-compose --env-file .env up -d
```

### Environment Variables Available

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_USER` | niskuldeep | PostgreSQL username |
| `DB_PASSWORD` | niskuldeep | PostgreSQL password |
| `DB_HOST` | localhost | PostgreSQL hostname |
| `DB_PORT` | 5432 | PostgreSQL port |
| `MONGO_USER` | niskuldeep | MongoDB username |
| `MONGO_PASSWORD` | niskuldeep | MongoDB password |
| `MONGO_HOST` | localhost | MongoDB hostname |
| `MONGO_PORT` | 27017 | MongoDB port |
| `REDIS_HOST` | localhost | Redis hostname |
| `REDIS_PORT` | 6379 | Redis port |
| `ELASTICSEARCH_HOST` | localhost | Elasticsearch hostname |
| `ELASTICSEARCH_PORT` | 9200 | Elasticsearch port |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka broker |
| `MAIL_HOST` | localhost | Mail server hostname |
| `MAIL_PORT` | 1025 | Mail server port |
| `MAIL_USERNAME` | example | Mail username |
| `MAIL_PASSWORD` | example | Mail password |
| `GRAFANA_ADMIN_PASSWORD` | admin | Grafana admin password |
| `SPRING_PROFILES_ACTIVE` | dev | Spring profile |

## Development Workflow

### 1. First Time Setup

```bash
# Clone repository
cd services

# Option A: Use .env.local directly (recommended)
docker-compose --env-file .env.local up -d

# Option B: Create custom .env
cp .env.local .env
# Edit .env if needed
docker-compose --env-file .env up -d

# Wait for services to start (~60 seconds)
sleep 30

# Verify all services are running
curl http://localhost:8761  # Eureka should respond
```

### 2. Access Services

```bash
# API Gateway
open http://localhost:8222/swagger-ui.html

# Prometheus
open http://localhost:9090

# Grafana (admin/admin)
open http://localhost:3000

# Order Service Swagger
open http://localhost:8070/swagger-ui.html

# Kibana
open http://localhost:5601

# PostgreSQL (via PgAdmin)
open http://localhost:5050

# MongoDB (via Mongo Express)
open http://localhost:8081
```

### 3. Customize Credentials (Optional)

If you want to use different passwords for local development:

```bash
# Copy template to .env
cp .env.example .env

# Edit with your preferred credentials
cat > .env << EOF
DB_USER=myuser
DB_PASSWORD=mypassword
MONGO_USER=mongoadmin
MONGO_PASSWORD=mongosecure
GRAFANA_ADMIN_PASSWORD=myadmin123
...
EOF

# Start with custom env
docker-compose --env-file .env up -d
```

### 4. Stop Services

```bash
# Stop and keep data
docker-compose down

# Stop and remove all data
docker-compose down -v
```

## Security Notes

⚠️ **Important:**
- `.env` files are in `.gitignore` and will NOT be committed
- `.env.local` is provided as a safe example for local development
- **Never commit actual passwords to git**
- For production, use cloud provider secret managers (AWS Secrets Manager, GCP Secret Manager)

## Files Explanation

| File | Purpose | Git Tracked |
|------|---------|-------------|
| `.env.example` | Template with example values | ✅ Yes (safe) |
| `.env.local` | Local development defaults | ✅ Yes (safe - example values) |
| `.env` | Your custom environment (if created) | ❌ No (.gitignore) |

## Docker Compose with Environment Variables

The `docker-compose.yml` file uses these variables:

```yaml
# Example from docker-compose.yml
postgresql:
  environment:
    POSTGRES_USER: ${DB_USER}           # Reads from .env
    POSTGRES_PASSWORD: ${DB_PASSWORD}   # Reads from .env
    POSTGRES_DB: postgres
  # If not found in .env, uses values from compose file

mongodb:
  environment:
    - MONGO_INITDB_ROOT_USERNAME=${MONGO_USER}
    - MONGO_INITDB_ROOT_PASSWORD=${MONGO_PASSWORD}
```

## Common Issues

### Issue: Services fail to start with .env.local

**Solution:** Make sure you're passing the env file to docker-compose:
```bash
# ❌ Wrong - won't use .env.local
docker-compose up -d

# ✅ Correct - uses .env.local
docker-compose --env-file .env.local up -d
```

### Issue: Database connection fails

**Solution:** Verify credentials match in both `.env` and the running container:
```bash
# Check PostgreSQL is running
docker exec ms_pg_sql psql -U niskuldeep -l

# Check MongoDB is running
docker exec mongo_db mongo -u niskuldeep -p niskuldeep --authenticationDatabase admin
```

### Issue: Can't connect to services from host

**Solution:** Ensure the services are on the same network and ports are exposed:
```bash
# Check networks
docker network ls

# Verify port mappings
docker ps | grep ms_
```

## Production Setup

For production deployment (GCP Cloud Run, Kubernetes, etc.):

1. **Do NOT use .env files**
2. **Use cloud provider secrets:**
   - GCP: Secret Manager
   - AWS: Secrets Manager
   - Kubernetes: Secrets objects
   - HashiCorp Vault

Example for GCP Cloud Run:
```bash
gcloud run deploy order-service \
  --set-env-vars=DB_PASSWORD=secret:db-password:latest \
  --set-env-vars=MONGO_PASSWORD=secret:mongo-password:latest
```

See `GCP_DEPLOYMENT.md` for complete production setup.

---

## Quick Reference

```bash
# Start with example values
docker-compose --env-file .env.local up -d

# Start with custom values
cp .env.local .env
# Edit .env as needed
docker-compose --env-file .env up -d

# View logs
docker-compose logs -f order-service

# Stop all services
docker-compose down

# Verify all services
curl http://localhost:8761  # Eureka
curl http://localhost:8070/actuator/health  # Order Service
curl http://localhost:9090  # Prometheus
```

---

**For detailed security practices, see:** [`SECURITY.md`](../SECURITY.md)  
**For production deployment, see:** [`GCP_DEPLOYMENT.md`](GCP_DEPLOYMENT.md)

