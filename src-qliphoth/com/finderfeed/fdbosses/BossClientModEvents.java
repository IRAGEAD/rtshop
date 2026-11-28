/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.BoneTransformationController
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.HeadBoneTransformation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDEntityRenderLayerOptions
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDEntityRendererBuilder
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDEntityTransformation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.tile.renderer.FDBlockEntityRendererBuilder
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.tile.renderer.FDBlockEntityTransformation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.tile.renderer.FDBlockRenderLayerOptions
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_parser.TextBlockProcessor
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_parser.TextBlockProcessors
 *  com.finderfeed.fdlib.systems.trails.FDTrailRenderer
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.client.NullEntityRenderer
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.particle.ParticleProvider
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderers
 *  net.minecraft.client.renderer.entity.EntityRendererProvider
 *  net.minecraft.client.renderer.entity.ThrownItemRenderer
 *  net.minecraft.client.renderer.item.ItemProperties
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.EntityRenderersEvent$RegisterRenderers
 *  net.minecraftforge.client.event.RegisterGuiOverlaysEvent
 *  net.minecraftforge.client.event.RegisterParticleProvidersEvent
 *  net.minecraftforge.client.gui.overlay.IGuiOverlay
 *  net.minecraftforge.client.gui.overlay.VanillaGuiOverlay
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
 *  org.joml.Math
 *  org.joml.Random
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.BossParticles;
import com.finderfeed.fdbosses.client.boss_screen.text_block_processors.BossConfigFloatValueProcessor;
import com.finderfeed.fdbosses.client.boss_screen.text_block_processors.MobEffectTextProcessor;
import com.finderfeed.fdbosses.client.overlay.ElectrifiedOverlay;
import com.finderfeed.fdbosses.client.overlay.GeburahSinsOverlay;
import com.finderfeed.fdbosses.client.overlay.MalkuthWeaknessOverlay;
import com.finderfeed.fdbosses.client.particles.FlameWithStoneParticle;
import com.finderfeed.fdbosses.client.particles.IceChunkParticle;
import com.finderfeed.fdbosses.client.particles.arc_lightning.ArcLightningParticle;
import com.finderfeed.fdbosses.client.particles.arc_preparation_particle.ArcAttackPreparationParticle;
import com.finderfeed.fdbosses.client.particles.chesed_attack_ray.ChesedAttackRayParticle;
import com.finderfeed.fdbosses.client.particles.colored_jumping_particles.ColoredJumpingParticle;
import com.finderfeed.fdbosses.client.particles.malkuth_slash.MalkuthHorizontalSlashParticle;
import com.finderfeed.fdbosses.client.particles.rush_particle.RushParticle;
import com.finderfeed.fdbosses.client.particles.smoke_particle.BigSmokeParticle;
import com.finderfeed.fdbosses.client.particles.sonic_particle.SonicParticle;
import com.finderfeed.fdbosses.client.particles.square_preparation_particle.RectanglePreparationParticle;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticle;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedRenderer;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_crystal.ChesedCrystalEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_mini_ray.ChesedMiniRayRenderer;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_one_shot_vertical_ray.ChesedOneShotVerticalRayRenderer;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_vertical_ray.ChesedVerticalRayAttackRenderer;
import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterRenderer;
import com.finderfeed.fdbosses.content.entities.chesed_boss.falling_block.ChesedFallingBlockRenderer;
import com.finderfeed.fdbosses.content.entities.chesed_boss.flying_block_entity.FlyingBlockEntityRenderer;
import com.finderfeed.fdbosses.content.entities.chesed_boss.kinetic_field.ChesedKineticFieldRenderer;
import com.finderfeed.fdbosses.content.entities.chesed_boss.radial_earthquake.RadialEarthquakeRenderer;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ray_reflector.RayReflectorRenderer;
import com.finderfeed.fdbosses.content.entities.chesed_sword_buff.FlyingSwordRenderer;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahRenderer;
import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahCastingCircleRenderer;
import com.finderfeed.fdbosses.content.entities.geburah.chain_trap.GeburahChainTrapRenderer;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_bell.GeburahBellRenderer;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_earthquake.GeburahEarthquakeRenderer;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_opening_floor.GeburahOpeningFloorRenderer;
import com.finderfeed.fdbosses.content.entities.geburah.judgement_ball_projectile.JudgementBallExplosionParticle;
import com.finderfeed.fdbosses.content.entities.geburah.justice_hammer.JusticeHammerAttack;
import com.finderfeed.fdbosses.content.entities.geburah.justice_hammer.JusticeHammerAttackRenderer;
import com.finderfeed.fdbosses.content.entities.geburah.particles.geburah_ray.GeburahRayParticle;
import com.finderfeed.fdbosses.content.entities.geburah.particles.geburah_ray.GeburahRayStrikeDecalParticle;
import com.finderfeed.fdbosses.content.entities.geburah.respawn_point_setter_block.GeburahRespiteBlockEntityRenderer;
import com.finderfeed.fdbosses.content.entities.geburah.rotating_weapons.GeburahRotatingWeaponsBoneController;
import com.finderfeed.fdbosses.content.entities.geburah.scales_controller.GeburahScalesBoneController;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_boulder.MalkuthBoulderRenderer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon.MalkuthCannonProjectileRenderer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_cannon.MalkuthCannonRenderer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_chain.MalkuthChainRenderer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_crush.MalkuthCrushAttack;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_earthquake.MalkuthEarthquakeRenderer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_fireball.MalkuthFireballRenderer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_floor.MalkuthFloorRenderer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_giant_sword.MalkuthGiantSwordSlashRenderer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_repair_crystal.MalkuthRepairCrystalRenderer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_repair_crystal.MalkuthRepairEntityRenderer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_slash.MalkuthSlashRenderer;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_warrior.MalkuthWarriorEntity;
import com.finderfeed.fdbosses.content.items.chesed.PhaseSphereOverlay;
import com.finderfeed.fdbosses.content.items.geburah.DivineGearItem;
import com.finderfeed.fdbosses.content.items.malkuth.MalkuthFistChainRenderer;
import com.finderfeed.fdbosses.content.projectiles.renderers.BlockProjectileRenderer;
import com.finderfeed.fdbosses.content.projectiles.renderers.MalkuthPlayerFireIceBallRenderer;
import com.finderfeed.fdbosses.ik_2d.InverseKinematics2BoneTransform;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdbosses.init.BossItems;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdbosses.init.BossTileEntities;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.BoneTransformationController;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.head.HeadBoneTransformation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDEntityRenderLayerOptions;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDEntityRendererBuilder;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDEntityTransformation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.tile.renderer.FDBlockEntityRendererBuilder;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.tile.renderer.FDBlockEntityTransformation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.tile.renderer.FDBlockRenderLayerOptions;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_parser.TextBlockProcessor;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_parser.TextBlockProcessors;
import com.finderfeed.fdlib.systems.trails.FDTrailRenderer;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.client.NullEntityRenderer;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Random;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD, value={Dist.CLIENT}, modid="fdbosses")
public class BossClientModEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            TextBlockProcessors.register((ResourceLocation)FDBosses.location("effect"), (TextBlockProcessor)new MobEffectTextProcessor());
            TextBlockProcessors.register((ResourceLocation)FDBosses.location("config_float"), (TextBlockProcessor)new BossConfigFloatValueProcessor());
            ItemProperties.register((Item)BossItems.GEBURAH_EXPLOSIVE_CRYSTAL.get(), (ResourceLocation)FDBosses.location("exploding"), (stack, level, entity, id) -> {
                int k = stack.m_41773_() / 10 % 2;
                return k;
            });
            FDBlockEntityTransformation baseTransform = (trophy, matrices, pticks) -> matrices.m_252781_(Axis.f_252436_.m_252977_((float)trophy.getAngleFromState()));
            FDBlockEntityTransformation chesedTransform = (trophy, matrices, pticks) -> {
                matrices.m_85841_(0.3f, 0.3f, 0.3f);
                baseTransform.apply(trophy, matrices, pticks);
            };
            BlockEntityRenderers.m_173590_(BossTileEntities.CHESED_TROPHY.get(), (BlockEntityRendererProvider)FDBlockEntityRendererBuilder.builder().addLayer(FDBlockRenderLayerOptions.builder().model(BossModels.CHESED).renderType(RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/chesed.png"))).transformation(chesedTransform).build()).addLayer(FDBlockRenderLayerOptions.builder().model(BossModels.CHESED_CRYSTAL_LAYER).renderType((entity, pticks) -> RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/chesed_crystals.png"))).color((entity, pticks) -> new FDColor(1.0f, 1.0f, 1.0f, 1.0f)).transformation(chesedTransform).build()).build());
            FDBlockEntityTransformation malkuthTransform = (trophy, matrices, pticks) -> {
                matrices.m_85841_(0.3f, 0.3f, 0.3f);
                baseTransform.apply(trophy, matrices, pticks);
            };
            BlockEntityRenderers.m_173590_(BossTileEntities.MALKUTH_TROPHY.get(), (BlockEntityRendererProvider)FDBlockEntityRendererBuilder.builder().addLayer(FDBlockRenderLayerOptions.builder().model(BossModels.MALKUTH_SCREEN).renderType(RenderType.m_110473_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_screen.png"))).transformation(malkuthTransform).build()).addLayer(FDBlockRenderLayerOptions.builder().model(BossModels.MALKUTH_SCREEN).light(0xF000F0).renderType((entity, pticks) -> RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_screen_emissive.png"))).transformation(malkuthTransform).build()).build());
            BlockEntityRenderers.m_173590_(BossTileEntities.GEBURAH_TROPHY.get(), (BlockEntityRendererProvider)FDBlockEntityRendererBuilder.builder().addLayer(FDBlockRenderLayerOptions.builder().model(BossModels.GEBURAH).renderType(RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/geburah/geburah.png"))).transformation((geburahTrophyBlockEntity, poseStack, v) -> {
                baseTransform.apply(geburahTrophyBlockEntity, poseStack, v);
                poseStack.m_85841_(0.1f, 0.1f, 0.1f);
            }).build()).build());
        });
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "electrified", (IGuiOverlay)new ElectrifiedOverlay());
        event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "malkuth_weakness", (IGuiOverlay)new MalkuthWeaknessOverlay());
        event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "chesed_item_overlay", (IGuiOverlay)new PhaseSphereOverlay());
        event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "geburah_sin_overlay", (IGuiOverlay)new GeburahSinsOverlay());
    }

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpecial(BossParticles.RUSH_PARTICLE.get(), (ParticleProvider)new RushParticle.Factory());
        event.registerSpriteSet(BossParticles.BIS_SMOKE.get(), BigSmokeParticle.Factory::new);
        event.registerSpriteSet(BossParticles.SONIC_PARTICLE.get(), SonicParticle.Factory::new);
        event.registerSpriteSet(BossParticles.FLAME_WITH_STONE.get(), FlameWithStoneParticle.Factory::new);
        event.registerSpriteSet(BossParticles.ICE_CHUNK.get(), IceChunkParticle.Factory::new);
        event.registerSpecial(BossParticles.ARC_LIGHTNING.get(), (ParticleProvider)new ArcLightningParticle.Factory());
        event.registerSpecial(BossParticles.CHESED_RAY_ATTACK.get(), (ParticleProvider)new ChesedAttackRayParticle.Factory());
        event.registerSpecial(BossParticles.MALKUTH_HORIZONTAL_SLASH.get(), (ParticleProvider)new MalkuthHorizontalSlashParticle.Factory());
        event.registerSpecial(BossParticles.ARC_ATTACK_PREPARATION_PARTICLE.get(), (ParticleProvider)new ArcAttackPreparationParticle.Factory());
        event.registerSpecial(BossParticles.RECTANGLE_PREPARATION_PARTICLE.get(), (ParticleProvider)new RectanglePreparationParticle.Factory());
        event.registerSpecial(BossParticles.STRIPE_PARTICLE.get(), (ParticleProvider)new StripeParticle.Factory());
        event.registerSpecial(BossParticles.GEBURAH_RAY_ATTACK.get(), (ParticleProvider)new GeburahRayParticle.Factory());
        event.registerSpecial(BossParticles.JUDGEMENT_BALL_EXPLOSION.get(), (ParticleProvider)new JudgementBallExplosionParticle.Factory());
        event.registerSpecial(BossParticles.GEBURAH_RAY_DECAL.get(), (ParticleProvider)new GeburahRayStrikeDecalParticle.Factory());
        event.registerSpecial(BossParticles.COLORED_JUMPING_PARTICLE.get(), (ParticleProvider)new ColoredJumpingParticle.Factory());
    }

    @SubscribeEvent
    public static void addRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BossTileEntities.GEBURAH_RESPITE_BLOCK.get(), GeburahRespiteBlockEntityRenderer::new);
        event.registerEntityRenderer(BossEntities.GEBURAH_OPENING_FLOOR.get(), GeburahOpeningFloorRenderer::new);
        event.registerEntityRenderer(BossEntities.GEBURAH_CASTING_CIRCLE_SIN_CRYSTAL.get(), GeburahCastingCircleRenderer::new);
        event.registerEntityRenderer(BossEntities.GEBURAH_CASTING_CIRCLE_CHAIN_TRAP.get(), GeburahCastingCircleRenderer::new);
        event.registerEntityRenderer(BossEntities.GEBURAH_CASTING_CIRCLE_RAY.get(), GeburahCastingCircleRenderer::new);
        event.registerEntityRenderer(BossEntities.GEBURAH_CASTING_CIRCLE_JUDGEMENT_BIRD.get(), GeburahCastingCircleRenderer::new);
        event.registerEntityRenderer(BossEntities.DIVINE_GEAR.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.DIVINE_GEAR).renderType((divineGear, v) -> RenderType.m_110452_((ResourceLocation)DivineGearItem.DIVINE_GEAR)).light(0xF000F0).build()).build());
        FDEntityTransformation malkuthFistTransform = (entity, poseStack, pticks) -> FDRenderUtil.applyMovementMatrixRotations((PoseStack)poseStack, (Vec3)entity.cachedDeltaMovement);
        event.registerEntityRenderer(BossEntities.MALKUTH_FIST_CHAIN.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH_FIST_FLYING).renderType(RenderType.m_110458_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_fist_flying.png"))).transformation(malkuthFistTransform).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH_FIST_FLYING).renderType(RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_fist_flying_emissive.png"))).light(0xF000F0).transformation(malkuthFistTransform).build()).freeRender((FDFreeEntityRenderer)new MalkuthFistChainRenderer()).build());
        event.registerEntityRenderer(BossEntities.GEBURAH_BOSS_SPAWNER.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.JUSTITIA).renderType(RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/geburah/justitia.png"))).renderCondition(entity -> ((BossSpawnerEntity)((Object)entity)).isActive()).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.JUSTITIA).renderType(RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/entities/geburah/justitia_max_light.png"))).light(0xF000F0).renderCondition(entity -> ((BossSpawnerEntity)((Object)entity)).isActive()).build()).build());
        event.registerEntityRenderer(BossEntities.GEBURAH_BELL.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().ignoreHurtOverlay(true).model(BossModels.GEBURAH_BELL).renderType((geburahBell, v) -> RenderType.m_110470_((ResourceLocation)FDBosses.location("textures/entities/geburah/geburah_bell.png"))).color((geburahBell, v) -> {
            int appearDuration = 20;
            float alpha = !geburahBell.m_21224_() ? FDEasings.easeOut((float)Mth.m_14036_((float)(((float)geburahBell.f_19797_ + v) / (float)appearDuration), (float)0.0f, (float)1.0f)) : 1.0f - FDEasings.easeOut((float)Mth.m_14036_((float)(((float)geburahBell.f_20919_ + v) / (float)((Animation)BossAnims.GEBURAH_BELL_RING.get()).getAnimTime()), (float)0.0f, (float)1.0f));
            if (geburahBell.isRed()) {
                return new FDColor(1.0f, 0.3f, 0.1f, alpha * 0.75f);
            }
            return new FDColor(0.3f, 0.8f, 1.0f, alpha * 0.75f);
        }).transformation((geburahBell, matrices, pticks) -> {
            int appearDuration = 20;
            float t = Mth.m_14036_((float)(((float)geburahBell.f_19797_ + pticks) / (float)appearDuration), (float)0.0f, (float)1.0f);
            Random random = new Random((long)geburahBell.m_19879_() * 34L);
            float rndFly = (float)Math.sin(random.nextFloat() * (float)Math.PI * 2.0f + ((float)geburahBell.f_19797_ + pticks) / 10.0f) * 0.1f;
            matrices.m_85837_(0.0, (double)(1.0f - FDEasings.easeOut((float)t)) * 0.25 + (double)rndFly, 0.0);
        }).build()).freeRender((FDFreeEntityRenderer)new GeburahBellRenderer()).build());
        event.registerEntityRenderer(BossEntities.JUDGEMENT_BIRD.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.JUDGEMENT_BIRD).renderType(RenderType.m_110473_((ResourceLocation)FDBosses.location("textures/entities/geburah/judgement_bird.png"))).transformation((judgementBirdEntity, poseStack, v) -> poseStack.m_252880_(0.0f, judgementBirdEntity.m_20206_() / 2.0f, 0.0f)).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.JUDGEMENT_BIRD_LAYER).light(0xF000F0).renderType(RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/geburah/judgement_bird_emissive.png"))).transformation((judgementBirdEntity, poseStack, v) -> poseStack.m_252880_(0.0f, judgementBirdEntity.m_20206_() / 2.0f, 0.0f)).build()).build());
        event.registerEntityRenderer(BossEntities.GEBURAH_CHAIN_TRAP_SUMMON_PROJECTILE.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().renderType(RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/entities/geburah/crystal_of_sin_entity.png"))).model(BossModels.JUDGEMENT_BALL).light(0xF000F0).transformation((judgementBallProjectile, poseStack, v) -> {
            poseStack.m_252880_(0.0f, judgementBallProjectile.m_20206_() / 2.0f, 0.0f);
            float time = ((float)judgementBallProjectile.f_19797_ + v) * 4.0f + (float)judgementBallProjectile.m_19879_();
            poseStack.m_252781_(Axis.f_252436_.m_252977_(time));
            poseStack.m_252781_(Axis.f_252403_.m_252977_(time));
            poseStack.m_85841_(0.5f, 0.5f, 0.5f);
        }).build()).freeRender((judgementBallProjectile, v, v1, poseStack, multiBufferSource, i) -> {
            poseStack.m_85836_();
            poseStack.m_252880_(0.0f, judgementBallProjectile.m_20206_() / 2.0f, 0.0f);
            FDTrailRenderer.renderTrail((Object)judgementBallProjectile, judgementBallProjectile.trail, (VertexConsumer)multiBufferSource.m_6299_(RenderType.m_110502_()), (PoseStack)poseStack, (float)0.1f, (int)4, (int)10, (float)v1, (FDColor)new FDColor(0.5f, 0.7f, 0.9f, 0.0f), (FDColor)new FDColor(0.5f, 0.7f, 0.9f, 0.7f));
            poseStack.m_85849_();
        }).shouldRender((explosiveCrystal, frustum, v, v1, v2) -> frustum.m_113029_(explosiveCrystal.m_20191_().m_82400_(10.0))).build());
        event.registerEntityRenderer(BossEntities.GEBURAH_SIN_CRYSTAL.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().renderType(RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/entities/geburah/crystal_of_sin_entity.png"))).model(BossModels.JUDGEMENT_BALL).light(0xF000F0).transformation((judgementBallProjectile, poseStack, v) -> {
            poseStack.m_252880_(0.0f, judgementBallProjectile.m_20206_() / 2.0f, 0.0f);
            float time = ((float)judgementBallProjectile.f_19797_ + v) * 4.0f + (float)judgementBallProjectile.m_19879_();
            poseStack.m_252781_(Axis.f_252436_.m_252977_(time));
            poseStack.m_252781_(Axis.f_252403_.m_252977_(time));
            poseStack.m_85841_(0.5f, 0.5f, 0.5f);
        }).build()).addLayer(FDEntityRenderLayerOptions.builder().renderType(RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/entities/geburah/crystal_of_sin_entity_layer.png"))).model(BossModels.JUDGEMENT_BALL_LAYER).light(0xF000F0).transformation((judgementBallProjectile, poseStack, v) -> {
            poseStack.m_252880_(0.0f, judgementBallProjectile.m_20206_() / 2.0f, 0.0f);
            float time = ((float)judgementBallProjectile.f_19797_ + v) * 4.0f + (float)judgementBallProjectile.m_19879_();
            poseStack.m_252781_(Axis.f_252436_.m_252977_(time));
            poseStack.m_252781_(Axis.f_252403_.m_252977_(time));
            poseStack.m_85841_(0.5f, 0.5f, 0.5f);
        }).build()).freeRender((judgementBallProjectile, v, v1, poseStack, multiBufferSource, i) -> {
            poseStack.m_85836_();
            poseStack.m_252880_(0.0f, judgementBallProjectile.m_20206_() / 2.0f, 0.0f);
            FDTrailRenderer.renderTrail((Object)judgementBallProjectile, judgementBallProjectile.trail, (VertexConsumer)multiBufferSource.m_6299_(RenderType.m_110502_()), (PoseStack)poseStack, (float)0.1f, (int)4, (int)10, (float)v1, (FDColor)new FDColor(0.5f, 0.7f, 0.9f, 0.0f), (FDColor)new FDColor(0.5f, 0.7f, 0.9f, 0.7f));
            poseStack.m_85849_();
        }).shouldRender((explosiveCrystal, frustum, v, v1, v2) -> frustum.m_113029_(explosiveCrystal.m_20191_().m_82400_(10.0))).build());
        event.registerEntityRenderer(BossEntities.JUSTICE_HAMMER.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().renderType(RenderType.m_110502_()).model(BossModels.JUSTICE_HAMMER).transformation((justiceHammerAttack, poseStack, v) -> poseStack.m_252781_(Axis.f_252436_.m_252977_(90.0f))).color((justiceHammerAttack, v) -> {
            float alpha = JusticeHammerAttack.EASING.apply((float)justiceHammerAttack.f_19797_ + v);
            return new FDColor(0.3f, 0.7f, 1.0f, alpha * 0.75f);
        }).light(0xF000F0).build()).freeRender((FDFreeEntityRenderer)new JusticeHammerAttackRenderer()).shouldRender((justiceHammerAttack, frustum, v, v1, v2) -> frustum.m_113029_(justiceHammerAttack.m_20191_().m_82400_(30.0))).build());
        event.registerEntityRenderer(BossEntities.GEBURAH_JUDGEMENT_BALL.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().renderType(RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/entities/geburah/judgement_ball.png"))).model(BossModels.JUDGEMENT_BALL).light(0xF000F0).transformation((judgementBallProjectile, poseStack, v) -> {
            poseStack.m_252880_(0.0f, judgementBallProjectile.m_20206_() / 2.0f, 0.0f);
            FDRenderUtil.applyMovementMatrixRotations((PoseStack)poseStack, (Vec3)judgementBallProjectile.m_20184_());
        }).build()).addLayer(FDEntityRenderLayerOptions.builder().renderType((judgementBallProjectile, v) -> {
            float tick = ((float)judgementBallProjectile.f_19797_ + v) % 8.0f;
            return RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/entities/geburah/judgement_ball_" + (int)Math.floor(tick / 2.0f) + ".png"));
        }).model(BossModels.JUDGEMENT_BALL_LAYER).transformation((judgementBallProjectile, poseStack, v) -> {
            poseStack.m_252880_(0.0f, judgementBallProjectile.m_20206_() / 2.0f, 0.0f);
            FDRenderUtil.applyMovementMatrixRotations((PoseStack)poseStack, (Vec3)judgementBallProjectile.m_20184_());
        }).light(0xF000F0).build()).freeRender((judgementBallProjectile, v, v1, poseStack, multiBufferSource, i) -> {
            poseStack.m_85836_();
            poseStack.m_252880_(0.0f, judgementBallProjectile.m_20206_() / 2.0f, 0.0f);
            FDTrailRenderer.renderTrail((Object)judgementBallProjectile, judgementBallProjectile.trail, (VertexConsumer)multiBufferSource.m_6299_(RenderType.m_110502_()), (PoseStack)poseStack, (float)0.15f, (int)4, (int)10, (float)v1, (FDColor)new FDColor(0.5f, 0.7f, 0.9f, 0.0f), (FDColor)new FDColor(0.5f, 0.7f, 0.9f, 0.7f));
            poseStack.m_85849_();
        }).shouldRender((judgementBallProjectile, frustum, v, v1, v2) -> frustum.m_113029_(judgementBallProjectile.m_20191_().m_82400_(10.0))).build());
        event.registerEntityRenderer(BossEntities.GEBURAH_EARTHQUAKE.get(), GeburahEarthquakeRenderer::new);
        event.registerEntityRenderer(BossEntities.GEBURAH_CHAIN_TRAP.get(), GeburahChainTrapRenderer::new);
        event.registerEntityRenderer(BossEntities.GEBURAH.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.GEBURAH_LAYER).ignoreHurtOverlay(true).renderType((geburah, v) -> RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/misc/white_dot.png"))).transformation(BossClientModEvents.generateGeburahHurtTransformation(0)).color((geburah, v) -> {
            int tick = 25 - geburah.sinnedTicks;
            float alpha = 1.0f - FDEasings.easeIn((float)(((float)tick + v) / 25.0f));
            return new FDColor(1.0f, 0.8f, 0.3f, 0.3f * alpha);
        }).renderCondition(geburah -> geburah.sinnedTicks > 0).light(0xF000F0).addBoneController("rotating_weapons", (BoneTransformationController)new GeburahRotatingWeaponsBoneController()).addBoneController("scales_plates", (BoneTransformationController)new GeburahScalesBoneController()).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.GEBURAH_LAYER).ignoreHurtOverlay(true).renderType((geburah, v) -> RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/misc/white_dot.png"))).transformation(BossClientModEvents.generateGeburahHurtTransformation(6354)).color((geburah, v) -> {
            int tick = 25 - geburah.sinnedTicks;
            float alpha = 1.0f - FDEasings.easeIn((float)(((float)tick + v) / 25.0f));
            return new FDColor(0.3f, 0.8f, 1.0f, 0.2f * alpha);
        }).renderCondition(geburah -> geburah.sinnedTicks > 0).light(0xF000F0).addBoneController("rotating_weapons", (BoneTransformationController)new GeburahRotatingWeaponsBoneController()).addBoneController("scales_plates", (BoneTransformationController)new GeburahScalesBoneController()).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.GEBURAH).ignoreHurtOverlay(true).renderType((geburah, v) -> RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/geburah/geburah.png"))).addBoneController("rotating_weapons", (BoneTransformationController)new GeburahRotatingWeaponsBoneController()).addBoneController("scales_plates", (BoneTransformationController)new GeburahScalesBoneController()).build()).freeRender((FDFreeEntityRenderer)new GeburahRenderer()).shouldRender((geburahEntity, frustum, v, v1, v2) -> true).build());
        EntityRendererProvider warriorRenderer = FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH_WARRIOR).renderType(RenderType.m_110458_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_warrior.png"))).addBoneController("head", (BoneTransformationController)new HeadBoneTransformation()).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH_WARRIOR).renderType((entity, pticks) -> {
            if (((MalkuthAttackType)((Object)((Object)entity.m_20088_().m_135370_(MalkuthWarriorEntity.WARRIOR_TYPE)))).isFire()) {
                return RenderType.m_110473_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_warrior_fire.png"));
            }
            return RenderType.m_110473_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_warrior_ice.png"));
        }).light(0xF000F0).addBoneController("head", (BoneTransformationController)new HeadBoneTransformation()).build()).build();
        event.registerEntityRenderer(BossEntities.FIRE_MALKUTH_WARRIOR.get(), warriorRenderer);
        event.registerEntityRenderer(BossEntities.ICE_MALKUTH_WARRIOR.get(), warriorRenderer);
        event.registerEntityRenderer(BossEntities.MALKUTH_REPAIR_CRYSTAL.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH_REPAIR_CRYSTAL).renderType(RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_crystal_base.png"))).ignoreHurtOverlay(true).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH_REPAIR_CRYSTAL).renderType((malkuthRepairCrystal, v) -> malkuthRepairCrystal.getCrystalType().isFire() ? RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_crystal_fire.png")) : RenderType.m_110497_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_crystal_ice.png"))).ignoreHurtOverlay(true).light(0xF000F0).color((malkuthRepairCrystal, v) -> new FDColor(1.0f, 0.75f, 0.75f, 0.5f)).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH_REPAIR_CRYSTAL).renderType((malkuthRepairCrystal, v) -> malkuthRepairCrystal.getCrystalType().isFire() ? RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_crystal_fire.png")) : RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_crystal_ice.png"))).ignoreHurtOverlay(true).light(0xF000F0).color((malkuthRepairCrystal, v) -> new FDColor(1.0f, 1.0f, 1.0f, 1.0f)).build()).freeRender((FDFreeEntityRenderer)new MalkuthRepairCrystalRenderer()).build());
        FDEntityTransformation platformTransform = (platform, matrices, v) -> {
            float time = (float)platform.f_19797_ + v;
            Random random = new Random((long)platform.m_19879_() * 33L);
            int timeToRise = 20 + random.nextInt(10);
            float p = FDMathUtil.clamp((float)(((float)platform.f_19797_ + v) / (float)timeToRise), (float)0.0f, (float)1.0f);
            float platformFloat = FDEasings.easeIn((float)p) * org.joml.Math.sin((float)(time / 25.0f + random.nextFloat() * (float)Math.PI * 2.0f)) * 0.05f;
            float translation = -5.0f + 5.0f * FDEasings.easeOutBack((float)p) + platformFloat;
            float randomYRot = platform.m_19879_() % 4 * 90;
            matrices.m_252880_(0.0f, translation, 0.0f);
            matrices.m_252781_(Axis.f_252436_.m_252977_(randomYRot));
            int d = random.nextInt(2) == 1 ? -1 : 1;
            float someRot = (float)(-15 * d) + FDEasings.easeOutBack((float)p) * 15.0f * (float)d;
            matrices.m_252781_(Axis.f_252495_.m_252977_(someRot));
            matrices.m_252781_(Axis.f_252392_.m_252977_(someRot));
        };
        event.registerEntityRenderer(BossEntities.MALKUTH_PLATFORM.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH_PLATFORM).renderType(RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_platform.png"))).transformation(platformTransform).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH_PLATFORM).renderType(RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_platform_emissive.png"))).transformation(platformTransform).build()).shouldRender((malkuthPlatform, frustum, v, v1, v2) -> frustum.m_113029_(new AABB(-5.0, -5.0, -5.0, 5.0, 5.0, 5.0).m_82383_(malkuthPlatform.m_20182_()))).build());
        event.registerEntityRenderer(BossEntities.MALKUTH_BOSS_SPAWNER.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH_SPAWNER).transformation((malkuthBossSpawner, poseStack, v) -> poseStack.m_252781_(Axis.f_252436_.m_252977_(90.0f))).renderType(RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_boss_spawner_emissive.png"))).light(0xF000F0).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH_SPAWNER).transformation((malkuthBossSpawner, poseStack, v) -> poseStack.m_252781_(Axis.f_252436_.m_252977_(90.0f))).renderType(RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_boss_spawner_solid.png"))).build()).shouldRender((malkuthBossSpawner, frustum, v, v1, v2) -> malkuthBossSpawner.isActive()).build());
        event.registerEntityRenderer(BossEntities.MALKUTH_CANNON.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().renderType((entity, pticks) -> {
            if (entity.getCannonType().isFire()) {
                return RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_cannon.png"));
            }
            return RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_cannon_ice.png"));
        }).model(BossModels.MALKUTH_CANNON).ignoreHurtOverlay(true).build()).addLayer(FDEntityRenderLayerOptions.builder().renderType((entity, pticks) -> RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_cannon_emissive.png"))).renderCondition(m -> m.getCannonType().isFire()).model(BossModels.MALKUTH_CANNON).ignoreHurtOverlay(true).build()).freeRender((FDFreeEntityRenderer)new MalkuthCannonRenderer()).build());
        InverseKinematics2BoneTransform rightLegIK = new InverseKinematics2BoneTransform(Direction.Axis.Z, Direction.Axis.X, "leg_right_control_start", "leg_right_control_end", "leg_right", "leg_right_lower", "leg_right_boot", false);
        InverseKinematics2BoneTransform leftLegIK = new InverseKinematics2BoneTransform(Direction.Axis.Z, Direction.Axis.X, "leg_left_control_start", "leg_left_control_end", "leg_left", "leg_left_lower", "leg_left_boot", false);
        event.registerEntityRenderer(BossEntities.MALKUTH.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH).renderType(RenderType.m_110458_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_solid.png"))).addBoneController("head", (BoneTransformationController)new HeadBoneTransformation()).addBoneController("leg_right_holder", rightLegIK).addBoneController("leg_left_holder", leftLegIK).ignoreHurtOverlay(true).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.MALKUTH).renderType(RenderType.m_110473_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_emissive.png"))).addBoneController("head", (BoneTransformationController)new HeadBoneTransformation()).addBoneController("leg_right_holder", rightLegIK).addBoneController("leg_left_holder", leftLegIK).ignoreHurtOverlay(true).light(0xF000F0).build()).build());
        event.registerEntityRenderer(BossEntities.MALKUTH_CRUSH.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().transformation((malkuthCrushAttack, poseStack, v) -> {
            Vec3i nrm = ((Direction)malkuthCrushAttack.m_20088_().m_135370_(MalkuthCrushAttack.DIRECTION)).m_122436_();
            FDRenderUtil.applyMovementMatrixRotations((PoseStack)poseStack, (Vec3)new Vec3((double)nrm.m_123341_(), (double)nrm.m_123342_(), (double)nrm.m_123343_()));
        }).light(0xF000F0).model(BossModels.MALKUTH_CRUSH_ATTACK).renderType((malkuthCrushAttack, v) -> {
            if (malkuthCrushAttack.isBothFireAndIce()) {
                return RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_crash.png"));
            }
            if (malkuthCrushAttack.isFire()) {
                return RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_crush_fire.png"));
            }
            return RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/malkuth/malkuth_crush_ice.png"));
        }).build()).build());
        event.registerEntityRenderer(BossEntities.CHESED.get(), FDEntityRendererBuilder.builder().shouldRender((entity, frustum, x, y, z) -> true).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.CHESED).ignoreHurtOverlay(true).renderType(RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/chesed.png"))).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.CHESED_CRYSTAL_LAYER).ignoreHurtOverlay(true).renderType((entity, pticks) -> RenderType.m_110473_((ResourceLocation)FDBosses.location("textures/entities/chesed_crystals.png"))).color((entity, pticks) -> new FDColor(1.0f, 1.0f, 1.0f, 0.0f)).renderCondition(chesedEntity -> true).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.CHESED_CRYSTAL_LAYER).ignoreHurtOverlay(true).renderType((entity, pticks) -> RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/chesed_crystals.png"))).color((entity, pticks) -> new FDColor(1.0f, 1.0f, 1.0f, 1.0f)).renderCondition(chesedEntity -> true).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.CHESED_INFLATED).ignoreHurtOverlay(true).renderType((entity, pticks) -> RenderType.m_234338_((ResourceLocation)FDBosses.location("textures/entities/chesed_white_tex.png"))).color((entity, pticks) -> {
            ChesedEntity e = entity;
            float alpha = FDMathUtil.lerp((float)e.drainPercentOld, (float)e.getMonolithDrainPercent(), (float)pticks) * 0.3f;
            return new FDColor(0.1f, 1.0f, 1.0f, alpha);
        }).renderCondition(entity -> entity.getMonolithDrainPercent() != 0.0f).build()).freeRender((FDFreeEntityRenderer)new ChesedRenderer()).build());
        event.registerEntityRenderer(BossEntities.CHESED_ELECTRIC_SPHERE.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.CHESED_ELECTRIC_SPHERE).transformation((entity, matrices, pticks) -> {
            matrices.m_252880_(0.0f, 0.5f, 0.0f);
            float time = (float)entity.f_19797_ + pticks;
            float md = 16.0f;
            float scale = Mth.m_14036_((float)(time / 20.0f), (float)0.0f, (float)1.0f) * (org.joml.Math.sin((float)(time * 2.0f)) / md + (1.0f - 1.0f / md));
            matrices.m_85841_(scale, scale, scale);
        }).renderType(RenderType.m_110470_((ResourceLocation)FDBosses.location("textures/entities/electric_orb.png"))).build()).build());
        event.registerEntityRenderer(BossEntities.CHESED_CRYSTAL.get(), FDEntityRendererBuilder.builder().shouldRender((entity, frustum, x, y, z) -> true).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.CHESED_CRYSTAL).renderCondition(entity -> true).transformation((entity, stack, partialTicks) -> {
            FDRenderUtil.applyMovementMatrixRotations((PoseStack)stack, (Vec3)((ChesedCrystalEntity)entity).getCrystalFacingDirection());
            stack.m_252781_(FDRenderUtil.rotationDegrees((Vector3f)FDRenderUtil.YP(), (float)((float)entity.m_19879_() * 42.343f)));
            stack.m_85841_(3.2f, 3.0f, 3.2f);
        }).renderType(RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/chesed_crystal.png"))).build()).build());
        event.registerEntityRenderer(BossEntities.CHESED_MONOLITH.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.CHESED_MONOLITH).renderType(RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/chesed_monolith.png"))).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.CHESED_MONOLITH).renderType(RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/chesed_monolith_emissive.png"))).build()).build());
        event.registerEntityRenderer(BossEntities.CHESED_KINETIC_FIELD.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.CHESED_KINETIC_FIELD).renderType(RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/kinetic_field_spear.png"))).build()).shouldRender((entity, frustum, v, v1, v2) -> true).freeRender((x$0, x$1, x$2, x$3, x$4, x$5) -> ChesedKineticFieldRenderer.render(x$0, x$1, x$2, x$3, x$4, x$5)).build());
        event.registerEntityRenderer(BossEntities.CHESED_BOSS_SPAWNER.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.BOSS_SPAWNER).renderType(RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/boss_spawner.png"))).renderCondition(entity -> ((BossSpawnerEntity)((Object)entity)).isActive()).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.BOSS_SPAWNER).renderType(RenderType.m_110470_((ResourceLocation)FDBosses.location("textures/entities/boss_spawner_crystals.png"))).renderCondition(entity -> ((BossSpawnerEntity)((Object)entity)).isActive()).color((entity, v) -> new FDColor(1.0f, 1.0f, 1.0f, 0.25f)).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.BOSS_SPAWNER_CRYSTAL_LAYER).renderType(RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/boss_spawner_crystals.png"))).renderCondition(entity -> ((BossSpawnerEntity)((Object)entity)).isActive()).build()).build());
        event.registerEntityRenderer(BossEntities.CHESED_RAY_REFLECTOR.get(), FDEntityRendererBuilder.builder().addLayer(FDEntityRenderLayerOptions.builder().transformation((reflector, poseStack, v) -> poseStack.m_85841_(0.8f, 0.8f, 0.8f)).model(BossModels.CHESED_RAY_REFLECTOR).renderType((e, p) -> RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/chesed_ray_reflector.png"))).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.CHESED_RAY_REFLECTOR).transformation((reflector, poseStack, v) -> poseStack.m_85841_(0.8f, 0.8f, 0.8f)).renderType((e, p) -> RenderType.m_110473_((ResourceLocation)FDBosses.location("textures/entities/chesed_ray_reflector_crystals.png"))).color((entity, pticks) -> new FDColor(1.0f, 1.0f, 1.0f, 0.0f)).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.CHESED_RAY_REFLECTOR).transformation((reflector, poseStack, v) -> poseStack.m_85841_(0.8f, 0.8f, 0.8f)).renderType((e, p) -> RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/entities/chesed_ray_reflector_crystals.png"))).color((entity, pticks) -> {
            float animTime = ((Animation)BossAnims.RAY_REFLECTOR_ACTIVATE.get()).getAnimTime();
            float t = FDMathUtil.lerp((float)entity.activeTickerO, (float)entity.getActiveTicker(), (float)pticks);
            float p = t / animTime;
            float r = FDMathUtil.lerp((float)0.5f, (float)0.3f, (float)p);
            float g = FDMathUtil.lerp((float)0.5f, (float)0.95f, (float)p);
            float b = FDMathUtil.lerp((float)0.5f, (float)1.0f, (float)p);
            if (entity.isActivating()) {
                float offsetTime = t - animTime / 2.0f;
                float t2 = FDMathUtil.clamp((float)(offsetTime / 10.0f), (float)0.0f, (float)1.0f);
                float p2 = (double)t2 <= 0.25 ? 1.0f - (float)Math.pow(4.0f * t2 - 1.0f, 2.0) : 1.0f - (float)Math.pow(1.335 * ((double)t2 - 0.25), 2.0);
                r = FDMathUtil.lerp((float)r, (float)1.0f, (float)p2);
                g = FDMathUtil.lerp((float)g, (float)1.0f, (float)p2);
                b = FDMathUtil.lerp((float)b, (float)1.0f, (float)p2);
            }
            return new FDColor(r, g, b, 1.0f);
        }).build()).addLayer(FDEntityRenderLayerOptions.builder().model(BossModels.CHESED_RAY_REFLECTOR).renderCondition(entity -> {
            int ticksToReveal = 3;
            float animTime = ((Animation)BossAnims.RAY_REFLECTOR_ACTIVATE.get()).getAnimTime();
            return (float)entity.getActiveTicker() > animTime - (float)ticksToReveal;
        }).transformation((reflector, poseStack, v) -> poseStack.m_85841_(0.8f, 0.8f, 0.8f)).renderType((e, p) -> RenderType.m_110473_((ResourceLocation)FDBosses.location("textures/entities/chesed_ray_reflector_crosshair.png"))).color((entity, pticks) -> {
            int ticksToReveal = 3;
            float animTime = ((Animation)BossAnims.RAY_REFLECTOR_ACTIVATE.get()).getAnimTime();
            float t = Mth.m_14036_((float)((float)entity.getActiveTicker() + pticks - (animTime - (float)ticksToReveal)), (float)0.0f, (float)ticksToReveal);
            return new FDColor(0.3f, 0.95f, 1.0f, t / (float)ticksToReveal);
        }).build()).freeRender((FDFreeEntityRenderer)new RayReflectorRenderer()).build());
        event.registerEntityRenderer(BossEntities.EARTH_SHATTER.get(), EarthShatterRenderer::new);
        event.registerEntityRenderer(BossEntities.BLOCK_PROJECTILE.get(), BlockProjectileRenderer::new);
        event.registerEntityRenderer(BossEntities.CHESED_FALLING_BLOCK.get(), ChesedFallingBlockRenderer::new);
        event.registerEntityRenderer(BossEntities.FLYING_BLOCK.get(), FlyingBlockEntityRenderer::new);
        event.registerEntityRenderer(BossEntities.RADIAL_EARTHQUAKE.get(), RadialEarthquakeRenderer::new);
        event.registerEntityRenderer(BossEntities.EYE_OF_CHESED.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(BossEntities.EYE_OF_MALKUTH.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(BossEntities.EYE_OF_GEBURAH.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(BossEntities.CHESED_VERTICAL_RAY_ATTACK.get(), ChesedVerticalRayAttackRenderer::new);
        event.registerEntityRenderer(BossEntities.CHESED_ONE_SHOT_VERTICAL_RAY_ATTACK.get(), ChesedOneShotVerticalRayRenderer::new);
        event.registerEntityRenderer(BossEntities.CHESED_FIRE_TRAIL.get(), NullEntityRenderer::new);
        event.registerEntityRenderer(BossEntities.FLYING_SWORD.get(), FlyingSwordRenderer::new);
        event.registerEntityRenderer(BossEntities.MALKUTH_SLASH.get(), MalkuthSlashRenderer::new);
        event.registerEntityRenderer(BossEntities.MALKUTH_CHAIN.get(), MalkuthChainRenderer::new);
        event.registerEntityRenderer(BossEntities.MALKUTH_CANNON_PROJECTILE.get(), MalkuthCannonProjectileRenderer::new);
        event.registerEntityRenderer(BossEntities.MALKUTH_GIANT_SWORD.get(), MalkuthGiantSwordSlashRenderer::new);
        event.registerEntityRenderer(BossEntities.MALKUTH_BOULDER.get(), MalkuthBoulderRenderer::new);
        event.registerEntityRenderer(BossEntities.MALKUTH_EARTHQUAKE.get(), MalkuthEarthquakeRenderer::new);
        event.registerEntityRenderer(BossEntities.MALKUTH_FLOOR.get(), MalkuthFloorRenderer::new);
        event.registerEntityRenderer(BossEntities.MALKUTH_FIREBALL.get(), MalkuthFireballRenderer::new);
        event.registerEntityRenderer(BossEntities.MALKUTH_PLAYER_FIREBALL.get(), MalkuthPlayerFireIceBallRenderer::new);
        event.registerEntityRenderer(BossEntities.MALKUTH_REPAIR_ENTITY.get(), MalkuthRepairEntityRenderer::new);
        event.registerEntityRenderer(BossEntities.CHESED_MINI_RAY.get(), ChesedMiniRayRenderer::new);
    }

    private static FDEntityTransformation<GeburahEntity> generateGeburahHurtTransformation(int offset) {
        return (geburah, poseStack, v) -> {
            Random random = new Random((long)geburah.f_19797_ * (234L + (long)offset) + (long)offset);
            Random random2 = new Random((long)(geburah.f_19797_ + 1) * (234L + (long)offset) + (long)offset);
            Vec3 offs1 = new Vec3((double)(random.nextFloat() * 2.0f - 1.0f), (double)(random.nextFloat() * 2.0f - 1.0f), (double)(random.nextFloat() * 2.0f - 1.0f));
            Vec3 offs2 = new Vec3((double)(random2.nextFloat() * 2.0f - 1.0f), (double)(random2.nextFloat() * 2.0f - 1.0f), (double)(random2.nextFloat() * 2.0f - 1.0f));
            Vec3 t = FDMathUtil.interpolateVectors((Vec3)offs1, (Vec3)offs2, (float)FDEasings.easeInOut((float)v));
            int tick = 25 - geburah.sinnedTicks;
            float alpha = 1.0f - FDEasings.easeOut((float)(((float)tick + v) / 25.0f));
            float ampl = 0.75f * alpha;
            poseStack.m_85837_(t.f_82479_ * (double)ampl, t.f_82480_ * (double)ampl, t.f_82481_ * (double)ampl);
        };
    }
}

