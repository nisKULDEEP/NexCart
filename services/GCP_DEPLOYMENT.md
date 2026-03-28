# GCP Cloud Run Deployment Guide

## Overview

This guide provides step-by-step instructions to deploy the NexCart microservices architecture to **Google Cloud Platform (GCP) Cloud Run** with managed PostgreSQL, Redis, and Elasticsearch.

## Prerequisites

- GCP Project with billing enabled
- `gcloud` CLI installed and authenticated: `gcloud auth login`
- Docker installed locally
- `kubectl` installed (for GKE alternative)

## Architecture on GCP

```
┌─────────────────────────────────────────────────────────┐
│              Cloud Load Balancer                         │
│           (Distributed across regions)                  │
└──────────────────────┬──────────────────────────────────┘
                       │
    ┌──────────────────┼──────────────────┐
    │                  │                  │
┌───▼────┐      ┌─────▼──┐        ┌─────▼──┐
│ Config │      │ Gateway│        │Discovery
│ Server │      │ Service│        │ Service
└────────┘      └────────┘        └────────┘
                   │
    ┌──────────────┼──────────────┬──────────────┐
    │              │              │              │
┌───▼──┐      ┌───▼──┐      ┌───▼──┐      ┌───▼──┐
│Order │      │Payment│     │Product│     │Customer
│Service      │Service│     │Service│     │Service
└──────┘      └──────┘      └──────┘      └──────┘
    │              │              │
    └──────────────┼──────────────┘
                   │
        ┌──────────┼──────────┐
        │          │          │
    ┌───▼──┐  ┌──▼───┐  ┌───▼──┐
    │Cloud │  │Cloud │  │Managed
    │ SQL  │  │Firestore Memorystore
    │(PG)  │  │(Backup)  │(Redis)
    └──────┘  └────────┘ └──────┘
```

## Step 1: Configure GCP Project

```bash
# Set your project variables
export PROJECT_ID="your-gcp-project-id"
export REGION="us-central1"
export REGISTRY_URL="${REGION}-docker.pkg.dev"

# Set default project
gcloud config set project $PROJECT_ID

# Enable required APIs
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  sql-component.googleapis.com \
  redis.googleapis.com \
  compute.googleapis.com \
  servicenetworking.googleapis.com

# Create Artifact Registry repository
gcloud artifacts repositories create microservices-repo \
  --repository-format=docker \
  --location=$REGION

# Configure Docker authentication
gcloud auth configure-docker ${REGISTRY_URL}
```

## Step 2: Set Up Networking

```bash
# Create VPC network for private connectivity
gcloud compute networks create microservices-net \
  --region=$REGION \
  --subnet-mode=custom

gcloud compute networks subnets create microservices-subnet \
  --network=microservices-net \
  --region=$REGION \
  --range=10.0.0.0/16

# Create Serverless VPC Connector (for Cloud Run to reach Cloud SQL/Redis)
gcloud compute networks vpc-access connectors create microservices-connector \
  --network microservices-net \
  --region $REGION \
  --range 10.8.0.0/28
```

## Step 3: Deploy PostgreSQL (Cloud SQL)

```bash
# Create Cloud SQL instance
gcloud sql instances create order-processing-db \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=$REGION \
  --network=microservices-net \
  --no-assign-ip \
  --availability-type=REGIONAL \
  --backup-start-time=02:00

# Get connection name
export CLOUDSQL_CONNECTION=$(gcloud sql instances describe order-processing-db \
  --format='value(connectionName)' \
  --project=$PROJECT_ID)

# Create databases
gcloud sql databases create order --instance=order-processing-db
gcloud sql databases create product --instance=order-processing-db
gcloud sql databases create payment --instance=order-processing-db
gcloud sql databases create customer --instance=order-processing-db

# Set root password
gcloud sql users set-password postgres \
  --instance=order-processing-db \
  --password=your-secure-password
```

## Step 4: Deploy Redis (Memorystore)

```bash
# Create Redis instance
gcloud redis instances create order-processing-cache \
  --size=1 \
  --region=$REGION \
  --redis-version=7.0 \
  --network=microservices-net

# Get Redis host:port
export REDIS_HOST=$(gcloud redis instances describe order-processing-cache \
  --region=$REGION \
  --format='value(host)')
export REDIS_PORT=$(gcloud redis instances describe order-processing-cache \
  --region=$REGION \
  --format='value(port)')

echo "Redis: $REDIS_HOST:$REDIS_PORT"
```

## Step 5: Build and Push Container Images

```bash
# Build all service images
for SERVICE in config-server discovery gateway order payment product customer notification; do
  echo "Building $SERVICE..."
  
  docker build \
    --tag ${REGISTRY_URL}/${PROJECT_ID}/microservices-repo/${SERVICE}:latest \
    --file services/${SERVICE}/Dockerfile \
    services/${SERVICE}/
  
  docker push ${REGISTRY_URL}/${PROJECT_ID}/microservices-repo/${SERVICE}:latest
done

# Verify images
gcloud artifacts docker images list ${REGISTRY_URL}/${PROJECT_ID}/microservices-repo/
```

## Step 6: Deploy to Cloud Run

### 6.1 Deploy Config Server (Foundation Service)

```bash
gcloud run deploy config-server \
  --image=${REGISTRY_URL}/${PROJECT_ID}/microservices-repo/config-server:latest \
  --region=$REGION \
  --platform=managed \
  --memory=512Mi \
  --cpu=1 \
  --timeout=3600 \
  --vpc-connector=microservices-connector \
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloud" \
  --no-allow-unauthenticated \
  --max-instances=5 \
  --min-instances=1

# Get Config Server URL
export CONFIG_SERVER_URL=$(gcloud run services describe config-server \
  --region=$REGION \
  --format='value(status.url)')

echo "Config Server URL: $CONFIG_SERVER_URL"
```

### 6.2 Deploy Discovery Service (Eureka)

```bash
gcloud run deploy discovery-service \
  --image=${REGISTRY_URL}/${PROJECT_ID}/microservices-repo/discovery:latest \
  --region=$REGION \
  --platform=managed \
  --memory=512Mi \
  --cpu=1 \
  --timeout=3600 \
  --vpc-connector=microservices-connector \
  --set-env-vars="SPRING_CONFIG_IMPORT=optional:configserver:${CONFIG_SERVER_URL}" \
  --no-allow-unauthenticated \
  --max-instances=3 \
  --min-instances=1

export DISCOVERY_URL=$(gcloud run services describe discovery-service \
  --region=$REGION \
  --format='value(status.url)')
```

### 6.3 Deploy Order Service

```bash
gcloud run deploy order-service \
  --image=${REGISTRY_URL}/${PROJECT_ID}/microservices-repo/order:latest \
  --region=$REGION \
  --platform=managed \
  --memory=1Gi \
  --cpu=2 \
  --timeout=3600 \
  --vpc-connector=microservices-connector \
  --set-env-vars=\
"SPRING_CONFIG_IMPORT=optional:configserver:${CONFIG_SERVER_URL},\
SPRING_DATASOURCE_URL=jdbc:postgresql://${CLOUDSQL_CONNECTION}/order,\
SPRING_DATASOURCE_USERNAME=postgres,\
SPRING_DATASOURCE_PASSWORD=your-secure-password,\
SPRING_DATA_ELASTICSEARCH_URIS=http://elasticsearch:9200" \
  --allow-unauthenticated \
  --max-instances=10 \
  --min-instances=2

export ORDER_URL=$(gcloud run services describe order-service \
  --region=$REGION \
  --format='value(status.url)')
```

### 6.4 Deploy Payment Service

```bash
gcloud run deploy payment-service \
  --image=${REGISTRY_URL}/${PROJECT_ID}/microservices-repo/payment:latest \
  --region=$REGION \
  --platform=managed \
  --memory=512Mi \
  --cpu=1 \
  --timeout=3600 \
  --vpc-connector=microservices-connector \
  --set-env-vars=\
"SPRING_CONFIG_IMPORT=optional:configserver:${CONFIG_SERVER_URL},\
SPRING_DATASOURCE_URL=jdbc:postgresql://${CLOUDSQL_CONNECTION}/payment,\
SPRING_DATASOURCE_USERNAME=postgres,\
SPRING_DATASOURCE_PASSWORD=your-secure-password" \
  --no-allow-unauthenticated \
  --max-instances=5 \
  --min-instances=1
```

### 6.5 Deploy Product Service

```bash
gcloud run deploy product-service \
  --image=${REGISTRY_URL}/${PROJECT_ID}/microservices-repo/product:latest \
  --region=$REGION \
  --platform=managed \
  --memory=512Mi \
  --cpu=1 \
  --timeout=3600 \
  --vpc-connector=microservices-connector \
  --set-env-vars=\
"SPRING_CONFIG_IMPORT=optional:configserver:${CONFIG_SERVER_URL},\
SPRING_DATASOURCE_URL=jdbc:postgresql://${CLOUDSQL_CONNECTION}/product,\
SPRING_DATASOURCE_USERNAME=postgres,\
SPRING_DATASOURCE_PASSWORD=your-secure-password,\
SPRING_DATA_REDIS_HOST=${REDIS_HOST},\
SPRING_DATA_REDIS_PORT=${REDIS_PORT}" \
  --no-allow-unauthenticated \
  --max-instances=5 \
  --min-instances=1
```

### 6.6 Deploy Customer Service

```bash
gcloud run deploy customer-service \
  --image=${REGISTRY_URL}/${PROJECT_ID}/microservices-repo/customer:latest \
  --region=$REGION \
  --platform=managed \
  --memory=512Mi \
  --cpu=1 \
  --timeout=3600 \
  --vpc-connector=microservices-connector \
  --set-env-vars=\
"SPRING_CONFIG_IMPORT=optional:configserver:${CONFIG_SERVER_URL},\
SPRING_DATASOURCE_URL=jdbc:postgresql://${CLOUDSQL_CONNECTION}/customer,\
SPRING_DATASOURCE_USERNAME=postgres,\
SPRING_DATASOURCE_PASSWORD=your-secure-password" \
  --no-allow-unauthenticated \
  --max-instances=5 \
  --min-instances=1
```

### 6.7 Deploy Notification Service

```bash
gcloud run deploy notification-service \
  --image=${REGISTRY_URL}/${PROJECT_ID}/microservices-repo/notification:latest \
  --region=$REGION \
  --platform=managed \
  --memory=512Mi \
  --cpu=1 \
  --timeout=3600 \
  --vpc-connector=microservices-connector \
  --set-env-vars=\
"SPRING_CONFIG_IMPORT=optional:configserver:${CONFIG_SERVER_URL}" \
  --no-allow-unauthenticated \
  --max-instances=3 \
  --min-instances=1
```

### 6.8 Deploy API Gateway

```bash
gcloud run deploy gateway-service \
  --image=${REGISTRY_URL}/${PROJECT_ID}/microservices-repo/gateway:latest \
  --region=$REGION \
  --platform=managed \
  --memory=512Mi \
  --cpu=1 \
  --timeout=3600 \
  --vpc-connector=microservices-connector \
  --set-env-vars=\
"SPRING_CONFIG_IMPORT=optional:configserver:${CONFIG_SERVER_URL},\
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=${DISCOVERY_URL}/eureka/" \
  --allow-unauthenticated \
  --max-instances=10 \
  --min-instances=2

export GATEWAY_URL=$(gcloud run services describe gateway-service \
  --region=$REGION \
  --format='value(status.url)')

echo "✅ API Gateway available at: $GATEWAY_URL"
```

## Step 7: Configure Cloud Monitoring

```bash
# Export metrics to Cloud Monitoring (automatically done via Cloud Run)
# Create uptime checks for critical endpoints
gcloud monitoring uptime-configs create \
  --display-name="Order Service Health" \
  --resource-type=uptime-url \
  --monitored-resource-type=uptime-url \
  --monitored-resource-labels=host=${ORDER_URL} \
  --protocol=HTTPS \
  --path=/actuator/health \
  --check-interval=60 \
  --timeout=10
```

## Step 8: Verification

```bash
# List all deployed services
gcloud run services list --region=$REGION

# Test Order Service endpoint
curl ${GATEWAY_URL}/api/v1/orders

# Check logs
gcloud logging read "resource.type=cloud_run_revision" \
  --limit 50 \
  --format json

# View metrics
gcloud monitoring metrics-descriptors list
```

## Step 9: Configure Custom Domain (Optional)

```bash
# Map custom domain to Cloud Run service
gcloud run domain-mappings create \
  --service=gateway-service \
  --domain=api.yourdomain.com \
  --region=$REGION

# Follow DNS setup instructions provided
```

## Troubleshooting

### Service fails to start
```bash
# Check logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=order-service" \
  --limit 100 \
  --format json

# Check service status
gcloud run services describe order-service --region=$REGION
```

### Connectivity issues
- Verify VPC connector is properly configured
- Check Cloud SQL firewall rules
- Ensure Redis instance is in same VPC

### Performance issues
- Check Cloud Run CPU/Memory allocation
- Scale up `--max-instances` if hitting limits
- Review `--memory` allocation (default 512Mi may be insufficient)

## Cost Estimation

| Service | CPU | Memory | Est. Monthly Cost |
|---------|-----|--------|-------------------|
| Config Server | 1 | 512Mi | $15 |
| Discovery Service | 1 | 512Mi | $15 |
| Gateway | 1 | 512Mi | $20 |
| Order Service | 2 | 1Gi | $50 |
| Payment Service | 1 | 512Mi | $15 |
| Product Service | 1 | 512Mi | $15 |
| Customer Service | 1 | 512Mi | $15 |
| Notification Service | 1 | 512Mi | $10 |
| **Cloud SQL (shared)** | - | - | $50 |
| **Redis** | - | - | $30 |
| **Infrastructure** | - | - | $50 |
| **Total** | - | - | **~$275-350/month** |

## Cleanup

```bash
# Delete all Cloud Run services
for SERVICE in config-server discovery gateway order payment product customer notification; do
  gcloud run services delete $SERVICE --region=$REGION --quiet
done

# Delete Cloud SQL instance
gcloud sql instances delete order-processing-db --quiet

# Delete Redis instance
gcloud redis instances delete order-processing-cache --region=$REGION --quiet

# Delete VPC connector
gcloud compute networks vpc-access connectors delete microservices-connector --region=$REGION --quiet

# Delete Artifact Registry
gcloud artifacts repositories delete microservices-repo --location=$REGION --quiet
```

---

**For more information**, see [Google Cloud Run Documentation](https://cloud.google.com/run/docs)

