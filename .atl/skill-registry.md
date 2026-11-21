# Skill Registry

**Delegator use only.** Any agent that launches sub-agents reads this registry to resolve compact rules, then injects them directly into sub-agent prompts. Sub-agents do NOT read this registry or individual SKILL.md files.

See `_shared/skill-resolver.md` for the full resolution protocol.

Project: **turnos-odontologia** · Generated: 2026-11-21 · Project skills pinned in `skills-lock.json`.

## Project Skills (in the repo, shared by the 5 team members)

| Trigger | Skill | Path |
|---------|-------|------|
| Spring Boot code: controllers, validation, ProblemDetail, Spring Data, Flyway, configuration | java-springboot | `.claude/skills/java-springboot/SKILL.md` |
| Writing, reviewing or fixing JUnit 5 / AssertJ / Mockito tests (Gradle) | java-junit | `.claude/skills/java-junit/SKILL.md` |
| Spring Security: authn/authz, session cookie, CSRF, validation, secrets, headers | springboot-security | `.claude/skills/springboot-security/SKILL.md` |
| Designing or reviewing PostgreSQL tables, constraints, indexes (Flyway migrations) | postgresql-table-design | `.claude/skills/postgresql-table-design/SKILL.md` |
| Frontend `frontend/` with Vite + TypeScript vanilla, `vite.config.ts` | vite | `.claude/skills/vite/SKILL.md` |
| Exploring a change before proposing it (`opsx explore`) | openspec-explore | `.claude/skills/openspec-explore/SKILL.md` |
| Creating a change with proposal, specs, design and tasks (`opsx propose`) | openspec-propose | `.claude/skills/openspec-propose/SKILL.md` |
| Revising an existing change's artifacts (`opsx update`) | openspec-update-change | `.claude/skills/openspec-update-change/SKILL.md` |
| Implementing a change's tasks (`opsx apply`) | openspec-apply-change | `.claude/skills/openspec-apply-change/SKILL.md` |
| Syncing delta specs to main specs (`opsx sync`) | openspec-sync-specs | `.claude/skills/openspec-sync-specs/SKILL.md` |
| Archiving a completed change (`opsx archive`) | openspec-archive-change | `.claude/skills/openspec-archive-change/SKILL.md` |

## User Skills (global, only on the leader's machine)

| Trigger | Skill | Path |
|---------|-------|------|
| Start or resume project foundation | active-orchestrator | `~/.claude/skills/active-orchestrator/SKILL.md` |
| Choose ODD or SDD for one unit of work | active-delivery | `~/.claude/skills/active-delivery/SKILL.md` |
| ClickUp roadmap publishing (not used: tracking is local) | active-roadmap | `~/.claude/skills/active-roadmap/SKILL.md` |
| Generate or update AGENTS.md / CLAUDE.md | agent-instruction | `~/.claude/skills/agent-instruction/SKILL.md` |
| Discovery checklist and market research | discovery-research | `~/.claude/skills/discovery-research/SKILL.md` |
| Competitor research from a URL | web-scraper | `~/.claude/skills/web-scraper/SKILL.md` |
| Build the 10-file knowledge base | kb-creator | `~/.claude/skills/kb-creator/SKILL.md` |
| Generate or update CHANGES.md | roadmap-generator | `~/.claude/skills/roadmap-generator/SKILL.md` |
| Find and install skills | find-skills | `~/.claude/skills/find-skill/SKILL.md` |
| Create or improve a skill | skill-creator | `~/.claude/skills/skill-creator/SKILL.md` |
| Visual foundation (project policy is `off`) | design-system | `~/.claude/skills/design-system/SKILL.md` |
| UI polish and animation (not applicable: visual policy `off`) | emil-design-eng | `~/.claude/skills/emil-design-eng/SKILL.md` |
| Frontend taste rules v1 (not applicable: visual policy `off`) | design-taste-frontend-v1 | `~/.claude/skills/design-taste-frontend-v1/SKILL.md` |

## Compact Rules

Pre-digested rules per skill. Delegators copy matching blocks into sub-agent prompts as `## Project Standards (auto-resolved)`.

### java-springboot
- Detect before writing: read `build.gradle` for Boot 3 vs 4, Java 21, starters, Flyway and package layout; match them.
- Never upgrade Spring Boot, Java or dependencies implicitly; no explicit versions for BOM-managed artifacts, never `latest`.
- Ask before adding infrastructure (broker, cache, scheduler, new DB).
- Package by feature; constructor injection; controllers return DTOs/records, never entities; services own `@Transactional`.
- Bean Validation with `@Valid` on request records; one `@RestControllerAdvice` returning `ProblemDetail`.
- Boot 3 uses Jackson 2 (`com.fasterxml.jackson.databind`); Boot 4 uses Jackson 3 (`tools.jackson`).
- Put `@EnableJpaAuditing`/`@EnableScheduling` etc. on separate `@Configuration` classes, not on `@SpringBootApplication` (slice tests break).
- Proxied annotations (`@Transactional`, `@Async`, `@Cacheable`) do nothing on self-invocation or private/final methods.
- Verify with `./gradlew build`; Testcontainers integration tests need Docker.
- No secrets in code, properties or logs.

### java-junit
- Copy existing test conventions; do not add AssertJ/Mockito/JaCoCo to a project lacking them without asking (this project uses JUnit 5 + AssertJ).
- Gradle 9 requires `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` and `useJUnitPlatform()`.
- Assert on outcomes and state; compare values, avoid bare `assertNotNull`; never catch `AssertionError` to pass.
- Mock only collaborators across a boundary (repository, clock, sender); prefer hand-written fakes for stateful ones (e.g. in-memory `TurnoRepository`); never mock value objects or the class under test.
- Time: inject `java.time.Clock` (`Clock.fixed`) and use `Instant.now(clock)`; never `Instant.now()` in code under test. Fix time zone explicitly.
- Unit tests `*Test` (no I/O); integration tests `*IT` or `@Tag("integration")` in a separate task.
- NEVER delete, `@Disabled`, weaken or skip a failing test; never add `-x test`. If the fix changes production behavior, ask first.
- Run the narrowest test first (`./gradlew test --tests '...'`), then `./gradlew check`.
- New code gets happy path, boundary and error-case tests (one per spec scenario in this project).

### springboot-security
- Project decision DD-10: **session with cookie, not JWT**. Ignore the skill's JWT examples; cookies `HttpOnly`, `Secure`, `SameSite`.
- Browser session app → keep CSRF **enabled** and send the token from the frontend.
- `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")`; deny by default; patient sees only own data.
- Bean Validation on DTOs (`@NotBlank`, `@Email`, `@Size`); parameterized queries only, never string-concatenated SQL.
- Passwords with `PasswordEncoder` (BCrypt/Argon2), never plaintext.
- No secrets in source: `${DB_PASSWORD}` placeholders, `.env` ignored, `.env.example` committed.
- Configure CORS at the filter-chain level with explicit origins (Vite dev server), never `*`.
- Never log passwords, tokens or patient health data.
- Rate-limit login and public reservation endpoints; return 429.

### postgresql-table-design
- PK `BIGINT GENERATED ALWAYS AS IDENTITY` (not `serial`); `TIMESTAMPTZ` for times (never `timestamp`); `TEXT` + `CHECK` instead of `varchar(n)`; `NUMERIC` for money.
- `NOT NULL` wherever semantically required; `CHECK` combined with `NOT NULL` (NULL passes CHECK).
- PostgreSQL does NOT index FK columns: add indexes manually.
- `snake_case`, unquoted identifiers.
- Overlap prevention: `EXCLUDE USING gist (sillon_id WITH =, tstzrange(inicio, fin_con_margen) WITH &&)` needs `CREATE EXTENSION btree_gist` (DD-03).
- `UNIQUE NULLS NOT DISTINCT` (PG15+) when only one NULL is allowed.
- Evolving business values: `TEXT` + `CHECK` or lookup table rather than `ENUM`.
- Identity sequences have gaps; that's normal.

### vite
- Use `vite.config.ts` and ESM only, no CommonJS.
- Template `vanilla-ts`: no framework plugins (no React/Vue).
- Dev proxy to the backend: `server.proxy: { '/api': 'http://localhost:8080' }` so the session cookie stays same-origin.
- Env vars via `import.meta.env` and `loadEnv`; only `VITE_`-prefixed vars reach the client, so never put secrets there.
- Commands: `vite` (dev), `vite build`, `vite preview`.
- Vite 8 uses Rolldown/Oxc instead of esbuild/Rollup; check the installed major before copying config.

### openspec (explore / propose / update / apply / sync / archive)
- The `openspec` CLI owns all state: run `openspec status` / `openspec list` before acting; never create `openspec/` files by hand.
- Schema `spec-driven`, language `es`. Each requirement needs scenarios in `#### Scenario:` with GIVEN/WHEN/THEN.
- `apply`: work task by task from `tasks.md`, check each box only after its test passes.
- `archive` only after verification; it syncs delta specs into `openspec/specs/`.
- One change per branch `change/C-NN-<name>`; PR to `master`, reviewed by the leader.

## Project Conventions

| File | Path | Notes |
|------|------|-------|
| Roadmap | `CHANGES.md` | 32 changes, owners, reserved migrations, team rules (no direct commits to master, fictitious data only) |
| Knowledge base | `knowledge-base/` | 10 canonical files; rules in `05_reglas_de_negocio.md`, decisions in `09_decisiones_y_supuestos.md` |
| OpenSpec config | `openspec/config.yaml` | Schema and context for OPSX |
| Visual policy | `design-system/README.md` | `Mode: off` |
| Orchestrator state | `.active-orchestrator-state.json` | Foundation progress |
| AGENTS.md / CLAUDE.md | — | Not yet created (next phase: agent-instruction) |

Read the convention files listed above for project-specific patterns and rules. All referenced paths have been extracted — no need to read index files to discover more.
