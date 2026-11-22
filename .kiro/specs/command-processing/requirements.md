# Requirements: Command Processing

## Requirement 1: Validate, Store, and Publish Command
**User Story:** As a user, I want every command (any operation other than `GET` or `OPTIONS`) validated with domain rules, stored first, and followed by an event so it can be handled asynchronously.

1.1. WHEN any valid command (not `GET` or `OPTIONS`) is submitted, the system shall validate it using domain rules, assign a unique `CommandId` and `timestamp`, persist the command, publish a `CommandStoredEvent`, and return a `CommandReceipt` (`commandId`, `timestamp`).
1.2. IF a command fails domain validation, THEN the system shall reject it without persisting the command or publishing an event.

## Requirement 2: Asynchronous Command Handling & Persistence
**User Story:** As a user, I want stored commands handled asynchronously and resulting domain state persisted.

2.1. WHEN a `CommandStoredEvent` is published, the system shall handle the command asynchronously and persist the resulting `Task` state so subsequent queries return it.
2.2. IF asynchronous handling fails (such as an unknown `TaskId` on update), THEN the system shall record the failure on the stored command without modifying unrelated tasks.
2.3. The system shall route each command type through a typed event channel to its own dedicated command listener so commands are published and handled independently per command type without dispatching to unrelated listeners.
