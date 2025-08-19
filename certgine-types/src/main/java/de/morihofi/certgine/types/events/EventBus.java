/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.events;

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

    private static class OwnedListener {
        final Class<?> eventType;
        final EventListener<?> listener;

        OwnedListener(Class<?> eventType, EventListener<?> listener) {
            this.eventType = eventType;
            this.listener = listener;
        }
    }

    private final Map<Object, List<OwnedListener>> ownedListeners = new ConcurrentHashMap<>();

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
     * Registers an {@link EventSubscriber} owned by the specified module or component.
     *
     * <p>The registration is tracked so that all listeners for the given owner can be
     * removed in one operation via {@link #unregisterAll(Object)}.</p>
     *
     * @param owner      owning module or component
     * @param subscriber subscriber instance
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void register(Object owner, EventSubscriber subscriber) {
        register(subscriber);
        if (owner != null) {
            for (Class<? extends AbstractEvent> type : subscriber.canHandle()) {
                track(owner, type, (EventListener) subscriber);
            }
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
        ownedListeners.values().forEach(list -> list.removeIf(l -> l.listener.equals(subscriber)));
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
     * Registers a listener for the given event type and associates it with the specified owner.
     *
     * @param owner     owning module or component
     * @param eventType event class
     * @param listener  listener to register
     * @param <T>       type of the event
     */
    public <T> void subscribe(Object owner, Class<T> eventType, EventListener<? super T> listener) {
        subscribe(eventType, listener);
        if (owner != null) {
            track(owner, eventType, listener);
        }
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
        ownedListeners.values().forEach(l -> l.removeIf(o -> o.listener.equals(listener) && o.eventType.equals(eventType)));
    }

    /**
     * Unregisters all listeners owned by the specified object.
     *
     * @param owner owning module or component
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void unregisterAll(Object owner) {
        List<OwnedListener> list = ownedListeners.remove(owner);
        if (list != null) {
            for (OwnedListener holder : list) {
                unsubscribe((Class) holder.eventType, (EventListener) holder.listener);
            }
        }
    }

    private void track(Object owner, Class<?> type, EventListener<?> listener) {
        ownedListeners.computeIfAbsent(owner, k -> new CopyOnWriteArrayList<>())
                .add(new OwnedListener(type, listener));
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