package com.dental.security;

import com.dental.entity.Role;

public record CurrentUser(Long accountId, String username, Role role, String sessionId) {}
