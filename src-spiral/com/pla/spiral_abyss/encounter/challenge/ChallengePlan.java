/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss.encounter.challenge;

import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.challenge.ChallengeCatalog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public final class ChallengePlan {
    public static final int VERSION = 6;

    private ChallengePlan() {
    }

    public static List<Challenge> select(long seed, ChallengeCatalog.Snapshot catalog, Map<Challenge.Difficulty, List<Challenge.Type>> allowed, List<Challenge> saved, Context context) {
        EnumMap<Challenge.Difficulty, Challenge> retained = new EnumMap<Challenge.Difficulty, Challenge>(Challenge.Difficulty.class);
        for (Challenge challenge : saved) {
            if (!allowed.getOrDefault((Object)challenge.difficulty(), List.of()).contains((Object)challenge.type()) || catalog.tier(challenge.type(), challenge.difficulty()) == null || !ChallengeCatalog.available(challenge) || !context.accepts(List.of(challenge))) continue;
            retained.put(challenge.difficulty(), challenge);
        }
        if (retained.size() == Challenge.Difficulty.values().length) {
            List<Challenge> ordered = Arrays.stream(Challenge.Difficulty.values()).map(retained::get).toList();
            if (context.accepts(ordered)) {
                return ordered;
            }
        }
        ArrayList<List<Challenge>> options = new ArrayList<List<Challenge>>();
        for (Challenge.Difficulty difficulty : Challenge.Difficulty.values()) {
            ArrayList<Challenge.Type> types = new ArrayList<Challenge.Type>(allowed.getOrDefault((Object)difficulty, List.of()));
            Collections.shuffle(types, new Random(seed ^ (long)difficulty.ordinal()));
            Challenge old = saved.stream().filter(c -> c.difficulty() == difficulty && types.contains((Object)c.type())).findFirst().orElse(null);
            if (old != null) {
                types.remove((Object)old.type());
                types.add(0, old.type());
            }
            ArrayList<Challenge> choices = new ArrayList<Challenge>();
            for (Challenge.Type type : types) {
                if (catalog.tier(type, difficulty) == null) continue;
                for (Challenge candidate : catalog.candidates(type, difficulty, seed ^ (long)type.ordinal() << 32 ^ (long)difficulty.ordinal(), old)) {
                    if (!context.accepts(List.of(candidate))) continue;
                    choices.add(candidate);
                }
            }
            if (choices.isEmpty()) {
                throw new IllegalArgumentException("No available challenge." + difficulty.key() + " choice for this floor");
            }
            options.add(choices);
        }
        ArrayList<Challenge> arrayList = new ArrayList<Challenge>();
        if (!ChallengePlan.search(arrayList, options, retained, context, new int[]{100000}) && !ChallengePlan.search(arrayList, options, Map.of(), context, new int[]{100000})) {
            throw new IllegalArgumentException("No feasible easy, medium and hard challenge set for this floor");
        }
        return List.copyOf(arrayList);
    }

    private static boolean search(List<Challenge> result, List<List<Challenge>> options, Map<Challenge.Difficulty, Challenge> retained, Context context, int[] budget) {
        if (result.size() == Challenge.Difficulty.values().length) {
            return true;
        }
        Challenge fixed = retained.get((Object)Challenge.Difficulty.values()[result.size()]);
        for (Challenge candidate : fixed == null ? options.get(result.size()) : List.of(fixed)) {
            budget[0] = budget[0] - 1;
            if (budget[0] < 0) {
                return false;
            }
            result.add(candidate);
            if (context.accepts(result) && ChallengePlan.search(result, options, retained, context, budget)) {
                return true;
            }
            result.remove(result.size() - 1);
        }
        return false;
    }

    public record Context(List<Integer> populations, Predicate<Challenge> eligible) {
        public Context {
            populations = List.copyOf(populations);
            if (populations.isEmpty() || populations.stream().anyMatch(count -> count < 1)) {
                throw new IllegalArgumentException("Missing wave populations");
            }
        }

        boolean accepts(List<Challenge> selected) {
            return selected.stream().allMatch(this.eligible) && this.populations.stream().allMatch(count -> ChallengeCatalog.feasible(selected, count));
        }
    }
}

