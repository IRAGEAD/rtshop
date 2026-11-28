/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.StringTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.EquipmentSlot
 */
package com.pla.spiral_abyss.encounter.challenge;

import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

public record Challenge(Type type, Difficulty difficulty, int target, double percentage, List<ResourceLocation> ids, List<EquipmentSlot> forbiddenSlots) {
    public Challenge {
        Objects.requireNonNull(type);
        Objects.requireNonNull(difficulty);
        if (target < 1) {
            throw new IllegalArgumentException("Challenge target must be positive");
        }
        if (!Double.isFinite(percentage) || percentage < 0.0 || percentage > 100.0 || Challenge.counted(type) && percentage == 0.0) {
            throw new IllegalArgumentException("Counted challenge percentage must be greater than zero and at most 100");
        }
        ids = List.copyOf(ids);
        forbiddenSlots = List.copyOf(forbiddenSlots);
    }

    public Challenge(Type type, Difficulty difficulty, int target, List<ResourceLocation> ids, List<EquipmentSlot> slots) {
        this(type, difficulty, target, Challenge.counted(type) ? Challenge.defaultPercentage(difficulty) : 0.0, ids, slots);
    }

    public static boolean counted(Type type) {
        return type != Type.TIME && type != Type.NO_ARMOR && type != Type.ARMOR_ENCHANTMENT && type != Type.TOTEM;
    }

    public static double defaultPercentage(Difficulty difficulty) {
        return switch (difficulty) {
            default -> throw new IncompatibleClassChangeError();
            case Difficulty.EASY -> 20.0;
            case Difficulty.MEDIUM -> 25.0;
            case Difficulty.HARD -> 33.333333333333336;
        };
    }

    public int requiredCount(int waveEnemies) {
        return Challenge.counted(this.type) ? Math.max(1, (int)Math.ceil((double)Math.max(0, waveEnemies) * this.percentage / 100.0 - 1.0E-9)) : this.target;
    }

    public Component description() {
        return this.description(-1);
    }

    public static int requiredEquipmentPlayers(int participants) {
        return Math.max(1, (participants + 2) / 3);
    }

    public Component description(int waveEnemies) {
        MutableComponent detail = Component.m_237119_();
        for (ResourceLocation id : this.ids) {
            if (!detail.m_7360_().isEmpty()) {
                detail.m_130946_(", ");
            }
            detail.m_7220_(this.registryName(id));
        }
        for (EquipmentSlot slot : this.forbiddenSlots) {
            if (!detail.m_7360_().isEmpty()) {
                detail.m_130946_(", ");
            }
            detail.m_7220_((Component)Component.m_237115_((String)("challenge.spiral_abyss.slot." + slot.m_20751_())));
        }
        Integer amount = this.type == Type.TIME ? (this.target % 60 == 0 ? Integer.toString(this.target / 60) : String.format(Locale.ROOT, "%.2f", (double)this.target / 60.0)) : (Challenge.counted(this.type) ? (waveEnemies >= 0 ? Integer.valueOf(this.requiredCount(waveEnemies)) : String.format(Locale.ROOT, "%.2f%%", this.percentage)) : Integer.valueOf(this.target));
        boolean singular = Challenge.counted(this.type) && waveEnemies >= 0 && this.requiredCount(waveEnemies) == 1;
        String suffix = singular || this.type == Type.TIME && this.target == 60 ? ".one" : "";
        return Component.m_237110_((String)("challenge.spiral_abyss." + this.type.key() + suffix), (Object[])new Object[]{amount, detail});
    }

    private Component registryName(ResourceLocation id) {
        return switch (this.type) {
            case Type.ARMOR_ENCHANTMENT, Type.WEAPON_ENCHANTMENT -> {
                if (AbyssRegistries.ENCHANTMENTS.containsKey(id)) {
                    yield Component.m_237115_((String)AbyssRegistries.ENCHANTMENTS.getValue(id).m_44704_());
                }
                yield Component.m_237113_((String)id.toString());
            }
            case Type.WEAPON -> {
                if (AbyssRegistries.ITEMS.containsKey(id)) {
                    yield AbyssRegistries.ITEMS.getValue(id).m_41466_();
                }
                yield Component.m_237113_((String)id.toString());
            }
            case Type.PLAYER_EFFECT, Type.MOB_EFFECT -> {
                if (AbyssRegistries.MOB_EFFECTS.containsKey(id)) {
                    yield AbyssRegistries.MOB_EFFECTS.getValue(id).m_19482_();
                }
                yield Component.m_237113_((String)id.toString());
            }
            case Type.PROJECTILE -> {
                if (AbyssRegistries.ENTITY_TYPES.containsKey(id)) {
                    yield AbyssRegistries.ENTITY_TYPES.getValue(id).m_20676_();
                }
                yield Component.m_237113_((String)id.toString());
            }
            default -> Component.m_237113_((String)id.toString());
        };
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.m_128359_("Type", this.type.name());
        tag.m_128359_("Difficulty", this.difficulty.name());
        tag.m_128405_("Target", this.target);
        tag.m_128347_("Percentage", this.percentage);
        ListTag entries = new ListTag();
        this.ids.forEach(id -> entries.add((Object)StringTag.m_129297_((String)id.toString())));
        tag.m_128365_("Ids", (Tag)entries);
        ListTag slots = new ListTag();
        this.forbiddenSlots.forEach(slot -> slots.add((Object)StringTag.m_129297_((String)slot.m_20751_())));
        tag.m_128365_("Slots", (Tag)slots);
        return tag;
    }

    public static Challenge load(CompoundTag tag) {
        ArrayList<ResourceLocation> ids = new ArrayList<ResourceLocation>();
        for (Object id : tag.m_128437_("Ids", 8)) {
            ResourceLocation parsed = ResourceLocation.m_135820_((String)id.m_7916_());
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid saved challenge ID");
            }
            ids.add(parsed);
        }
        ArrayList<EquipmentSlot> slots = new ArrayList<EquipmentSlot>();
        for (Tag slot : tag.m_128437_("Slots", 8)) {
            slots.add(EquipmentSlot.m_20747_((String)slot.m_7916_()));
        }
        Type type = Type.valueOf(tag.m_128461_("Type"));
        Difficulty difficulty = Difficulty.valueOf(tag.m_128461_("Difficulty"));
        return new Challenge(type, difficulty, tag.m_128451_("Target"), tag.m_128441_("Percentage") ? tag.m_128459_("Percentage") : (Challenge.counted(type) ? Challenge.defaultPercentage(difficulty) : 0.0), ids, slots);
    }

    public static enum Type {
        TIME,
        NO_ARMOR,
        TAKE_DAMAGE,
        ARMOR_ENCHANTMENT,
        WEAPON_ENCHANTMENT,
        WEAPON,
        PROJECTILE,
        PLAYER_EFFECT,
        MOB_EFFECT,
        EXPLOSION,
        PLAYER_BURNING,
        MOB_BURNING,
        FISHING,
        TOTEM,
        TRICKSHOT,
        ENDER_PEARL_HIT,
        SHIELD_BLOCK;


        public String key() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public static enum Difficulty {
        EASY,
        MEDIUM,
        HARD;


        public String key() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}

