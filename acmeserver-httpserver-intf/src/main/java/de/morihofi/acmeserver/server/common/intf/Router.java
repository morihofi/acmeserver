package de.morihofi.acmeserver.server.common.intf;


import de.morihofi.acmeserver.types.httpserver.HandlerType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Router that manages the mapping of paths to handlers.
 * The Router handles the routing logic, allowing handlers to be registered and retrieved
 * based on the request path and HTTP method type.
 */
public class Router {
    private final Map<HandlerType, Map<String, Endpoint>> staticHandlers = new HashMap<>();
    private final Map<HandlerType, List<VariableEndpoint>> variableHandlers = new HashMap<>();
    private final Map<String, Handler> beforeHandlers = new HashMap<>();
    private final Map<String, Handler> afterHandlers = new HashMap<>();

    /**
     * Adds a new handler to the router.
     *
     * @param endpoint The {@code Endpoint} containing the handler to be added.
     */
    public void addHandler(Endpoint endpoint) {
        HandlerType type = endpoint.getType();
        String path = endpoint.getPath();

        if (path.contains("{")) {
            // Path with variables
            variableHandlers.computeIfAbsent(type, k -> new ArrayList<>())
                    .add(new VariableEndpoint(path, endpoint));
        } else {
            // Static path
            staticHandlers.computeIfAbsent(type, k -> new HashMap<>())
                    .put(path, endpoint);
        }
    }

    /**
     * Removes a handler for the given path from the router.
     *
     * @param path The path of the handler to remove.
     */
    public void removeHandler(String path) {
        for (HandlerType type : HandlerType.values()) {
            staticHandlers.getOrDefault(type, Collections.emptyMap()).remove(path);

            variableHandlers.getOrDefault(type, Collections.emptyList())
                    .removeIf(variableEndpoint -> variableEndpoint.template.equals(path));
        }
    }


    /**
     * Retrieves the handler for a given path and method.
     *
     * @param path  The request path.
     * @param method The HTTP method as a {@code String}.
     * @return The corresponding {@code Handler}, or {@code null} if no handler matches.
     */
    public Handler getHandler(String path, String method, HandlerContext context) throws Exception {
        return getHandler(path, HandlerType.valueOf(method), context);
    }

    /**
     * Retrieves the handler for a given path and {@code HandlerType}.
     *
     * @param path   The request path.
     * @param method The {@code HandlerType} representing the HTTP method.
     * @return The corresponding {@code Handler}, or {@code null} if no handler matches.
     */
    public Handler getHandler(String path, HandlerType method, HandlerContext context) throws Exception {
        invokeHandlers(beforeHandlers, path, context);

        // Check for static path
        Endpoint staticEndpoint = staticHandlers.getOrDefault(method, Collections.emptyMap()).get(path);
        if (staticEndpoint != null) {
            invokeHandlers(afterHandlers, path, context);
            return staticEndpoint.getHandler();
        }

        // Check for variable path
        List<VariableEndpoint> varEndpoints = variableHandlers.getOrDefault(method, Collections.emptyList());
        for (VariableEndpoint variableEndpoint : varEndpoints) {
            Map<String, String> pathVariables = variableEndpoint.match(path);
            if (pathVariables != null) {
                invokeHandlers(afterHandlers, path, context);
                return variableEndpoint.endpoint.getHandler();
            }
        }

        invokeHandlers(afterHandlers, path, context);
        return null; // No matching handler
    }

    /**
     * Invokes handlers matching the path from a map of prefix handlers.
     *
     * @param handlers The map of prefix to handlers.
     * @param path     The request path.
     */
    private void invokeHandlers(Map<String, Handler> handlers, String path, HandlerContext context) throws Exception {

        for (Map.Entry<String, Handler> handlerEntry : handlers.entrySet()){
            Handler handler = handlerEntry.getValue();
            String prefix = handlerEntry.getKey();


            if (matchesPrefix(prefix, path)) {
                handler.handle(context); // Assuming `handle()` is the method to invoke the handler
            }
        }
    }

    /**
     * Checks if a given path matches a prefix.
     *
     * @param prefix The prefix, which may include wildcards (e.g., "/api/*").
     * @param path   The path to match.
     * @return {@code true} if the path matches the prefix; otherwise, {@code false}.
     */
    private boolean matchesPrefix(String prefix, String path) {
        if (prefix.endsWith("*")) {
            String base = prefix.substring(0, prefix.length() - 1); // Remove '*'
            return path.startsWith(base);
        }
        return path.equals(prefix);
    }

    /**
     * Retrieves the value of a specific path parameter from the request path.
     *
     * @param path  The request path.
     * @param param The name of the parameter to retrieve.
     * @return The value of the specified parameter, or {@code null} if not found.
     */
    public String getPathParam(String path, String param) {
        for (HandlerType type : HandlerType.values()) {
            List<VariableEndpoint> varEndpoints = variableHandlers.getOrDefault(type, Collections.emptyList());
            for (VariableEndpoint variableEndpoint : varEndpoints) {
                Map<String, String> pathVariables = variableEndpoint.match(path);
                if (pathVariables != null && pathVariables.containsKey(param)) {
                    return pathVariables.get(param);
                }
            }
        }
        return null; // Parameter not found
    }

    /**
     * Adds a beforeHandler for the given prefix.
     *
     * @param prefix  The prefix for which the beforeHandler applies.
     * @param handler The beforeHandler to add.
     */
    public void addBeforeHandler(String prefix, Handler handler) {
        beforeHandlers.put(normalizePrefix(prefix), handler);
    }

    /**
     * Adds an afterHandler for the given prefix.
     *
     * @param prefix  The prefix for which the afterHandler applies.
     * @param handler The afterHandler to add.
     */
    public void addAfterHandler(String prefix, Handler handler) {
        afterHandlers.put(normalizePrefix(prefix), handler);
    }

    /**
     * Removes a beforeHandler for the given prefix.
     *
     * @param prefix The prefix for which the beforeHandler applies.
     */
    public void removeBeforeHandler(String prefix) {
        beforeHandlers.remove(normalizePrefix(prefix));
    }

    /**
     * Removes an afterHandler for the given prefix.
     *
     * @param prefix The prefix for which the afterHandler applies.
     */
    public void removeAfterHandler(String prefix) {
        afterHandlers.remove(normalizePrefix(prefix));
    }

    /**
     * Normalizes a prefix by ensuring it does not end with a trailing slash.
     *
     * @param prefix The prefix to normalize.
     * @return The normalized prefix.
     */
    private String normalizePrefix(String prefix) {
        return prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    }

    /**
     * Checks if any handler is registered for a specific path.
     * @param path The path to check for registered handlers.
     * @return {@code true} if any handler is registered for the path, {@code false} otherwise.
     */
    public boolean isAnyHandlerRegisteredForPath(String path) {
        for (HandlerType type : HandlerType.values()) {
            if (staticHandlers.getOrDefault(type, Collections.emptyMap()).containsKey(path)) {
                return true; // Static handler found
            }

            List<VariableEndpoint> varEndpoints = variableHandlers.getOrDefault(type, Collections.emptyList());
            for (VariableEndpoint variableEndpoint : varEndpoints) {
                if (variableEndpoint.match(path) != null) {
                    return true; // Variable handler found
                }
            }
        }
        return false; // No handlers registered for the path
    }

    /**
     * Represents a variable-based endpoint with dynamic path matching.
     */
    private static class VariableEndpoint {
        private final String template;
        private final Endpoint endpoint;
        private final Pattern pattern;
        private final List<String> variableNames;

        /**
         * Constructs a {@code VariableEndpoint} with the given template and endpoint.
         *
         * @param template The template string containing variables (e.g., "/api/{id}").
         * @param endpoint The {@code Endpoint} associated with this variable path.
         */
        public VariableEndpoint(String template, Endpoint endpoint) {
            this.template = template;
            this.endpoint = endpoint;
            this.variableNames = new ArrayList<>();
            this.pattern = compileTemplate(template);
        }

        /**
         * Compiles a path template into a regex {@code Pattern}.
         *
         * @param template The template string containing variables (e.g., "/api/{id}").
         * @return The compiled {@code Pattern} for matching request paths.
         */
        private Pattern compileTemplate(String template) {
            StringBuilder regex = new StringBuilder();
            Matcher matcher = Pattern.compile("\\{([^/]+)}").matcher(template);

            int lastIndex = 0;
            while (matcher.find()) {
                // Append static text (non-variable part of the path)
                regex.append(Pattern.quote(template.substring(lastIndex, matcher.start())));
                // Append the variable placeholder regex
                regex.append("([^/]+)");
                // Store the variable name
                variableNames.add(matcher.group(1));
                lastIndex = matcher.end();
            }

            // Append the remaining static part of the template
            regex.append(Pattern.quote(template.substring(lastIndex)));
            return Pattern.compile("^" + regex + "$");
        }

        /**
         * Matches a given path against the template pattern and extracts variables.
         *
         * @param path The request path to match.
         * @return A map of variable names to values, or {@code null} if the path does not match.
         */
        public Map<String, String> match(String path) {
            Matcher matcher = pattern.matcher(path);
            if (!matcher.matches()) {
                return null;
            }

            Map<String, String> variables = new HashMap<>();
            for (int i = 0; i < variableNames.size(); i++) {
                variables.put(variableNames.get(i), matcher.group(i + 1));
            }
            return variables;
        }
    }
}
