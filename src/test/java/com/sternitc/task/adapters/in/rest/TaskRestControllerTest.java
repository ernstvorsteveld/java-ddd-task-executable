package com.sternitc.task.adapters.in.rest;

import com.mongodb.client.MongoDatabase;
import com.sternitc.task.TaskApplication;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.mongodb.MongoDBContainer;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("TaskRestController HTTP API")
class TaskRestControllerTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-21T15:00:00Z");
    private static final String FIXED_NOW_ISO = "2026-11-21T15:00:00Z";

    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0");
    private static ConfigurableApplicationContext context;
    private static MongoDatabase database;
    private static int serverPort;

    @BeforeAll
    static void startServer() {
        MONGO.start();
        String databaseName = "task_rest_test_" + UUID.randomUUID().toString().replace("-", "");

        var app = new SpringApplication(FixedClockConfig.class, TaskApplication.class);
        context = app.run(
                "--server.port=0",
                "--task.mongodb.uri=" + MONGO.getConnectionString(),
                "--task.mongodb.database=" + databaseName
        );
        database = context.getBean(MongoDatabase.class);

        serverPort = Integer.parseInt(Objects.requireNonNull(
                context.getEnvironment().getProperty("local.server.port"),
                "local.server.port must be set"
        ));
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = serverPort;
    }

    @AfterAll
    static void stopServer() {
        RestAssured.reset();
        if (context != null) {
            context.close();
        }
        MONGO.stop();
    }

    @BeforeEach
    void cleanDatabase() {
        database.getCollection("tasks").drop();
        database.getCollection("commands").drop();
    }

    @Configuration
    static class FixedClockConfig {

        @Bean
        Clock clock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }

    @Nested
    @DisplayName("Requirement 1: HTTP REST Endpoints")
    class HttpEndpointsSpec {

        @Test
        @DisplayName("1.1 & 1.2 POST /tasks returns 202 Accepted with CommandAcceptedResponsePayload and persists task with linkedEntities asynchronously")
        void createsTaskOverHttp() {
            String requestBody = """
                    {
                      "type": "DO_AANVRAAG",
                      "owner": "PS_IE",
                      "startBefore": "2026-11-22T09:00:00Z",
                      "finishBefore": "2026-11-22T17:00:00Z",
                      "executor": {
                        "type": "USER",
                        "id": "alice"
                      },
                      "linkedEntities": [
                        { "typeName": "ENTITY_TYPE_1", "entityId": "ent-100" },
                        { "typeName": "ENTITY_TYPE_3", "entityId": "ent-300" }
                      ]
                    }
                    """;

            given()
                    .port(serverPort)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
            .when()
                    .post("/tasks")
            .then()
                    .statusCode(202)
                    .contentType(ContentType.JSON)
                    .body("commandId", not(emptyOrNullString()))
                    .body("timestamp", equalTo(FIXED_NOW_ISO));

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    given()
                            .port(serverPort)
                    .when()
                            .get("/tasks")
                    .then()
                            .statusCode(200)
                            .contentType(ContentType.JSON)
                            .body("count", equalTo(1))
                            .body("pageNumber", equalTo(0))
                            .body("pageSize", equalTo(20))
                            .body("tasks", hasSize(1))
                            .body("tasks[0].id", not(emptyOrNullString()))
                            .body("tasks[0].type", equalTo("DO_AANVRAAG"))
                            .body("tasks[0].version", equalTo(1))
                            .body("tasks[0].status", equalTo("CREATED"))
                            .body("tasks[0].owner", equalTo("PS_IE"))
                            .body("tasks[0].executor.type", equalTo("USER"))
                            .body("tasks[0].executor.id", equalTo("alice"))
                            .body("tasks[0].linkedEntities", hasSize(2))
                            .body("tasks[0].linkedEntities[0].typeName", equalTo("ENTITY_TYPE_1"))
                            .body("tasks[0].linkedEntities[0].entityId", equalTo("ent-100"))
                            .body("tasks[0].linkedEntities[1].typeName", equalTo("ENTITY_TYPE_3"))
                            .body("tasks[0].linkedEntities[1].entityId", equalTo("ent-300"))
            );
        }

        @Test
        @DisplayName("1.1 & 1.2 PUT /tasks/{taskId} returns 202 Accepted and asynchronously updates the task")
        void updatesTaskOverHttp() {
            given()
                    .port(serverPort)
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "type": "DO_CONTRACT",
                              "owner": "PO_VG",
                              "startBefore": "2026-11-22T09:00:00Z",
                              "finishBefore": "2026-11-22T17:00:00Z",
                              "executor": { "type": "USER", "id": "alice" }
                            }
                            """)
            .when()
                    .post("/tasks")
            .then()
                    .statusCode(202);

            String taskId = await().atMost(Duration.ofSeconds(5)).until(() ->
                    given()
                            .port(serverPort)
                    .when()
                            .get("/tasks")
                    .then()
                            .statusCode(200)
                            .extract()
                            .path("tasks[0].id"),
                    Objects::nonNull
            );

            given()
                    .port(serverPort)
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "status": "IN_PROGRESS",
                              "startBefore": "2026-11-23T09:00:00Z",
                              "finishBefore": "2026-11-23T17:00:00Z",
                              "executor": { "type": "ROLE", "id": "APPROVER" }
                            }
                            """)
            .when()
                    .put("/tasks/{taskId}", taskId)
            .then()
                    .statusCode(202)
                    .contentType(ContentType.JSON)
                    .body("commandId", not(emptyOrNullString()))
                    .body("timestamp", equalTo(FIXED_NOW_ISO));

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    given()
                            .port(serverPort)
                            .queryParam("status", "IN_PROGRESS")
                            .queryParam("executorType", "ROLE")
                            .queryParam("executorId", "APPROVER")
                    .when()
                            .get("/tasks")
                    .then()
                            .statusCode(200)
                            .body("count", equalTo(1))
                            .body("status", equalTo("IN_PROGRESS"))
                            .body("executorType", equalTo("ROLE"))
                            .body("executorId", equalTo("APPROVER"))
                            .body("tasks[0].id", equalTo(taskId))
                            .body("tasks[0].version", equalTo(2))
                            .body("tasks[0].status", equalTo("IN_PROGRESS"))
                            .body("tasks[0].executor.type", equalTo("ROLE"))
                            .body("tasks[0].executor.id", equalTo("APPROVER"))
            );
        }

        @Test
        @DisplayName("1.1 & 1.2 GET /tasks filters by query parameters and echoes pagination and filter parameters")
        void searchesTasksWithFiltersAndPaginationOverHttp() {
            given()
                    .port(serverPort)
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "type": "DO_AANVRAAG",
                              "owner": "PS_IE",
                              "startBefore": "2026-11-22T09:00:00Z",
                              "finishBefore": "2026-11-22T17:00:00Z",
                              "executor": { "type": "USER", "id": "alice" }
                            }
                            """)
            .when()
                    .post("/tasks")
            .then()
                    .statusCode(202);

            given()
                    .port(serverPort)
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "type": "DO_OPDRACHT",
                              "owner": "PS_UE",
                              "startBefore": "2026-11-22T09:00:00Z",
                              "finishBefore": "2026-11-22T17:00:00Z",
                              "executor": { "type": "USER", "id": "bob" }
                            }
                            """)
            .when()
                    .post("/tasks")
            .then()
                    .statusCode(202);

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    given()
                            .port(serverPort)
                    .when()
                            .get("/tasks")
                    .then()
                            .statusCode(200)
                            .body("count", equalTo(2))
            );

            given()
                    .port(serverPort)
                    .queryParam("executorType", "USER")
                    .queryParam("executorId", "alice")
                    .queryParam("type", "DO_AANVRAAG")
                    .queryParam("owner", "PS_IE")
                    .queryParam("status", "CREATED")
                    .queryParam("pageNumber", 0)
                    .queryParam("pageSize", 10)
            .when()
                    .get("/tasks")
            .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("count", equalTo(1))
                    .body("pageNumber", equalTo(0))
                    .body("pageSize", equalTo(10))
                    .body("executorType", equalTo("USER"))
                    .body("executorId", equalTo("alice"))
                    .body("type", equalTo("DO_AANVRAAG"))
                    .body("owner", equalTo("PS_IE"))
                    .body("status", equalTo("CREATED"))
                    .body("tasks", hasSize(1))
                    .body("tasks[0].type", equalTo("DO_AANVRAAG"));
        }

        @Test
        @DisplayName("1.3 & 1.4 returns 400 Bad Request with uniform ErrorResponsePayload on domain validation errors")
        void returnsUniformErrorPayloadOnDomainValidationError() {
            given()
                    .port(serverPort)
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "type": "DO_AANVRAAG",
                              "owner": "PS_IE",
                              "startBefore": "2026-11-22T17:00:00Z",
                              "finishBefore": "2026-11-22T09:00:00Z",
                              "executor": { "type": "USER", "id": "alice" }
                            }
                            """)
            .when()
                    .post("/tasks")
            .then()
                    .statusCode(400)
                    .contentType(ContentType.JSON)
                    .body("errors", hasSize(1))
                    .body("errors[0].errorCode", equalTo("INVALID_PLANNING_WINDOW"))
                    .body("errors[0].timestamp", equalTo(FIXED_NOW_ISO))
                    .body("errors[0].errorDescription", not(emptyOrNullString()));
        }

        @Test
        @DisplayName("1.3 & 1.4 returns 400 Bad Request with uniform ErrorResponsePayload on Bean Validation, malformed JSON, and invalid query parameters")
        void returnsUniformErrorPayloadOnRequestValidationAndMalformedInput() {
            given()
                    .port(serverPort)
                    .contentType(ContentType.JSON)
                    .body("{}")
            .when()
                    .post("/tasks")
            .then()
                    .statusCode(400)
                    .contentType(ContentType.JSON)
                    .body("errors", not(empty()))
                    .body("errors.size()", greaterThan(0))
                    .body("errors.errorCode", everyItem(equalTo("VALIDATION_ERROR")))
                    .body("errors.timestamp", everyItem(equalTo(FIXED_NOW_ISO)))
                    .body("errors.errorDescription", everyItem(not(emptyOrNullString())));

            given()
                    .port(serverPort)
                    .contentType(ContentType.JSON)
                    .body("{not-valid-json")
            .when()
                    .post("/tasks")
            .then()
                    .statusCode(400)
                    .contentType(ContentType.JSON)
                    .body("errors", hasSize(1))
                    .body("errors[0].errorCode", equalTo("INVALID_REQUEST"))
                    .body("errors[0].timestamp", equalTo(FIXED_NOW_ISO))
                    .body("errors[0].errorDescription", notNullValue());

            given()
                    .port(serverPort)
                    .queryParam("executorType", "USER")
            .when()
                    .get("/tasks")
            .then()
                    .statusCode(400)
                    .contentType(ContentType.JSON)
                    .body("errors", hasSize(1))
                    .body("errors[0].errorCode", equalTo("INVALID_TASK_EXECUTOR"))
                    .body("errors[0].timestamp", equalTo(FIXED_NOW_ISO))
                    .body("errors[0].errorDescription", not(emptyOrNullString()));

            given()
                    .port(serverPort)
                    .queryParam("pageNumber", -1)
            .when()
                    .get("/tasks")
            .then()
                    .statusCode(400)
                    .contentType(ContentType.JSON)
                    .body("errors", not(empty()))
                    .body("errors[0].timestamp", equalTo(FIXED_NOW_ISO))
                    .body("errors[0].errorDescription", not(emptyOrNullString()));
        }
    }
}
