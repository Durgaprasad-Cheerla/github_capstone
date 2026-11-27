# Changelog

## Unreleased

- Add a local Reading Shelf app for book inventory and reading-status tracking, with input validation, loopback-only access, and persistent JSON storage.
- Normalize title and author for duplicate detection and status changes: trimmed and case-insensitive (Locale.ROOT); new books are stored trimmed with the first-entered casing; saved data is not migrated (SCRUM-223, SCRUM-225).
- Reject create requests whose title exceeds 100 or author exceeds 50 characters after trimming (SCRUM-226).
- Reject request bodies over 1,048,576 bytes with 413 PAYLOAD_TOO_LARGE using a bounded read (SCRUM-226).
- Keep the status-change message visible after the inventory reloads (SCRUM-220).
