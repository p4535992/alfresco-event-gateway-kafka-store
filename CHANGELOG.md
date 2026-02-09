**Changelog**
Baseline: commit `b622d422ab4abbac13db85912dee0d2c10b67576`.

**Unreleased**
Added:
- Repo tooling and formatting configs (EditorConfig, Git attributes, Prettier, lint-staged, Husky, VS Code settings).
- Checkstyle rules and suppressions.
- OpenAPI template overrides under `openapi-templates/`.
- Single-module Spring Boot sources and tests under `src/main/java` and `src/test/java` (migrated from the previous multi-module layout).
- `src/main/resources/application.yml` with default config and a `legacy` profile.

Changed:
- Base Java package renamed to `org.alfresco.event.gateway.kafka` (from `org.alfresco.quarkus.gateway.kafka`) with corresponding source/test path moves.
- `docker-compose/docker-compose.yml` uses standard Keycloak (16.1.1) and switches ACS auth properties to the `alfresco-keycloak` subsystem settings.
- README updated for the new stack, legacy profile, and compose workflow.

Removed:
- Legacy multi-module projects (`alfresco-event-gateway-*`) in favor of a single module.
- `src/main/resources/application-legacy.properties` (superseded by the `legacy` profile in `application.yml`).
- Duplicate `ACSClientException` outside the package tree.
