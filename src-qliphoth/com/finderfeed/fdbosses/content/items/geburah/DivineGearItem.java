/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.AnimatedItem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDFreeItemRenderer
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemAnimationHandler
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemAnimationSystem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemModelOptions
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRenderer
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRendererOptions
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.animated_item.AnimatedItemStackContext
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TooltipFlag
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.extensions.common.IClientItemExtensions
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.items.geburah;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.items.geburah.DivineGear;
import com.finderfeed.fdbosses.content.items.geburah.DivineGearItemRenderer;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdlib.data_structures.Pair;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.AnimatedItem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDFreeItemRenderer;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemAnimationHandler;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemAnimationSystem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemModelOptions;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRenderer;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRendererOptions;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.animated_item.AnimatedItemStackContext;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Vector3f;

public class DivineGearItem
extends Item
implements AnimatedItem {
    public static final ResourceLocation DIVINE_GEAR = FDBosses.location("textures/entities/geburah/divine_gear.png");

    public DivineGearItem(Item.Properties properties) {
        super(properties);
    }

    public InteractionResult m_6225_(UseOnContext ctx) {
        Player player = ctx.m_43723_();
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            if (ctx.m_43724_() == InteractionHand.MAIN_HAND) {
                Direction direction = ctx.m_43719_();
                ItemStack item = ctx.m_43722_();
                int charges = DivineGearItem.getCharge(item);
                if (charges > 0 || player.m_7500_()) {
                    BlockPos pos = ctx.m_8083_();
                    Pair<BlockPos, Direction> posAndDir = DivineGearItem.getPosAndDirection(ctx.m_43725_(), pos, direction);
                    pos = (BlockPos)posAndDir.first;
                    direction = (Direction)posAndDir.second;
                    if (DivineGearItem.canPlaceOn(ctx.m_43725_(), pos, direction)) {
                        Vec3 place = DivineGearItem.getSpawnPlace(pos, direction);
                        DivineGear.summon(player, place);
                        if (!player.m_7500_()) {
                            DivineGearItem.setOnCooldown(item);
                            DivineGearItem.setCharge(item, charges - 1);
                        }
                        return InteractionResult.SUCCESS;
                    }
                    return InteractionResult.FAIL;
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.m_6225_(ctx);
    }

    public static Pair<BlockPos, Direction> getPosAndDirection(Level level, BlockPos clickedPos, Direction direction) {
        BlockState state = level.m_8055_(clickedPos);
        if (state.m_60812_((BlockGetter)level, clickedPos).m_83281_()) {
            for (Direction dir : Direction.values()) {
                BlockPos offs = clickedPos.m_121955_(dir.m_122436_());
                if (!level.m_8055_(offs).m_60812_((BlockGetter)level, offs).m_83281_()) continue;
                return new Pair((Object)clickedPos.m_121955_(dir.m_122424_().m_122436_()), (Object)dir);
            }
            return new Pair((Object)clickedPos.m_7495_(), (Object)direction);
        }
        return new Pair((Object)clickedPos, (Object)direction);
    }

    public static Vec3 getSpawnPlace(BlockPos pos, Direction direction) {
        Vec3 place;
        if (direction == Direction.UP) {
            place = pos.m_252807_().m_82520_(0.0, 0.5, 0.0);
        } else if (Direction.Plane.HORIZONTAL.test(direction)) {
            double x = direction.m_122429_();
            double z = direction.m_122431_();
            place = pos.m_252807_().m_82520_(x * 2.0, -1.5, z * 2.0);
        } else {
            place = pos.m_252807_().m_82520_(0.0, -3.5, 0.0);
        }
        return place;
    }

    public static boolean canPlaceOn(Level level, BlockPos pos, Direction direction) {
        BlockPos centerPos = pos.m_7918_(direction.m_122429_() * 2, direction.m_122430_() * 2, direction.m_122431_() * 2);
        Vec3 center = centerPos.m_252807_();
        AABB checkAABB = new AABB(center.f_82479_ - 1.5, center.f_82480_ - 1.5, center.f_82481_ - 1.5, center.f_82479_ + 1.5, center.f_82480_ + 1.5, center.f_82481_ + 1.5).m_82400_(0.2);
        return level.m_8055_(pos.m_121955_(direction.m_122436_())).m_60812_((BlockGetter)level, pos).m_83281_() && level.m_8055_(centerPos).m_60812_((BlockGetter)level, pos).m_83281_() && level.m_45976_(DivineGear.class, checkAABB).isEmpty();
    }

    public void m_6883_(ItemStack stack, Level level, Entity entity, int p_41407_, boolean p_41408_) {
        super.m_6883_(stack, level, entity, p_41407_, p_41408_);
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            int charges = DivineGearItem.getCharge(stack);
            if (charges < BossConfigs.BOSS_CONFIG.get().itemConfig.divineGearCharges) {
                int cooldown = DivineGearItem.getCooldown(stack);
                if (cooldown > 0) {
                    DivineGearItem.setCooldown(stack, cooldown - 1);
                } else {
                    DivineGearItem.setOnCooldown(stack);
                    DivineGearItem.setCharge(stack, charges + 1);
                }
            }
        }
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    public void animatedItemTick(AnimatedItemStackContext ctx) {
        FDItemAnimationSystem animSystem = FDItemAnimationHandler.getItemAnimationSystem((AnimatedItemStackContext)ctx);
        if (animSystem != null) {
            animSystem.startAnimation("IDLE", AnimationTicker.builder(BossAnims.DIVINE_GEAR_ITEM_IDLE).build());
        }
    }

    public void m_7373_(ItemStack p_41421_, Level level, List<Component> components, TooltipFlag p_41424_) {
        super.m_7373_(p_41421_, level, components, p_41424_);
        components.add((Component)Component.m_237115_((String)"fdbosses.word.divine_gear_description").m_130940_(ChatFormatting.GOLD));
    }

    public static CompoundTag getData(ItemStack itemStack) {
        CompoundTag tag = itemStack.m_41698_("divineGearData");
        if (!tag.m_128441_("charge")) {
            tag.m_128405_("charge", BossConfigs.BOSS_CONFIG.get().itemConfig.divineGearCharges);
        }
        return tag;
    }

    public static int getCooldown(ItemStack itemStack) {
        CompoundTag tag = DivineGearItem.getData(itemStack);
        return tag.m_128451_("cooldown");
    }

    public static int getCharge(ItemStack itemStack) {
        CompoundTag tag = DivineGearItem.getData(itemStack);
        return tag.m_128451_("charge");
    }

    public static void setCooldown(ItemStack itemStack, int cooldown) {
        CompoundTag tag = DivineGearItem.getData(itemStack);
        tag.m_128405_("cooldown", cooldown);
    }

    public static void setCharge(ItemStack itemStack, int charge) {
        CompoundTag tag = DivineGearItem.getData(itemStack);
        tag.m_128405_("charge", charge);
    }

    public static void setOnCooldown(ItemStack itemStack) {
        DivineGearItem.setCooldown(itemStack, BossConfigs.BOSS_CONFIG.get().itemConfig.divineGearChargeReplenishTime);
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(FDModelItemRenderer.createExtensions((FDModelItemRendererOptions)FDModelItemRendererOptions.create().addModel(FDItemModelOptions.builder().modelInfo(BossModels.DIVINE_GEAR).renderType((ctx, item) -> RenderType.m_110452_((ResourceLocation)DIVINE_GEAR)).build()).setScale(ctx -> Float.valueOf(0.25f)).addRotation3(itemDisplayContext -> new Vector3f()).addTranslation(itemDisplayContext -> new Vector3f(0.0f, -0.1f, 0.0f)).freeRender((FDFreeItemRenderer)new DivineGearItemRenderer())));
    }
}

