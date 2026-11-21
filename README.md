# KIT QGen

A desktop application that generates formatted question papers (.docx) from uploaded question-bank documents. It extracts questions and header metadata from Word files, stores them in a searchable question bank, and builds exam papers following configurable rules (marks, sections, CO units) with content-duplicate-free selection.

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

| Layer      | Technology                                  |
|------------|---------------------------------------------|
| Backend    | Java 17, Spring Boot 3.2.5, Spring Data JPA |
| Frontend   | JSP (Jakarta), JSTL, HTML/CSS               |
| Desktop    | Electron 30, electron-builder                |
| Documents  | Apache POI 5.2.5, jsoup 1.17.2              |
| Database   | H2 (embedded, file-based)                    |
| Build      | Maven (wrapper included), packaged as WAR    |

## Project Structure

```text
.
├── src/                          # Application source code
│   ├── main/
│   │   ├── java/com/qpss/       # Spring Boot backend
│   │   │   ├── controller/      # Web layer endpoints
│   │   │   ├── document/        # DOCX extraction & rendering
│   │   │   │   ├── model/       # AST, parsed question models
│   │   │   │   ├── parser/      # Document parsing logic
│   │   │   │   └── renderer/    # Word document rendering
│   │   │   ├── domain/          # Enums, distribution, selection, validation
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── exception/       # Custom domain exceptions
│   │   │   ├── repository/      # Spring Data JPA interfaces
│   │   │   ├── service/         # Business logic, selection engine, imports
│   │   │   └── util/            # Constants and shared helpers
│   │   ├── resources/           # Config, DB schema, static assets
│   │   └── webapp/              # JSP views
│   └── test/                    # Unit & integration tests
├── electron/                    # Electron desktop wrapper
│   ├── main.js                  # Electron entry point
│   ├── package.json             # Electron configuration
│   └── resources/               # Icons, splash screen
├── scripts/
│   └── build.ps1                # Build & package script
├── pom.xml                      # Maven configuration
├── mvnw, mvnw.cmd               # Maven wrapper
└── README.md
```

## Getting Started

### Prerequisites

- Java 17
- Node.js (for Electron packaging)

### Run locally

```bash
./mvnw clean package
java -jar target/qpss-1.0.1.war
```

Open http://localhost:8080. The H2 database and seed data are created automatically on first start — no external database required.

### Run tests

```bash
./mvnw clean test
```

### Build Desktop Installer (Windows)

```powershell
.\scripts\build.ps1
```

This builds the Spring Boot WAR, then packages it with Electron into a Windows installer at `electron/dist/KIT QGen Setup 1.0.1.exe`.
