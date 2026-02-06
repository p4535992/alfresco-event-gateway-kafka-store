# Alfresco Event Gateway (Spring Boot)

Single-module Spring Boot 3.4 gateway that talks to Alfresco Content Services (ACS) Community 23.4 via REST (out-of-process) and produces/consumes on Kafka.

**Requirements**
- Java 21
- Maven (or `./mvnw`)
- ACS Community 23.4.x reachable via REST
- Kafka broker

**Configuration**
Defaults live in `src/main/resources/application.yml`. The minimum properties to review:
- `spring.kafka.bootstrap-servers`
- `alfresco.host`
- `alfresco.authentication.username`
- `alfresco.authentication.password`
- `alfresco.event.gateway.api.version`
- `alfresco.event.gateway.api.base-path`
- `alfresco.event.gateway.storage.kafka.topic` (optional)

If you need the previous, larger set of properties, see `src/main/resources/application-legacy.properties` and enable the `legacy` profile.

**Run Locally**
1. Start ACS and Kafka (for example using the stack under `docker-compose`).
2. Build and test:
   - `./mvnw clean verify`
3. Run:
   - `./mvnw spring-boot:run`

**Docker Compose (Optional)**
The `docker-compose` folder provides an ACS + Kafka stack for local development. The gateway itself is run via Maven from this project.
