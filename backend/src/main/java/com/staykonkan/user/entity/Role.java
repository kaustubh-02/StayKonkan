package com.staykonkan.user.entity;

/**
 * Platform roles. ADMIN is never self-assignable at registration —
 * AuthServiceImpl explicitly rejects it (see AuthServiceImpl.register).
 * OWNER can list properties; USER can browse/book. A single user is
 * exactly one role in this schema (kept simple by design — if a real
 * need for multi-role users emerges, e.g. a USER who becomes an OWNER
 * without losing booking history, migrate to a join table then).
 */
public enum Role {
    ADMIN,
    OWNER,
    USER
}
