# 🚀 Developer Quick Start Guide

## 5-Minute Local Setup

### Prerequisites
- Docker & Docker Compose installed
- Java 17+ (for IDE support, optional for Docker)
- Git

### Step 1: Start Services (30 seconds)
```bash
cd services
docker-compose --env-file .env.local up -d
```

### Step 2: Wait for Startup (~60 seconds)
```bash
# Wait for services to be ready
sleep 60

# Verify Eureka is responding
curl http://localhost:8761
```

### Step 3: Access Applications

**API Documentation:**
- Order Service: http://localhost:8070/swagger-ui.html
- Payment Service: http://localhost:8080/swagger-ui.html
- Product Service: http://localhost:8090/swagger-ui.html
- Customer Service: http://localhost:8085/swagger-ui.html
- Notification Service: http://localhost:8095/swagger-ui.html
- Gateway: http://localhost:8222/swagger-ui.html

**Monitoring:**
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Kibana: http://localhost:5601

**Database Management:**
- PgAdmin: http://localhost:5050
- Mongo Express: http://localhost:8081

---

## Environment Customization

### Option 1: Use Defaults (No Changes Needed)
```bash
docker-compose --env-file .env.local up -d
```

### Option 2: Customize Credentials
```bash
# Copy to .env and edit
cp services/.env.local services/.env

# Edit as needed
nano services/.env

# Start with custom environment
docker-compose --env-file services/.env up -d
```

---

## Common Commands

```bash
# View all running containers
docker-compose ps

# View service logs
docker-compose logs -f order-service

# Stop all services (keep data)
docker-compose down

# Stop and remove all data
docker-compose down -v

# Rebuild specific service
docker-compose build order-service

# Restart service
docker-compose restart order-service
```

---

## Test First Order

```bash
# Create a customer first
curl -X POST http://localhost:8222/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "John",
    "lastname": "Doe",
    "email": "john@example.com"
  }'

# Create an order
curl -X POST http://localhost:8222/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "totalAmount": 99.99
  }'

# Search orders in Elasticsearch
curl http://localhost:9200/orders/_search
```

---

## Troubleshooting

### Services won't start
```bash
# Check Docker is running
docker ps

# View error logs
docker-compose logs

# Check specific service
docker-compose logs order-service

# Rebuild from scratch
docker-compose down -v
docker-compose build --no-cache
docker-compose --env-file .env.local up -d
```

### Can't connect to services
```bash
# Verify network connectivity
docker network inspect services_microservices-net

# Check port conflicts
lsof -i :8070  # Replace 8070 with your port
```

### Database connection issues
```bash
# Test PostgreSQL
docker exec ms_pg_sql psql -U niskuldeep -d order -c "SELECT 1"

# Test MongoDB
docker exec mongo_db mongo -u niskuldeep -p niskuldeep --authenticationDatabase admin
```

---

## Development Workflow

### 1. Make Changes to Code
```bash
# Edit files in your IDE
# Changes are reflected in running containers if hot-reload is enabled
```

### 2. Rebuild If Needed
```bash
# Rebuild a specific service
cd services/order
mvn clean package -DskipTests

# Or rebuild via Docker
docker-compose build order-service
```

### 3. View Results
```bash
# Check Swagger UI for new endpoints
open http://localhost:8070/swagger-ui.html

# Monitor via Grafana
open http://localhost:3000
```

---

## File Structure Reference

```
services/
├── .env.local              # Example environment (safe defaults)
├── .env.example            # Template for custom .env
├── docker-compose.yml      # Orchestration config
├── prometheus.yml          # Metrics scraping config
├── ENV_SETUP.md           # Detailed environment setup
│
├── order/                  # Order microservice
├── payment/                # Payment microservice
├── product/                # Product microservice
├── customer/               # Customer microservice
├── notification/           # Notification microservice
├── gateway/                # API Gateway
├── discovery/              # Eureka Server
├── config-server/          # Config Server
│
└── grafana-dashboards/     # Grafana dashboard definitions
```

---

## Documentation References

| Document | Purpose |
|----------|---------|
| **README.md** | Project overview and key features |
| **SECURITY.md** | Credential management & security practices |
| **ENV_SETUP.md** | Detailed environment configuration |
| **GCP_DEPLOYMENT.md** | Cloud deployment instructions |
| **SECURITY_AUDIT_REPORT.md** | Security verification details |

---

## Help & Support

### Local Development
- Check `ENV_SETUP.md` for detailed environment setup
- Review service READMEs in each `services/*/README.md`

### Production Deployment
- See `GCP_DEPLOYMENT.md` for cloud deployment
- Review `SECURITY.md` for credential management

### API Documentation
- Swagger UI available at each service `/swagger-ui.html`
- OpenAPI specs at each service `/api-docs`

---

**Ready to start?** Run this command now:
```bash
cd services && docker-compose --env-file .env.local up -d && sleep 60 && open http://localhost:8070/swagger-ui.html
```

Happy developing! 🎉

