# 25 — Deployment

**Priority:** P0 (can be done early, in parallel with feature work)
**Depends on:** 01
**Parallelizable with:** all feature tickets

## Summary

k3s deployment manifests and GitHub Actions CI/CD pipeline.

## Kubernetes Manifests

### Deployment
- Image: `10.10.2.181:30500/nightscout-java:latest`
- Replicas: 1 (single instance is fine for personal use)
- Resource limits: 512Mi memory, 500m CPU
- Liveness/readiness probes on `/actuator/health`
- `ndots: 2` in pod DNS config

### Service
- ClusterIP on port 8080
- Expose via ohdeere-gateway (ticket separate)

### ConfigMap / Sealed Secrets
- `SPRING_DATASOURCE_URL` — `jdbc:postgresql://wimo-pg-rw.postgres:5432/nightscout`
- `SPRING_DATASOURCE_USERNAME` / `PASSWORD` — Sealed Secret
- `API_SECRET` — Sealed Secret
- `DISPLAY_UNITS`, `ENABLE`, threshold env vars — ConfigMap

### Database
- Create `nightscout` database on CloudNativePG cluster
- Grant permissions to service account

## GitHub Actions

```yaml
on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: [self-hosted, ohdeere-k3s]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      - run: mvn verify
      - run: mvn spring-boot:build-image -DskipTests
      # Push to local registry
      - run: docker push 10.10.2.181:30500/nightscout-java:latest
      - run: kubectl rollout restart deployment/nightscout-java
```

## Acceptance Criteria

- [ ] k3s deployment manifest
- [ ] Service + Ingress (or gateway route)
- [ ] Sealed Secrets for credentials
- [ ] ConfigMap for Nightscout settings
- [ ] GitHub Actions workflow: build, test, push, deploy
- [ ] Database created on CloudNativePG cluster
- [ ] Health check passes after deployment
- [ ] Gateway route configured (e.g., `nightscout.ohdeere.se`)
