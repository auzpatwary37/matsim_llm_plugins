# Contributing to MATSim LLM Plugins

Thank you for your interest in contributing to MATSim LLM Plugins!

This project follows the MATSim development guidelines. Please read this document
carefully before contributing.

## Development Requirements

- **Java 21** or higher
- **Maven 3.6+**
- Code must compile before committing

## Commit Rules

We follow [MATSim's commit rules](https://matsim.org/docs/devguide/commit-rules.html):

- Use **Conventional Commits** for PR titles:
  - `feat: Add new tool for route planning`
  - `fix: Resolve null pointer in ChatManager`
  - `docs: Update README with new examples`
  - `refactor: Simplify tool execution loop`

- Write meaningful commit messages in English
- Do not commit personal data or large binary files
- Only commit to `org.matsim.contrib` via Pull Request

## Pull Request Process

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes ensuring code compiles
4. Submit a Pull Request to `main`
5. Ensure PR title follows Conventional Commits format

## Code Style

- Use tabs for indentation (follows MATSim conventions)
- Add GPL header to new Java files:
  ```java
  /* *********************************************************************** *
  * project: org.matsim.*
  * copyright       : (C) 2025 by the members listed in the COPYING,
  *                   LICENSE and AUTHORS files
  * *********************************************************************** *
  ```
- Write code compatible with MATSim 2025.x

## Questions?

- Check the [MATSim documentation](https://matsim.org/docs/)
- Visit the [MATSim FAQ](https://matsim.org/faq)
- Open an issue for bugs or feature requests

---

For more details on MATSim development, see:
- https://matsim.org/docs/contributing/
- https://matsim.org/docs/devguide/conventions.html