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
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.HumanoidArm
 *  net.minecraftforge.client.event.EntityRenderersEvent$AddLayers
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.AnvilStation;
import dev.dyslexic.cinematic.client.ClientMenus;
import dev.dyslexic.cinematic.client.ClientPayloads;
import dev.dyslexic.cinematic.client.HammerModel;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.client.event.EntityRenderersEvent;

public final class PropLayer
extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public PropLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static double hammer(Entity entity) {
        return ClientPayloads.atStation(entity.m_19879_()) == StationKind.ANVIL ? AnvilStation.hammerShown(ClientMenus.sinceMove(entity.m_19879_())) : 0.0;
    }

    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        double shown = PropLayer.hammer((Entity)player);
        if (shown <= 0.001) {
            return;
        }
        pose.m_85836_();
        ((PlayerModel)this.m_117386_()).m_6002_(HumanoidArm.RIGHT, pose);
        HammerModel.renderInArm(pose, buffers, shown, light);
        pose.m_85849_();
    }

    public static void addTo(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            LivingEntityRenderer livingEntityRenderer = event.getSkin(skin);
            if (!(livingEntityRenderer instanceof PlayerRenderer)) continue;
            PlayerRenderer renderer = (PlayerRenderer)livingEntityRenderer;
            renderer.m_115326_((RenderLayer)new PropLayer((RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>)renderer));
        }
    }
}

