/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.MinecraftServer
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.event.TickEvent$ServerTickEvent
 *  net.minecraftforge.event.server.ServerStoppingEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.finderfeed.fdbosses;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.FORGE)
public class FDBossesServerScheduler {
    private static final List<DelayedServerAction> DELAYED_SERVER_ACTIONS = new ArrayList<DelayedServerAction>();

    @SubscribeEvent
    public static void tickActions(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Iterator<DelayedServerAction> iterator = DELAYED_SERVER_ACTIONS.iterator();
        while (iterator.hasNext()) {
            DelayedServerAction action = iterator.next();
            if (!action.tick()) continue;
            action.getServerAction().accept(event.getServer());
            iterator.remove();
        }
    }

    @SubscribeEvent
    public static void onServerShutdown(ServerStoppingEvent event) {
        DELAYED_SERVER_ACTIONS.clear();
    }

    public static void addDelayedAction(int lifetime, Consumer<MinecraftServer> action) {
        DELAYED_SERVER_ACTIONS.add(new DelayedServerAction(lifetime, action));
    }

    private static class DelayedServerAction {
        private int lifetime;
        private Consumer<MinecraftServer> serverAction;

        public DelayedServerAction(int lifetime, Consumer<MinecraftServer> serverAction) {
            this.lifetime = lifetime;
            this.serverAction = serverAction;
        }

        public Consumer<MinecraftServer> getServerAction() {
            return this.serverAction;
        }

        public int getLifetime() {
            return this.lifetime;
        }

        public boolean tick() {
            --this.lifetime;
            return this.lifetime <= 0;
        }
    }
}

