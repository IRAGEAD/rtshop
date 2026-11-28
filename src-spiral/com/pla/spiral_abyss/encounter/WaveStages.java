/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.QliphothEncounters;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class WaveStages<T> {
    private List<List<FloorDefinition.Enemy>> sequence = List.of();
    private final List<T> enemies = new ArrayList<T>();
    private int index = -1;
    private int removed;
    private int additional;
    private boolean spawning;

    public void prepare(List<FloorDefinition.Enemy> lineup) {
        if (!this.enemies.isEmpty() || this.spawning) {
            throw new IllegalStateException("Clear the previous encounter before preparing stages");
        }
        TreeMap batches = new TreeMap();
        lineup.forEach(enemy -> batches.computeIfAbsent(enemy.stage(), unused -> new ArrayList()).add(enemy));
        if (batches.isEmpty()) {
            throw new IllegalArgumentException("Empty encounter lineup");
        }
        ArrayList<List<FloorDefinition.Enemy>> isolated = new ArrayList<List<FloorDefinition.Enemy>>();
        for (List batch : batches.values()) {
            List<FloorDefinition.Enemy> ordinary = batch.stream().filter(enemy -> !QliphothEncounters.supports(enemy.id())).toList();
            if (!ordinary.isEmpty()) {
                isolated.add(ordinary);
            }
            for (FloorDefinition.Enemy enemy2 : batch) {
                if (!QliphothEncounters.supports(enemy2.id())) continue;
                for (int count = 0; count < enemy2.count(); ++count) {
                    isolated.add(List.of(new FloorDefinition.Enemy(enemy2.id(), 1, enemy2.category(), enemy2.stage(), enemy2.role())));
                }
            }
        }
        this.sequence = List.copyOf(isolated);
        this.index = -1;
        this.removed = 0;
        this.additional = 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean advance(Function<List<FloorDefinition.Enemy>, List<T>> spawn) {
        if (this.spawning || !this.enemies.isEmpty() || this.index + 1 >= this.sequence.size()) {
            return false;
        }
        this.spawning = true;
        ++this.index;
        try {
            List<T> created = spawn.apply(this.sequence.get(this.index));
            int expected = this.sequence.get(this.index).stream().mapToInt(FloorDefinition.Enemy::count).sum();
            this.enemies.addAll(created);
            if (created.size() != expected) {
                throw new IllegalStateException("Stage spawned " + created.size() + " of " + expected + " enemies");
            }
            boolean bl = true;
            return bl;
        }
        finally {
            this.spawning = false;
        }
    }

    public void removeInvalid(Predicate<T> invalid) {
        int before = this.enemies.size();
        this.enemies.removeIf(invalid);
        this.removed += before - this.enemies.size();
    }

    public boolean add(T enemy) {
        if (this.index < 0 || this.enemies.contains(enemy)) {
            return false;
        }
        this.enemies.add(enemy);
        ++this.additional;
        return true;
    }

    public List<T> enemies() {
        return List.copyOf(this.enemies);
    }

    public int index() {
        return this.index;
    }

    public int size() {
        return this.sequence.size();
    }

    public int removed() {
        return this.removed;
    }

    public int total() {
        return this.additional + this.sequence.stream().flatMap(Collection::stream).mapToInt(FloorDefinition.Enemy::count).sum();
    }

    public boolean complete() {
        return !this.spawning && this.index >= 0 && this.index + 1 == this.sequence.size() && this.enemies.isEmpty();
    }

    public void clear(Consumer<T> discard) {
        List<T> previous = List.copyOf(this.enemies);
        this.enemies.clear();
        this.sequence = List.of();
        this.index = -1;
        this.removed = 0;
        this.additional = 0;
        this.spawning = false;
        previous.forEach(discard);
    }
}

