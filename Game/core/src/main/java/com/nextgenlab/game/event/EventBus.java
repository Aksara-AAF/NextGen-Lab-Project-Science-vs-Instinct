package com.nextgenlab.game.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBus {

    private static EventBus instance;
    private final Map<Class<?>, List<GameEventListener<?>>> listeners = new HashMap<>();

    private EventBus() {}

    public static EventBus getInstance() {
        if (instance == null) instance = new EventBus();
        return instance;
    }

    public <E> void subscribe(Class<E> type, GameEventListener<E> listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    public <E> void unsubscribe(Class<E> type, GameEventListener<E> listener) {
        List<GameEventListener<?>> list = listeners.get(type);
        if (list != null) list.remove(listener);
    }

    @SuppressWarnings("unchecked")
    public <E> void publish(E event) {
        List<GameEventListener<?>> list = listeners.get(event.getClass());
        if (list == null) return;
        for (GameEventListener<?> l : new ArrayList<>(list))
            ((GameEventListener<E>) l).onEvent(event);
    }
}
