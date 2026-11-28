/*
 * Decompiled with CFR 0.152.
 */
package net.spell_engine.api.event;

import net.spell_engine.api.event.Event;

public final class StagedEvent<T> {
    public final Event<T> PRE = new Event();
    public final Event<T> POST = new Event();
}

