package com.staykonkan.user.entity;

/** Account lifecycle status. SUSPENDED is admin-triggered and blocks login. */
public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    DELETED
}
