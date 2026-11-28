/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.lopymine.ip.config.InventoryParticlesConfig
 *  net.lopymine.ip.config.sub.InventoryParticleConfig
 *  net.lopymine.ip.element.mod.InventoryParticle
 *  net.lopymine.ip.renderer.InventoryParticlesRenderer
 *  net.lopymine.ipi.config.InventoryInteractionsConfig
 *  net.lopymine.ipi.renderer.CursorItemRenderer
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 *  net.minecraftforge.fml.ModList
 */
package dev.dyslexic.cinematic.client;

import java.util.Locale;
import net.lopymine.ip.config.InventoryParticlesConfig;
import net.lopymine.ip.config.sub.InventoryParticleConfig;
import net.lopymine.ip.element.mod.InventoryParticle;
import net.lopymine.ip.renderer.InventoryParticlesRenderer;
import net.lopymine.ipi.config.InventoryInteractionsConfig;
import net.lopymine.ipi.renderer.CursorItemRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;

final class InventoryEffectsProbe {
    private InventoryEffectsProbe() {
    }

    static void defaults() {
        InventoryParticlesConfig config = InventoryParticlesConfig.getInstance();
        config.getMainConfig().setModEnabled(true);
        InventoryParticleConfig particles = config.getParticleConfig();
        particles.setCursorSpawnEnabled(true);
        particles.setHoveredSlotSpawnEnabled(false);
        particles.setGuiSlotsSpawnEnabled(false);
        particles.setGuiActionsSpawnEnabled(true);
        particles.setGuiActionTakeSpawnEnabled(true);
        particles.setGuiActionPutSpawnEnabled(true);
        particles.setGuiActionQuickMoveSpawnEnabled(true);
        if (InventoryEffectsProbe.hasInteractions()) {
            InventoryInteractionsConfig.getInstance().getMainConfig().setModEnabled(true);
        }
        InventoryEffectsProbe.clear();
    }

    static void clear() {
        InventoryParticlesRenderer renderer = InventoryParticlesRenderer.getInstance();
        renderer.getScreenElements().clear();
        renderer.getPendingElements().clear();
    }

    static int particles() {
        InventoryParticlesRenderer renderer = InventoryParticlesRenderer.getInstance();
        return renderer.getScreenElements().size() + renderer.getPendingElements().size();
    }

    static Item cursor() {
        return InventoryParticlesRenderer.getInstance().getCursor().getCurrentStack().m_41720_();
    }

    static boolean hasInteractions() {
        return ModList.get().isLoaded("inventory_interactions");
    }

    static Item physicsItem() {
        return InventoryEffectsProbe.hasInteractions() ? CursorItemRenderer.getInstance().getCursorItem().getItem() : Items.f_41852_;
    }

    static float angle() {
        return InventoryEffectsProbe.hasInteractions() ? CursorItemRenderer.getInstance().getCursorItem().getRenderAngle(1.0f) : 0.0f;
    }

    static double x() {
        return InventoryParticlesRenderer.getInstance().getCursor().getX();
    }

    static double y() {
        return InventoryParticlesRenderer.getInstance().getCursor().getY();
    }

    static boolean near(double x, double y) {
        return InventoryParticlesRenderer.getInstance().getScreenElements().stream().anyMatch(p -> Math.abs(p.getX() - x) < 40.0 && Math.abs(p.getY() - y) < 40.0);
    }

    static String positions() {
        StringBuilder out = new StringBuilder();
        for (InventoryParticle p : InventoryParticlesRenderer.getInstance().getScreenElements()) {
            out.append(String.format(Locale.ROOT, " (%.0f, %.0f)", p.getX(), p.getY()));
        }
        return out.toString();
    }

    static void actionOnly() {
        InventoryEffectsProbe.defaults();
        InventoryParticlesConfig.getInstance().getParticleConfig().setCursorSpawnEnabled(false);
        InventoryEffectsProbe.clear();
    }

    static void allSlots() {
        InventoryEffectsProbe.actionOnly();
        InventoryParticleConfig p = InventoryParticlesConfig.getInstance().getParticleConfig();
        p.setGuiActionsSpawnEnabled(false);
        p.setHoveredSlotSpawnEnabled(true);
        p.setGuiSlotsSpawnEnabled(true);
    }

    static void disable() {
        InventoryParticleConfig p = InventoryParticlesConfig.getInstance().getParticleConfig();
        p.setCursorSpawnEnabled(false);
        p.setGuiActionsSpawnEnabled(false);
        p.setHoveredSlotSpawnEnabled(false);
        p.setGuiSlotsSpawnEnabled(false);
        InventoryEffectsProbe.clear();
    }
}

