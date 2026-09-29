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

## Prescription documents (MinIO)

Customer prescription scans and photos are **not** stored in MySQL. The bytes go to
an S3-compatible object store; `prescription` only keeps the metadata
(`document_object_key`, `document_filename`, `document_content_type`,
`document_size_bytes`, `document_uploaded_at`).

`docker compose up -d` starts MinIO alongside MySQL on the same command:

| | |
|---|---|
| API | `http://localhost:9000` |
| Console | `http://localhost:9001` (`myuser` / `verysecret`) |
| Bucket | `prescription-documents`, private (no anonymous access) |

The bucket is created on first start. Documents are only served through
`GET /api/prescriptions/{id}/document`; the storage key is never returned to a
client.

Outside local development, point the backend at the real store with:

| Variable | Default |
|---|---|
| `MINIO_ENDPOINT` | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | `myuser` |
| `MINIO_SECRET_KEY` | `verysecret` |
| `MINIO_BUCKET` | `prescription-documents` |

## What goes here

- EER/ERD source files and exports
- Data dictionary / column reference
- Ad-hoc scripts and queries
- Migration coordination notes