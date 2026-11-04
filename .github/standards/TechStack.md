# Technology Stack Standards

## Approved Language & Runtime
- Java 17 (LTS)

## Build Tool
- Apache Maven 3.9+ with a `pom.xml` at the repository root

## Approved Libraries & Frameworks
- JUnit 5 (Jupiter) for unit and integration tests
- Mockito (`mockito-core` and `mockito-junit-jupiter`) for test doubles
- Jackson (`jackson-databind`) for JSON serialization and deserialization, including local application data
- Java 17 built-in `com.sun.net.httpserver.HttpServer` for a small local HTTP server; use the `jdk.httpserver` module
- Plain HTML, CSS, and JavaScript for browser UI assets; no frontend framework is approved
- Maven Exec Plugin (`org.codehaus.mojo:exec-maven-plugin`) version `3.5.0` to run the application through Maven
- Maven Compiler Plugin (`release` set to 17)
- Maven Surefire Plugin (runs JUnit 5 tests)
- Java standard library (`java.nio.file`, `java.net.http`, `java.util`, `java.util.logging`, `java.time`) before any third-party package

## Local Browser Application Profile
- The approved minimal web-app profile is Java 17 `HttpServer`, static HTML/CSS/JavaScript served by the application, JSON persistence through Jackson, and no additional framework or runtime service.
- Bind the server to the loopback interface only (`127.0.0.1`); the default browser URL is `http://127.0.0.1:8080/`. If the port is occupied, report the startup failure clearly; do not silently bind to an arbitrary interface or claim the server started.
- Configure the Maven Exec Plugin in `pom.xml` with a pinned version and an application entry point so the documented command can start the app, for example `mvn compile exec:java`. The Architecture Agent must define the exact entry point and any supported configuration before implementation.
- When requirements call for local persistence, define the application-specific data location in the approved requirements or architecture. Store runtime data outside source/resource directories, keep generated user data out of version control, and handle unreadable, malformed, or unwritable data explicitly.
- Do not add authentication to this single-user, loopback-only demo unless approved requirements change. Do not expose it on a network interface or describe it as production-hardened.
- Tests must be able to inject a temporary data path and an ephemeral server port so tests do not touch the user's data file or rely on port 8080.

## Approved Tools
- Git and GitHub (version control, pull requests)
- GitHub Copilot's authorized GitHub MCP PR-creation tool (for example, `github-create_pull_request`), with authenticated GitHub CLI (`gh`) as fallback; PR creation requires explicit user approval, and merge remains a human action
- GitHub Copilot (agent mode, chat, instructions, skills)
- GitHub Dependabot (dependency vulnerability alerts and update pull requests), configured in `.github/dependabot.yml`
- MCP servers for Jira and Confluence, configured in the IDE workspace (used by Copilot, not a project dependency)
- Mermaid (architecture diagrams in Markdown)

## Test Naming Convention
- Unit tests: `*Test.java` (for example `SyncEngineTest.java`)
- Integration tests: `*IntegrationTest.java` (for example `ReadingShelfIntegrationTest.java`)
- Both are run by Maven Surefire through `mvn test` and `mvn verify`

## Project Layout
- Production code: `src/main/java/`
- Browser assets: `src/main/resources/web/`
- Tests: `src/test/java/`
- Test resources: `src/test/resources/`
- Build file: `pom.xml`
- Local startup instructions and localhost URL: `README.md` (required when the story calls for a runnable app)
- Dependabot configuration: `.github/dependabot.yml`

## Documentation
- Markdown (`.md`) for all SDLC artifacts

## Pending Decisions (not yet approved)
- None

## Prohibited
- Third-party libraries or Maven plugins not listed above, unless the user approves them and the decision is recorded in the Change Log
- Hardcoded credentials, tokens, or API keys
- Java versions other than 17 for the project build
- Binding a local-only app to `0.0.0.0` or another non-loopback address without explicit scope approval
