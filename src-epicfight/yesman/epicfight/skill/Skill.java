/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.ai.attributes.Attribute
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.CreativeModeTab
 *  net.minecraft.world.item.ItemStack
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.api.distmarker.OnlyIn
 *  net.minecraftforge.registries.ForgeRegistries
 */
package yesman.epicfight.skill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPSkillRequest;
import yesman.epicfight.network.server.SPSetSkillContainerValue;
import yesman.epicfight.network.server.SPSkillExecutionFeedback;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.modules.ChargeableSkill;
import yesman.epicfight.skill.modules.HoldableSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;
import yesman.epicfight.world.entity.eventlistener.SkillCancelEvent;
import yesman.epicfight.world.entity.eventlistener.SkillCastEvent;

public abstract class Skill {
    private final Map<Attribute, AttributeModifier> attributes = new HashMap<Attribute, AttributeModifier>();
    protected final ResourceLocation registryName;
    protected final SkillCategory category;
    protected final CreativeModeTab creativeTab;
    protected final ActivateType activateType;
    protected final Resource resource;
    protected float consumption;
    protected int maxDuration;
    protected int maxStackSize;

    public static SkillBuilder<Skill> createBuilder() {
        return new SkillBuilder<Skill>();
    }

    public static SkillBuilder<Skill> createIdentityBuilder() {
        return ((SkillBuilder)new SkillBuilder().setCategory(SkillCategories.IDENTITY)).setResource(Resource.NONE);
    }

    public static SkillBuilder<Skill> createMoverBuilder() {
        return ((SkillBuilder)new SkillBuilder().setCategory(SkillCategories.MOVER)).setResource(Resource.STAMINA);
    }

    public Skill(SkillBuilder<? extends Skill> builder) {
        if (builder.registryName == null) {
            IllegalArgumentException e = new IllegalArgumentException("No registry name is given for " + this.getClass().getCanonicalName());
            e.printStackTrace();
        }
        this.registryName = builder.registryName;
        this.category = builder.category;
        this.creativeTab = builder.tab;
        this.activateType = builder.activateType;
        this.resource = builder.resource;
    }

    public void setParams(CompoundTag parameters) {
        this.consumption = parameters.m_128457_("consumption");
        this.maxDuration = parameters.m_128451_("max_duration");
        this.maxStackSize = parameters.m_128441_("max_stacks") ? parameters.m_128451_("max_stacks") : 1;
        this.attributes.clear();
        if (parameters.m_128441_("attribute_modifiers")) {
            ListTag attributeList = parameters.m_128437_("attribute_modifiers", 10);
            for (Tag tag : attributeList) {
                CompoundTag comp = (CompoundTag)tag;
                String attribute = comp.m_128461_("attribute");
                Attribute attr = (Attribute)ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.parse((String)attribute));
                AttributeModifier modifier = ParseUtil.toAttributeModifier(comp);
                this.attributes.put(attr, modifier);
            }
        }
    }

    public boolean isExecutableState(PlayerPatch<?> executor) {
        return !((Player)executor.getOriginal()).m_5833_() && !executor.isInAir() && executor.getEntityState().canUseSkill();
    }

    public boolean canExecute(SkillContainer container) {
        return this.checkExecuteCondition(container);
    }

    public boolean checkExecuteCondition(SkillContainer container) {
        return true;
    }

    @OnlyIn(value=Dist.CLIENT)
    public void validationFeedback(SkillContainer container) {
    }

    @OnlyIn(value=Dist.CLIENT)
    public Object getExecutionPacket(SkillContainer container, FriendlyByteBuf args) {
        return new CPSkillRequest(container.getSlot(), CPSkillRequest.WorkType.CAST, args);
    }

    @OnlyIn(value=Dist.CLIENT)
    public FriendlyByteBuf gatherArguments(SkillContainer container, ControlEngine controlEngine) {
        return null;
    }

    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        SPSkillExecutionFeedback feedbackPacket = SPSkillExecutionFeedback.executed(container.getSlotId());
        ServerPlayerPatch executor = container.getServerExecutor();
        if (executor.isHoldingAny()) {
            if (executor.getHoldingSkill() instanceof ChargeableSkill) {
                feedbackPacket.getBuffer().writeInt(executor.getAccumulatedChargeAmount());
            }
            if (executor.getHoldingSkill() == this) {
                executor.getHoldingSkill().onStopHolding(container, feedbackPacket);
            }
            executor.resetHolding();
        } else {
            container.activate();
        }
        EpicFightNetworkManager.sendToPlayer(feedbackPacket, (ServerPlayer)executor.getOriginal(), new Object[0]);
    }

    public void cancelOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executor = container.getServerExecutor();
        SkillCancelEvent skillCancelEvent = new SkillCancelEvent(executor, container);
        executor.getEventListener().triggerEvents(PlayerEventListener.EventType.SKILL_CANCEL_EVENT, skillCancelEvent);
        EpicFightNetworkManager.sendToPlayer(SPSkillExecutionFeedback.expired(container.getSlotId()), (ServerPlayer)executor.getOriginal(), new Object[0]);
    }

    public final float getDefaultConsumptionAmount(PlayerPatch<?> executor) {
        return switch (this.resource) {
            case Resource.STAMINA -> executor.getModifiedStaminaConsume(this.consumption);
            case Resource.WEAPON_CHARGE, Resource.COOLDOWN -> 1.0f;
            default -> 0.0f;
        };
    }

    @OnlyIn(value=Dist.CLIENT)
    public void executeOnClient(SkillContainer container, FriendlyByteBuf args) {
    }

    @OnlyIn(value=Dist.CLIENT)
    public void cancelOnClient(SkillContainer container, FriendlyByteBuf args) {
        LocalPlayerPatch executor = container.getClientExecutor();
        SkillCancelEvent skillCancelEvent = new SkillCancelEvent(executor, container);
        executor.getEventListener().triggerEvents(PlayerEventListener.EventType.SKILL_CANCEL_EVENT, skillCancelEvent);
    }

    public void onTracked(SkillContainer container, EpicFightNetworkManager.PayloadBundleBuilder payloadBuilder) {
    }

    public void onInitiate(SkillContainer container) {
        container.maxDuration = this.maxDuration;
        for (Map.Entry<Attribute, AttributeModifier> stat : this.attributes.entrySet()) {
            AttributeInstance attr = ((Player)container.getExecutor().getOriginal()).m_21051_(stat.getKey());
            if (attr.m_22109_(stat.getValue())) continue;
            attr.m_22118_(stat.getValue());
        }
    }

    @OnlyIn(value=Dist.CLIENT)
    public void onInitiateClient(SkillContainer container) {
    }

    @OnlyIn(value=Dist.CLIENT)
    public void onRemoveClient(SkillContainer container) {
    }

    public void onRemoved(SkillContainer container) {
        for (Map.Entry<Attribute, AttributeModifier> stat : this.attributes.entrySet()) {
            AttributeInstance attr = ((Player)container.getExecutor().getOriginal()).m_21051_(stat.getKey());
            if (!attr.m_22109_(stat.getValue())) continue;
            attr.m_22130_(stat.getValue());
        }
    }

    public void onReset(SkillContainer container) {
    }

    public void setConsumption(SkillContainer container, float value) {
        container.resource = Mth.m_14036_((float)value, (float)0.0f, (float)container.getMaxResource());
        if (value >= container.getMaxResource()) {
            if (container.stack < this.maxStackSize) {
                ++container.stack;
                container.resource = 0.0f;
                container.prevResource = 0.0f;
            } else {
                container.resource = container.getMaxResource();
                container.prevResource = container.getMaxResource();
            }
        } else if (value == 0.0f && container.stack > 0) {
            --container.stack;
        }
    }

    public void updateContainer(SkillContainer container) {
        container.prevResource = container.resource;
        container.prevDuration = container.duration;
        if (this.resource == Resource.COOLDOWN && container.stack < this.maxStackSize) {
            container.setResource(container.resource + this.getCooldownRegenPerSecond(container.getExecutor()) * 0.05f);
        }
        if (container.isActivated()) {
            if (this.activateType == ActivateType.DURATION) {
                --container.duration;
            }
            boolean isEnd = false;
            if (this.activateType == ActivateType.TOGGLE) {
                if (container.stack <= 0 && !((Player)container.getExecutor().getOriginal()).m_7500_()) {
                    isEnd = true;
                }
            } else if (this.activateType != ActivateType.HELD && container.duration <= 0) {
                isEnd = true;
            }
            if (isEnd) {
                if (!container.getExecutor().isLogicalClient() && this.activateType != ActivateType.HELD) {
                    this.cancelOnServer(container, null);
                }
                container.deactivate();
            }
        }
        if (this.activateType == ActivateType.HELD && container.getExecutor().getHoldingSkill() == this) {
            HoldableSkill holdableSkill = (HoldableSkill)((Object)this);
            holdableSkill.holdTick(container);
            if (!container.getExecutor().isLogicalClient()) {
                container.getExecutor().resetActionTick();
                Skill skill = this;
                if (skill instanceof ChargeableSkill) {
                    ChargeableSkill chargingSkill = (ChargeableSkill)((Object)skill);
                    if (container.getExecutor().getSkillChargingTicks(1.0f) > (float)chargingSkill.getAllowedMaxChargingTicks()) {
                        SPSkillExecutionFeedback feedbackPacket = SPSkillExecutionFeedback.executed(container.getSlotId());
                        feedbackPacket.getBuffer().writeInt(container.getExecutor().getAccumulatedChargeAmount());
                        chargingSkill.onStopHolding(container, feedbackPacket);
                        container.getExecutor().resetHolding();
                        EpicFightNetworkManager.sendToPlayer(feedbackPacket, (ServerPlayer)container.getServerExecutor().getOriginal(), new Object[0]);
                    }
                }
            }
        }
    }

    public boolean isActivated(SkillContainer container) {
        return this.equals(container.getSkill()) ? container.isActivated() : false;
    }

    public boolean isDisabled(SkillContainer container) {
        return this.equals(container.getSkill()) ? container.isDisabled() : true;
    }

    public void setConsumptionSynchronize(SkillContainer container, float amount) {
        if (this.equals(container.containingSkill)) {
            Skill.setSkillConsumptionSynchronize(container, amount);
        }
    }

    public void setMaxDurationSynchronize(SkillContainer container, int amount) {
        if (this.equals(container.containingSkill)) {
            Skill.setSkillMaxDurationSynchronize(container, amount);
        }
    }

    public void setDurationSynchronize(SkillContainer container, int amount) {
        if (this.equals(container.containingSkill)) {
            Skill.setSkillDurationSynchronize(container, amount);
        }
    }

    public void setStackSynchronize(SkillContainer container, int amount) {
        if (this.equals(container.containingSkill)) {
            Skill.setSkillStackSynchronize(container, amount);
        }
    }

    public void setMaxResourceSynchronize(SkillContainer container, float amount) {
        if (this.equals(container.containingSkill)) {
            Skill.setSkillMaxResourceSynchronize(container, amount);
        }
    }

    public static void setSkillConsumptionSynchronize(SkillContainer skillContainer, float fVal) {
        skillContainer.setResource(fVal);
        EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.resource(skillContainer.getSlot(), fVal, ((Player)skillContainer.getExecutor().getOriginal()).m_19879_()), (ServerPlayer)skillContainer.getServerExecutor().getOriginal(), new Object[0]);
    }

    public static void setSkillDurationSynchronize(SkillContainer skillContainer, int iVal) {
        skillContainer.setDuration(iVal);
        EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.duration(skillContainer.getSlot(), iVal, ((Player)skillContainer.getExecutor().getOriginal()).m_19879_()), (ServerPlayer)skillContainer.getServerExecutor().getOriginal(), new Object[0]);
    }

    public static void setSkillMaxDurationSynchronize(SkillContainer skillContainer, int iVal) {
        skillContainer.setMaxDuration(iVal);
        EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.maxDuration(skillContainer.getSlot(), iVal, ((Player)skillContainer.getExecutor().getOriginal()).m_19879_()), (ServerPlayer)skillContainer.getServerExecutor().getOriginal(), new Object[0]);
    }

    public static void setSkillStackSynchronize(SkillContainer skillContainer, int iVal) {
        skillContainer.setStack(iVal);
        EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.stacks(skillContainer.getSlot(), iVal, ((Player)skillContainer.getExecutor().getOriginal()).m_19879_()), (ServerPlayer)skillContainer.getServerExecutor().getOriginal(), new Object[0]);
    }

    public static void setSkillMaxResourceSynchronize(SkillContainer skillContainer, float fVal) {
        skillContainer.setMaxResource(fVal);
        EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.maxResource(skillContainer.getSlot(), fVal, ((Player)skillContainer.getExecutor().getOriginal()).m_19879_()), (ServerPlayer)skillContainer.getServerExecutor().getOriginal(), new Object[0]);
    }

    public ResourceLocation getRegistryName() {
        return this.registryName;
    }

    public String getTranslationKey() {
        return String.format("skill.%s.%s", this.getRegistryName().m_135827_(), this.getRegistryName().m_135815_());
    }

    public float getCooldownRegenPerSecond(PlayerPatch<?> playerpatch) {
        return 1.0f;
    }

    public SkillCategory getCategory() {
        return this.category;
    }

    public CreativeModeTab getCreativeTab() {
        return this.creativeTab;
    }

    public int getMaxStack() {
        return this.maxStackSize;
    }

    public int getMaxDuration() {
        return this.maxDuration;
    }

    public float getConsumption() {
        return this.consumption;
    }

    public Set<Map.Entry<Attribute, AttributeModifier>> getModfierEntry() {
        return this.attributes.entrySet();
    }

    public boolean resourcePredicate(PlayerPatch<?> playerpatch, SkillCastEvent event) {
        return playerpatch.consumeForSkill(this, this.resource, event.getArguments());
    }

    public boolean shouldDeactivateAutomatically(PlayerPatch<?> executor) {
        return !((Player)executor.getOriginal()).m_7500_();
    }

    public ActivateType getActivateType() {
        return this.activateType;
    }

    public Resource getResourceType() {
        return this.resource;
    }

    public Skill getPriorSkill() {
        return null;
    }

    public Skill registerPropertiesToAnimation() {
        return this;
    }

    @OnlyIn(value=Dist.CLIENT)
    public void onScreen(LocalPlayerPatch playerpatch, float resolutionX, float resolutionY) {
    }

    @OnlyIn(value=Dist.CLIENT)
    public List<Component> getTooltipOnItem(ItemStack itemStack, CapabilityItem cap, PlayerPatch<?> playerpatch) {
        return new ArrayList<Component>();
    }

    @OnlyIn(value=Dist.CLIENT)
    public List<Object> getTooltipArgsOfScreen(List<Object> list) {
        return list;
    }

    @OnlyIn(value=Dist.CLIENT)
    public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
    }

    @OnlyIn(value=Dist.CLIENT)
    public ResourceLocation getSkillTexture() {
        return ResourceLocation.fromNamespaceAndPath((String)this.getRegistryName().m_135827_(), (String)String.format("textures/gui/skills/%s/%s.png", this.category.toString().toLowerCase(Locale.ROOT), this.getRegistryName().m_135815_()));
    }

    @OnlyIn(value=Dist.CLIENT)
    public boolean shouldDraw(SkillContainer container) {
        return false;
    }

    public String toString() {
        return this.getRegistryName().toString();
    }

    public Component getDisplayName() {
        return Component.m_237115_((String)String.format("%s.%s.%s", "skill", this.getRegistryName().m_135827_(), this.getRegistryName().m_135815_()));
    }

    public Set<WeaponCategory> getAvailableWeaponCategories() {
        return null;
    }

    @OnlyIn(value=Dist.CLIENT)
    public boolean getCustomConsumptionTooltips(SkillBookScreen.AttributeIconList consumeIconList) {
        return false;
    }

    public static enum Resource {
        NONE((skillContainer, playerpatch, amount) -> true, (skillContainer, playerpatch, amount) -> {}),
        WEAPON_CHARGE((skillContainer, playerpatch, amount) -> (float)skillContainer.getStack() >= amount, (skillContainer, playerpatch, amount) -> Skill.setSkillStackSynchronize(skillContainer, skillContainer.getStack() - 1)),
        COOLDOWN((skillContainer, playerpatch, amount) -> (float)skillContainer.getStack() >= amount, (skillContainer, playerpatch, amount) -> Skill.setSkillStackSynchronize(skillContainer, skillContainer.getStack() - 1)),
        STAMINA((skillContainer, playerpatch, amount) -> playerpatch.hasStamina(amount), (skillContainer, playerpatch, amount) -> {
            playerpatch.resetActionTick();
            playerpatch.setStamina(playerpatch.getStamina() - amount);
        }),
        HEALTH((skillContainer, playerpatch, amount) -> ((Player)playerpatch.getOriginal()).m_21223_() > amount, (skillContainer, playerpatch, amount) -> ((ServerPlayer)playerpatch.getOriginal()).m_21153_(((ServerPlayer)playerpatch.getOriginal()).m_21223_() - amount));

        public final ResourcePredicate predicate;
        public final ResourceConsumer consumer;

        private Resource(ResourcePredicate predicate, ResourceConsumer consumer) {
            this.predicate = predicate;
            this.consumer = consumer;
        }

        @FunctionalInterface
        public static interface ResourcePredicate {
            public boolean canExecute(SkillContainer var1, PlayerPatch<?> var2, float var3);
        }

        @FunctionalInterface
        public static interface ResourceConsumer {
            public void consume(SkillContainer var1, ServerPlayerPatch var2, float var3);
        }
    }

    public static enum ActivateType {
        ONE_SHOT,
        DURATION,
        DURATION_INFINITE,
        TOGGLE,
        HELD;

    }
}

