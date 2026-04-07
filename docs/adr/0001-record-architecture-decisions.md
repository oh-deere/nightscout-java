# 1. Record architecture decisions

Date: 2026-04-07

## Status

Accepted

## Context

This service is being rewritten from the upstream Nightscout (Node.js + MongoDB) to
Java 25 / Spring Boot 4 / PostgreSQL. The rewrite has accumulated several
non-obvious design decisions — what to drop, what to replace, what to keep
compatible — that aren't visible from reading the code alone.

We need a lightweight way to record these decisions so future contributors (and
future-us) can understand *why* something is the way it is, and so each decision
sits next to the trade-offs that were considered.

## Decision

We will use **Architecture Decision Records (ADRs)** as described by Michael Nygard
([blog post](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)).

- ADRs live under `docs/adr/`.
- Filename format: `NNNN-short-kebab-title.md` (zero-padded sequence).
- Each ADR has the sections: **Status**, **Context**, **Decision**, **Consequences**.
- Status moves through **Proposed → Accepted → (later) Deprecated / Superseded by NNNN**.
- A new ADR may supersede an older one rather than editing it; the old file stays
  in place and gets a `Status: Superseded by 00NN` line so the history is
  preserved.
- Trivial choices don't need an ADR. Write one when:
  - the decision is hard to reverse (data model, API contract, dependency choice),
  - the decision rules out an obviously appealing alternative,
  - or you want to remember the reason in 18 months.

## Consequences

- Reviewers can point to ADRs in PRs instead of re-litigating the same trade-offs.
- New contributors get a guided tour of the project's load-bearing decisions.
- One more thing to maintain — but only when something significant is decided,
  not on every change.
