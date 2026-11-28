/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.data.CombatRole;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public final class BossWavePolicy {
    private BossWavePolicy() {
    }

    public static void validate(int floor, int wave, FloorDefinition.Wave definition) {
        boolean single;
        List<Integer> counts = definition.waveTypes().isEmpty() ? List.of(Integer.valueOf(definition.categories().getOrDefault((Object)MobCategory.BOSS, 0) + definition.bosses().stream().mapToInt(FloorDefinition.Enemy::count).sum())) : definition.waveTypes().stream().map(type -> type.groups().stream().filter(group -> group.category() == MobCategory.BOSS).mapToInt(FloorDefinition.WaveGroup::count).sum()).toList();
        boolean forbidden = floor < 5 || floor == 5 && wave < 3 || floor == 6 && wave == 1;
        boolean bl = single = floor == 5 && wave == 3 || floor == 6 && wave == 2;
        if (counts.stream().anyMatch(count -> forbidden ? count != 0 : single && count != 1)) {
            throw new IllegalArgumentException("Floor " + floor + " wave " + wave + (forbidden ? " forbids boss groups" : " requires exactly one boss"));
        }
        if (floor == 6 && wave == 3 && counts.stream().anyMatch(count -> count < 1 || count > 2)) {
            throw new IllegalArgumentException("Floor 6 wave 3 requires one or two bosses");
        }
        if (!(forbidden || definition.bosses().isEmpty() && !definition.waveTypes().stream().flatMap(type -> type.groups().stream()).anyMatch(group -> group.category() == MobCategory.BOSS && group.role() != CombatRole.ANY))) {
            throw new IllegalArgumentException("Cycle boss slots require category boss with role any, not explicit fixed IDs");
        }
    }

    public static List<FloorDefinition.Enemy> sequential(List<FloorDefinition.Enemy> enemies) {
        TreeMap stages = new TreeMap();
        enemies.forEach(enemy -> stages.computeIfAbsent(enemy.stage(), unused -> new ArrayList()).add(enemy));
        ArrayList<FloorDefinition.Enemy> result = new ArrayList<FloorDefinition.Enemy>();
        int stage = 0;
        for (List group : stages.values()) {
            boolean boss = false;
            for (FloorDefinition.Enemy enemy2 : group) {
                if (enemy2.category() == MobCategory.BOSS || enemy2.category() == null) continue;
                result.add(new FloorDefinition.Enemy(enemy2.id(), enemy2.count(), enemy2.category(), stage, enemy2.role()));
            }
            for (FloorDefinition.Enemy enemy2 : group) {
                if (enemy2.category() != MobCategory.BOSS && enemy2.category() != null) continue;
                for (int count = 0; count < enemy2.count(); ++count) {
                    if (boss) {
                        ++stage;
                    }
                    result.add(new FloorDefinition.Enemy(enemy2.id(), 1, enemy2.category(), stage, enemy2.role()));
                    boss = true;
                }
            }
            ++stage;
        }
        return List.copyOf(result);
    }
}

