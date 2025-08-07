/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.server.common.intf;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple middleware pipeline managing before and after handlers.
 * Handlers are matched by prefix and executed in registration order.
 */
public class MiddlewarePipeline {
    private final Map<String, Handler> beforeHandlers = new HashMap<>();
    private final Map<String, Handler> afterHandlers = new HashMap<>();

    /**
     * Registers a handler executed before the main route handler.
     *
     * @param prefix  path prefix to match
     * @param handler handler to execute
     */
    public void before(String prefix, Handler handler) {
        beforeHandlers.put(normalizePrefix(prefix), handler);
    }

    /**
     * Registers a handler executed after the main route handler.
     *
     * @param prefix  path prefix to match
     * @param handler handler to execute
     */
    public void after(String prefix, Handler handler) {
        afterHandlers.put(normalizePrefix(prefix), handler);
    }

    /**
     * Removes a previously registered before handler.
     *
     * @param prefix path prefix used during registration
     */
    public void removeBefore(String prefix) {
        beforeHandlers.remove(normalizePrefix(prefix));
    }

    /**
     * Removes a previously registered after handler.
     *
     * @param prefix path prefix used during registration
     */
    public void removeAfter(String prefix) {
        afterHandlers.remove(normalizePrefix(prefix));
    }

    /**
     * Executes all before handlers matching the given path.
     *
     * @param path    request path
     * @param context handler context
     */
    public void executeBefore(String path, HandlerContext context) throws Exception {
        invokeHandlers(beforeHandlers, path, context);
    }

    /**
     * Executes all after handlers matching the given path.
     *
     * @param path    request path
     * @param context handler context
     */
    public void executeAfter(String path, HandlerContext context) throws Exception {
        invokeHandlers(afterHandlers, path, context);
    }

    private void invokeHandlers(Map<String, Handler> handlers, String path, HandlerContext context) throws Exception {
        for (Map.Entry<String, Handler> entry : handlers.entrySet()) {
            if (matchesPrefix(entry.getKey(), path)) {
                entry.getValue().handle(context);
            }
        }
    }

    private boolean matchesPrefix(String prefix, String path) {
        if (prefix.endsWith("*")) {
            String base = prefix.substring(0, prefix.length() - 1);
            return path.startsWith(base);
        }
        return path.equals(prefix);
    }

    private String normalizePrefix(String prefix) {
        return prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    }
}
