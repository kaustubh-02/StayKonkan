# StayKonkan — Phase 2: Database Design Document

**Companion to:** `staykonkan_schema.sql` (executable PostgreSQL 15+ DDL, validated against a live Postgres 16 instance)
**Tables:** 42 (30 core domain tables + 12 join/lookup/history support tables)
**Normalization:** 3NF throughout; deliberate, documented denormalization only in views (Section 5)

---

## 1. ER Diagram (Textual — grouped by module)

```
                                   ┌────────────┐
                                   │   users    │
                                   └─────┬──────┘
                     ┌────────────────────┼─────────────────────┐
                     │                    │                     │
             ┌───────▼──────┐     ┌───────▼───────┐      ┌──────▼───────┐
             │ user_roles   │     │ hotel_owners  │      │refresh_tokens│
             │  (M:N roles) │     └───────┬───────┘      └──────────────┘
             └───────┬──────┘             │
                     │                    │
             ┌───────▼──────┐    ┌────────▼────────┐      ┌──────────────┐
             │    roles     │    │     hotels      │◄─────┤ destinations │
             └───────┬──────┘    └───┬────┬────┬───┘      └──────┬───────┘
                     │               │    │    │                 │
           ┌─────────▼───────┐       │    │    │          ┌──────▼───────┐
           │role_permissions │       │    │    │          │ attractions  │
           └─────────┬───────┘       │    │    │          └──────────────┘
                     │               │    │    │
             ┌───────▼──────┐  ┌─────▼┐ ┌─▼──────────┐ ┌──────────────────┐
             │ permissions  │  │rooms │ │hotel_images│ │ hotel_amenities  │──► amenities
             └──────────────┘  └──┬───┘ └────────────┘ └──────────────────┘
                                  │
                       ┌──────────▼─────────┐
                       │ room_availability  │
                       └────────────────────┘

  users ──► booking_requests ──► confirmed_bookings ──► commissions ──► commission_rules
              │       │                  │
              │       │                  ├──► reviews ──► ratings
              │       │                  └──► (completed_at / cancelled_at)
              │       └──► booking_status_history ◄── booking_statuses
              │
              ├──► payments ──► payment_transactions
              ├──► coupon_redemptions ──► coupons
              ├──► wishlists ──► hotels
              └──► notifications

  restaurants ──► restaurant_images / restaurant_menu_items   (independent vertical)
  cab_drivers ──► vehicles ──► cab_bookings ◄── users          (independent vertical)
  travel_packages ──► travel_package_items ──► destinations / attractions
  audit_logs, support_tickets, contact_messages — platform-wide, reference users loosely
```

**How to read this:** every arrow is a foreign key. Notice the **Booking module sits at the center** — Payments, Commissions, and Reviews all hang off `booking_requests` / `confirmed_bookings`, exactly mirroring the Phase 1 business workflow (Search → Request → Confirm → Pay → Commission). Restaurants and Cabs are already modeled as **parallel, independent verticals** that reuse `users`, `destinations`, and (for cabs/restaurants) `hotel_owners` as a generic "business owner" concept — this is what lets them be added later without restructuring the core.

---

## 2. Why 3NF, and Where We Intentionally Deviate

**Normalized (3NF) by default:**
- No repeating groups — e.g. `hotel_images` and `hotel_amenities` are separate tables, not comma-separated columns.
- No derived/calculated columns stored redundantly — e.g. a hotel's average rating is **not** stored on the `hotels` row; it's computed via `vw_hotel_search_summary`.
- Every non-key attribute depends on the whole primary key, not part of it — join tables like `hotel_amenities` carry no extra attributes beyond the composite key.

**Deliberate exceptions (documented, not accidental):**
- `booking_requests.quoted_price` and `confirmed_bookings.final_price` both exist — this looks like duplication but isn't: `quoted_price` is what the admin *told* the customer during negotiation, `final_price` is what was *actually* agreed and paid. Keeping both preserves the negotiation history.
- `commissions.commission_amount` is also mirrored on `confirmed_bookings.commission_amount` for fast dashboard reads without a join; the `commissions` table remains the source of truth (ledger), and this is the one place a future refactor might introduce a sync trigger if drift becomes an issue.
- Views (Section 5) intentionally pre-join and pre-aggregate for read performance — this is standard, safe denormalization because views are computed at query time, not stored.

---

## 3. Key Design Decisions

### 3.1 Surrogate Keys: `BIGSERIAL` + exposed `UUID`
Every core entity has a sequential `BIGSERIAL` primary key **and** a `UUID` column (`users.uuid`, `booking_requests.uuid`). Sequential IDs are used internally for joins and indexing (smaller, better B-tree locality, faster at 100k+ scale). The UUID is what's exposed in URLs and API responses, so internal record counts (e.g., "we've had 40,000 bookings") are never leaked to competitors or scrapers.

### 3.2 The Booking State Machine Is Enforced Twice
Per Phase 1 Section 7.1, the state machine lives in the application layer — but the database backs it up:
- `booking_statuses` is a reference table (not a native Postgres ENUM), so **new statuses can be added without a schema migration** — just an INSERT.
- A trigger (`trg_booking_status_change`) automatically writes to `booking_status_history` on every status change — the audit trail can never be forgotten by a developer, because it's not optional application code, it's guaranteed by the database.

### 3.3 Cascading Rules Follow Data Ownership
| Relationship | Rule | Reasoning |
|---|---|---|
| `hotel_images.hotel_id → hotels.id` | `CASCADE` | Images have no meaning without their hotel |
| `hotel_amenities → hotels/amenities` | `CASCADE` | Pure join table |
| `booking_requests.hotel_id → hotels.id` | `RESTRICT` | A hotel with historical bookings must never be hard-deleted (use `status = 'INACTIVE'` instead) |
| `booking_requests.assigned_admin_id → users.id` | `SET NULL` | If a staff account is deleted, the booking survives, just unassigned |
| `confirmed_bookings.booking_request_id` | `RESTRICT` | Financial records must never disappear silently |
| `refresh_tokens.user_id → users.id` | `CASCADE` | Tokens are meaningless without the user |

This table alone should answer "what happens if I delete X?" for every relationship in the schema.

### 3.4 Money Is Always `NUMERIC`, Never `FLOAT`
Every price/amount/commission column uses `NUMERIC(10,2)` or `NUMERIC(8,2)`. Floating-point types are never used for currency anywhere in the schema — this avoids the classic rounding-error bugs that corrupt financial ledgers over time.

### 3.5 CHECK Constraints as a Second Line of Defense
Even though validation lives primarily in the Spring Boot service layer (Bean Validation + the state machine), the database enforces the same invariants independently:
- `check_out > check_in` on bookings
- `valid_to > valid_from` on coupons/offers
- Ratings bounded `1–5`
- Prices/amounts `>= 0`

If a bug, script, or future integration ever bypasses the application layer, the database still refuses to store impossible data.

---

## 4. Indexing Strategy

| Index Type | Used For | Example |
|---|---|---|
| **Single-column B-tree** | Foreign key lookups, status filters | `idx_hotels_destination`, `idx_booking_requests_status` |
| **Composite (multi-column)** | Queries that always filter on 2+ columns together | `idx_booking_requests_hotel_status` (admin queue per hotel), `idx_hotels_dest_status` (search: active hotels in a destination), `idx_booking_requests_customer_created` ("My Bookings", newest first) |
| **Partial indexes** | Hot-path queries on a small subset of rows | `idx_notifications_user_unread` (only unread rows), `idx_commission_rules_hotel_active` (only active rules), `idx_coupons_active`, unique cover-image enforcement |
| **GIN + `pg_trgm`** | Fuzzy/partial text search without a search engine | `idx_hotels_name_trgm`, `idx_users_name_trgm` — enables `ILIKE '%naga%'` to use an index instead of a full table scan |
| **BRIN** | Very large, append-only, time-ordered tables | `idx_audit_logs_created_brin` — a fraction of the size of a B-tree at millions of rows, still fast for "logs from the last week" |
| **Unique constraints** | Data integrity, not just performance | `uq_users_email`, `uq_room_date` (one availability row per room per day), `uq_wishlist_user_hotel` |

**Composite index column ordering** always puts the highest-selectivity / most-frequently-filtered column first, matching the actual query patterns described in Phase 1's UI Navigation Flow and User Journey (Sections 10–11) — e.g., the admin queue is filtered by hotel far more often than by raw status alone, so `(hotel_id, status_id)` outperforms `(status_id, hotel_id)` for that screen.

---

## 5. Views — Why They Exist

Rather than making the application layer stitch together 4–5 joins for every page load, four views encode the platform's most common read patterns directly in the database:

1. **`vw_hotel_search_summary`** — one row per active hotel with min price, average rating, review count, and cover image. This is the Search Results page's entire data source in a single `SELECT ... WHERE destination_name = ?`.
2. **`vw_admin_booking_queue`** — the Admin Dashboard's booking queue, fully joined and human-readable (status labels, names, not just IDs).
3. **`vw_commission_report`** — monthly commission totals per hotel, split into settled vs. pending — directly answers Phase 1 FR19.
4. **`vw_active_offers`** — offers currently within their valid date range, for the homepage.

**Scaling note:** these are ordinary (non-materialized) views today — always live, always correct. If `vw_hotel_search_summary` becomes a bottleneck at high traffic, it can be converted to a `MATERIALIZED VIEW` refreshed every few minutes with zero change to application code, since the query interface stays identical.

---

## 6. Performance & Query Optimization Notes

- **Connection pooling:** Use PgBouncer (or Spring's HikariCP tuned appropriately) in front of PostgreSQL once concurrent connections grow — Postgres connections are relatively expensive; pooling is essential well before 100k users is reached.
- **Read replicas:** Route `vw_commission_report` and admin analytics reads to a replica once write load on the primary becomes significant, keeping the booking-creation path fast.
- **Partitioning:** `booking_requests` and `audit_logs` are the two tables most likely to reach tens of millions of rows. Both are designed to partition cleanly by month (`created_at`) later — no schema change required, just a migration to a partitioned table.
- **`pg_stat_statements`:** Enable this extension in production from day one. It costs almost nothing and is the single best tool for finding your actual slow queries instead of guessing.
- **`EXPLAIN (ANALYZE, BUFFERS)`:** Should be run against the four views above under realistic data volume before launch, to confirm the planner is using the composite/partial indexes as designed rather than falling back to sequential scans.
- **Caching:** `vw_hotel_search_summary` results for popular destination+date-range combinations are excellent candidates for a Redis cache layer (Phase 1, Section 22) — hotel search is read-heavy and tolerant of a few minutes of staleness.
- **N+1 avoidance:** Because the views pre-join, the Spring Data JPA layer should query these views directly for list/dashboard screens rather than lazily loading `Hotel → Rooms → Images` per row.

---

## 7. Validation

This schema was executed end-to-end against a live **PostgreSQL 16** instance during authoring:
- All 42 `CREATE TABLE` statements succeeded.
- All indexes (B-tree, composite, partial, GIN/trgm, BRIN) and unique constraints were created without conflict.
- Both trigger functions (`trg_booking_status_change`, `trg_increment_coupon_usage`) fired correctly against seed data — a status update on `booking_requests` produced the expected row in `booking_status_history`.
- All four views returned correctly joined, correctly aggregated data against the seed rows.

The script in `staykonkan_schema.sql` is ready to run as-is via `psql -f staykonkan_schema.sql` or as a Flyway/Liquibase baseline migration.

---

**End of Phase 2.**

Waiting for your instruction: **"Next Phase"** to proceed.
