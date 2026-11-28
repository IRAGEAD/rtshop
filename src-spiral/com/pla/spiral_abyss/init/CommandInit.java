/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.network.chat.Component
 */
package com.pla.spiral_abyss.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pla.spiral_abyss.world.AbyssParties;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class CommandInit {
    private CommandInit() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_((String)"spiral_abyss").then(((LiteralArgumentBuilder)Commands.m_82127_((String)"reset").requires(source -> source.m_6761_(2))).executes(c -> {
            AbyssParties.get(((CommandSourceStack)c.getSource()).m_81377_()).resetGlobal();
            if (!((CommandSourceStack)c.getSource()).m_230897_()) {
                ((CommandSourceStack)c.getSource()).m_288197_(() -> Component.m_237115_((String)"message.spiral_abyss.global_reset"), false);
            }
            return 1;
        }))).executes(c -> AbyssParties.get(((CommandSourceStack)c.getSource()).m_81377_()).portals().openArenaMenu(((CommandSourceStack)c.getSource()).m_81375_()) ? 1 : 0)).then(Commands.m_82127_((String)"floor").then(Commands.m_82129_((String)"number", (ArgumentType)IntegerArgumentType.integer((int)1, (int)6)).executes(c -> AbyssParties.get(((CommandSourceStack)c.getSource()).m_81377_()).enterFloor(((CommandSourceStack)c.getSource()).m_81375_(), IntegerArgumentType.getInteger((CommandContext)c, (String)"number")) ? 1 : 0)))).then(Commands.m_82127_((String)"next_floor").executes(c -> AbyssParties.get(((CommandSourceStack)c.getSource()).m_81377_()).nextFloor(((CommandSourceStack)c.getSource()).m_81375_()) ? 1 : 0))).then(((LiteralArgumentBuilder)Commands.m_82127_((String)"skip_floor").requires(source -> source.m_6761_(2))).then(Commands.m_82129_((String)"number", (ArgumentType)IntegerArgumentType.integer((int)1, (int)6)).executes(c -> AbyssParties.get(((CommandSourceStack)c.getSource()).m_81377_()).skipFloors(((CommandSourceStack)c.getSource()).m_81375_(), IntegerArgumentType.getInteger((CommandContext)c, (String)"number")) ? 1 : 0))));
    }
}

