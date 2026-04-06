# 04 — Storage Layer

**Priority:** P0 — blocks API endpoints
**Depends on:** 02, 03
**Parallelizable with:** 05 (auth)

## Summary

Implement Spring Data JDBC repositories for all core entities with the query patterns Nightscout clients expect.

## Repositories

### EntryRepository

```java
public interface EntryRepository extends CrudRepository<Entry, UUID> {
    List<Entry> findByTypAndDateMsGreaterThanEqualOrderByDateMsDesc(String type, long dateMs);
    List<Entry> findTop1ByTypeOrderByDateMsDesc(String type);  // current SGV
    List<Entry> findByDateMsBetweenOrderByDateMsDesc(long from, long to);
    @Query("SELECT * FROM entries ORDER BY date_ms DESC LIMIT :count")
    List<Entry> findLatest(int count);
}
```

### TreatmentRepository

```java
public interface TreatmentRepository extends CrudRepository<Treatment, UUID> {
    List<Treatment> findByCreatedAtAfterOrderByCreatedAtDesc(Instant since);
    List<Treatment> findByEventTypeAndCreatedAtAfter(String eventType, Instant since);
}
```

### DeviceStatusRepository, ProfileRepository, FoodRepository, ActivityRepository

Similar patterns — date-range queries, latest-N queries.

## Key Query Patterns to Support

These are the queries Nightscout clients actually make:

| Pattern | Example | SQL equivalent |
|---------|---------|---------------|
| Latest N entries | `GET /entries.json?count=10` | `ORDER BY date_ms DESC LIMIT 10` |
| Current SGV | `GET /entries/current.json` | `WHERE type='sgv' ORDER BY date_ms DESC LIMIT 1` |
| Date range | `?find[date][$gte]=1700000000000` | `WHERE date_ms >= ?` |
| Type filter | `?find[type]=sgv` | `WHERE type = ?` |
| Treatments since | `?find[created_at][$gte]=...` | `WHERE created_at >= ?` |
| Latest devicestatus | `?count=1` | `ORDER BY created_at DESC LIMIT 1` |

## Acceptance Criteria

- [ ] Repository interfaces for all 6 entities
- [ ] Implementations in `impl` sub-packages where custom queries are needed
- [ ] `jsonb` converters registered and working
- [ ] Integration tests with Testcontainers PostgreSQL
- [ ] Tests verify: insert, deduplication (unique constraint), date-range queries, latest-N queries
- [ ] Pagination support for large result sets

## Notes

- For complex dynamic queries (the MongoDB-style `find[field][$op]=value` pattern), consider a custom `JdbcTemplate`-based query builder rather than trying to express everything via Spring Data method names.
- Deduplication: use `INSERT ... ON CONFLICT DO UPDATE` (PostgreSQL upsert) to match MongoDB's upsert behavior.
