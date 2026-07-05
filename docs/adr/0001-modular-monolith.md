# ADR 0001: Start as a modular monolith

**Status:** Accepted

## Context

HEA needs clear domain boundaries but is still a single-team portfolio application. Splitting authentication, appointments, records, and reporting into separate deployable services would add network failure modes and operational overhead before scale requires them.

## Decision

Use one Spring Boot deployment organized by domain modules. Modules communicate through services and domain events, not cross-controller calls. Database tables remain separated by clear ownership.

## Consequences

The system is simple to run and test while preserving extraction boundaries. A future service split should follow measured load, ownership, or compliance needs rather than fashion.
