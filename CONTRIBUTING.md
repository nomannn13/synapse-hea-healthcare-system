# Contributing

1. Create a focused branch such as `feature/appointment-reschedule`.
2. Keep controllers thin; business rules belong in services.
3. Add or update tests for every behavioral change.
4. Run `make test` before opening a pull request.
5. Use Conventional Commit messages (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`).
6. Never commit secrets, real patient data, or generated build directories.

## Definition of done

A change is done when validation, authorization, error handling, tests, API documentation, and requirement traceability are updated where relevant.
