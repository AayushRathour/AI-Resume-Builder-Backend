# AI Resume Builder - Backend (Spring Boot Microservices)

I designed and built this backend as a full microservices architecture for the AI Resume Builder platform. It includes authentication, resume management, AI processing, template management, export workflows, job matching, notifications, centralized routing, and service discovery.

## Repository
- Backend repo: https://github.com/AayushRathour/AI-Resume-Builder-Backend

## Architecture Overview
This backend is built with Spring Boot + Spring Cloud and follows a service-per-domain approach.

Core runtime flow:
1. Client sends API requests to `api-gateway`.
2. Gateway validates JWT and routes calls to target services.
3. Services communicate through Eureka discovery and, where required, RabbitMQ events.
4. Data is persisted in MySQL databases (service-isolated).

## Microservices and Ports
- `eureka-server` - `8761`
- `api-gateway` - `8080`
- `auth-service` - `8081`
- `resume-service` - `8082`
- `section-service` - `8083`
- `template-service` - `8084`
- `ai-service` - `8085`
- `export-service` - `8086`
- `jobmatch-service` - `8087`
- `notification-service` - `8088`

## Tech Stack
- Java 17
- Spring Boot
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka
- Spring Data JPA
- MySQL
- RabbitMQ
- OpenFeign + Resilience4j
- Maven Wrapper

## Multi-Module Layout
This is a Maven aggregator project with modules:
- `ai-service`
- `api-gateway`
- `auth-service`
- `eureka-server`
- `export-service`
- `jobmatch-service`
- `notification-service`
- `resume-service`
- `section-service`
- `template-service`

## Prerequisites
- JDK 17
- Maven (or use `mvnw`/`mvnw.cmd`)
- MySQL
- RabbitMQ (recommended for full event-driven behavior)

## Local Development Setup
From backend root:

```bash
./mvnw -DskipTests package
```

Run services in this order:
1. `eureka-server`
2. Core domain services (`auth`, `resume`, `section`, `template`, `ai`, `export`, `jobmatch`, `notification`)
3. `api-gateway`

## Required Environment Variables
Each service supports env-based configuration (`spring.config.import=optional:file:.env[.properties]`).

### Shared (All/Most Services)
```env
APP_CORS_ALLOWED_ORIGINS=https://ai-resume-builder-frontend-react.vercel.app,http://localhost:5173,http://localhost:3000
EUREKA_CLIENT_ENABLED=true
EUREKA_SERVER_URL=http://13.233.173.205:8761/eureka/
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://13.233.173.205:8761/eureka/
JWT_SECRET=replace_with_min_32_char_secret

SPRING_RABBITMQ_HOST=<rabbitmq-host>
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=<rabbitmq-user>
SPRING_RABBITMQ_PASSWORD=<rabbitmq-password>
```

### Database (Per Service)
```env
SPRING_DATASOURCE_URL=jdbc:mysql://<db-host>:3306/<service_db>?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=<db-user>
SPRING_DATASOURCE_PASSWORD=<db-password>
```

### Auth Service Specific
```env
APP_FRONTEND_BASE_URL=https://ai-resume-builder-frontend-react.vercel.app
APP_OAUTH2_SUCCESS_REDIRECT_URL=https://ai-resume-builder-frontend-react.vercel.app/oauth/callback
APP_OAUTH2_FAILURE_REDIRECT_URL=https://ai-resume-builder-frontend-react.vercel.app/login
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
RAZORPAY_KEY_ID=<razorpay-key-id>
RAZORPAY_KEY_SECRET=<razorpay-key-secret>
```

### AI / Job APIs
```env
GEMINI_API_KEY=<gemini-key>
GEMINI_MODEL=gemini-2.0-flash
ADZUNA_APP_ID=<adzuna-id>
ADZUNA_APP_KEY=<adzuna-key>
THEIRSTACK_API_KEY=<theirstack-key>
```

## Production Deployment (EC2)
Deployment assets are already included under `deploy/ec2`:
- systemd service units
- bootstrap scripts
- health check script
- env templates

See detailed EC2 guide:
- `deploy/ec2/README.md`

## API Access Notes
- Frontend should call API Gateway (`:8080`), not Eureka (`:8761`).
- Eureka is only for service discovery and monitoring.

## Build and Test
Build all modules:
```bash
./mvnw -DskipTests package
```

Run tests for a specific service:
```bash
cd auth-service
./mvnw test
```

## My Contribution Statement
I created this backend as a complete microservices system with:
- secure auth + JWT + OAuth2
- centralized API gateway routing
- resilient inter-service communication
- event-driven notification flows
- AI-assisted resume intelligence and ATS/job matching
- production-ready deployment scripts for AWS EC2

## Author
Aayush Rathour
