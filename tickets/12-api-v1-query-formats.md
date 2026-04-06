# 12 — API v1: Query Parsing & Output Formats

**Priority:** P1
**Depends on:** 06 (entries endpoint as reference implementation)
**Parallelizable with:** 13-15

## Summary

Implement the MongoDB-style query parameter parsing and multi-format output that Nightscout clients rely on.

## Query Parameter Parsing

Nightscout uses MongoDB-style query parameters that must be translated to SQL:

| Nightscout Query | Meaning | SQL |
|-----------------|---------|-----|
| `find[date][$gte]=1700000000000` | date >= value | `WHERE date_ms >= 1700000000000` |
| `find[date][$lte]=1700099999999` | date <= value | `WHERE date_ms <= 1700099999999` |
| `find[sgv][$gt]=0` | sgv > 0 | `WHERE sgv > 0` |
| `find[type]=sgv` | exact match | `WHERE type = 'sgv'` |
| `find[type][$in][]=sgv&find[type][$in][]=mbg` | in list | `WHERE type IN ('sgv','mbg')` |
| `find[dateString][$gte]=2024-01-15` | string comparison | `WHERE date_string >= '2024-01-15'` |
| `count=50` | limit | `LIMIT 50` |

Build a generic query parser that converts these parameters into a `WHERE` clause using parameterized queries (prevent SQL injection).

## Output Formats

Nightscout supports multiple output formats via URL suffix or `Accept` header:

| Format | URL | Content-Type |
|--------|-----|-------------|
| JSON (default) | `/entries.json` | `application/json` |
| CSV | `/entries.csv` | `text/csv` |
| TSV | `/entries.tsv` | `text/tab-separated-values` |
| XML | `/entries.xml` | `application/xml` |

JSON is the only format used by uploaders. CSV/TSV are used by reporting tools. XML is rarely used but some old tools expect it.

## Acceptance Criteria

- [ ] Generic query parser converts MongoDB-style params to parameterized SQL
- [ ] Supports: `$gte`, `$lte`, `$gt`, `$lt`, `$eq`, `$ne`, `$in`, `$regex`
- [ ] SQL injection prevention via parameterized queries (never string concatenation)
- [ ] JSON output (primary)
- [ ] CSV output for entries and treatments
- [ ] Query parser shared across all v1 endpoints
- [ ] Tests for all operators with edge cases

## Notes

- The `$regex` operator is used sparingly — mainly for filtering `device` field. Can use PostgreSQL `~` operator.
- Priority: JSON > CSV > TSV. XML can be deferred.
