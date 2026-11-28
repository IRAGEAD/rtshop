/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.Lighting
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.logging.LogUtils
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.EntityRenderDispatcher
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.client.screen.BannerBossRenderer;
import com.pla.spiral_abyss.client.screen.OptionalGeoPreview;
import com.pla.spiral_abyss.encounter.data.BossOptions;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

final class EnemyPreviewRenderer {
    private final Map<ResourceLocation, LivingEntity> entities = new HashMap<ResourceLocation, LivingEntity>();
    private final Set<ResourceLocation> unavailable = new HashSet<ResourceLocation>();
    private final BannerBossRenderer geoPortraits = new BannerBossRenderer(0L);

    EnemyPreviewRenderer() {
    }

    Component name(ResourceLocation id) {
        EntityType<?> type = AbyssRegistries.ENTITY_TYPES.getValue(id);
        return type == null ? Component.m_237113_((String)id.toString()) : type.m_20676_();
    }

    void clear() {
        this.entities.clear();
        this.unavailable.clear();
        this.geoPortraits.clear();
    }

    void render(GuiGraphics graphics, ResourceLocation id, int x, int y, int width, int height) {
        this.render(graphics, id, BossOptions.PreviewAnimation.DEFAULT, x, y, width, height);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void render(GuiGraphics graphics, ResourceLocation id, BossOptions.PreviewAnimation animation, int x, int y, int width, int height) {
        Minecraft client = Minecraft.m_91087_();
        if (client.f_91073_ == null || this.unavailable.contains(id)) {
            this.fallback(graphics, x, y, width, height);
            return;
        }
        LivingEntity entity = this.entities.get(id);
        if (entity == null || entity.m_9236_() != client.f_91073_) {
            try {
                Entity created;
                EntityType<?> type = AbyssRegistries.ENTITY_TYPES.getValue(id);
                Entity entity2 = created = type == null ? null : type.m_20615_((Level)client.f_91073_);
                if (!(created instanceof LivingEntity)) {
                    this.unavailable.add(id);
                    this.fallback(graphics, x, y, width, height);
                    return;
                }
                LivingEntity living = (LivingEntity)created;
                BannerBossRenderer.initializeModel(living);
                living.m_146922_(180.0f);
                living.f_20883_ = 180.0f;
                living.f_20886_ = 180.0f;
                living.f_20885_ = 180.0f;
                entity = living;
                this.entities.put(id, living);
            }
            catch (RuntimeException exception) {
                this.unavailable.add(id);
                LogUtils.getLogger().warn("Cannot create Abyss enemy preview {}", (Object)id, (Object)exception);
                this.fallback(graphics, x, y, width, height);
                return;
            }
        }
        if (OptionalGeoPreview.supports(entity, animation)) {
            this.geoPortraits.render(graphics, id, animation, x, y, width, height);
            return;
        }
        float modelWidth = Math.max(0.5f, entity.m_20205_() * 1.35f);
        float modelHeight = Math.max(0.5f, entity.m_20206_() * 1.15f);
        float scale = Math.min((float)(width - 4) / modelWidth, (float)(height - 4) / modelHeight);
        EntityRenderDispatcher dispatcher = client.m_91290_();
        Quaternionf previousCamera = new Quaternionf((Quaternionfc)dispatcher.m_253208_());
        graphics.m_280168_().m_85836_();
        graphics.m_280588_(x, y, x + width, y + height);
        try {
            graphics.m_280168_().m_252880_((float)x + (float)width / 2.0f, (float)(y + height - 3), 100.0f);
            graphics.m_280168_().m_85841_(scale, scale, -scale);
            graphics.m_280168_().m_252781_(new Quaternionf().rotateZ((float)Math.PI).rotateY((float)Math.toRadians(15.0)));
            Lighting.m_166384_();
            dispatcher.m_252923_(new Quaternionf());
            dispatcher.m_114468_(false);
            LivingEntity model = entity;
            RenderSystem.runAsFancy(() -> dispatcher.m_114384_((Entity)model, 0.0, 0.0, 0.0, 0.0f, 1.0f, graphics.m_280168_(), (MultiBufferSource)graphics.m_280091_(), 0xF000F0));
            graphics.m_280262_();
        }
        catch (RuntimeException exception) {
            this.unavailable.add(id);
            LogUtils.getLogger().warn("Cannot render Abyss enemy preview {}", (Object)id, (Object)exception);
        }
        finally {
            dispatcher.m_252923_(previousCamera);
            dispatcher.m_114468_(true);
            graphics.m_280168_().m_85849_();
            graphics.m_280618_();
            Lighting.m_84931_();
        }
        if (this.unavailable.contains(id)) {
            this.fallback(graphics, x, y, width, height);
        }
    }

    private void fallback(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.m_280653_(Minecraft.m_91087_().f_91062_, (Component)Component.m_237113_((String)"?"), x + width / 2, y + height / 2, -2561542);
    }
}

