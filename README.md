# GitHub Capstone Project

This project is a Java 17 Maven application built as part of the GitHub Copilot Agentic SDLC capstone. The current implementation follows the approved stack and focuses on a local browser-based Reading Shelf app served on the loopback interface.

## Overview

The application is a simple single-user reading management tool that allows a user to:

- add books to a personal reading list
- search and filter books
- view book metadata and review status
- manage a lightweight local library inventory

The project is intentionally designed to run locally without external authentication or network exposure.

## Tech Stack

- Java 17
- Maven 3.9.9 (via Maven Wrapper)
- JUnit 5
- Mockito
- Jackson
- Java built-in `HttpServer`
- Plain HTML, CSS, and JavaScript

## Project Structure

```text
.github/
  agents/
  standards/
  dependabot.yml
src/
  main/
    java/
    resources/
  test/
    java/
.mvn/
  wrapper/
README.md
pom.xml
mvnw
mvnw.cmd
```

## Prerequisites

- Java 17 JDK
- Internet access to download Maven Wrapper dependencies on first use

## Run the project

From the repository root, use the Maven Wrapper:

```powershell
./mvnw.cmd clean verify
./mvnw.cmd exec:java
```

Then open the app in your browser:

```text
http://127.0.0.1:8080/
```

## Local data

The app stores user data in a local JSON file outside source directories:

```text
data/reading-shelf.json
```

This file is intentionally not committed to version control and should be treated as runtime-generated state.

## Testing

Run the full Maven test suite:

```powershell
./mvnw.cmd clean verify
```

## Notes

- The app binds only to the loopback address (`127.0.0.1`).
- The project is designed for a local demo and does not expose production-grade authentication or network services.
- Future work can expand the app with additional book workflows, validation, and richer UI behavior.
