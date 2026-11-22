# Task API — `curl` Examples

Base URL: `http://localhost:8080`

## Create Task (`POST /tasks`)

Returns `202 Accepted` with `CommandAcceptedResponsePayload` (`commandId`, `timestamp`).

```bash
curl -i -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "type": "DO_AANVRAAG",
    "owner": "PS_IE",
    "startBefore": "2026-11-23T09:00:00Z",
    "finishBefore": "2026-11-23T17:00:00Z",
    "executor": {
      "type": "USER",
      "id": "alice"
    },
    "linkedEntities": [
      {
        "typeName": "ENTITY_TYPE_1",
        "entityId": "ent-100"
      }
    ]
  }'
```

## Update Task (`PUT /tasks/{taskId}`)

Replace `<TASK_ID>` with a `TaskId` UUID returned from `GET /tasks`. Returns `202 Accepted` with `CommandAcceptedResponsePayload`.

```bash
curl -i -X PUT http://localhost:8080/tasks/<TASK_ID> \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS",
    "startBefore": "2026-11-24T09:00:00Z",
    "finishBefore": "2026-11-24T17:00:00Z",
    "executor": {
      "type": "ROLE",
      "id": "APPROVER"
    }
  }'
```

## Search Tasks (`GET /tasks`)

Returns `200 OK` with `TasksResponsePayload` (`tasks`, `count`, `pageNumber`, `pageSize`, and query parameters).

### Default page (`pageNumber=0`, `pageSize=20`)

```bash
curl -i "http://localhost:8080/tasks"
```

### With filters and pagination

```bash
curl -i "http://localhost:8080/tasks?executorType=USER&executorId=alice&status=CREATED&type=DO_AANVRAAG&owner=PS_IE&pageNumber=0&pageSize=20"
```
