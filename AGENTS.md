# AGENTS.md

This repository contains a full-stack inventory/business-management app with a Spring Boot API and Angular frontend.

## Project map

- Backend source: [backend/src/main/java/com/example/taskmanager](backend/src/main/java/com/example/taskmanager)
- Backend config: [backend/src/main/resources](backend/src/main/resources)
- Frontend app: [frontend/src/app](frontend/src/app)
- Main docs: [README.md](README.md)
- Backend build config: [backend/pom.xml](backend/pom.xml)
- Frontend build config: [frontend/package.json](frontend/package.json)

## Working commands

- Backend dev run:
  ```bash
  cd backend
  mvn spring-boot:run
  ```
- Frontend dev run:
  ```bash
  cd frontend
  npm install
  npm start
  ```
- Backend tests:
  ```bash
  cd backend
  mvn test
  ```
- Frontend tests:
  ```bash
  cd frontend
  npm test -- --watch=false --browsers=ChromeHeadless
  ```

## Architecture and conventions

- The app is a Spring Boot 3.x service with REST controllers, JPA repositories, DTOs, and a security layer.
- Authentication is based on Spring Security sessions/cookies, not JWT. Preserve the current auth flow and role checks.
- The application has an admin bootstrap flow (`/auth/setup`), employee/user management, and inventory/business modules.
- The default development database is PostgreSQL, using the local Docker service defined in [docker-compose.yml](docker-compose.yml). MySQL is supported via the `mysql` Spring profile.
- The project uses JasperReports for PDF exports; report templates live under [backend/src/main/resources/reports](backend/src/main/resources/reports).
- Frontend is Angular 20 with standalone components and reactive forms; keep changes aligned with the existing service/model abstractions.

## Important notes for agents

- Use the Maven config as the source of truth for the backend version. The backend targets Java 25 and Spring Boot 3.5.16.
- Prefer small, targeted changes that follow the existing controller/service/repository layering instead of introducing new architectural patterns.
- Do not weaken security rules, account validation, or admin-only checks without explicit intent.
- Keep report generation, authentication, and database profile behavior unchanged unless the task specifically requires those systems to change.
- When fixing frontend issues, prefer the current Angular service and model contracts over adding parallel APIs or duplicate state structures.

## Suggested validation flow

1. Reach the smallest relevant backend or frontend area.
2. Make a targeted fix that matches the existing project style.
3. Run the most relevant validation command (backend tests or a frontend build/test for the affected area).
4. If a change touches auth, inventory logic, or reporting, verify the surrounding behavior before concluding the task.

## Related docs

- [README.md](README.md)
- [backend/pom.xml](backend/pom.xml)
- [frontend/package.json](frontend/package.json)
