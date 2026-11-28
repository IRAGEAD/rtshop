/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDClientHelpers
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.AnimationContext
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
 *  com.mojang.blaze3d.shaders.FogShape
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferBuilder$RenderedBuffer
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.math.Axis
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.Options
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Style
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.InputEvent$Key
 *  net.minecraftforge.client.event.InputEvent$MouseButton$Post
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.client.event.ViewportEvent$ComputeFogColor
 *  net.minecraftforge.client.event.ViewportEvent$RenderFog
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.event.entity.player.ItemTooltipEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  org.joml.Matrix4f
 */
package com.finderfeed.fdbosses;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.data_components.ItemCoreDataComponent;
import com.finderfeed.fdbosses.content.entities.geburah.sins.HandleButtonPressSinPacket;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSins;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_boss_spawner.MalkuthBossSpawner;
import com.finderfeed.fdbosses.content.items.CoreItem;
import com.finderfeed.fdbosses.content.items.WeaponCoreItem;
import com.finderfeed.fdbosses.content.items.geburah.DivineGearItem;
import com.finderfeed.fdbosses.content.util.GainLoseValue;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossEffects;
import com.finderfeed.fdbosses.init.BossItems;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdbosses.init.GeburahSins;
import com.finderfeed.fdlib.FDClientHelpers;
import com.finderfeed.fdlib.data_structures.Pair;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.AnimationContext;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.FORGE, value={Dist.CLIENT})
public class BossClientEvents {
    public static Double cachedGamma = null;
    public static int chesedGazeEffectTick = 0;
    public static int chesedGazeEffectTickO = 0;
    public static int chesedGazeEffectTickMax = 20;
    public static int chesedDarkenEffectTick = 0;
    public static int chesedDarkenEffectTickO = 0;
    public static int chesedDarkenEffectTickMax = 10;
    private static GainLoseValue hellscapeSkyValue = new GainLoseValue(0, 100);
    private static final List<Integer> ALLOWED_TO_USE_BUTTONS = List.of(Integer.valueOf(256));
    public static boolean hasPlayerJumpedUnderSinEffect = false;
    private static FDModel DIVINE_GEAR_MODEL;
    private static float divineGearAnimTime;
    private static FDModel SWORD_MODEL;
    public static final ResourceLocation MALKUTH_FIRE_SWORD;
    private static final ResourceLocation MALKUTH_COWARD_EXECUTION_SQUARE;
    public static final ResourceLocation HELL_FROST_LIGHT;
    public static final ResourceLocation HELL_FIRE_LIGHT;

    @SubscribeEvent
    public static void handleButtonPressSin(InputEvent.Key event) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (Minecraft.m_91087_().m_91104_()) {
            return;
        }
        if (player != null && level != null && event.getAction() == 1 && !ALLOWED_TO_USE_BUTTONS.contains(event.getKey()) && !player.m_21224_()) {
            FDPacketHandler.INSTANCE.sendToServer((Object)new HandleButtonPressSinPacket());
        }
    }

    @SubscribeEvent
    public static void handleButtonPressSinMouse(InputEvent.MouseButton.Post event) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (Minecraft.m_91087_().m_91104_()) {
            return;
        }
        if (player != null && level != null && event.getAction() == 1 && !player.m_21224_()) {
            FDPacketHandler.INSTANCE.sendToServer((Object)new HandleButtonPressSinPacket());
        }
    }

    @SubscribeEvent
    public static void postClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null || !BossUtil.isPlayerInSurvival((Player)player)) {
            hasPlayerJumpedUnderSinEffect = false;
            return;
        }
        PlayerSins playerSins = PlayerSins.getPlayerSins((Player)player);
        if (playerSins.hasSinActive(GeburahSins.PRESSED_TOO_MANY_BUTTONS_SIN.get())) {
            KeyMapping key = Minecraft.m_91087_().f_91066_.f_92089_;
            if (key.m_90857_()) {
                if (player.f_108618_.f_108572_) {
                    hasPlayerJumpedUnderSinEffect = true;
                    player.m_6862_(false);
                }
            } else {
                hasPlayerJumpedUnderSinEffect = false;
            }
        } else {
            hasPlayerJumpedUnderSinEffect = false;
        }
    }

    @SubscribeEvent
    public static void collectTooltips(ItemTooltipEvent event) {
        List componentList = event.getToolTip();
        ItemStack itemStack = event.getItemStack();
        ItemCoreDataComponent.CoreType coreType = WeaponCoreItem.getItemCore(itemStack);
        if (coreType != null) {
            CoreItem item = coreType.getItem();
            componentList.add(item.m_7968_().m_41786_().m_6881_().m_130948_(Style.f_131099_.m_178520_(coreType.getTextColor())));
        }
    }

    @SubscribeEvent
    public static void tickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player != null) {
            BossClientEvents.tickChesedGaze((Player)player);
            BossClientEvents.tickChesedDarken((Player)player);
            BossClientEvents.lowerGammaWhenAnyDarkenEffect((Player)player);
            if (!Minecraft.m_91087_().m_91104_()) {
                divineGearAnimTime += 1.0f;
            }
        } else {
            divineGearAnimTime = 0.0f;
        }
        BossClientEvents.tickHellscapeSky();
    }

    private static void lowerGammaWhenAnyDarkenEffect(Player player) {
        Options options = Minecraft.m_91087_().f_91066_;
        if (player.m_21023_(MobEffects.f_19611_) && player.m_21023_((MobEffect)BossEffects.CHESED_DARKEN.get())) {
            if (cachedGamma == null) {
                cachedGamma = (Double)options.m_231927_().m_231551_();
            }
            options.m_231927_().m_231514_((Object)0.0);
        } else if (cachedGamma != null) {
            options.m_231927_().m_231514_((Object)cachedGamma);
            cachedGamma = null;
        }
    }

    private static void tickChesedGaze(Player player) {
        if (player != null) {
            chesedGazeEffectTickO = chesedGazeEffectTick;
            chesedGazeEffectTick = player.m_21023_((MobEffect)BossEffects.CHESED_GAZE.get()) ? Mth.m_14045_((int)(chesedGazeEffectTick + 1), (int)0, (int)chesedGazeEffectTickMax) : Mth.m_14045_((int)(chesedGazeEffectTick - 1), (int)0, (int)chesedGazeEffectTickMax);
        } else {
            chesedGazeEffectTick = 0;
        }
    }

    private static void tickChesedDarken(Player player) {
        if (player != null) {
            chesedDarkenEffectTickO = chesedDarkenEffectTick;
            chesedDarkenEffectTick = player.m_21023_((MobEffect)BossEffects.CHESED_DARKEN.get()) ? Mth.m_14045_((int)(chesedDarkenEffectTick + 1), (int)0, (int)chesedDarkenEffectTickMax) : Mth.m_14045_((int)(chesedDarkenEffectTick - chesedDarkenEffectTickMax / 4), (int)0, (int)chesedDarkenEffectTickMax);
        } else {
            chesedDarkenEffectTick = 0;
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (chesedGazeEffectTick != 0) {
            chesedGazeEffectTickMax = 20;
            float p = BossClientEvents.getChesedGazePercent(event.getPartialTick());
            float r = event.getRed();
            float g = event.getGreen();
            float b = event.getBlue();
            event.setRed(r * (1.0f - p));
            event.setGreen(g * (1.0f - p));
            event.setBlue(b * (1.0f - p));
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level == null) {
            return;
        }
        if (chesedGazeEffectTick != 0) {
            event.setCanceled(true);
            float levelTime = (float)(((double)level.m_46467_() + event.getPartialTick()) % 120.0);
            float p = BossClientEvents.getChesedGazePercent(event.getPartialTick());
            float farPlane = event.getFarPlaneDistance();
            float nearPlane = event.getNearPlaneDistance();
            event.setFogShape(FogShape.SPHERE);
            float farPlaneDistance = farPlane * (1.0f - p) + p * (2.0f * ((float)Math.sin((levelTime /= 120.0f) * (float)Math.PI * 2.0f) + 1.0f) / 2.0f + 3.0f);
            float nearPlaneDistance = nearPlane * (1.0f - p);
            event.setFarPlaneDistance(farPlaneDistance);
            event.setNearPlaneDistance(nearPlaneDistance);
        }
    }

    public static float getChesedGazePercent(double pticks) {
        float time = (float)Mth.m_14139_((double)pticks, (double)chesedGazeEffectTickO, (double)chesedGazeEffectTick);
        float p = FDEasings.easeOut((float)(time / (float)chesedGazeEffectTickMax));
        return p;
    }

    public static float getChesedDarkenPercent(double pticks) {
        float time = (float)Mth.m_14139_((double)pticks, (double)chesedDarkenEffectTickO, (double)chesedDarkenEffectTick);
        float p = FDEasings.easeOut((float)(time / (float)chesedDarkenEffectTickMax));
        return p;
    }

    @SubscribeEvent
    public static void renderLevelStageEvent(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            BossClientEvents.renderHellscapeSkybox(event);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            BossClientEvents.renderMalkuthCowardExecution(event, event.getPartialTick());
            BossClientEvents.renderDivineGearPlacement(event);
        }
    }

    private static void renderDivineGearPlacement(RenderLevelStageEvent event) {
        Player player = FDClientHelpers.getClientPlayer();
        if (player == null) {
            return;
        }
        ItemStack stack = player.m_21205_();
        if (!stack.m_150930_(BossItems.DIVINE_GEAR.get()) || player.m_36335_().m_41519_(BossItems.DIVINE_GEAR.get())) {
            return;
        }
        HitResult hitResult = Minecraft.m_91087_().f_91077_;
        if (hitResult.m_6662_() != HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult blockHitResult = (BlockHitResult)hitResult;
        BlockPos blockPos = blockHitResult.m_82425_();
        Direction direction = blockHitResult.m_82434_();
        Pair<BlockPos, Direction> posAndDir = DivineGearItem.getPosAndDirection((Level)Minecraft.m_91087_().f_91073_, blockPos, direction);
        blockPos = (BlockPos)posAndDir.first;
        direction = (Direction)posAndDir.second;
        if (DIVINE_GEAR_MODEL == null) {
            DIVINE_GEAR_MODEL = new FDModel(BossModels.DIVINE_GEAR.get());
        }
        if (!DivineGearItem.canPlaceOn((Level)Minecraft.m_91087_().f_91073_, blockPos, direction)) {
            return;
        }
        int charges = DivineGearItem.getCharge(stack);
        if (charges == 0) {
            return;
        }
        Vec3 worldRenderPos = DivineGearItem.getSpawnPlace(blockPos, direction);
        Vec3 cameraPos = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
        Vec3 offset = worldRenderPos.m_82546_(cameraPos);
        PoseStack matrices = event.getPoseStack();
        matrices.m_85836_();
        matrices.m_85837_(offset.f_82479_, offset.f_82480_, offset.f_82481_);
        ResourceLocation location = DivineGearItem.DIVINE_GEAR;
        DIVINE_GEAR_MODEL.resetTransformations();
        float time = divineGearAnimTime + event.getPartialTick();
        Animation anim = (Animation)BossAnims.DIVINE_GEAR_ITEM_IDLE.get();
        anim.applyAnimation(new AnimationContext(), DIVINE_GEAR_MODEL, time % (float)anim.getAnimTime());
        BufferBuilder vertex = Tesselator.m_85913_().m_85915_();
        vertex.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85818_);
        RenderSystem.setShader(GameRenderer::m_172814_);
        FDRenderUtil.bindTexture((ResourceLocation)location);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        DIVINE_GEAR_MODEL.render(matrices, (VertexConsumer)vertex, 0xF000F0, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, 0.25f);
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)vertex.m_231175_());
        matrices.m_85849_();
        RenderSystem.disableDepthTest();
    }

    private static void renderMalkuthCowardExecution(RenderLevelStageEvent event, float pticks) {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null || player.m_7500_() || player.m_5833_()) {
            return;
        }
        Vec3 forward = player.m_20156_().m_82542_(1.0, 0.0, 1.0).m_82541_();
        Vec3 camPos = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
        Vec3 offsetPos = player.m_20318_(pticks);
        PoseStack matrices = event.getPoseStack();
        Vec3 offset = offsetPos.m_82546_(camPos);
        if (!player.m_21023_((MobEffect)BossEffects.MARK_OF_A_COWARD.get())) {
            return;
        }
        MobEffectInstance instance = player.m_21124_((MobEffect)BossEffects.MARK_OF_A_COWARD.get());
        int duration = instance.m_19557_();
        int maxtime = 60;
        int maxtime2 = 7;
        float time = Mth.m_14036_((float)((float)duration - pticks), (float)0.0f, (float)maxtime);
        float time2 = Mth.m_14036_((float)((float)duration - pticks), (float)0.0f, (float)maxtime2);
        float p = time / (float)maxtime;
        float p2 = time2 / (float)maxtime2;
        if (p == 0.0f) {
            return;
        }
        float easeOut = FDEasings.easeIn((float)(1.0f - p));
        float easeInP2 = FDEasings.easeOutBack((float)p2);
        BufferBuilder builder = Tesselator.m_85913_().m_85915_();
        builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85818_);
        FDRenderUtil.bindTexture((ResourceLocation)MALKUTH_COWARD_EXECUTION_SQUARE);
        RenderSystem.setShader(GameRenderer::m_172814_);
        matrices.m_85836_();
        matrices.m_85837_(offset.f_82479_, offset.f_82480_ + 0.001, offset.f_82481_);
        matrices.m_252781_(Axis.f_252436_.m_252977_(easeOut * 360.0f * 6.0f));
        QuadRenderer.start((VertexConsumer)builder).pose(matrices).size((1.0f - p) * 2.0f).color(1.0f, 1.0f, 1.0f, 1.0f - p).direction(new Vec3(0.0, 1.0, 0.0)).renderBack().render();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)builder.m_231175_());
        matrices.m_85849_();
        RenderSystem.disableDepthTest();
        if (SWORD_MODEL == null) {
            SWORD_MODEL = new FDModel(BossModels.MALKUTH_SWORD.get());
        }
        builder = Tesselator.m_85913_().m_85915_();
        builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85818_);
        FDRenderUtil.bindTexture((ResourceLocation)MALKUTH_FIRE_SWORD);
        RenderSystem.setShader(GameRenderer::m_172814_);
        matrices.m_85836_();
        float offsetP = -3.0f * (1.0f - easeInP2);
        matrices.m_85837_(offset.f_82479_ + forward.f_82479_ * 0.1, offset.f_82480_ + 5.0 + (double)offsetP, offset.f_82481_ + forward.f_82481_ * 0.1);
        matrices.m_252781_(Axis.f_252403_.m_252977_(180.0f));
        matrices.m_252781_(Axis.f_252436_.m_252977_(easeOut * 360.0f * 6.0f));
        SWORD_MODEL.render(matrices, (VertexConsumer)builder, 0xF000F0, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, 1.0f - p);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)builder.m_231175_());
        matrices.m_85849_();
        RenderSystem.disableDepthTest();
    }

    private static void tickHellscapeSky() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null) {
            hellscapeSkyValue.reset();
            return;
        }
        float rad = 30.0f;
        AABB box = new AABB((double)(-rad), (double)(-rad), (double)(-rad), (double)rad, (double)rad, (double)rad).m_82383_(player.m_20182_());
        Level level = player.m_9236_();
        long time = level.m_46468_() % 24000L;
        if (time < 13000L || time > 23000L) {
            hellscapeSkyValue.backward();
            return;
        }
        List spawners = level.m_6443_(MalkuthBossSpawner.class, box, arg_0 -> BossClientEvents.lambda$tickHellscapeSky$0((Player)player, rad, arg_0));
        if (spawners.isEmpty()) {
            hellscapeSkyValue.backward();
        } else {
            hellscapeSkyValue.forward();
        }
    }

    private static void renderHellscapeSkybox(RenderLevelStageEvent event) {
        float alpha = hellscapeSkyValue.getPercent(event.getPartialTick());
        if (alpha == 0.0f) {
            return;
        }
        PoseStack matrices = event.getPoseStack();
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        BufferBuilder builder = Tesselator.m_85913_().m_85915_();
        builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85819_);
        float time = (float)level.m_46467_() + event.getPartialTick();
        float flash = (float)(Math.sin(time * 0.025f) + 1.0) / 2.0f;
        flash = flash * 0.8f + 0.2f;
        float flashCounter = (float)(Math.sin(time * 0.025f + (float)Math.PI) + 1.0) / 2.0f;
        flashCounter = flashCounter * 0.8f + 0.2f;
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShader(GameRenderer::m_172820_);
        RenderSystem.blendFunc((int)770, (int)1);
        FDRenderUtil.bindTexture((ResourceLocation)HELL_FIRE_LIGHT);
        matrices.m_85836_();
        Matrix4f mat = RenderSystem.getModelViewMatrix();
        matrices.m_252931_(mat);
        matrices.m_85836_();
        matrices.m_252781_(Axis.f_252403_.m_252977_(-90.0f));
        matrices.m_252781_(Axis.f_252529_.m_252977_(time * 0.015f));
        matrices.m_252781_(Axis.f_252436_.m_252977_(20.0f));
        BossClientEvents.renderSkybox(matrices, (VertexConsumer)builder, 300.0f, 1.0f, 1.0f, 1.0f, 0.5f * alpha);
        BossClientEvents.renderSkybox(matrices, (VertexConsumer)builder, 300.0f, 1.0f, 1.0f, 1.0f, flash * alpha);
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)builder.m_231175_());
        matrices.m_85849_();
        matrices.m_85836_();
        builder = Tesselator.m_85913_().m_85915_();
        builder.m_166779_(VertexFormat.Mode.QUADS, DefaultVertexFormat.f_85819_);
        FDRenderUtil.bindTexture((ResourceLocation)HELL_FROST_LIGHT);
        matrices.m_252781_(Axis.f_252529_.m_252977_(180.0f));
        matrices.m_252781_(Axis.f_252436_.m_252977_(time * 0.015f + 90.0f));
        matrices.m_252781_(Axis.f_252403_.m_252977_(20.0f));
        BossClientEvents.renderSkybox(matrices, (VertexConsumer)builder, 299.0f, 1.0f, 1.0f, 1.0f, 0.5f * alpha);
        BossClientEvents.renderSkybox(matrices, (VertexConsumer)builder, 299.0f, 1.0f, 1.0f, 1.0f, flashCounter * 0.8f * alpha);
        BufferUploader.m_231202_((BufferBuilder.RenderedBuffer)builder.m_231175_());
        matrices.m_85849_();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        matrices.m_85849_();
    }

    private static void renderSkybox(PoseStack matrices, VertexConsumer vertexConsumer, float boxRadius, float r, float g, float b, float a) {
        Matrix4f m = matrices.m_85850_().m_252922_();
        vertexConsumer.m_252986_(m, -boxRadius, -boxRadius, -boxRadius).m_7421_(0.0f, 0.0f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, boxRadius, -boxRadius, -boxRadius).m_7421_(0.5f, 0.0f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, boxRadius, boxRadius, -boxRadius).m_7421_(0.5f, 0.25f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, -boxRadius, boxRadius, -boxRadius).m_7421_(0.0f, 0.25f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, -boxRadius, boxRadius, boxRadius).m_7421_(0.5f, 0.5f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, boxRadius, boxRadius, boxRadius).m_7421_(0.0f, 0.5f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, boxRadius, -boxRadius, boxRadius).m_7421_(0.0f, 0.25f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, -boxRadius, -boxRadius, boxRadius).m_7421_(0.5f, 0.25f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, boxRadius, -boxRadius, -boxRadius).m_7421_(0.5f, 0.25f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, boxRadius, -boxRadius, boxRadius).m_7421_(1.0f, 0.25f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, boxRadius, boxRadius, boxRadius).m_7421_(1.0f, 0.5f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, boxRadius, boxRadius, -boxRadius).m_7421_(0.5f, 0.5f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, -boxRadius, boxRadius, -boxRadius).m_7421_(0.5f, 0.75f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, -boxRadius, boxRadius, boxRadius).m_7421_(0.0f, 0.75f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, -boxRadius, -boxRadius, boxRadius).m_7421_(0.0f, 0.5f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, -boxRadius, -boxRadius, -boxRadius).m_7421_(0.5f, 0.5f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, boxRadius, boxRadius, -boxRadius).m_7421_(0.0f, 1.0f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, boxRadius, boxRadius, boxRadius).m_7421_(0.0f, 0.75f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, -boxRadius, boxRadius, boxRadius).m_7421_(0.5f, 0.75f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, -boxRadius, boxRadius, -boxRadius).m_7421_(0.5f, 1.0f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, -boxRadius, -boxRadius, -boxRadius).m_7421_(1.0f, 0.5f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, -boxRadius, -boxRadius, boxRadius).m_7421_(1.0f, 0.75f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, boxRadius, -boxRadius, boxRadius).m_7421_(0.5f, 0.75f).m_85950_(r, g, b, a).m_5752_();
        vertexConsumer.m_252986_(m, boxRadius, -boxRadius, -boxRadius).m_7421_(0.5f, 0.5f).m_85950_(r, g, b, a).m_5752_();
    }

    private static /* synthetic */ boolean lambda$tickHellscapeSky$0(Player player, float rad, MalkuthBossSpawner e) {
        return e.m_20182_().m_82554_(player.m_20182_()) <= (double)rad;
    }

    static {
        divineGearAnimTime = 0.0f;
        MALKUTH_FIRE_SWORD = FDBosses.location("textures/item/malkuth_sword_fire.png");
        MALKUTH_COWARD_EXECUTION_SQUARE = FDBosses.location("textures/util/malkuth_cowardice_prepare.png");
        HELL_FROST_LIGHT = FDBosses.location("textures/skyboxes/hellscape/hell_frost_light.png");
        HELL_FIRE_LIGHT = FDBosses.location("textures/skyboxes/hellscape/hell_light.png");
    }
}

