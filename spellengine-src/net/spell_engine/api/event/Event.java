/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.api.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public final class Event<T> {
    private boolean executing = false;
    private final List<T> handlers = new ArrayList<T>();

    public void register(T listener) {
        this.handlers.add(listener);
    }

    public boolean isListened() {
        return !this.handlers.isEmpty();
    }

    public void invoke(Consumer<T> function) {
        if (this.executing) {
            return;
        }
        this.executing = true;
        for (T handler : this.handlers) {
            function.accept(handler);
        }
        this.executing = false;
    }

    @Nullable
    public <R> R invokeWithResult(Function<T, R> function) {
        T handler;
        if (this.executing) {
            return null;
        }
        this.executing = true;
        R result = null;
        Iterator<T> iterator = this.handlers.iterator();
        while (iterator.hasNext() && (result = (R)function.apply(handler = iterator.next())) == null) {
        }
        this.executing = false;
        return result;
    }
}

