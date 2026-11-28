/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.datafixers.util.Pair
 *  io.netty.util.internal.StringUtil
 *  net.minecraft.core.particles.ParticleType
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.nbt.TagParser
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.util.profiling.ProfilerFiller
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.api.distmarker.OnlyIn
 *  net.minecraftforge.eventbus.api.Event
 *  net.minecraftforge.fml.ModLoader
 *  net.minecraftforge.registries.ForgeRegistries
 *  org.jetbrains.annotations.Nullable
 */
package yesman.epicfight.world.capabilities.item;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import io.netty.util.internal.StringUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.EpicFightConditions;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.server.SPDatapackSync;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponCapabilityPresets;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.capabilities.provider.ExtraEntryProvider;

public class WeaponTypeReloadListener
extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "capabilities/weapons/types";
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceLocation, Function<Item, CapabilityItem.Builder>> PRESETS = Maps.newHashMap();
    private static final Map<ResourceLocation, CompoundTag> CAPABILITY_COMPOUNDS = Maps.newHashMap();

    public static void registerDefaultWeaponTypes() {
        HashMap typeEntry = Maps.newHashMap();
        typeEntry.put(EpicFightMod.identifier("axe"), WeaponCapabilityPresets.AXE);
        typeEntry.put(EpicFightMod.identifier("fist"), WeaponCapabilityPresets.FIST);
        typeEntry.put(EpicFightMod.identifier("hoe"), WeaponCapabilityPresets.HOE);
        typeEntry.put(EpicFightMod.identifier("pickaxe"), WeaponCapabilityPresets.PICKAXE);
        typeEntry.put(EpicFightMod.identifier("shovel"), WeaponCapabilityPresets.SHOVEL);
        typeEntry.put(EpicFightMod.identifier("sword"), WeaponCapabilityPresets.SWORD);
        typeEntry.put(EpicFightMod.identifier("spear"), WeaponCapabilityPresets.SPEAR);
        typeEntry.put(EpicFightMod.identifier("greatsword"), WeaponCapabilityPresets.GREATSWORD);
        typeEntry.put(EpicFightMod.identifier("uchigatana"), WeaponCapabilityPresets.UCHIGATANA);
        typeEntry.put(EpicFightMod.identifier("tachi"), WeaponCapabilityPresets.TACHI);
        typeEntry.put(EpicFightMod.identifier("longsword"), WeaponCapabilityPresets.LONGSWORD);
        typeEntry.put(EpicFightMod.identifier("dagger"), WeaponCapabilityPresets.DAGGER);
        typeEntry.put(EpicFightMod.identifier("bow"), WeaponCapabilityPresets.BOW);
        typeEntry.put(EpicFightMod.identifier("crossbow"), WeaponCapabilityPresets.CROSSBOW);
        typeEntry.put(EpicFightMod.identifier("trident"), WeaponCapabilityPresets.TRIDENT);
        typeEntry.put(EpicFightMod.identifier("shield"), WeaponCapabilityPresets.SHIELD);
        WeaponCapabilityPresetRegistryEvent weaponCapabilityPresetRegistryEvent = new WeaponCapabilityPresetRegistryEvent(typeEntry);
        ModLoader.get().postEvent((Event)weaponCapabilityPresetRegistryEvent);
        PRESETS.putAll(weaponCapabilityPresetRegistryEvent.getTypeEntry());
    }

    public WeaponTypeReloadListener() {
        super(GSON, DIRECTORY);
    }

    protected void apply(Map<ResourceLocation, JsonElement> packEntry, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        WeaponTypeReloadListener.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : packEntry.entrySet()) {
            CompoundTag compTag = null;
            try {
                compTag = TagParser.m_129359_((String)entry.getValue().toString());
            }
            catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
            try {
                CompoundTag comptagFinal = compTag;
                PRESETS.put(entry.getKey(), itemstack -> WeaponTypeReloadListener.deserializeWeaponCapabilityBuilder((ResourceLocation)entry.getKey(), comptagFinal));
                CAPABILITY_COMPOUNDS.put(entry.getKey(), compTag);
            }
            catch (Exception e) {
                EpicFightMod.LOGGER.warn("Error while deserializing weapon type datapack: " + String.valueOf(entry.getKey()));
                e.printStackTrace();
            }
        }
    }

    public static Function<Item, CapabilityItem.Builder> getOrThrow(String typeName) {
        ResourceLocation rl = ResourceLocation.parse((String)typeName);
        if (!PRESETS.containsKey(rl)) {
            throw new IllegalArgumentException("Can't find weapon type: " + String.valueOf(rl));
        }
        return PRESETS.get(rl);
    }

    public static Function<Item, CapabilityItem.Builder> get(String typeName) {
        return WeaponTypeReloadListener.get(ResourceLocation.parse((String)typeName));
    }

    public static Function<Item, CapabilityItem.Builder> get(ResourceLocation typeName) {
        return PRESETS.get(typeName);
    }

    public static void register(ResourceLocation rl, CapabilityItem.Builder builder) {
        PRESETS.put(rl, item -> builder);
    }

    public static WeaponCapability.Builder deserializeWeaponCapabilityBuilder(ResourceLocation rl, CompoundTag tag) {
        return WeaponTypeReloadListener.deserializeWeaponCapabilityBuilder(rl, tag, null);
    }

    @Deprecated
    public static WeaponCapability.Builder deserializeWeaponCapabilityBuilder(ResourceLocation rl, CompoundTag tag, @Nullable ExtraEntryProvider extraEntryProvider) {
        SoundEvent sound;
        WeaponCapability.Builder builder = WeaponCapability.builder();
        if (!tag.m_128441_("category") || StringUtil.isNullOrEmpty((String)tag.m_128461_("category"))) {
            throw new IllegalArgumentException("Define weapon category.");
        }
        builder.category(WeaponCategory.ENUM_MANAGER.getOrThrow(tag.m_128461_("category")));
        builder.collider(ColliderPreset.deserializeSimpleCollider(tag.m_128469_("collider")));
        builder.canBePlacedOffhand(tag.m_128441_("usable_in_offhand") ? tag.m_128471_("usable_in_offhand") : true);
        if (tag.m_128441_("hit_particle")) {
            ParticleType particleType = (ParticleType)ForgeRegistries.PARTICLE_TYPES.getValue(ResourceLocation.parse((String)tag.m_128461_("hit_particle")));
            if (particleType == null) {
                EpicFightMod.LOGGER.warn("Can't find a particle type " + tag.m_128461_("hit_particle") + " in " + String.valueOf(rl));
            } else if (!(particleType instanceof HitParticleType)) {
                EpicFightMod.LOGGER.warn(tag.m_128461_("hit_particle") + " is not a hit particle type in " + String.valueOf(rl));
            } else {
                builder.hitParticle((HitParticleType)particleType);
            }
        }
        if (tag.m_128441_("swing_sound")) {
            sound = (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse((String)tag.m_128461_("swing_sound")));
            if (sound == null) {
                EpicFightMod.LOGGER.warn("Can't find a swing sound " + tag.m_128461_("swing_sound") + " in " + String.valueOf(rl));
            } else {
                builder.swingSound(sound);
            }
        }
        if (tag.m_128441_("hit_sound")) {
            sound = (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse((String)tag.m_128461_("hit_sound")));
            if (sound == null) {
                EpicFightMod.LOGGER.warn("Can't find a hit sound " + tag.m_128461_("hit_sound") + " in " + String.valueOf(rl));
            } else {
                builder.hitSound(sound);
            }
        }
        CompoundTag combosTag = tag.m_128469_("combos");
        for (Object key : combosTag.m_128431_()) {
            Style style = Style.ENUM_MANAGER.getOrThrow((String)key);
            ListTag comboAnimations = combosTag.m_128437_((String)key, 8);
            ArrayList anims = new ArrayList();
            for (int i = 0; i < comboAnimations.size(); ++i) {
                AnimationManager.AnimationAccessor animation;
                String animId = comboAnimations.m_128778_(i);
                AnimationManager.AnimationAccessor animationAccessor = animation = extraEntryProvider == null ? AnimationManager.byKey(animId) : extraEntryProvider.getExtraOrBuiltInAnimation(animId);
                if (animation == null) {
                    EpicFightMod.LOGGER.warn("Can't find an animation named " + comboAnimations.m_128778_(i) + " in " + String.valueOf(rl));
                    continue;
                }
                anims.add(animation);
            }
            builder.newStyleCombo(style, anims.toArray(new AnimationManager.AnimationAccessor[0]));
        }
        CompoundTag innateSkillsTag = tag.m_128469_("innate_skills");
        for (Object key : innateSkillsTag.m_128431_()) {
            Style style = Style.ENUM_MANAGER.getOrThrow((String)key);
            builder.innateSkill(style, arg_0 -> WeaponTypeReloadListener.lambda$deserializeWeaponCapabilityBuilder$2(innateSkillsTag, (String)key, arg_0));
        }
        CompoundTag livingmotionModifierTag = tag.m_128469_("livingmotion_modifier");
        for (String sStyle : livingmotionModifierTag.m_128431_()) {
            Style style = Style.ENUM_MANAGER.getOrThrow(sStyle);
            CompoundTag styleAnimationTag = livingmotionModifierTag.m_128469_(sStyle);
            for (String sLivingmotion : styleAnimationTag.m_128431_()) {
                AnimationManager.AnimationAccessor animation;
                LivingMotion livingmotion = LivingMotion.ENUM_MANAGER.getOrThrow(sLivingmotion);
                String animId = styleAnimationTag.m_128461_(sLivingmotion);
                AnimationManager.AnimationAccessor animationAccessor = animation = extraEntryProvider == null ? AnimationManager.byKey(animId) : extraEntryProvider.getExtraOrBuiltInAnimation(animId);
                if (animation == null) {
                    EpicFightMod.LOGGER.warn("No animation named {}", (Object)styleAnimationTag.m_128461_(sLivingmotion));
                    continue;
                }
                builder.livingMotionModifier(style, livingmotion, animation);
            }
        }
        CompoundTag stylesTag = tag.m_128469_("styles");
        ArrayList conditions = Lists.newArrayList();
        Style defaultStyle = Style.ENUM_MANAGER.getOrThrow(stylesTag.m_128461_("default"));
        for (Tag caseTag : stylesTag.m_128437_("cases", 10)) {
            CompoundTag caseCompTag = (CompoundTag)caseTag;
            ArrayList conditionList = Lists.newArrayList();
            for (Tag offhandTag : caseCompTag.m_128437_("conditions", 10)) {
                CompoundTag offhandCompound = (CompoundTag)offhandTag;
                Supplier conditionProvider = EpicFightConditions.getConditionOrThrow(ResourceLocation.parse((String)offhandCompound.m_128461_("predicate")));
                Condition.EntityPatchCondition condition = (Condition.EntityPatchCondition)conditionProvider.get();
                condition.read(offhandCompound);
                conditionList.add(condition);
            }
            conditions.add(Pair.of(entitypatch -> {
                for (Condition.EntityPatchCondition condition : conditionList) {
                    if (condition.predicate(entitypatch)) continue;
                    return false;
                }
                return true;
            }, (Object)Style.ENUM_MANAGER.getOrThrow(caseCompTag.m_128461_("style"))));
        }
        builder.styleProvider(entitypatch -> {
            for (Pair entry : conditions) {
                if (!((Predicate)entry.getFirst()).test(entitypatch)) continue;
                return (Style)entry.getSecond();
            }
            return defaultStyle;
        });
        if (tag.m_128441_("offhand_item_compatible_predicate")) {
            ListTag offhandValidatorList = tag.m_128437_("offhand_item_compatible_predicate", 10);
            ArrayList conditionList = Lists.newArrayList();
            for (Tag offhandTag : offhandValidatorList) {
                CompoundTag offhandCompound = (CompoundTag)offhandTag;
                Supplier conditionProvider = EpicFightConditions.getConditionOrThrow(ResourceLocation.parse((String)offhandCompound.m_128461_("predicate")));
                Condition.EntityPatchCondition condition = (Condition.EntityPatchCondition)conditionProvider.get();
                condition.read(offhandCompound);
                conditionList.add(condition);
            }
            builder.weaponCombinationPredicator(entitypatch -> {
                for (Condition.EntityPatchCondition condition : conditionList) {
                    if (condition.predicate(entitypatch)) continue;
                    return false;
                }
                return true;
            });
        }
        if (tag.m_128441_("custom_tags")) {
            for (Tag customTag : tag.m_128437_("custom_tags", 8)) {
                builder.addTag(ResourceLocation.parse((String)customTag.m_7916_()));
            }
        }
        builder.addTag(rl);
        return builder;
    }

    public static int getTagCount() {
        return CAPABILITY_COMPOUNDS.size();
    }

    public static Stream<CompoundTag> getWeaponTypeDataStream() {
        Stream<CompoundTag> tagStream = CAPABILITY_COMPOUNDS.entrySet().stream().map(entry -> {
            ((CompoundTag)entry.getValue()).m_128359_("registry_name", ((ResourceLocation)entry.getKey()).toString());
            return (CompoundTag)entry.getValue();
        });
        return tagStream;
    }

    public static Set<Map.Entry<ResourceLocation, Function<Item, CapabilityItem.Builder>>> entries() {
        return PRESETS.entrySet();
    }

    public static void clear() {
        PRESETS.clear();
        WeaponTypeReloadListener.registerDefaultWeaponTypes();
    }

    @OnlyIn(value=Dist.CLIENT)
    public static void processServerPacket(SPDatapackSync packet) {
        if (packet.getType() == SPDatapackSync.Type.WEAPON_TYPE) {
            PRESETS.clear();
            WeaponTypeReloadListener.registerDefaultWeaponTypes();
            for (CompoundTag tag : packet.getTags()) {
                ResourceLocation rl = ResourceLocation.parse((String)tag.m_128461_("registry_name"));
                try {
                    PRESETS.put(rl, itemstack -> WeaponTypeReloadListener.deserializeWeaponCapabilityBuilder(rl, tag));
                }
                catch (Exception e) {
                    throw new RuntimeException("Weapon type " + String.valueOf(rl) + " encountered an error", e);
                }
            }
            ItemCapabilityReloadListener.weaponTypeProcessedCheck();
        }
    }

    private static /* synthetic */ Skill lambda$deserializeWeaponCapabilityBuilder$2(CompoundTag innateSkillsTag, String key, ItemStack itemstack) {
        return SkillManager.getSkill(innateSkillsTag.m_128461_(key));
    }
}

