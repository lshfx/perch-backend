# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/perch`: application code, grouped by feature layers (controller, service, mapper, security, utils, config).
- `src/main/resources`: runtime configuration such as `application.yml` plus `static/` and `templates/` assets.
- `src/test/java/com/perch`: test sources (currently a Spring Boot context test).
- `pom.xml`: Maven build definition and dependency versions.
- `target/`: build output (generated; do not edit).

## Build, Test, and Development Commands
- `./mvnw clean package`: compile and package the Spring Boot JAR.
- `./mvnw test`: run the JUnit 5 test suite.
- `./mvnw spring-boot:run`: start the application locally with `application.yml`.
- Windows equivalents: `mvnw.cmd test`, `mvnw.cmd spring-boot:run`.

## Coding Style & Naming Conventions
- Java 21, standard Java conventions: 4-space indentation, PascalCase classes, `camelCase` methods/fields, `UPPER_SNAKE` constants.
- Keep packages aligned with existing layout (`controller`, `service`, `mapper`, `security`, `utils`).
- Lombok is in use; prefer it for boilerplate in DTOs/entities where already adopted.
- No formatter or linter is configured in `pom.xml`; rely on IDE formatting and consistent style with nearby code.

## Testing Guidelines
- Framework: Spring Boot Test + JUnit 5 (`@SpringBootTest`).
- Test files live under `src/test/java` and follow `*Test` or `*Tests` naming.
- Add focused unit tests for new services and integration tests for controller endpoints where behavior changes.

## Commit & Pull Request Guidelines
- Current history uses short, single-line summaries (often in Chinese) and no conventional-commit prefixes.
- Keep commit messages concise and descriptive (one sentence, imperative).
- PRs should include: a brief summary, testing notes (commands and results), and any config/env changes required to run locally.

## Security & Configuration Tips
- `application.yml` contains local defaults; sensitive values should come from environment variables (e.g., `DEEPSEEK_API_KEY`, `SERVER_REDIS_PWD`, `JWT_SECRET`).
- Local services expected by default config: PostgreSQL on `127.0.0.1:9999`, Redis on `localhost:6380`, and optional AI services (DeepSeek API, Ollama).

## AI & Model Environment
- **Local LLM**: Ensure Ollama is running on `localhost:11434`. 
- **Embeddings**: This project uses `BGE-M3` via Ollama; make sure the model is pulled (`ollama pull bge-m3`) before running AI features.
