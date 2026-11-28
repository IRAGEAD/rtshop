/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.model.PlayerModel
 *  net.minecraft.client.player.AbstractClientPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.LivingEntityRenderer
 *  net.minecraft.client.renderer.entity.RenderLayerParent
 *  net.minecraft.client.renderer.entity.layers.RenderLayer
 *  net.minecraft.client.renderer.entity.player.PlayerRenderer
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraftforge.client.event.EntityRenderersEvent$AddLayers
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.CampfireArmor;
import dev.dyslexic.cinematic.client.ClientPayloads;
import dev.dyslexic.cinematic.client.LegMesh;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.EntityRenderersEvent;

public final class KneeLayer
extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final float THIGH = -2.1f;
    public static final float KNEE = 1.5f;

    public KneeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static void addTo(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = (PlayerRenderer)event.getSkin(skin);
            if (renderer == null) continue;
            renderer.m_115326_((RenderLayer)new KneeLayer((RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>)renderer));
        }
    }

    public static boolean sitting(AbstractClientPlayer player) {
        return ClientPayloads.atStation(player.m_19879_()) == StationKind.CAMPFIRE;
    }

    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!KneeLayer.sitting(player)) {
            return;
        }
        pose.m_85836_();
        ((PlayerModel)this.m_117386_()).f_102813_.m_104299_(pose);
        if (!player.m_20145_()) {
            LegMesh.drawOnModel(pose, buffers, player, true, 1.5f, light, LivingEntityRenderer.m_115338_((LivingEntity)player, (float)0.0f));
        }
        CampfireArmor.drawOnModel(pose, buffers, player, 1.5f, light);
        pose.m_85849_();
    }
}

