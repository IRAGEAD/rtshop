/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package com.finderfeed.fdbosses.content.items.malkuth;

import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;

public class MalkuthFistDataComponent {
    public static final Codec<MalkuthFistDataComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group((App)Codec.BOOL.fieldOf("canSkipCooldown").forGetter(MalkuthFistDataComponent::canSkipCooldown), (App)Codec.INT.fieldOf("entityHookCooldown").forGetter(MalkuthFistDataComponent::getEntityHookCooldown)).apply((Applicative)instance, MalkuthFistDataComponent::new));
    public static final NetworkCodec<MalkuthFistDataComponent> STREAM_CODEC = NetworkCodec.composite((NetworkCodec)NetworkCodec.BOOL, MalkuthFistDataComponent::canSkipCooldown, (NetworkCodec)NetworkCodec.INT, MalkuthFistDataComponent::getEntityHookCooldown, MalkuthFistDataComponent::new);
    private boolean canSkipCooldown = false;
    private int entityHookCooldown = 0;

    public MalkuthFistDataComponent() {
    }

    public MalkuthFistDataComponent(MalkuthFistDataComponent dataComponent) {
        this.canSkipCooldown = dataComponent.canSkipCooldown;
        this.entityHookCooldown = dataComponent.entityHookCooldown;
    }

    public MalkuthFistDataComponent(boolean canSkipCooldown, int entityHookCooldown) {
        this.canSkipCooldown = canSkipCooldown;
        this.entityHookCooldown = entityHookCooldown;
    }

    public void setCanSkipCooldown(boolean canSkipCooldown) {
        this.canSkipCooldown = canSkipCooldown;
    }

    public boolean canSkipCooldown() {
        return this.canSkipCooldown;
    }

    public void setEntityHookCooldown(int entityHookCooldown) {
        this.entityHookCooldown = entityHookCooldown;
    }

    public int getEntityHookCooldown() {
        return this.entityHookCooldown;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        MalkuthFistDataComponent that = (MalkuthFistDataComponent)o;
        return this.canSkipCooldown == that.canSkipCooldown && this.entityHookCooldown == that.entityHookCooldown;
    }

    public int hashCode() {
        return Objects.hash(this.canSkipCooldown, this.entityHookCooldown);
    }
}

