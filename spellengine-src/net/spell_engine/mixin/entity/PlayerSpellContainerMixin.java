/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.Player
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.spell_engine.mixin.entity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import net.spell_engine.internals.container.SpellContainerSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Player.class})
public class PlayerSpellContainerMixin
implements SpellContainerSource.Owner {
    private final Map<String, List<SpellContainerSource.SourcedContainer>> spellContainerCache = new LinkedHashMap<String, List<SpellContainerSource.SourcedContainer>>();
    private final Map<ResourceLocation, List<Spell.Modifier>> spellModifierCache = new HashMap<ResourceLocation, List<Spell.Modifier>>();
    private LinkedHashMap<String, SpellContainer> serverSideSpellContainers = new LinkedHashMap();
    private boolean serverSideSpellContainersDirty = false;
    private SpellContainerSource.Result currentSpellContainers = SpellContainerSource.Result.EMPTY;
    private SpellContainer lastMainHandContainer = SpellContainer.EMPTY;
    public Map<String, Object> lastProviderStates = new HashMap<String, Object>();

    @Override
    public Map<String, List<SpellContainerSource.SourcedContainer>> spellContainerCache() {
        return this.spellContainerCache;
    }

    @Override
    public Map<ResourceLocation, List<Spell.Modifier>> spellModifierCache() {
        return this.spellModifierCache;
    }

    @Override
    public LinkedHashMap<String, SpellContainer> serverSideSpellContainers() {
        return this.serverSideSpellContainers;
    }

    @Override
    public void markServerSideSpellContainersDirty() {
        this.serverSideSpellContainersDirty = true;
    }

    @Override
    public void setSpellContainers(SpellContainerSource.Result result) {
        this.currentSpellContainers = result;
    }

    @Override
    public SpellContainerSource.Result getSpellContainers() {
        return this.currentSpellContainers;
    }

    @Inject(method={"m_8119_"}, at={@At(value="TAIL")})
    private void tick_TAIL_SpellEngine_SpellContainer(CallbackInfo ci) {
        SpellContainer mainHandContainer;
        Player player = (Player)this;
        if (this.serverSideSpellContainersDirty) {
            SpellContainerSource.syncServerSideContainers(player);
            this.serverSideSpellContainersDirty = false;
        }
        if (!Objects.equals(mainHandContainer = SpellContainerHelper.containerFromItemStack(player.m_21205_()), this.lastMainHandContainer)) {
            SpellContainerSource.setDirty(player, SpellContainerSource.MAIN_HAND);
        }
        for (SpellContainerSource.Entry entry : SpellContainerSource.sources) {
            if (entry.checker() == null) continue;
            Object currentState = entry.checker().current(player);
            if (!currentState.equals(this.lastProviderStates.get(entry.name()))) {
                SpellContainerSource.setDirty(player, entry.name());
            }
            this.lastProviderStates.put(entry.name(), currentState);
        }
        SpellContainerSource.update(player);
        this.lastMainHandContainer = mainHandContainer;
    }
}

