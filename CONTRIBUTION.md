# Contribution Guidelines

Thank you for considering contributing to this project! Please follow these guidelines to help keep the project organized and maintainable.

---

## System Architecture Context

This is a **modular monolith** with 4 feature modules owned by individual team members:

| Module | Package | Owner | Student ID |
|--------|---------|-------|------------|
| **Catalog & Inventory** | `com.myopty.catalog` | Amarasekara K.N. | IT25103193 |
| **Order & Prescription** | `com.myopty.order` | Welikuburawatta M.D.L | IT25300262 |
| **Workflow & Communication** | `com.myopty.workflow` | Karunarathna KHMMM | IT25101670 |
| **Payment & Billing** | `com.myopty.billing` | Kankanamge P M G | IT24300359 |
| **Shared Infrastructure** | `com.myopty.shared` | Team-wide | — |

---

## Commit Message Format

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Types

- **feat**: A new feature
- **fix**: A bug fix
- **docs**: Documentation only changes
- **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
- **refactor**: A code change that neither fixes a bug nor adds a feature
- **perf**: A code change that improves performance
- **test**: Adding missing tests or correcting existing tests
- **chore**: Changes to the build process or auxiliary tools and libraries

### Scope Convention

Use the **module name** as scope for feature/fix commits:

```
feat(catalog): add frame CRUD endpoints
fix(order): prescription validation for progressive lenses
docs(workflow): update API spec for dealer emails
refactor(billing): simplify invoice generation
test(shared): add auth integration tests
chore(catalog): add flyway migration for stock_entry
```

### Examples

```
feat(catalog): add frame CRUD endpoints

Implements POST/GET/PUT/DELETE for /api/frames with validation.
Includes category filtering and pagination.

Closes #12
```

```
fix(order): prescription validation for progressive lenses

Adds server-side validation for add_power field when is_progressive=true.
Returns 400 with field errors if missing.

Fixes #45
```

```
docs: update README with module responsibilities
```

---

## Pull Request Guidelines

### PR Title Format

Use the same format as commit messages:

```
<type>[optional scope]: <description>
```

### PR Description Template

When creating a PR, please include:

```markdown
## Description
Brief description of the changes

## Module
- [ ] catalog
- [ ] order
- [ ] workflow
- [ ] billing
- [ ] shared

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update
- [ ] Refactor
- [ ] Other (please describe)

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] My code follows the project's style guidelines
- [ ] I have performed a self-review of my code
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] I have made corresponding changes to the documentation
- [ ] My changes generate no new warnings
- [ ] Any dependent changes have been merged and published
- [ ] Flyway migration included (if schema changed)
- [ ] API spec updated (if endpoints changed)
```

### Review Process

1. All PRs require at least **one approval** before merging
2. **Module owner must approve** changes to their module
3. Ensure all CI checks pass
4. Address all review comments
5. Squash commits if requested by maintainers

---

## Branch Naming

Use descriptive branch names following this pattern:

```
<type>/<module>-<short-description>
```

### Module Prefixes

| Module | Prefix |
|--------|--------|
| Catalog & Inventory | `catalog` |
| Order & Prescription | `order` |
| Workflow & Communication | `workflow` |
| Payment & Billing | `billing` |
| Shared Infrastructure | `shared` |

### Examples

- `feat/catalog-add-frame-crud`
- `fix/order-prescription-validation`
- `docs/workflow-api-spec`
- `refactor/billing-invoice-generation`
- `test/shared-auth-integration`
- `chore/catalog-flyway-stock-entry`

---

## Module Ownership Rules

### Owner Responsibilities
- Each student **fully owns** their module package end-to-end:
  - Controllers, Services, Repositories
  - Domain entities & DTOs
  - Flyway migrations for their tables
  - Unit & integration tests
  - API documentation (OpenAPI annotations)

### Cross-Module Changes
- **Never** directly import another module's entities/repositories
- Use **service interfaces** defined in `shared` or **events** for communication
- If you need data from another module, request an API endpoint or event
- Changes spanning modules require **both owners' approval**

### Shared Module (`com.myopty.shared`)
- Changes require **team consensus** (all 4 members)
- Includes: auth, user entities, global exceptions, common DTOs, config
- PRs to `shared` need 2+ approvals

---

## Database Migrations

### Rules
- Each module owns its tables → creates its own Flyway migration files
- **Never** modify another module's tables in your migration
- **Never** modify a migration after it's been applied (create a new one instead)

### Naming Convention
```
V<version>__<module>_<description>.sql
```

### Examples
```
V2__catalog_create_frame_table.sql
V3__order_create_prescription_table.sql
V4__workflow_create_todo_task_table.sql
V5__billing_create_payment_table.sql
V6__shared_create_app_user_table.sql
V7__catalog_add_frame_image_url.sql
V8__order_create_progressive_order_table.sql
V9__order_add_order_rejection.sql
V10__order_create_notification_table.sql
```

### Applied Version Map
Check this before creating a migration. The versions above are the agreed
allocations; this is which of them have actually landed.

| Version | Module | File | Status |
|---------|--------|------|--------|
| V2 | catalog | `V2__catalog_create_frame_table.sql` | allocated, not applied |
| V3 | order | `V3__order_create_prescription_table.sql` | applied |
| V4 | workflow | `V4__workflow_create_todo_task_table.sql` | allocated, not applied |
| V5 | billing | `V5__billing_create_payment_table.sql` | allocated, not applied |
| V6 | shared | `V6__shared_create_app_user_table.sql` | allocated, not applied |
| V7 | catalog | `V7__catalog_add_frame_image_url.sql` | allocated, not applied |
| V8 | order | `V8__order_create_progressive_order_table.sql` | applied (PR #3) |
| V9 | order | `V9__order_add_order_rejection.sql` | applied (PR #4) |
| V10 | order | `V10__order_create_notification_table.sql` | pending merge (PR #8) |

**V11 is the next free version.** Catalog is the module most likely to claim it, so
check team chat before taking it.

V10 is listed as pending rather than applied on purpose: a migration that has been
written, reviewed and merged has still not been run against a real database, and
this table records what has actually happened to one. It also still has to be
announced in team chat before it is applied anywhere, as the rule above requires.

### Migration Version Coordination
- Use sequential versions across modules (check latest V number before creating)
- Announce new migrations in team chat before applying
- A version listed in the allocated examples above but not in the applied table is
  still free for its owner, so do not skip past it

---

## Code Style

- Follow the existing code style in each module
- Run linters/formatters before committing (`./mvnw spotless:apply` if configured)
- Write meaningful variable and function names
- Keep functions small and focused (single responsibility)
- **Package-private** by default for internal classes; `public` only for API surfaces

### Layer Structure (per module)
```
com.myopty.<module>/
├── controller/     # REST endpoints, request/response DTOs
├── service/        # Business logic, interfaces + implementations
├── repository/     # Spring Data JDBC repositories
├── domain/         # Entities, enums, value objects
├── dto/            # Data transfer objects (request/response)
├── mapper/         # Entity ↔ DTO mapping
├── exception/      # Module-specific exceptions
└── event/          # Domain events (published to other modules)
```

---

## API Design Standards

- RESTful, plural nouns: `/api/frames`, `/api/orders/progressive`
- Version in URL if breaking: `/api/v1/...`
- Standard response envelope:
  ```json
  { "success": true, "data": {...}, "meta": {...} }
  { "success": false, "error": { "code": "...", "message": "..." } }
  ```
- Pagination: `?page=0&size=20&sort=createdAt,desc`
- Filtering: `?category=men&minPrice=1000&maxPrice=5000`
- All endpoints documented with OpenAPI annotations (`@Operation`, `@Parameter`, `@ApiResponse`)

---

## Testing Requirements

### Minimum Coverage
- **Unit tests**: All service logic (≥80% coverage target)
- **Integration tests**: All controller endpoints
- **Repository tests**: Custom queries

### Commands
```bash
# All tests
./mvnw test

# Module-specific
./mvnw test -Dtest=*Catalog*
./mvnw test -Dtest=*Order*
./mvnw test -Dtest=*Workflow*
./mvnw test -Dtest=*Billing*
./mvnw test -Dtest=*Shared*

# With coverage report
./mvnw test jacoco:report
```

---

## Reporting Issues

When reporting issues, please include:

- Clear description of the problem
- Steps to reproduce
- Expected vs actual behavior
- **Module affected** (catalog/order/workflow/billing/shared)
- Environment details (OS, Java version, Docker version)
- Screenshots if applicable
- Related issue/PR numbers

---

## Definition of Done

A task/story is **done** when:

- [ ] Code implemented in correct module
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] Flyway migration created (if schema change)
- [ ] API endpoints documented (OpenAPI)
- [ ] README/API docs updated if needed
- [ ] Code reviewed and approved by module owner
- [ ] Merged to main branch
- [ ] Deployed to staging (if applicable)