/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraftforge.common.capabilities.Capability
 *  net.minecraftforge.common.capabilities.CapabilityManager
 *  net.minecraftforge.common.capabilities.CapabilityToken
 *  net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent
 *  org.jetbrains.annotations.Nullable
 */
package yesman.epicfight.world.capabilities;

import java.util.Optional;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.projectile.ProjectilePatch;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

public class EpicFightCapabilities {
    public static final Capability<EntityPatch> CAPABILITY_ENTITY = CapabilityManager.get((CapabilityToken)new CapabilityToken<EntityPatch>(){});
    public static final Capability<CapabilityItem> CAPABILITY_ITEM = CapabilityManager.get((CapabilityToken)new CapabilityToken<CapabilityItem>(){});
    public static final Capability<ProjectilePatch> CAPABILITY_PROJECTILE = CapabilityManager.get((CapabilityToken)new CapabilityToken<ProjectilePatch>(){});
    public static final Capability<CapabilitySkill> CAPABILITY_SKILL = CapabilityManager.get((CapabilityToken)new CapabilityToken<CapabilitySkill>(){});

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(CapabilityItem.class);
        event.register(EntityPatch.class);
        event.register(ProjectilePatch.class);
        event.register(CapabilitySkill.class);
    }

    public static CapabilityItem getItemStackCapability(ItemStack stack) {
        return stack.m_41619_() ? CapabilityItem.EMPTY : (CapabilityItem)stack.getCapability(CAPABILITY_ITEM).orElse((Object)CapabilityItem.EMPTY);
    }

    public static CapabilityItem getItemStackCapabilityOr(ItemStack stack, @Nullable CapabilityItem defaultCap) {
        return stack.m_41619_() ? defaultCap : (CapabilityItem)stack.getCapability(CAPABILITY_ITEM).orElse((Object)defaultCap);
    }

    public static Optional<CapabilityItem> getItemCapability(ItemStack stack) {
        return stack.m_41619_() ? Optional.empty() : stack.getCapability(CAPABILITY_ITEM).resolve();
    }

    @Nullable
    public static <T extends EntityPatch> T getEntityPatch(@Nullable Entity entity, Class<T> type) {
        EntityPatch entitypatch;
        if (entity != null && (entitypatch = (EntityPatch)entity.getCapability(CAPABILITY_ENTITY).orElse(null)) != null && type.isAssignableFrom(entitypatch.getClass())) {
            return (T)entitypatch;
        }
        return null;
    }

    @Nullable
    public static PlayerPatch getPlayerPatch(@Nullable Player player) {
        EntityPatch entitypatch;
        if (player != null && (entitypatch = (EntityPatch)player.getCapability(CAPABILITY_ENTITY).orElse(null)) != null && PlayerPatch.class.isAssignableFrom(entitypatch.getClass())) {
            return (PlayerPatch)entitypatch;
        }
        return null;
    }

    @Nullable
    public static ServerPlayerPatch getServerPlayerPatch(@Nullable ServerPlayer serverPlayer) {
        EntityPatch entitypatch;
        if (serverPlayer != null && (entitypatch = (EntityPatch)serverPlayer.getCapability(CAPABILITY_ENTITY).orElse(null)) != null && ServerPlayerPatch.class.isAssignableFrom(entitypatch.getClass())) {
            return (ServerPlayerPatch)entitypatch;
        }
        return null;
    }

    @Nullable
    public static LocalPlayerPatch getLocalPlayerPatch(@Nullable LocalPlayer localPlayer) {
        EntityPatch entitypatch;
        if (localPlayer != null && (entitypatch = (EntityPatch)localPlayer.getCapability(CAPABILITY_ENTITY).orElse(null)) != null && LocalPlayerPatch.class.isAssignableFrom(entitypatch.getClass())) {
            return (LocalPlayerPatch)entitypatch;
        }
        return null;
    }

    public static <T extends EntityPatch<?>> Optional<T> getUnparameterizedEntityPatch(@Nullable Entity entity, Class<T> type) {
        EntityPatch entitypatch;
        if (entity != null && (entitypatch = (EntityPatch)entity.getCapability(CAPABILITY_ENTITY).orElse(null)) != null && type.isAssignableFrom(entitypatch.getClass())) {
            return Optional.of(entitypatch);
        }
        return Optional.empty();
    }

    public static <E extends Entity, T extends EntityPatch<E>> Optional<T> getParameterizedEntityPatch(@Nullable Entity entity, Class<E> entitytype, Class<?> patchtype) {
        EntityPatch entitypatch;
        if (entity != null && entitytype.isAssignableFrom(entity.getClass()) && (entitypatch = (EntityPatch)entity.getCapability(CAPABILITY_ENTITY).orElse(null)) != null && patchtype.isAssignableFrom(entitypatch.getClass())) {
            return Optional.of(entitypatch);
        }
        return Optional.empty();
    }

    public static Optional<PlayerPatch<?>> getPlayerPatchAsOptional(@Nullable Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        EntityPatch entitypatch = (EntityPatch)entity.getCapability(CAPABILITY_ENTITY).orElse(null);
        if (entitypatch instanceof PlayerPatch) {
            PlayerPatch playerpatch = (PlayerPatch)entitypatch;
            return Optional.of(playerpatch);
        }
        return Optional.empty();
    }

    public static Optional<ServerPlayerPatch> getServerPlayerPatchAsOptional(@Nullable Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        EntityPatch entitypatch = (EntityPatch)entity.getCapability(CAPABILITY_ENTITY).orElse(null);
        if (entitypatch instanceof ServerPlayerPatch) {
            ServerPlayerPatch serverplayerpatch = (ServerPlayerPatch)entitypatch;
            return Optional.of(serverplayerpatch);
        }
        return Optional.empty();
    }

    public static Optional<LocalPlayerPatch> getLocalPlayerPatchAsOptional(@Nullable Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        EntityPatch entitypatch = (EntityPatch)entity.getCapability(CAPABILITY_ENTITY).orElse(null);
        if (entitypatch instanceof LocalPlayerPatch) {
            LocalPlayerPatch localplayerpatch = (LocalPlayerPatch)entitypatch;
            return Optional.of(localplayerpatch);
        }
        return Optional.empty();
    }
}

