# GitHub Capstone Project

This repository provides a reusable Agentic SDLC workflow intended to adapt to the user-provided story and approved requirements, rather than assume a particular product such as a Reading Shelf. Each project should derive its requirements, design, plan, implementation, review, and verification from that project's scope.

The current repository's implementation scaffold is a Java 17 Maven starter: `org.example.App` prints `Hello World!`. The workflow is product-agnostic, but its current technology standards in `.github/standards/TechStack.md` are Java/Maven-specific; supporting other technology stacks requires updating those standards and the specialist agents that enforce them.

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
  main/java/
  test/java/
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

## Build and test

From PowerShell at the repository root, use the Maven Wrapper:

```powershell
.\mvnw.cmd clean verify
```

The current test suite checks that the application entry point runs. Run the application with:

```powershell
.\mvnw.cmd exec:java
```
