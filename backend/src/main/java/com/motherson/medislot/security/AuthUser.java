package com.motherson.medislot.security;

/** The authenticated principal rebuilt from the JWT on every request. */
public record AuthUser(Long id, Role role, Long hospitalId, String email) {}
