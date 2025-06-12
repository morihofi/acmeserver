package de.morihofi.acmeserver.utils.event;

import de.morihofi.acmeserver.types.events.AbstractEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple thread-safe event bus implementation for global events. It dispatches
 * events to all listeners registered for a given class. Events are delivered in
 * the order they are published. The bus is used across modules to decouple
 * components.
 */
public final class GlobalEventBus {

    private static final Map<Class<?>, List<EventListener<?>>> LISTENERS = new ConcurrentHashMap<>();

    private GlobalEventBus() {
    }

    /**
     * Registers an {@link EventSubscriber} for all event types returned by its {@link EventSubscriber#canHandle()} method.
     *
     * @param subscriber subscriber instance
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void register(EventSubscriber subscriber) {
        for (Class<? extends AbstractEvent> type : subscriber.canHandle()) {
            subscribe((Class) type, (EventListener) subscriber);
        }
    }

    /**
     * Unregisters an {@link EventSubscriber} from all events it handles.
     *
     * @param subscriber subscriber instance
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void unregister(EventSubscriber subscriber) {
        for (Class<? extends AbstractEvent> type : subscriber.canHandle()) {
            unsubscribe((Class) type, (EventListener) subscriber);
        }
    }

    /**
     * Registers a listener for the given event type.
     *
     * @param eventType event class
     * @param listener  listener to register
     * @param <T>       type of the event
     */
    public static <T> void subscribe(Class<T> eventType, EventListener<? super T> listener) {
        LISTENERS.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Unregisters a listener from the given event type.
     *
     * @param eventType event class
     * @param listener  listener to unregister
     * @param <T>       type of the event
     */
    public static <T> void unsubscribe(Class<T> eventType, EventListener<? super T> listener) {
        List<EventListener<?>> list = LISTENERS.get(eventType);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * Publishes an event to all registered listeners.
     *
     * @param event event data
     * @param <T>   type of the event
     */
    @SuppressWarnings("unchecked")
    public static <T> void publish(T event) {
        List<EventListener<?>> list = LISTENERS.get(event.getClass());
        if (list != null) {
            for (EventListener<?> l : list) {
                ((EventListener<T>) l).onEvent(event);
            }
        }
    }
}
