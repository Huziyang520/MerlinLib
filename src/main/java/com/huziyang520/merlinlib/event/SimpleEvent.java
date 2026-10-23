package com.huziyang520.merlinlib.event;

import java.util.List;
import java.util.function.Function;

/**
 * A tiny event with no context: listeners are simply called.
 */
public final class SimpleEvent<T> {

    private final Function<List<T>, T> invokerFactory;
    private volatile T invoker;
    private volatile List<T> listeners = List.of();

    private SimpleEvent(Class<T> listenerClass, Function<List<T>, T> invokerFactory) {
        this.invokerFactory = invokerFactory;
        this.invoker = invokerFactory.apply(List.of());
    }

    /**
     * Creates an event backed by a listener array.
     *
     * @param listenerClass  the listener interface
     * @param invokerFactory builds the "call everyone" invoker from the current listeners
     * @param <T>            the listener type
     * @return the new event
 */
    public static <T> SimpleEvent<T> createArrayBacked(Class<T> listenerClass, Function<List<T>, T> invokerFactory) {
        return new SimpleEvent<>(listenerClass, invokerFactory);
    }

    /**
     * Adds a listener. Safe to call while the game is running; the invoker is rebuilt once per registration.
     *
     * @param listener the listener to add
 */
    public synchronized void register(T listener) {
        List<T> next = new java.util.ArrayList<>(this.listeners);
        next.add(listener);
        this.invoker = this.invokerFactory.apply(List.copyOf(next));
        this.listeners = List.copyOf(next);
    }

    /** @return the invoker to call; never {@code null}, and a no-op when nothing is registered */
    public T invoker() {
        return this.invoker;
    }

    /** @return how many listeners are registered, for diagnostics */
    public int listenerCount() {
        return this.listeners.size();
    }
}
