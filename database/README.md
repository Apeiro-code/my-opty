# Database

Database-wide artifacts for the MyOpty system. The live schema is managed with **Flyway migrations**, each module owning its own tables.

- **Migration location:** `backend/src/main/resources/db/migration/`
- **Naming:** `V<version>__<module>_<description>.sql` (see CONTRIBUTION.md → Database Migrations)
- **Do not** edit a migration after it has been applied; create a new one instead.
- **ERD/EER reference:** see the diagrams in the root README.md (EER Diagram sections).

## Local MySQL

Start MySQL with Docker Compose:

```bash
cd backend
docker compose up -d
```

Migrations run automatically on backend startup (`./mvnw spring-boot:run`).

## What goes here

- EER/ERD source files and exports
- Data dictionary / column reference
- Ad-hoc scripts and queries
- Migration coordination notes