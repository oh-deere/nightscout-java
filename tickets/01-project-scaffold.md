# 01 — Project Scaffold

**Priority:** P0 — must be done first
**Depends on:** nothing
**Parallelizable with:** nothing

## Summary

Create the Maven project skeleton for `nightscout-java` following OhDeere conventions.

## Acceptance Criteria

- [ ] Maven project with `se.ohdeere.nightscout` base package
- [ ] Spring Boot 4.0.3 parent POM
- [ ] Java 25 (Temurin) configured in `pom.xml`
- [ ] Dependencies: Spring Web, Spring Data JDBC, Spring Security (OAuth2 Resource Server), Flyway, PostgreSQL driver, Spring Boot Actuator, Spring Boot Admin Client 4.0.2, OpenTelemetry
- [ ] `application.yml` with profiles: `local`, `k3s`
- [ ] Local profile connects to `localhost:5432/nightscout`
- [ ] k3s profile connects to `wimo-pg-rw.postgres:5432/nightscout`
- [ ] Spring JavaFormat plugin configured
- [ ] `spring-boot:build-image` configured for multi-arch (amd64 + arm64)
- [ ] GitHub Actions workflow: build, test, push to `10.10.2.181:30500`
- [ ] `.gitignore`, `CLAUDE.md` with project-specific notes
- [ ] Application starts and health endpoint responds

## Package Structure

```
se.ohdeere.nightscout
├── api
│   ├── auth
│   ├── v1
│   │   ├── entries
│   │   ├── treatments
│   │   ├── profiles
│   │   ├── devicestatus
│   │   ├── food
│   │   └── status
│   └── v3
├── exception
├── plugin
│   ├── bgnow
│   ├── iob
│   ├── cob
│   ├── ar2
│   ├── pump
│   └── ages
├── remote
│   └── dexcom
├── service
│   ├── entries
│   ├── treatments
│   ├── profiles
│   ├── devicestatus
│   ├── food
│   ├── alarm
│   └── auth
├── storage
│   ├── entries
│   ├── treatments
│   ├── profiles
│   ├── devicestatus
│   └── food
└── util
```

## Notes

- Single Maven module (no multi-module). The service is self-contained.
- Image registry: `10.10.2.181:30500/nightscout-java`
- Runner label: `ohdeere-k3s`
