package com.rtshop.rules;

import com.rtshop.RtShop;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 스킬북으로 배운 기술은 죽어도 남는다 (world_rules.json 의 keepSkillsOnDeath).
 * Epic Fight 의 게임 규칙 keepSkills("죽은 뒤에도 기술 유지")를 서버가 켜질 때 켠다. Epic Fight 가 없으면 아무 일도 하지 않는다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class KeepSkills {
    private KeepSkills() {
    }

    public static final String RULE = "keepSkills";

    /** 규칙을 찾아 켠다. 찾아서 켰으면(또는 이미 켜져 있으면) true. */
    public static boolean apply(MinecraftServer server) {
        if (!WorldRules.get().keepSkillsOnDeath) return false;
        final boolean[] found = {false};
        try {
            GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                @Override
                public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                    if (!key.getId().equals(RULE)) return;
                    found[0] = true;
                    GameRules.BooleanValue v = server.getGameRules().getRule(key);
                    if (!v.get()) {
                        v.set(true, server);
                        RtShop.LOGGER.info("[rtshop] 게임 규칙 keepSkills 를 켰습니다: 스킬북으로 배운 기술은 죽어도 남습니다.");
                    }
                }
            });
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] keepSkills 규칙을 켜지 못했습니다: {}", t.toString());
        }
        return found[0];
    }

    /** 지금 그 규칙이 켜져 있는가 (규칙이 없으면 false). */
    public static boolean on(MinecraftServer server) {
        final boolean[] on = {false};
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                if (key.getId().equals(RULE)) on[0] = server.getGameRules().getRule(key).get();
            }
        });
        return on[0];
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        apply(event.getServer());
    }
}
