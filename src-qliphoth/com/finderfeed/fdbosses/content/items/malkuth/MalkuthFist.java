/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemModelOptions
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRenderer
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRendererOptions
 *  com.finderfeed.fdlib.systems.shake.FDShakeData
 *  com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemCooldowns
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TooltipFlag
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.extensions.common.IClientItemExtensions
 *  net.minecraftforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.items.malkuth;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.particles.stripe_particle.StripeParticleOptions;
import com.finderfeed.fdbosses.content.items.malkuth.MalkuthFistChain;
import com.finderfeed.fdbosses.init.BossItems;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdbosses.init.BossSounds;
import com.finderfeed.fdbosses.packets.SlamParticlesPacket;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemModelOptions;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRenderer;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRendererOptions;
import com.finderfeed.fdlib.systems.shake.FDShakeData;
import com.finderfeed.fdlib.systems.shake.PositionedScreenShakePacket;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.client.particles.ball_particle.BallParticleOptions;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class MalkuthFist
extends Item {
    public MalkuthFist(Item.Properties properties) {
        super(properties);
    }

    public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
        if (!level.f_46443_) {
            ItemStack item = player.m_21120_(hand);
            if (hand == InteractionHand.MAIN_HAND) {
                if (this.usesAsChain(player)) {
                    MalkuthFistChain.summon(player, false);
                    player.m_6672_(hand);
                    return InteractionResultHolder.m_19096_((Object)item);
                }
                level.m_6263_(null, player.m_20185_(), player.m_20186_(), player.m_20189_(), BossSounds.MALKUTH_SWORD_EARTH_IMPACT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                SlamParticlesPacket packet = new SlamParticlesPacket(new SlamParticlesPacket.SlamData(player.m_20097_(), player.m_20182_().m_82520_(0.0, (double)0.1f, 0.0), new Vec3(1.0, 0.0, 0.0)).maxAngle((float)Math.PI * 2).maxSpeed(0.3f).collectRadius(2).maxParticleLifetime(30).count(20).maxVerticalSpeedEdges(0.15f).maxVerticalSpeedCenter(0.15f));
                FDPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> ((ServerLevel)level).m_46745_(player.m_20097_())), (Object)packet);
                BallParticleOptions ballParticleOptions = BallParticleOptions.builder().size(7.0f).scalingOptions(1, 0, 1).color(1.0f, 0.6f, 0.2f).brightness(2).build();
                ((ServerLevel)level).m_8767_((ParticleOptions)ballParticleOptions, player.m_20185_(), player.m_20186_(), player.m_20189_(), 1, 0.0, 0.0, 0.0, 0.0);
                PositionedScreenShakePacket.send((ServerLevel)((ServerLevel)level), (FDShakeData)FDShakeData.builder().amplitude(1.0f).frequency(10.0f).outTime(10).build(), (Vec3)player.m_20182_(), (double)20.0);
                int stripesCount = 6;
                for (int i = 0; i < stripesCount; ++i) {
                    FDColor endColor;
                    FDColor startColor;
                    if (i % 2 == 0) {
                        startColor = new FDColor(1.0f, 0.8f, 0.2f, 1.0f);
                        endColor = new FDColor(1.0f, 0.2f, 0.1f, 1.0f);
                    } else {
                        startColor = new FDColor(0.2f, 0.8f, 1.0f, 1.0f);
                        endColor = new FDColor(0.1f, 0.2f, 1.0f, 1.0f);
                    }
                    StripeParticleOptions options = StripeParticleOptions.createHorizontalCircling(startColor, endColor, new Vec3(0.0, 1.0, 0.0), (float)i / (float)stripesCount * (float)Math.PI * 2.0f, 0.1f, 5, 20, 0.0f, 2.0f, 0.5f, 0.75f, true, false);
                    ((ServerLevel)level).m_8767_((ParticleOptions)options, player.m_20185_(), player.m_20186_(), player.m_20189_(), 1, 0.0, 0.0, 0.0, 0.0);
                }
                Vec3 lookAngle = player.m_20154_().m_82542_(1.0, 0.0, 1.0);
                if (!lookAngle.equals((Object)Vec3.f_82478_)) {
                    lookAngle = lookAngle.m_82541_();
                }
                Vec3 speed = new Vec3(lookAngle.f_82479_ * (double)0.1f, 1.5, lookAngle.f_82481_ * (double)0.1f);
                FDLibCalls.setServerPlayerSpeed((ServerPlayer)((ServerPlayer)player), (Vec3)speed);
                player.m_36335_().m_41524_(BossItems.MALKUTH_FIST.get(), 5);
                return InteractionResultHolder.m_19096_((Object)item);
            }
            MalkuthFistChain.summon(player, true);
            player.m_6672_(hand);
            return InteractionResultHolder.m_19096_((Object)item);
        }
        return super.m_7203_(level, player, hand);
    }

    public boolean usesAsChain(Player player) {
        Vec3 lookAngle = player.m_20154_();
        double dot = lookAngle.m_82526_(new Vec3(0.0, 1.0, 0.0));
        if (player.m_20096_() && dot < -0.6) {
            ClipContext clipContext = new ClipContext(player.m_146892_(), player.m_146892_().m_82549_(lookAngle.m_82490_(3.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
            BlockHitResult res = player.m_9236_().m_45547_(clipContext);
            if (res.m_6662_() != HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }

    public int m_8105_(ItemStack p_41454_) {
        return 72000;
    }

    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        super.onStopUsing(stack, entity, count);
    }

    public void m_6883_(ItemStack stack, Level level, Entity entity, int slot, boolean p_41408_) {
        super.m_6883_(stack, level, entity, slot, p_41408_);
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            int hookCooldown = MalkuthFist.getHookCooldown(stack);
            ItemStack offhand = serverPlayer.m_21206_();
            ItemCooldowns cooldowns = serverPlayer.m_36335_();
            if (MalkuthFist.canSkipCooldown(stack)) {
                if (cooldowns.m_41519_(stack.m_41720_())) {
                    if (stack != offhand) {
                        MalkuthFist.setCanSkipCooldown(stack, false);
                        cooldowns.m_41527_(stack.m_41720_());
                    }
                } else {
                    MalkuthFist.setCanSkipCooldown(stack, false);
                }
            }
            if (!cooldowns.m_41519_(BossItems.MALKUTH_FIST.get()) && stack == offhand && hookCooldown > 0) {
                cooldowns.m_41524_(BossItems.MALKUTH_FIST.get(), hookCooldown);
            }
            if (hookCooldown > 0) {
                MalkuthFist.setHookCooldown(stack, hookCooldown - 1);
            }
        }
    }

    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.m_9236_().f_46443_) {
            MalkuthFist.setCanSkipCooldown(stack, false);
        }
        return super.onEntityItemUpdate(stack, entity);
    }

    public static CompoundTag getFistData(ItemStack itemStack) {
        return itemStack.m_41698_("malkuthFistData");
    }

    public static boolean canSkipCooldown(ItemStack itemStack) {
        CompoundTag tag = MalkuthFist.getFistData(itemStack);
        return tag.m_128471_("canSkipCooldown");
    }

    public static int getHookCooldown(ItemStack itemStack) {
        CompoundTag tag = MalkuthFist.getFistData(itemStack);
        return tag.m_128451_("hookCooldown");
    }

    public static void setCanSkipCooldown(ItemStack itemStack, boolean state) {
        CompoundTag tag = MalkuthFist.getFistData(itemStack);
        tag.m_128379_("canSkipCooldown", state);
    }

    public static void setHookCooldown(ItemStack itemStack, int cooldown) {
        CompoundTag tag = MalkuthFist.getFistData(itemStack);
        tag.m_128405_("hookCooldown", cooldown);
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    public void m_7373_(ItemStack p_41421_, @Nullable Level p_41422_, List<Component> components, TooltipFlag p_41424_) {
        super.m_7373_(p_41421_, p_41422_, components, p_41424_);
        components.add((Component)Component.m_237115_((String)"fdbosses.word.malkuth_fist_description").m_130940_(ChatFormatting.GOLD));
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(FDModelItemRenderer.createExtensions((FDModelItemRendererOptions)FDModelItemRendererOptions.create().addModel(FDItemModelOptions.builder().modelInfo(BossModels.MALKUTH_FIST).renderType((ctx, item) -> {
            if (ctx != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && ctx != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                return RenderType.m_110458_((ResourceLocation)FDBosses.location("textures/item/malkuth_fist_item.png"));
            }
            return RenderType.m_110458_((ResourceLocation)FDBosses.location("textures/item/malkuth_fist_item_offhand.png"));
        }).build()).addModel(FDItemModelOptions.builder().modelInfo(BossModels.MALKUTH_FIST_LAYER).renderType((ctx, item) -> {
            if (ctx != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && ctx != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                return RenderType.m_110458_((ResourceLocation)FDBosses.location("textures/item/malkuth_fist_item_layer.png"));
            }
            return RenderType.m_110458_((ResourceLocation)FDBosses.location("textures/item/malkuth_fist_item_offhand_layer.png"));
        }).build()).addModel(FDItemModelOptions.builder().modelInfo(BossModels.MALKUTH_FIST).renderType((ctx, item) -> {
            if (ctx != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && ctx != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                return RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/item/malkuth_fist_item_emissive.png"));
            }
            return RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/item/malkuth_fist_item_offhand_emissive.png"));
        }).build()).setScale(ctx -> {
            if (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                return Float.valueOf(0.75f);
            }
            return Float.valueOf(1.0f);
        }).addRotation3(itemDisplayContext -> {
            if (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                return new Vector3f(30.0f, 0.0f, 0.0f);
            }
            if (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                return new Vector3f(30.0f, 0.0f, 0.0f);
            }
            return new Vector3f();
        }).addTranslation(itemDisplayContext -> {
            if (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                return new Vector3f(0.05f, 0.0f, 0.0f);
            }
            if (itemDisplayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                return new Vector3f(0.2f, -0.325f, 0.05f);
            }
            if (itemDisplayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                return new Vector3f(0.2f, -0.325f, -0.05f);
            }
            return new Vector3f();
        })));
    }
}

