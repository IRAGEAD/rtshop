package com.rtshop.story;

import com.rtshop.RtShop;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * 바닐라 우드랜드 맨션 안에는 바닐라 우민(변명자·소환사·약탈자·환술사)이 나타나지 않는다.
 * 맨션은 히든 보스(환술사 모드)의 자리라서, 그 보스와 습격(레이드)에 딸린 우민은 그대로 둔다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class MansionIllagers {
    private MansionIllagers() {
    }

    public static final Set<String> BLOCKED = Set.of("minecraft:vindicator", "minecraft:evoker", "minecraft:pillager", "minecraft:illusioner");
    public static boolean enabled = true;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!enabled || !(event.getLevel() instanceof ServerLevel level)) return;
        Entity e = event.getEntity();
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        if (id == null || !BLOCKED.contains(id.toString())) return;
        if (e instanceof Raider r && r.getCurrentRaid() != null) return; // 습격은 그대로
        if (inMansion(level, e)) event.setCanceled(true);
    }

    public static boolean inMansion(ServerLevel level, Entity e) {
        try {
            Structure mansion = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("minecraft:mansion"));
            if (mansion == null) return false;
            var start = level.structureManager().getStructureAt(e.blockPosition(), mansion);
            return start.isValid() && start.getBoundingBox().inflatedBy(4).isInside(e.blockPosition());
        } catch (Exception ex) {
            return false;
        }
    }
}
