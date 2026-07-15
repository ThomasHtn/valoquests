# Contributing

## Before submitting a change

Run the complete verification lifecycle:

```bash
mvn clean verify
```

## Development rules

- Write source code, comments, JavaDoc, commit messages, and documentation in English.
- Document every declared function with JavaDoc.
- Use four spaces for indentation and keep declarations readable.
- Keep controllers focused on HTTP concerns.
- Place business rules in application or domain services.
- Define transaction boundaries at use-case level.
- Expose DTOs instead of persistence entities.
- Add a Flyway migration for every database schema change.
- Add or update tests whenever behavior changes.
- Never commit credentials, API keys, or local environment files.
