# Reading Shelf

A local, Java 17 reading-inventory application with a plain HTML/CSS/JavaScript interface and JSON persistence. The server is intended only for the local machine.

## Prerequisites

- Java 17 JDK
- Internet access may be required when the Maven Wrapper dependencies are first downloaded

## Start the application

From PowerShell at the repository root on Windows:

```powershell
.\mvnw.cmd compile exec:java
```

From a Unix-like shell at the repository root:

```sh
./mvnw compile exec:java
```

The application binds only to `127.0.0.1` and serves the browser UI at:

`http://127.0.0.1:8080/`

Port `8080` must be available. If binding fails, startup reports the failure and does not fall back to another port or interface.

## Persistence

The runtime inventory is stored at:

`data/runtime/reading-shelf.json`

The file is resolved relative to the Maven working directory/repository root. A missing runtime file starts as an empty shelf. A malformed or unreadable existing runtime file fails startup instead of being replaced.

The existing `data/reading-shelf.json` file is never read, migrated, copied, or overwritten. It remains separate from the runtime application data. The `data/` directory is excluded from normal Git version control, so runtime inventory content is not committed with source changes.

## Build and tests

Run the complete Maven verification suite from PowerShell:

```powershell
.\mvnw.cmd verify
```

Or from a Unix-like shell:

```sh
./mvnw verify
```

Unit tests use `*Test.java`; HTTP and persistence integration tests use `*IntegrationTest.java`. Tests use isolated temporary data paths and injected/ephemeral ports, not the runtime file or a fixed test port.

## Responsive browser smoke check

Perform this manual check in a modern browser after starting the application. The smoke check is the mitigated F-04 responsiveness check.

Run the complete interaction sequence at both viewport sizes:

| Viewport | Required result |
|---|---|
| `1280 × 800` | Inventory, create controls, and status controls are visible and usable |
| `375 × 667` | Same controls and feedback remain usable; vertical scrolling is allowed |

For each viewport:

1. Open `http://127.0.0.1:8080/` and confirm the complete inventory is displayed.
2. Submit a valid book title and author, and confirm the new inventory entry appears.
3. Change the new or an existing book's status and confirm the visible status and inventory update.
4. Submit invalid title or author fields and confirm field-specific feedback is visible.
5. Confirm no text or control is clipped or overlapped.
6. Confirm no horizontal page scrolling is required to reach or use controls.

Record the viewport, passed interactions, and any clipping or overlap observed. Automated Maven tests do not replace this browser check.
