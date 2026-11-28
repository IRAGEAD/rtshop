/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.commands.TeleportCommand
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.RelativeMovement
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Coerce
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.pla.spiral_abyss.mixin;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.pla.spiral_abyss.world.AbyssDimension;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={TeleportCommand.class})
public abstract class AbyssTeleportCommandMixin {
    @Inject(method={"performTeleport"}, at={@At(value="HEAD")})
    private static void spiralAbyss$blockTeleport(CommandSourceStack source, Entity entity, ServerLevel destination, double x, double y, double z, Set<RelativeMovement> relative, float yaw, float pitch, @Coerce Object facing, CallbackInfo callback) throws CommandSyntaxException {
        if (entity.m_9236_().m_46472_().equals(AbyssDimension.LEVEL) || destination.m_46472_().equals(AbyssDimension.LEVEL) || source.m_81373_() != null && source.m_81373_().m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
            throw new SimpleCommandExceptionType((Message)Component.m_237115_((String)"message.spiral_abyss.teleport_blocked")).create();
        }
    }
}

