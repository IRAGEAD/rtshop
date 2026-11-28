/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDClientHelpers
 *  com.finderfeed.fdlib.systems.bedrock.animations.Animation
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.AnimatedItem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemAnimationHandler
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemAnimationSystem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemModelOptions
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRenderer
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRendererOptions
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDServerItemAnimations
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.animated_item.AnimatedItemStackContext
 *  com.finderfeed.fdlib.util.FDColor
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TooltipFlag
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.client.extensions.common.IClientItemExtensions
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses.content.items.chesed;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.items.chesed.PhaseSphereHandler;
import com.finderfeed.fdbosses.init.BossAnims;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdlib.FDClientHelpers;
import com.finderfeed.fdlib.systems.bedrock.animations.Animation;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimationTicker;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.AnimatedItem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemAnimationHandler;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemAnimationSystem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDItemModelOptions;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRenderer;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDModelItemRendererOptions;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.FDServerItemAnimations;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.item.animated_item.AnimatedItemStackContext;
import com.finderfeed.fdlib.util.FDColor;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Vector3f;

public class PhaseSphere
extends Item
implements AnimatedItem {
    public PhaseSphere(Item.Properties p_41383_) {
        super(p_41383_);
    }

    public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
        if (!level.f_46443_) {
            if (PhaseSphereHandler.canContinueUsingChesedItem(player)) {
                FDServerItemAnimations.startItemAnimation((LivingEntity)player, (String)"USE", (AnimationTicker)AnimationTicker.builder(BossAnims.CHESED_ITEM_USE).setToNullTransitionTime(0).build(), (InteractionHand)hand);
                player.m_6672_(hand);
                level.m_6263_(null, player.m_20185_(), player.m_20186_(), player.m_20189_(), SoundEvents.f_12376_, SoundSource.PLAYERS, 1.0f, 0.9f);
                return InteractionResultHolder.m_19096_((Object)player.m_21120_(hand));
            }
            return InteractionResultHolder.m_19100_((Object)player.m_21120_(hand));
        }
        return super.m_7203_(level, player, hand);
    }

    public int m_8105_(ItemStack p_41454_) {
        return 72000;
    }

    public void m_5929_(Level p_41428_, LivingEntity entity, ItemStack p_41430_, int p_41431_) {
        super.m_5929_(p_41428_, entity, p_41430_, p_41431_);
    }

    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        super.onStopUsing(stack, entity, count);
        if (!entity.m_9236_().f_46443_) {
            entity.m_9236_().m_6263_(null, entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), (SoundEvent)SoundEvents.f_12377_.get(), SoundSource.PLAYERS, 1.0f, 0.75f);
        }
    }

    public void m_6883_(ItemStack stack, Level p_41405_, Entity entity, int slot, boolean p_41408_) {
        super.m_6883_(stack, p_41405_, entity, slot, p_41408_);
    }

    public void animatedItemTick(AnimatedItemStackContext ctx) {
        FDItemAnimationSystem animSystem = FDItemAnimationHandler.getItemAnimationSystem((AnimatedItemStackContext)ctx);
        if (animSystem != null) {
            animSystem.startAnimation("IDLE", AnimationTicker.builder((Animation)((Animation)BossAnims.CHESED_ITEM_IDLE.get())).build());
        }
    }

    public void m_7373_(ItemStack p_41421_, Level p_339594_, List<Component> components, TooltipFlag p_41424_) {
        super.m_7373_(p_41421_, p_339594_, components, p_41424_);
        components.add((Component)Component.m_237115_((String)"fdbosses.word.phase_sphere_description").m_130940_(ChatFormatting.AQUA));
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(FDModelItemRenderer.createExtensions((FDModelItemRendererOptions)FDModelItemRendererOptions.create().addModel(BossModels.CHESED_ITEM, RenderType.m_110458_((ResourceLocation)FDBosses.location("textures/item/chesed_item.png"))).addModel(FDItemModelOptions.builder().modelInfo(BossModels.CHESED_ITEM).renderType(RenderType.m_110488_((ResourceLocation)FDBosses.location("textures/item/chesed_item_emissive.png"))).itemColor((itemDisplayContext, itemStack) -> {
            Player player = FDClientHelpers.getClientPlayer();
            if (player.m_21211_().equals(itemStack)) {
                float pticks = FDRenderUtil.tryGetPartialTickIgnorePause();
                int useTime = PhaseSphereHandler.clientsideChesedItemUseTick;
                if (useTime != -1) {
                    float maxTime = BossConfigs.BOSS_CONFIG.get().itemConfig.phaseSphereUseDuration;
                    float time = Mth.m_14036_((float)((float)useTime + pticks), (float)0.0f, (float)maxTime);
                    float p = time / maxTime;
                    p = FDEasings.easeOut((float)p);
                    return new FDColor(1.0f, 1.0f - p, 1.0f - p, 1.0f);
                }
            }
            return new FDColor(1.0f, 1.0f, 1.0f, 1.0f);
        }).build()).setScale(itemDisplayContext -> Float.valueOf(0.75f)).addRotation(itemDisplayContext -> Float.valueOf(0.0f)).addTranslation(itemDisplayContext -> new Vector3f())));
    }
}

