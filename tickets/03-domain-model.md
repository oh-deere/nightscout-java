# 03 — Domain Model

**Priority:** P0 — blocks API and service layers
**Depends on:** 02 (schema design informs records)
**Parallelizable with:** 02 (can be written concurrently, then aligned)

## Summary

Define Java records for all core Nightscout entities, mapped to the PostgreSQL schema.

## Entities

### Entry (SGV / MBG / Calibration)

```java
record Entry(
    UUID id,
    String type,          // "sgv", "mbg", "cal"
    long dateMs,
    String dateString,
    String sysTime,
    Integer sgv,
    String direction,     // "Flat", "FortyFiveUp", "SingleUp", etc.
    Integer noise,
    Double filtered,
    Double unfiltered,
    Integer rssi,
    String device,
    int utcOffset,
    Instant createdAt
) {}
```

### Treatment

```java
record Treatment(
    UUID id,
    String eventType,
    String createdAtStr,   // original ISO string from uploader
    Instant createdAt,
    String enteredBy,
    String notes,
    Double insulin,
    Double carbs,
    Double glucose,
    String glucoseType,
    Double duration,
    int utcOffset,
    Map<String, Object> details  // jsonb — event-type-specific fields
) {}
```

### DeviceStatus

```java
record DeviceStatus(
    UUID id,
    Instant createdAt,
    String device,
    Map<String, Object> uploader,
    Map<String, Object> pump,
    Map<String, Object> openaps,
    Map<String, Object> loop,
    Map<String, Object> xdripjs,
    Map<String, Object> raw
) {}
```

### Profile

```java
record Profile(
    UUID id,
    Instant createdAt,
    String defaultProfile,
    Map<String, Object> store   // jsonb — named profiles
) {}
```

### Food / Activity

```java
record Food(UUID id, Instant createdAt, String name, String category,
            Double portion, String unit, Double carbs, Double fat,
            Double protein, Map<String, Object> details) {}

record Activity(UUID id, Instant createdAt, String name, Double duration,
                Map<String, Object> details) {}
```

## Acceptance Criteria

- [ ] Java records for all 6 core entities
- [ ] Records live in their respective domain packages under `storage`
- [ ] `@Table` annotations map to PostgreSQL table names
- [ ] Custom `ReadingConverter` / `WritingConverter` for `Map<String, Object>` <-> `jsonb`
- [ ] Direction enum or constants for the 9 Nightscout trend directions
- [ ] Unit tests for JSON serialization round-trip

## Notes

- Spring Data JDBC needs converters for `jsonb` <-> `Map<String, Object>`. Register these as `@Bean AbstractJdbcConfiguration`.
- Keep the records in the `storage` package. API DTOs (for v1 compatibility with `_id`, `dateString` etc.) are separate records in the `api` package.
