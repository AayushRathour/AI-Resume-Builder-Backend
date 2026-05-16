# EC2 Deployment (All Services)

This folder adds EC2-only deployment assets and does not affect local development.

## Services and Ports

- `eureka-server`: `8761`
- `api-gateway`: `8080`
- `auth-service`: `8081`
- `resume-service`: `8082`
- `section-service`: `8083`
- `template-service`: `8084`
- `ai-service`: `8085`
- `export-service`: `8086`
- `jobmatch-service`: `8087`
- `notification-service`: `8088`

## 1) Build JARs

From repo root:

```bash
./mvnw -DskipTests package
```

## 2) Create runtime env files

Copy each template from `deploy/ec2/env/*.env.example` to `.env` in each service directory:

- `eureka-server/.env`
- `api-gateway/.env`
- `auth-service/.env`
- `resume-service/.env`
- `section-service/.env`
- `template-service/.env`
- `ai-service/.env`
- `export-service/.env`
- `jobmatch-service/.env`
- `notification-service/.env`

## 3) Install systemd units

Set root path and user, then run:

```bash
sudo bash deploy/ec2/scripts/install-systemd.sh /opt/ResumeAI-Microservices ubuntu
```

## One-command bootstrap

On a fresh EC2 host, run:

```bash
bash deploy/ec2/scripts/bootstrap-ec2.sh /opt/ResumeAI-Microservices ubuntu
```

Optional (skip Maven build step):

```bash
bash deploy/ec2/scripts/bootstrap-ec2.sh /opt/ResumeAI-Microservices ubuntu true
```

## 4) Start all services in order

```bash
sudo systemctl daemon-reload
sudo systemctl enable resumeai-eureka-server resumeai-api-gateway resumeai-auth-service resumeai-resume-service resumeai-section-service resumeai-template-service resumeai-ai-service resumeai-export-service resumeai-jobmatch-service resumeai-notification-service
sudo systemctl start resumeai-eureka-server
sleep 8
sudo systemctl start resumeai-auth-service resumeai-resume-service resumeai-section-service resumeai-template-service resumeai-ai-service resumeai-export-service resumeai-jobmatch-service resumeai-notification-service
sleep 8
sudo systemctl start resumeai-api-gateway
```

## 5) Health check

```bash
bash deploy/ec2/scripts/health-check.sh
```
