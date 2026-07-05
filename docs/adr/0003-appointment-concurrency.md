# ADR 0003: Prevent appointment conflicts at service and database layers

**Status:** Accepted

Booking and rescheduling run inside a transaction, acquire a pessimistic lock on the doctor row, check overlapping appointments, and then write the appointment. A database index supports the overlap query. This avoids the classic “two users saw the same free slot” race.

A simple unique constraint on start time alone would not detect partially overlapping intervals, so it is not sufficient.
