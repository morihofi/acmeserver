package de.morihofi.acmeserver.types.events;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple thread-safe event bus implementation. Instances of this class manage
 * their own set of listeners and are typically owned by an
 * {@code IServerInstance}.
 */
@Slf4j
public class EventBus {

    private final Map<Class<?>, List<EventListener<?>>> listeners = new ConcurrentHashMap<>();

    /**
     * Registers an {@link EventSubscriber} for all event types returned by its {@link EventSubscriber#canHandle()} method.
     *
     * @param subscriber subscriber instance
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void register(EventSubscriber subscriber) {
        for (Class<? extends AbstractEvent> type : subscriber.canHandle()) {
            log.debug("Registering subscriber {} for event type {}", subscriber.getClass().getName(), type.getName());
            subscribe((Class) type, (EventListener) subscriber);
        }
    }

    /**
     * Unregisters an {@link EventSubscriber} from all events it handles.
     *
     * @param subscriber subscriber instance
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void unregister(EventSubscriber subscriber) {
        for (Class<? extends AbstractEvent> type : subscriber.canHandle()) {
            log.debug("Unregistering subscriber {} from event type {}", subscriber.getClass().getName(), type.getName());
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
    public <T> void subscribe(Class<T> eventType, EventListener<? super T> listener) {
        log.debug("Subscribing listener {} to event type {}", listener.getClass().getName(), eventType.getName());
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Unregisters a listener from the given event type.
     *
     * @param eventType event class
     * @param listener  listener to unregister
     * @param <T>       type of the event
     */
    public <T> void unsubscribe(Class<T> eventType, EventListener<? super T> listener) {
        log.debug("Unsubscribing listener {} from event type {}", listener.getClass().getName(), eventType.getName());
        List<EventListener<?>> list = listeners.get(eventType);
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
    public <T> void publish(T event) {
        List<EventListener<?>> list = listeners.get(event.getClass());
        log.debug("Publishing event of type {} to {} listener(s)", event.getClass().getName(), list != null ? list.size() : 0);
        if (list != null) {
            for (EventListener<?> l : list) {
                try {
                    ((EventListener<T>) l).onEvent(event);
                } catch (Exception e) {
                    log.error("Error while handling event {} in listener {}", event.getClass().getName(), l.getClass().getName(), e);
                }
            }
        }
    }
}