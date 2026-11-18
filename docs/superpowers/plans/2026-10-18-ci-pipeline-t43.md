# CI Pipeline Ticket T-43 Implementation Plan

> **Goal:** Update `.github/workflows/ci.yml` so T-43's six acceptance items are implemented: (1) backend tests run with `spring-security-test` resolvable, (2) frontend tests run via `npm test`, (3) OWASP Dependency-Check scan, (4) SonarQube scan against a locally Docker-CE-hosted SonarQube, (5) `-Djacoco.skip=true` on Windows/NTFS (avoids bundle.gif corruption), (6) secrets via env vars/secrets only — none hardcoded. Proper job separation + artifact uploads. Do not commit.

**Tech Stack:** GitHub Actions, JDK 17 (temurin) + Maven, Node 20 + npm, Docker CE, SonarQube Community, OWASP dependency-check action, JaCoCo 0.8.11.

**Global constraints:** runs-on ubuntu-latest unless noted; no hardcoded secrets (all credentials via `${{ secrets.* }}` → env); do not commit; trust CI for green tests where local execution is blocked (local `.m2` corruption per PROJECT.md blocks full `mvn test`).

## Files
- Modify: `.github/workflows/ci.yml` (single file rewrite)
- Create: `docs/superpowers/plans/2026-10-18-ci-pipeline-t43.md` (this plan)

## Jobs
1. `build-backend` (ubuntu-latest): checkout, JDK 17, `mvn -B package -DskipTests -Djacoco.skip=true`, upload `backend/target/*.jar` artifact.
2. `test-backend` (needs build-backend; matrix ubuntu-latest + windows-latest): checkout, JDK 17, `mvn -B dependency:resolve -DincludeScope=test` (proves spring-security-test resolves), `mvn -B test -Djacoco.skip=${{ windows ? true : false }}`, upload surefire reports (`backend-test-results-<os>`), upload jacoco report on ubuntu only.
3. `build-frontend` (ubuntu-latest): checkout, Node 20, `npm ci`, `npm run build`, upload `frontend/dist`.
4. `test-frontend` (needs build-frontend, ubuntu-latest): checkout, Node 20, `npm ci`, `npm test`, upload `frontend/coverage`.
5. `secret-scan` (ubuntu-latest): checkout, narrow regex scan for hardcoded secrets (AWS keys, ghp_, github_pat_, sk-, PEM blocks, sonar.token literals) over source + workflows; fails if found.
6. `owasp-dependency-check` (ubuntu-latest): checkout, dependency-check action fail-build-on-cvss 7, upload `reports/` artifact.
7. `sonarqube` (needs test-backend, test-frontend; ubuntu-latest): `sudo sysctl -w vm.max_map_count=262144`, `docker run -d sonarqube:community` (local Docker CE), poll `/api/system/status`, run `mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar` with `SONAR_HOST_URL=http://localhost:9000` and `SONAR_TOKEN=${{ secrets.SONAR_TOKEN }}` (env only), `docker stop/rm` in `if: always()`.
8. `build-docker` (needs build-backend, build-frontend): unchanged existing behavior.

**Verification gate:** YAML parses; frontend `npm test` green locally if node_modules usable; backend `mvn test-compile` attempted locally (expected blocked by corrupted local `.m2` per PROJECT.md — CI verifies on push). No commit.
