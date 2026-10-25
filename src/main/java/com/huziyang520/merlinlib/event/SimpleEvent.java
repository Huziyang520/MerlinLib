// A tiny event with no context: listeners are simply called.

package com.huziyang520.merlinlib.event;

import java.util.List;
import java.util.function.Function;

public final class SimpleEvent<T> {
    private final Function<List<T>, T> invokerFactory;
    private volatile T invoker;
    private volatile List<T> listeners = List.of();

    private SimpleEvent(Class<T> listenerClass, Function<List<T>, T> invokerFactory) {
        this.invokerFactory = invokerFactory;
        this.invoker = invokerFactory.apply(List.of());
    }

    public static <T> SimpleEvent<T> createArrayBacked(Class<T> listenerClass, Function<List<T>, T> invokerFactory) {
        return new SimpleEvent<>(listenerClass, invokerFactory);
    }

    public synchronized void register(T listener) {
        List<T> next = new java.util.ArrayList<>(this.listeners);
        next.add(listener);
        this.invoker = this.invokerFactory.apply(List.copyOf(next));
        this.listeners = List.copyOf(next);
    }

    public T invoker() {
        return this.invoker;
    }

    public int listenerCount() {
        return this.listeners.size();
    }
}
