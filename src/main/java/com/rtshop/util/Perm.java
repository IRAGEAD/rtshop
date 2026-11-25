package com.rtshop.util;

import com.mojang.brigadier.ParseResults;
import com.rtshop.RtShop;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * 운영자(OP) 기능은 크리에이티브(또는 관전) 모드일 때만 쓸 수 있다 (3.49).
 * 서바이벌·모험 모드의 OP 는 보통 플레이어와 같다: RT Shop 의 편집 창·[편집] 단추·운영자용 블록 놓기, 그리고 OP 전용 명령어 모두.
 * 다시 크리에이티브로 가는 /gamemode 만은 남겨 둔다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Perm {
    private Perm() {
    }

    /** 자체 점검(서버·화면)이 도는 동안에는 규칙을 쉬게 한다: 점검은 서바이벌의 가짜 플레이어로 운영자 기능을 부른다. */
    private static final boolean TESTING = Boolean.getBoolean("rtshop.clienttest") || Boolean.getBoolean("rtshop.selftest");
    /** 점검이 이 규칙 자체를 확인할 때 켠다. */
    public static volatile boolean enforceForTest;
    /** 서바이벌의 OP 에게도 남겨 두는 명령어 (크리에이티브로 돌아가는 길). */
    private static final Set<String> ALWAYS = Set.of("gamemode");

    private static boolean resting() {
        return TESTING && !enforceForTest;
    }

    /** 지금 운영자 기능을 쓸 수 있는가: OP 이고, 크리에이티브나 관전 모드다. */
    public static boolean op(Entity e) {
        if (!(e instanceof Player p) || !p.hasPermissions(2)) return false;
        return resting() || p.isCreative() || p.isSpectator();
    }

    /** OP 이지만 서바이벌이라 운영자 기능이 잠긴 상태인가. */
    public static boolean lockedOp(Entity e) {
        return e instanceof Player p && p.hasPermissions(2) && !op(p);
    }

    /** 이 명령어가 OP 전용인가: 권한이 없는 사람으로 다시 읽었을 때 끝까지 읽히지 않으면 그렇다. */
    public static boolean opOnly(ServerPlayer p, String input) {
        try {
            String in = input.startsWith("/") ? input.substring(1) : input;
            var dispatcher = p.server.getCommands().getDispatcher();
            ParseResults<CommandSourceStack> high = dispatcher.parse(in, p.createCommandSourceStack().withPermission(4));
            if (high.getReader().canRead()) return false; // 원래 없는(잘못 친) 명령어: 평소의 오류 안내에 맡긴다
            ParseResults<CommandSourceStack> low = dispatcher.parse(in, p.createCommandSourceStack().withPermission(0));
            return low.getReader().canRead();
        } catch (Throwable t) {
            return false;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCommand(CommandEvent event) {
        try {
            CommandSourceStack src = event.getParseResults().getContext().getSource();
            if (!(src.getEntity() instanceof ServerPlayer p) || !lockedOp(p)) return;
            String input = event.getParseResults().getReader().getString();
            String root = (input.startsWith("/") ? input.substring(1) : input).trim().split("\\s+")[0].toLowerCase(java.util.Locale.ROOT);
            if (root.contains(":")) root = root.substring(root.indexOf(':') + 1);
            if (ALWAYS.contains(root) || !opOnly(p, input)) return;
            event.setCanceled(true);
            p.sendSystemMessage(Component.translatable("rtshop.perm.survival_op").withStyle(ChatFormatting.YELLOW));
        } catch (Throwable ignored) {
        }
    }
}
