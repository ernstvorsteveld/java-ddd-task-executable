# Command Handling & Persistence Rules

1. **Generic Command-First Pipeline (All Non-`GET` / Non-`OPTIONS` Operations)**
   - Every command (any mutating operation / HTTP request other than `GET` or `OPTIONS`) must follow this sequence:
     1. **Domain Validation**: Validate the command using the exact same domain validation rules (`domain.model`) before storing. Reject invalid commands immediately without storing or publishing an event.
     2. **Persist Command First**: Persist every valid command via `ports.out` with a unique `CommandId` and `timestamp`.
     3. **Publish Event**: Immediately after persisting the command, publish a `CommandStoredEvent`.
     4. **Asynchronous Handling**: Handle the stored command asynchronously in-process (Virtual Threads / async event handler) and persist resulting domain aggregate state via `ports.out`.

2. **HTTP Response Contract for Commands**
   - Every `POST`, `PUT`, `PATCH`, `DELETE` (any non-`GET` / non-`OPTIONS`) endpoint returns **`202 Accepted`** with `CommandAcceptedResponsePayload` (`commandId`, `timestamp`) when input is valid, or the corresponding **`4xx` / `5xx`** using the uniform `ErrorResponsePayload` when invalid or failing.

3. **Persistence & Testing**
   - Persist both commands and domain aggregates through `ports.out` repositories implemented in `adapters.out`.
   - Test persistence and async command handling through `ports.in` against real outbound adapters and **Testcontainers** (zero code mocking).
