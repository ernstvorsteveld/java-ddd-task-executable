# Implementation Plan: <Feature Name>

Follow outside-in TDD (Red -> Green -> Refactor) exclusively through `ports.in` (no isolated unit tests, no code mocking). Mark each item `[x]` only after its tests pass.

- [ ] 1. Port contracts and test infrastructure setup
  - [ ] 1.1 Define `ports.in.<UseCase>` and `ports.out.<Repository/Client>` contracts (plus Testcontainers / WireMock test harness if external systems are required)
    - _Requirements: 1.1_
- [ ] 2. Requirement 1 behavior via `ports.in`
  - [ ] 2.1 Write failing JUnit 5 + AssertJ tests against `ports.in.<UseCase>` covering EARS criteria `1.1`, `1.2`, `1.3`
    - _Requirements: 1.1, 1.2, 1.3_
  - [ ] 2.2 Implement `domain` core model, `domain.application` service, and real `adapters.out` implementation to make the `ports.in` tests pass, then refactor
    - _Requirements: 1.1, 1.2, 1.3_
- [ ] 3. Requirement 2 behavior via `ports.in`
  - [ ] 3.1 Write failing JUnit 5 + AssertJ tests against `ports.in.<UseCase>` covering EARS criterion `2.1`
    - _Requirements: 2.1_
  - [ ] 3.2 Implement the required `domain`, `domain.application`, and `adapters.out` changes to pass, then refactor
    - _Requirements: 2.1_
- [ ] 4. Driving adapters (`adapters.in`) *(if applicable)*
  - [ ] 4.1 Wire `adapters.in` to delegate to `ports.in`
    - _Requirements: 1.1_
- [ ] 5. Architecture verification
  - [ ] 5.1 Verify all `ports.in` tests and ArchUnit hexagonal boundary rules pass
    - _Requirements: All_
