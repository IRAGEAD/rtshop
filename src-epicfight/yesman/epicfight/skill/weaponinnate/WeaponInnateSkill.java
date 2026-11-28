/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.blaze3d.platform.GlStateManager$DestFactor
 *  com.mojang.blaze3d.platform.GlStateManager$SourceFactor
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.ai.attributes.Attribute
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.enchantment.Enchantments
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.api.distmarker.OnlyIn
 *  org.lwjgl.opengl.GL11
 */
package yesman.epicfight.skill.weaponinnate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.api.utils.math.Vec2f;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

public abstract class WeaponInnateSkill
extends Skill {
    protected List<Map<AnimationProperty.AttackPhaseProperty<?>, Object>> properties = Lists.newArrayList();
    private static final Vec2f[] CLOCK_POS = new Vec2f[]{new Vec2f(0.5f, 0.5f), new Vec2f(0.5f, 0.0f), new Vec2f(0.0f, 0.0f), new Vec2f(0.0f, 1.0f), new Vec2f(1.0f, 1.0f), new Vec2f(1.0f, 0.0f)};

    public static SkillBuilder<WeaponInnateSkill> createWeaponInnateBuilder() {
        return ((SkillBuilder)new SkillBuilder().setCategory(SkillCategories.WEAPON_INNATE)).setResource(Skill.Resource.WEAPON_CHARGE);
    }

    public WeaponInnateSkill(SkillBuilder<? extends WeaponInnateSkill> builder) {
        super(builder);
    }

    @Override
    public boolean canExecute(SkillContainer container) {
        ItemStack itemstack = ((Player)container.getExecutor().getOriginal()).m_21205_();
        return super.canExecute(container) && EpicFightCapabilities.getItemStackCapability(itemstack).getInnateSkill(container.getExecutor(), itemstack) == this && ((Player)container.getExecutor().getOriginal()).m_20202_() == null && (!this.isActivated(container) || this.activateType == Skill.ActivateType.TOGGLE);
    }

    @Override
    public List<Component> getTooltipOnItem(ItemStack itemstack, CapabilityItem cap, PlayerPatch<?> playerCap) {
        ArrayList list = Lists.newArrayList();
        String traslatableText = this.getTranslationKey();
        list.add(Component.m_237115_((String)traslatableText).m_130940_(ChatFormatting.WHITE).m_7220_((Component)Component.m_237113_((String)String.format("[%.0f]", Float.valueOf(this.consumption))).m_130940_(ChatFormatting.AQUA)));
        list.add(Component.m_237115_((String)(traslatableText + ".tooltip")).m_130940_(ChatFormatting.DARK_GRAY));
        return list;
    }

    protected void generateTooltipforPhase(List<Component> list, ItemStack itemstack, CapabilityItem itemcap, PlayerPatch<?> playerpatch, Map<AnimationProperty.AttackPhaseProperty<?>, Object> propertyMap, String title) {
        double weaponBaseDamage = playerpatch.getWeaponAttribute(Attributes.f_22281_, itemstack);
        double armorNegation = playerpatch.getWeaponAttribute((Attribute)EpicFightAttributes.ARMOR_NEGATION.get(), itemstack);
        double impact = playerpatch.getWeaponAttribute((Attribute)EpicFightAttributes.IMPACT.get(), itemstack);
        double maxStrikes = playerpatch.getWeaponAttribute((Attribute)EpicFightAttributes.MAX_STRIKES.get(), itemstack);
        ValueModifier.ResultCalculator damageModifier = ValueModifier.calculator();
        ValueModifier.ResultCalculator armorNegationModifier = ValueModifier.calculator();
        ValueModifier.ResultCalculator impactModifier = ValueModifier.calculator();
        ValueModifier.ResultCalculator maxStrikesModifier = ValueModifier.calculator();
        this.getProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, propertyMap).ifPresent(damageModifier::attach);
        this.getProperty(AnimationProperty.AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, propertyMap).ifPresent(armorNegationModifier::attach);
        this.getProperty(AnimationProperty.AttackPhaseProperty.IMPACT_MODIFIER, propertyMap).ifPresent(impactModifier::attach);
        this.getProperty(AnimationProperty.AttackPhaseProperty.MAX_STRIKES_MODIFIER, propertyMap).ifPresent(maxStrikesModifier::attach);
        impactModifier.multiply(1.0f + (float)itemstack.getEnchantmentLevel(Enchantments.f_44980_) * 0.12f);
        double fBaseDamage = weaponBaseDamage;
        weaponBaseDamage = damageModifier.getResult(playerpatch.getModifiedBaseDamage((float)weaponBaseDamage));
        armorNegation = armorNegationModifier.getResult((float)armorNegation);
        impact = impactModifier.getResult((float)impact);
        maxStrikes = maxStrikesModifier.getResult((float)maxStrikes);
        list.add((Component)Component.m_237113_((String)title).m_130940_(ChatFormatting.UNDERLINE).m_130940_(ChatFormatting.GRAY));
        MutableComponent damageComponent = Component.m_237110_((String)"damage_source.epicfight.damage", (Object[])new Object[]{Component.m_237113_((String)ItemStack.f_41584_.format(weaponBaseDamage)).m_130940_(ChatFormatting.RED)}).m_130940_(ChatFormatting.DARK_GRAY);
        this.getProperty(AnimationProperty.AttackPhaseProperty.EXTRA_DAMAGE, propertyMap).ifPresent(extraDamageSet -> extraDamageSet.forEach(extraDamage -> extraDamage.setTooltips(itemstack, damageComponent, fBaseDamage)));
        list.add((Component)damageComponent);
        if (armorNegation != 0.0) {
            list.add((Component)Component.m_237113_((String)(ItemStack.f_41584_.format(armorNegation) + "% ")).m_130940_(ChatFormatting.GOLD).m_7220_((Component)Component.m_237115_((String)((Attribute)EpicFightAttributes.ARMOR_NEGATION.get()).m_22087_()).m_130940_(ChatFormatting.DARK_GRAY)));
        }
        if (impact != 0.0) {
            list.add((Component)Component.m_237110_((String)(((Attribute)EpicFightAttributes.IMPACT.get()).m_22087_() + ".value"), (Object[])new Object[]{Component.m_237113_((String)ItemStack.f_41584_.format(impact)).m_130940_(ChatFormatting.AQUA)}).m_130940_(ChatFormatting.DARK_GRAY));
        }
        list.add((Component)Component.m_237110_((String)(((Attribute)EpicFightAttributes.MAX_STRIKES.get()).m_22087_() + ".value"), (Object[])new Object[]{Component.m_237113_((String)ItemStack.f_41584_.format(maxStrikes)).m_130940_(ChatFormatting.WHITE)}).m_130940_(ChatFormatting.DARK_GRAY));
        Optional<StunType> stunOption = this.getProperty(AnimationProperty.AttackPhaseProperty.STUN_TYPE, propertyMap);
        stunOption.ifPresent(stunType -> list.add((Component)Component.m_237115_((String)stunType.toString()).m_130940_(ChatFormatting.DARK_GRAY)));
        if (!stunOption.isPresent()) {
            list.add((Component)Component.m_237115_((String)StunType.SHORT.toString()).m_130940_(ChatFormatting.DARK_GRAY));
        }
    }

    protected <V> Optional<V> getProperty(AnimationProperty.AttackPhaseProperty<V> propertyKey, Map<AnimationProperty.AttackPhaseProperty<?>, Object> map) {
        return Optional.ofNullable(map.get(propertyKey));
    }

    public WeaponInnateSkill newProperty() {
        this.properties.add(Maps.newHashMap());
        return this;
    }

    public <T> WeaponInnateSkill addProperty(AnimationProperty.AttackPhaseProperty<T> propertyKey, T object) {
        this.properties.get(this.properties.size() - 1).put(propertyKey, object);
        return this;
    }

    @Override
    @OnlyIn(value=Dist.CLIENT)
    public boolean shouldDraw(SkillContainer container) {
        return true;
    }

    @Override
    @OnlyIn(value=Dist.CLIENT)
    public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
        int stringWidth;
        int j;
        guiGraphics.m_280168_().m_85836_();
        guiGraphics.m_280168_().m_252880_(0.0f, (float)gui.getSlidingProgression(), 0.0f);
        boolean creative = ((Player)container.getExecutor().getOriginal()).m_7500_();
        boolean fullstack = creative || container.isFull();
        boolean canUse = !container.isDisabled() && container.getSkill().checkExecuteCondition(container);
        float cooldownRatio = fullstack || container.isActivated() ? 1.0f : container.getResource(partialTick);
        int vertexNum = 0;
        float iconSize = 32.0f;
        float bottom = y + iconSize;
        float right = x + iconSize;
        float middle = x + iconSize * 0.5f;
        float lastVertexX = 0.0f;
        float lastVertexY = 0.0f;
        float lastTexX = 0.0f;
        float lastTexY = 0.0f;
        if (cooldownRatio < 0.125f) {
            vertexNum = 6;
            lastTexX = cooldownRatio / 0.25f;
            lastTexY = 0.0f;
            lastVertexX = middle + iconSize * lastTexX;
            lastVertexY = y;
            lastTexX += 0.5f;
        } else if (cooldownRatio < 0.375f) {
            vertexNum = 5;
            lastTexX = 1.0f;
            lastTexY = (cooldownRatio - 0.125f) / 0.25f;
            lastVertexX = right;
            lastVertexY = y + iconSize * lastTexY;
        } else if (cooldownRatio < 0.625f) {
            vertexNum = 4;
            lastTexX = (cooldownRatio - 0.375f) / 0.25f;
            lastTexY = 1.0f;
            lastVertexX = right - iconSize * lastTexX;
            lastVertexY = bottom;
            lastTexX = 1.0f - lastTexX;
        } else if (cooldownRatio < 0.875f) {
            vertexNum = 3;
            lastTexX = 0.0f;
            lastTexY = (cooldownRatio - 0.625f) / 0.25f;
            lastVertexX = x;
            lastVertexY = bottom - iconSize * lastTexY;
            lastTexY = 1.0f - lastTexY;
        } else {
            vertexNum = 2;
            lastTexX = (cooldownRatio - 0.875f) / 0.25f;
            lastTexY = 0.0f;
            lastVertexX = x + iconSize * lastTexX;
            lastVertexY = y;
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture((int)0, (ResourceLocation)container.getSkill().getSkillTexture());
        RenderSystem.setShader(GameRenderer::m_172817_);
        RenderSystem.blendFunc((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        if (canUse) {
            if (container.getStack() > 0) {
                RenderSystem.setShaderColor((float)0.0f, (float)0.64f, (float)0.72f, (float)0.8f);
            } else {
                RenderSystem.setShaderColor((float)0.0f, (float)0.5f, (float)0.5f, (float)0.6f);
            }
        } else {
            RenderSystem.setShaderColor((float)0.5f, (float)0.5f, (float)0.5f, (float)0.6f);
        }
        Tesselator tessellator = Tesselator.m_85913_();
        BufferBuilder bufferbuilder = tessellator.m_85915_();
        bufferbuilder.m_166779_(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.f_85817_);
        for (j = 0; j < vertexNum; ++j) {
            bufferbuilder.m_252986_(guiGraphics.m_280168_().m_85850_().m_252922_(), x + iconSize * WeaponInnateSkill.CLOCK_POS[j].x, y + iconSize * WeaponInnateSkill.CLOCK_POS[j].y, 0.0f).m_7421_(WeaponInnateSkill.CLOCK_POS[j].x, WeaponInnateSkill.CLOCK_POS[j].y).m_5752_();
        }
        bufferbuilder.m_252986_(guiGraphics.m_280168_().m_85850_().m_252922_(), lastVertexX, lastVertexY, 0.0f).m_7421_(lastTexX, lastTexY).m_5752_();
        tessellator.m_85914_();
        if (canUse) {
            RenderSystem.setShaderColor((float)0.08f, (float)0.79f, (float)0.95f, (float)1.0f);
        } else {
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        }
        GL11.glCullFace((int)1028);
        bufferbuilder.m_166779_(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.f_85817_);
        for (j = 0; j < 2; ++j) {
            bufferbuilder.m_252986_(guiGraphics.m_280168_().m_85850_().m_252922_(), x + iconSize * WeaponInnateSkill.CLOCK_POS[j].x, y + iconSize * WeaponInnateSkill.CLOCK_POS[j].y, 0.0f).m_7421_(WeaponInnateSkill.CLOCK_POS[j].x, WeaponInnateSkill.CLOCK_POS[j].y).m_5752_();
        }
        for (j = CLOCK_POS.length - 1; j >= vertexNum; --j) {
            bufferbuilder.m_252986_(guiGraphics.m_280168_().m_85850_().m_252922_(), x + iconSize * WeaponInnateSkill.CLOCK_POS[j].x, y + iconSize * WeaponInnateSkill.CLOCK_POS[j].y, 0.0f).m_7421_(WeaponInnateSkill.CLOCK_POS[j].x, WeaponInnateSkill.CLOCK_POS[j].y).m_5752_();
        }
        bufferbuilder.m_252986_(guiGraphics.m_280168_().m_85850_().m_252922_(), lastVertexX, lastVertexY, 0.0f).m_7421_(lastTexX, lastTexY).m_5752_();
        tessellator.m_85914_();
        GL11.glCullFace((int)1029);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        if (container.isActivated() && (container.getSkill().getActivateType() == Skill.ActivateType.DURATION || container.getSkill().getActivateType() == Skill.ActivateType.DURATION_INFINITE)) {
            String s = String.format("%.0f", Float.valueOf((float)container.getRemainDuration() / 20.0f));
            stringWidth = (gui.getFont().m_92895_(s) - 6) / 3;
            guiGraphics.drawString(gui.getFont(), s, x + 13.0f - (float)stringWidth, y + 13.0f, 0xFFFFFF, true);
        } else if (!fullstack) {
            String s = String.valueOf((int)(cooldownRatio * 100.0f));
            stringWidth = (gui.getFont().m_92895_(s) - 6) / 3;
            guiGraphics.drawString(gui.getFont(), s, x + 13.0f - (float)stringWidth, y + 13.0f, 0xFFFFFF, true);
        }
        if (container.getSkill().getMaxStack() > 1) {
            String s = String.valueOf(container.getStack());
            stringWidth = (gui.getFont().m_92895_(s) - 6) / 3;
            guiGraphics.drawString(gui.getFont(), s, x + 25.0f - (float)stringWidth, y + 22.0f, 0xFFFFFF, true);
        }
        guiGraphics.m_280168_().m_85849_();
    }
}

