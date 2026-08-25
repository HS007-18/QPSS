# QPSS - Question Paper Setting System

QPSS generates formatted question papers (.docx) from uploaded question-bank documents. It extracts questions and header metadata from Word files, stores them in a searchable question bank, and builds exam papers following configurable rules (marks, sections, CO units) with content-duplicate-free selection.

## Features

- Upload question banks as .docx and auto-extract questions, units, marks and CO numbers
- Manual fix workflow for rows the extractor could not parse
- Configurable exam rules per exam type (INTERNAL_1, INTERNAL_2, SEMESTER), marks and unit ranges
- Automatic paper generation with balanced unit distribution and RBT pairing
- Content-duplicate detection within and across sessions
- Paper review, swap of questions and finalized draft persistence
- Export of finalized papers as Word documents (Part A / Part B layout)
- XSS/HTML sanitization of extracted question content

## Tech Stack

| Layer      | Technology                                |
|------------|-------------------------------------------|
| Backend    | Java 17, Spring Boot 3.2.5, Spring Data JPA |
| Frontend   | JSP (Jakarta), JSTL, HTML/CSS             |
| Documents  | Apache POI 5.2.5, jsoup 1.17.2             |
| Database   | MariaDB 10.11                             |
| Build      | Maven (wrapper included), packaged as WAR |
| Deploy     | Docker, Docker Compose                    |

## Project Structure

```
src/main/java/com/qpss/
├── frontend/            web layer: controllers, DTOs, error handling
├── backend/             business logic and persistence
│   ├── subject/         subjects
│   ├── session/         exam sessions
│   ├── questionbank/    question storage, uploads, parsing, swap
│   ├── examconfig/      exam rules (marks, units, COs, sections)
│   ├── selection/       question selection and validation engine
│   └── paper/           generated papers and drafts
├── documentextraction/  reads question-bank .docx files
├── documentoutput/      builds the final paper .docx
└── common/domain/       shared enums and domain types
src/main/webapp/WEB-INF/views/   JSP pages
src/main/resources/              config, DB schema and seed data
```

## Getting Started

### Prerequisites

- Java 17
- MariaDB 10.11 running on localhost:3306 (or set `DB_HOST` / `DB_PORT`)

### Run locally

```bash
mvnw clean package
java -jar target/qpss-1.0.0.war
```

Open http://localhost:8080. The database schema and seed rules are applied automatically on first start (user `root`, password `root` by default; override with `DB_USERNAME` / `DB_PASSWORD` env vars).

### Run tests

```bash
mvnw clean test
```

## Deploy with Docker

The repository ships a `docker-compose.yml` with two services: `db` (MariaDB) and `app` (the application). Uploaded files are persisted in `./storage`, and database data in the `db_data` volume.

```bash
cp .env.example .env   # set a strong DB_PASSWORD
docker compose up -d --build
```

The app is then available at http://localhost:8080.

### Deployment to a VPS (Ubuntu)

1. Install Docker and the compose plugin:
   ```bash
   curl -fsSL https://get.docker.com | sh
   ```
2. Clone the repository:
   ```bash
   git clone https://github.com/HS007-18/QPSS.git
   cd QPSS
   ```
3. Configure the database password and start:
   ```bash
   cp .env.example .env
   nano .env            # set DB_PASSWORD to a strong value
   docker compose up -d --build
   ```
4. Open the firewall:
   ```bash
   sudo ufw allow 8080
   ```
5. (Optional) Reverse proxy with Caddy for HTTPS:
   ```bash
   docker run -d --name caddy -p 80:80 -p 443:443 \
     -v $PWD/Caddyfile:/etc/caddy/Caddyfile \
     caddy:2
   ```
   Caddyfile: `your-domain.com { reverse_proxy qpss_app:8080 }` (connect Caddy to the same Docker network first).

### Backup and restore

```bash
docker compose exec db sh -c 'exec mariadb-dump -uroot -p"$MYSQL_ROOT_PASSWORD" qpss' > qpss_backup.sql
docker compose cp qpss_app:/app/storage ./storage-backup
```

Restore:

```bash
docker compose exec -T db sh -c 'exec mariadb -uroot -p"$MYSQL_ROOT_PASSWORD" qpss' < qpss_backup.sql
```

## Configuration

| Environment variable | Default     | Description                          |
|----------------------|-------------|--------------------------------------|
| `DB_HOST`            | `localhost` | Database host                        |
| `DB_PORT`            | `3306`      | Database port                        |
| `DB_USERNAME`        | `root`      | Database user                        |
| `DB_PASSWORD`        | `root`      | Database password                    |
| `qpss.storage.question-banks` | `storage/question-banks` | Upload storage directory |

## License

Internal college project.
