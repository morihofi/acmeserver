package de.morihofi.certgine.types.events;

/**
 * Functional interface for handling events.
 *
 * @param <T> type of event data
 */
@FunctionalInterface
public interface EventListener<T> {
    /**
     * Invoked when an event is published.
     *
     * @param event event data
     */
    void onEvent(T event) throws Exception;
}
