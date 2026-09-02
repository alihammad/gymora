# Gymora Constitution

This constitution governs how developers and AI coding agents design, implement, test, and evolve Gymora. It contains non-negotiable engineering principles, not project description. Principles marked (NON-NEGOTIABLE) take precedence over all other guidance.

## Core Principles

### I. Requirements First (NON-NEGOTIABLE)
- The approved specification (`specs/<feature>/spec.md` and approved plan artifacts) is the single source of truth for functional requirements.
- Agents and developers MUST NOT invent functionality, business rules, APIs, data fields, or user behavior that is not present in the approved specification.
- Ambiguous or missing requirements MUST be surfaced explicitly (as a question or documented `[NEEDS CLARIFICATION]`), never silently assumed. Any assumption that is made MUST be recorded in the spec or task notes.
- Code does not define requirements: scope changes MUST be reflected in the specification before implementation.

### II. Simplicity Over Complexity
- Prefer the simplest solution that satisfies the stated requirement.
- Do NOT introduce microservices, event-driven architecture, distributed systems, caching layers, queues, or background infrastructure without a documented, approved requirement.
- No speculative abstractions: "we might need it later" is not a justification. Extensibility comes from clean boundaries, not pre-built machinery.
- Technology and architecture choices are recorded in approved plan artifacts, not in this constitution; once approved they MUST NOT be changed without an explicit requirement or approved plan.

### III. Modular Architecture
- Organize code into cohesive modules with a single, well-defined responsibility and minimal coupling.
- Keep business logic separate from presentation, persistence, infrastructure, and external integrations. Dependencies MUST point inward (UI → domain → data); circular dependencies are prohibited.
- Every component MUST be independently testable and changeable.
- New functionality MUST follow existing module boundaries; create a new module only for a clear responsibility that does not fit an existing one.

### IV. API and Contract Stability
- Public contracts (module APIs, repository/use-case interfaces, persisted data contracts) MUST define explicit inputs, outputs, and error behavior.
- Contract changes MUST be deliberate and backwards-compatible where practical; breaking changes MUST be called out in the change description.
- Validate input at system boundaries (user input, persisted data reads, external data) and return predictable, well-defined errors.
- Internal implementation details MUST NOT leak across module boundaries.

### V. Data Integrity (NON-NEGOTIABLE)
- The data model and schema are first-class architectural artifacts. Schema changes MUST be explicit, tested migrations; destructive migration is never the default strategy.
- User data MUST never be silently discarded, corrupted, or transformed.
- Historical records MUST be protected from edits or deletion of template/configuration data (e.g., editing or deleting a routine or exercise MUST NOT alter completed workout history).
- Enforce constraints (uniqueness, referential integrity, ordering, non-negative values) at the persistence layer where possible and validate them at the application layer.
- Migrations MUST be reversible where practical and MUST always be covered by tests.

### VI. Security and Privacy
- Secure by default: least privilege; never hard-code secrets, credentials, tokens, or API keys.
- Validate and sanitize all untrusted input before use.
- Personal user data MUST NOT be exposed through logs, error messages, telemetry, or APIs. User-facing errors MUST NOT reveal implementation details or stack traces.
- Any feature involving authentication, authorization, personal data, or external integrations MUST address security and privacy in its design before implementation.

### VII. Testing
- Every feature that adds business logic or persistence changes MUST ship with appropriate automated tests: unit tests for business logic, integration tests for persistence/API boundaries, end-to-end tests for critical user journeys.
- Tests MUST verify behavior and acceptance criteria, not implementation details.
- NEVER weaken, delete, skip, or disable tests to make an implementation pass. Fixing a genuinely incorrect test requires explicit justification.
- Core domain rules (history protection, session recovery, duration/volume calculation) MUST have regression tests.

### VIII. Observability and Reliability
- Handle failure paths explicitly: no silent error swallowing and no silent data loss.
- Errors MUST be actionable and diagnosable; user-facing errors MUST be friendly and preserve recoverable state where possible.
- Log error paths and critical lifecycle events for production-critical flows; NEVER log sensitive personal data.
- External dependencies MUST have timeout, retry, or graceful-degradation behavior where relevant.

### IX. Code Quality
- Prefer readable, explicit code over clever code. Follow established language/framework conventions and the existing project style.
- Keep functions, classes, and modules small and focused. Each business rule MUST have exactly one implementation (no duplicated logic).
- Comments MUST explain non-obvious "why", never restate code.
- Remove dead code encountered within the scope of a change; unrelated cleanup is out of scope (see Principle X).

## AI Agent and Delivery Standards

### X. AI Agent Development Rules
- Agents MUST inspect the existing codebase (relevant modules, conventions, tests) before modifying it, and MUST NOT reference APIs, files, or dependencies they have not verified exist.
- Agents MUST make the smallest change necessary to satisfy the task: no refactoring unrelated code, no broad renames, no reformatting untouched files.
- Agents MUST NOT change architecture or technology choices without an explicit requirement or approved plan.
- If a requirement conflicts with the existing architecture, the agent MUST surface the conflict and stop, not make a large architectural change autonomously.
- Agents MUST run relevant build/tests/validation after making changes, and MUST NOT disable lint rules, tests, or type checks to make a build pass.

### XI. Incremental Delivery
- Deliver in small, independently verifiable vertical slices; every task MUST have a clear purpose and acceptance criteria.
- Prefer working end-to-end functionality over large layers of incomplete technical work.
- Keep changes small enough to review and safely revert.

### XII. Dependency Management
- Minimize external dependencies. Before adding one, verify that existing project or language capabilities cannot solve the problem.
- New dependencies MUST have a documented justification, be actively maintained, and be license- and version-compatible with the project.

### XIII. Performance
- Performance work MUST be driven by measurement or an explicit requirement, never assumption.
- Do not add complexity for speculative performance gains.
- Explicit performance requirements in the spec (e.g., responsiveness with years of workout history) MUST be verified when implemented.

### XIV. Documentation
- Document architectural decisions, non-obvious business rules, public contracts, and important operational behavior.
- Do not create documentation that merely restates obvious code.
- Documentation MUST be updated in the same change as the implementation it describes.

### XV. Definition of Done
A task is complete only when ALL of the following are true:
1. The requested functionality is implemented per the approved spec/task.
2. Acceptance criteria are satisfied.
3. Appropriate automated tests are added or updated, and existing relevant tests pass.
4. Code follows project conventions and this constitution.
5. No unrelated functionality has been changed.
6. Relevant documentation is updated where necessary.

## Conflict Resolution Hierarchy

When guidance conflicts, resolve in this order (higher wins):

1. This constitution (especially NON-NEGOTIABLE principles I and V).
2. The approved feature specification (functional requirements).
3. Approved plan/architecture artifacts.
4. Existing codebase conventions.
5. Individual or AI preference.

- Constitution vs. specification conflict: stop and surface the conflict; do not choose silently.
- Task vs. existing convention: follow the approved task/plan, keep the deviation minimal, and document it.

## Compliance Review

Before completing any task, agents MUST verify:

- [ ] The change is traceable to an approved spec/task; nothing was invented.
- [ ] Acceptance criteria are satisfied and the diff contains no unrelated changes.
- [ ] Tests were added/updated, the relevant suite passes, and no tests were disabled or skipped.
- [ ] No secrets in code; no sensitive data in logs or errors; untrusted input is validated.
- [ ] Any data migration is explicit, tested, and non-destructive.
- [ ] Documentation was updated where contracts or decisions changed.

Any failed check MUST be fixed or explicitly reported to the user before the task is reported as complete.

## Governance

- This constitution supersedes ad-hoc decisions and personal preferences; it governs human developers and AI agents equally.
- Amendments require a documented change with rationale, a version bump, and migration notes for affected work.
- Deviations from the Simplicity or Dependency Management principles require justification recorded in the relevant plan or change description.
- All code reviews MUST verify compliance with these principles.

**Version**: 1.0.0 | **Ratified**: 2026-09-02 | **Last Amended**: 2026-09-02
