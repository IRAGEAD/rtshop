/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss.boss_enviroment;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class TerrainLayers<K, V> {
    private final Map<K, Cell> cells = new LinkedHashMap<K, Cell>();
    private final Function<K, V> read;
    private final BiConsumer<K, V> write;

    public TerrainLayers(Function<K, V> read, BiConsumer<K, V> write) {
        this.read = read;
        this.write = write;
    }

    public boolean contains(K position) {
        return this.cells.containsKey(position);
    }

    public int size() {
        return this.cells.size();
    }

    public void repair(K position) {
        Cell cell = this.cells.get(position);
        if (cell == null) {
            return;
        }
        Object visible = cell.visible();
        if (!Objects.equals(this.read.apply(position), visible)) {
            this.write.accept(position, visible);
        }
    }

    public void put(UUID owner, K position, V value, int priority) {
        Cell cell = this.cells.computeIfAbsent(position, key -> new Cell(this.read.apply(key)));
        cell.owners.put(owner, new Layer<V>(value, priority));
        this.write.accept(position, cell.visible());
    }

    public void putAll(UUID owner, Map<K, V> values, int priority) {
        values.forEach((position, value) -> {
            Cell cell = this.cells.computeIfAbsent(position, key -> new Cell(this.read.apply(key)));
            cell.owners.put(owner, new Layer<Object>(value, priority));
        });
        values.keySet().forEach(position -> this.write.accept(position, this.cells.get(position).visible()));
    }

    public void remove(UUID owner) {
        Iterator<Map.Entry<K, Cell>> iterator = this.cells.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, Cell> entry = iterator.next();
            Cell cell = entry.getValue();
            if (cell.owners.remove(owner) == null) continue;
            Object visible = cell.visible();
            if (cell.owners.isEmpty()) {
                iterator.remove();
            }
            this.write.accept(entry.getKey(), visible);
        }
    }

    public void remove(UUID owner, K position) {
        Cell cell = this.cells.get(position);
        if (cell == null || cell.owners.remove(owner) == null) {
            return;
        }
        Object visible = cell.visible();
        if (cell.owners.isEmpty()) {
            this.cells.remove(position);
        }
        this.write.accept(position, visible);
    }

    public void clear() {
        LinkedHashMap originals = new LinkedHashMap();
        this.cells.forEach((key, cell) -> originals.put(key, cell.original));
        this.cells.clear();
        originals.forEach(this.write);
    }

    private final class Cell {
        final V original;
        final Map<UUID, Layer<V>> owners = new LinkedHashMap();

        Cell(V original) {
            this.original = original;
        }

        V visible() {
            Object value = this.original;
            int priority = Integer.MIN_VALUE;
            for (Layer layer : this.owners.values()) {
                if (layer.priority() < priority) continue;
                value = layer.value();
                priority = layer.priority();
            }
            return value;
        }
    }

    private record Layer<V>(V value, int priority) {
    }
}

