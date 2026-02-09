# Alfresco Event Gateway (Spring Boot)

<p align="center">
  <img title="alfresco" alt='alfresco' src='docs/images/alfresco.png'  width="229px" height="160px"></img>
</p>

Single-module Spring Boot 3.4 gateway that talks to Alfresco Content Services (ACS) Community 23.4 via REST (out-of-process) and produces/consumes on Kafka.

**Requirements**
- Java 21
- Maven (or `./mvnw`)
- ACS Community 23.4.x reachable via REST
- Kafka broker
- Keycloak (optional, only if you enable ACS SSO or use the provided `docker-compose`)

**Configuration**
Defaults live in `src/main/resources/application.yml`. The minimum properties to review:
- `spring.kafka.bootstrap-servers`
- `alfresco.host`
- `alfresco.authentication.username`
- `alfresco.authentication.password`
- `alfresco.event.gateway.api.version`
- `alfresco.event.gateway.api.base-path`
- `alfresco.event.gateway.storage.kafka.topic` (optional)

If you need the larger set of properties, enable the `legacy` profile in `src/main/resources/application.yml`.

**Run Locally**
1. Start ACS and Kafka (for example using the stack under `docker-compose`).
2. Build and test:
   - `./mvnw clean verify`
3. Run:
   - `./mvnw spring-boot:run`

**Docker Compose (Optional)**

The `docker-compose` folder provides an ACS + Kafka + Keycloak stack for local development. The gateway itself is run via Maven from this project.

Keycloak is a standard distribution (16.1.1) with the realm import from `docker-compose/realms/alfresco-realm.json`.
ACS is configured for the `alfresco-keycloak` authentication subsystem. Before running `docker-compose`, set these environment variables:

```powershell
$env:HOST_IP="127.0.0.1"
$env:SERVER_NAME="localhost:8999"
$env:KEYCLOAK_REALM="alfresco"
$env:ALFRESCO_KEYCLOAK_ADAPTER_CREDENTIALS_SECRET="change-me"
```

If you do not use HTTPS locally, adjust the Keycloak adapter URL in `docker-compose/docker-compose.yml` accordingly.

To start the stack:

```powershell
docker compose -f docker-compose/docker-compose.yml up -d
```

**Legacy Profile**

The `legacy` profile enables the larger property set in `src/main/resources/application.yml`, including the Keycloak/OIDC client settings used by the gateway
when interacting with ACS.

Run with the legacy profile:

```powershell
./mvnw spring-boot:run -Dspring-boot.run.profiles=legacy
```

## Event Sourcing POC

This is a POC to follow Event Sourcing approach in the Event Gateway. If you want to check the original documentation for the Alfresco Event Gateway please
visit the master branch [README](https://github.com/Alfresco/alfresco-event-gateway) file.

### Idea

The idea of this POC is applying the [Event Sourcing approach from Martin Fowler](https://martinfowler.com/eaaDev/EventSourcing.html) to the Alfresco Event
Gateway providing an implementation of an EventStorage interface that stores the ACS events consumed by the gateway in an event streaming data storage like
[Apache Kafka](https://kafka.apache.org/).

![Architecture](docs/images/architecture.png)

### Run POC

#### Pre-Requisites

Same as base event gateway configuration [here](https://github.com/Alfresco/alfresco-event-gateway#pre-requisites).

#### How To Run

The project provides a [docker-compose](docker-compose/docker-compose.yml) configuration of the components required to execute the Alfresco Event Gateway in a
local environment. This configuration has been modified to include all the Kafka infrastructure required for the POC (```kafka```
, ```schema-registry```
and ```ksqldb```).

Once all the docker containers are up and running the system is ready to store the ACS events in Kafka and hence, it provides all the advantages of the Event
Sourcing approach described by Martin Fowler.

#### Querying the events in Kafka

We can use [ksqlDB](https://ksqldb.io/) to create custom streams from Kafka and execute different queries over the ACS events.

We're showing here the access to ksqkDB client from the console, but you can use a GUI like [Conduktor](https://www.conduktor.io/) to do the same in a GUI.

The steps to execute some queries in ksqlDB are:

1. Access to the ksqlDB docker container:

```
$ docker exec -it ksqldb-server /bin/ksql
```

2. Create a new stream from the Kafka topic configured in the property ```alfresco.event.gateway.storage.kafka.topic``` (defaulted
   to ```alfresco-event-gateway```):

```
ksql> CREATE STREAM events(
    id VARCHAR,
    type VARCHAR,
    source VARCHAR,
    time VARCHAR,
    data STRUCT<eventGroupId VARCHAR,
                resource STRUCT<name VARCHAR,
                                nodeType VARCHAR>>)
WITH (KAFKA_TOPIC='alfresco-event-gateway',
      VALUE_FORMAT='json',
      PARTITIONS=1);
```

3. Run a push query to check live the number of events produced per node type:

```
ksql> SELECT data->resource->nodeType NODE_TYPE, count(*) TOTAL
      FROM events
      GROUP BY data->resource->nodeType
      EMIT CHANGES;
```

4. Run a push query to check live a histogram of the node types per event type, which shows how many nodes of each type has been created, updated or deleted:

```
ksql> SELECT type EVENT_TYPE, HISTOGRAM(data->resource->nodeType) NODE_TYPES
      FROM events
      GROUP BY type
      EMIT CHANGES;
```

5. Run a push query to check live when the system is experiencing a peak in its activity. The next query detects the reception of more than 10 events of a
   specific type within a 20 seconds interval:

```
SELECT type EVENT_TYPE, count(*) TOTAL, WINDOWSTART, WINDOWEND
FROM events
WINDOW TUMBLING (SIZE 20 SECONDS)
GROUP BY type HAVING COUNT(*) > 10
EMIT CHANGES;
```

From this point, you can modify the queries (and eventually the mapping of the stream created in the step 2) to produce any reporting data you are interested
in.
