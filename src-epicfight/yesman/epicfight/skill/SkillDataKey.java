/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 *  net.minecraft.core.IdMapper
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraftforge.registries.IForgeRegistry$BakeCallback
 *  net.minecraftforge.registries.IForgeRegistry$ClearCallback
 *  net.minecraftforge.registries.IForgeRegistry$CreateCallback
 *  net.minecraftforge.registries.IForgeRegistryInternal
 *  net.minecraftforge.registries.RegistryManager
 */
package yesman.epicfight.skill;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.IdMapper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryInternal;
import net.minecraftforge.registries.RegistryManager;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.api.utils.PacketBufferCodec;
import yesman.epicfight.api.utils.datastruct.ClearableIdMapper;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillDataKeys;

public class SkillDataKey<T> {
    private static final HashMultimap<Class<?>, SkillDataKey<?>> SKILL_DATA_KEYS = HashMultimap.create();
    private static final ResourceLocation CLASS_TO_DATA_KEYS = EpicFightMod.identifier("classtodatakeys");
    private static final ResourceLocation DATA_KEY_TO_ID = EpicFightMod.identifier("datakeytoid");
    private final PacketBufferCodec<T> packetCodec;
    private final T defaultValue;
    private final boolean syncronizeTrackingPlayers;

    public static SkillDataKeyCallbacks getRegistryCallback() {
        return SkillDataKeyCallbacks.INSTANCE;
    }

    public static <T> SkillDataKey<T> createSkillDataKey(PacketBufferCodec<T> packetCodec, T defaultValue, Class<?> ... skillClass) {
        return SkillDataKey.createSkillDataKey(packetCodec, defaultValue, false, skillClass);
    }

    public static <T> SkillDataKey<T> createSkillDataKey(PacketBufferCodec<T> packetCodec, T defaultValue, boolean syncronizeTrackingPlayers, Class<?> ... skillClass) {
        SkillDataKey<T> key = new SkillDataKey<T>(packetCodec, defaultValue, syncronizeTrackingPlayers);
        for (Class<?> cls : skillClass) {
            SKILL_DATA_KEYS.put(cls, key);
        }
        return key;
    }

    public static IdMapper<SkillDataKey<?>> getIdMap() {
        return (IdMapper)SkillDataKeys.REGISTRY.get().getSlaveMap(DATA_KEY_TO_ID, IdMapper.class);
    }

    public static Map<Class<?>, Set<SkillDataKey<?>>> getSkillDataKeyMap() {
        return (Map)SkillDataKeys.REGISTRY.get().getSlaveMap(CLASS_TO_DATA_KEYS, Map.class);
    }

    public SkillDataKey(PacketBufferCodec<T> packetCodec, T defaultValue, boolean syncronizeTrackingPlayers) {
        this.packetCodec = packetCodec;
        this.defaultValue = defaultValue;
        this.syncronizeTrackingPlayers = syncronizeTrackingPlayers;
    }

    public T readFromBuffer(FriendlyByteBuf buffer) {
        return this.packetCodec.decode(buffer);
    }

    public void writeToBuffer(FriendlyByteBuf buffer, T value) {
        this.packetCodec.encode(value, buffer);
    }

    public T defaultValue() {
        return this.defaultValue;
    }

    public int getId() {
        return SkillDataKey.getIdMap().m_7447_((Object)this);
    }

    public boolean syncronizeToTrackingPlayers() {
        return this.syncronizeTrackingPlayers;
    }

    private static class SkillDataKeyCallbacks
    implements IForgeRegistry.BakeCallback<SkillDataKey<?>>,
    IForgeRegistry.CreateCallback<SkillDataKey<?>>,
    IForgeRegistry.ClearCallback<SkillDataKey<?>> {
        static final SkillDataKeyCallbacks INSTANCE = new SkillDataKeyCallbacks();

        private SkillDataKeyCallbacks() {
        }

        public void onBake(IForgeRegistryInternal<SkillDataKey<?>> owner, RegistryManager stage) {
            ClearableIdMapper skillDataKeyMap = (ClearableIdMapper)((Object)owner.getSlaveMap(DATA_KEY_TO_ID, ClearableIdMapper.class));
            owner.forEach(arg_0 -> ((ClearableIdMapper)skillDataKeyMap).m_122667_(arg_0));
            Map skillDataKeys = (Map)owner.getSlaveMap(CLASS_TO_DATA_KEYS, Map.class);
            SkillManager.getSkillRegistry().forEach(skill -> {
                Class<?> skillClass = skill.getClass();
                HashSet dataKeySet = Sets.newHashSet();
                skillDataKeys.put(skillClass, dataKeySet);
                do {
                    if (!SKILL_DATA_KEYS.containsKey(skillClass)) continue;
                    dataKeySet.addAll(SKILL_DATA_KEYS.get(skillClass));
                } while (Skill.class.isAssignableFrom(skillClass = skillClass.getSuperclass()));
                if (!dataKeySet.isEmpty()) {
                    EpicFightMod.LOGGER.info("Data keys " + String.valueOf(dataKeySet.stream().map(arg_0 -> SkillDataKeys.REGISTRY.get().getKey(arg_0)).toList()) + " for " + String.valueOf(skill.getRegistryName()));
                }
            });
        }

        public void onCreate(IForgeRegistryInternal<SkillDataKey<?>> owner, RegistryManager stage) {
            owner.setSlaveMap(CLASS_TO_DATA_KEYS, (Object)Maps.newHashMap());
            owner.setSlaveMap(DATA_KEY_TO_ID, new ClearableIdMapper(owner.getKeys().size()));
        }

        public void onClear(IForgeRegistryInternal<SkillDataKey<?>> owner, RegistryManager stage) {
            ((Map)owner.getSlaveMap(CLASS_TO_DATA_KEYS, Map.class)).clear();
            ((ClearableIdMapper)((Object)owner.getSlaveMap(DATA_KEY_TO_ID, ClearableIdMapper.class))).clear();
        }
    }
}

