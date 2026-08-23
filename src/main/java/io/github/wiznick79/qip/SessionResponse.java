package io.github.wiznick79.qip;

import java.util.List;

record SessionResponse(
        boolean authenticated, String username, List<String> roles, String csrfHeaderName, String csrfToken) {}
