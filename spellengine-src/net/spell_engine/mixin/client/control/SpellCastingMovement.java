/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.player.Input
 *  net.minecraft.client.player.LocalPlayer
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.spell_engine.mixin.client.control;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.delivery.melee.Melee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={LocalPlayer.class})
public class SpellCastingMovement {
    @Shadow
    public Input f_108618_;
    @Shadow
    protected int f_108583_;

    @Inject(method={"m_8107_"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/player/Input;m_214106_(ZF)V", shift=At.Shift.AFTER)})
    private void tickMovement_ModifyInput(CallbackInfo ci) {
        Melee.ActiveAttack attack;
        LocalPlayer player = (LocalPlayer)this;
        SpellCaster.Client caster = (SpellCaster.Client)player;
        SpellCast.Process process = caster.getSpellCastProcess();
        if (process != null && ((Spell)process.spell().m_203334_()).active.cast != null && !player.m_20159_()) {
            float multiplier = ((Spell)process.spell().m_203334_()).active.cast.movement_speed * SpellEngineMod.config.movement_multiplier_speed_while_casting;
            this.f_108618_.f_108566_ *= multiplier;
            this.f_108618_.f_108567_ *= multiplier;
            this.f_108583_ = 0;
        }
        if ((attack = caster.getCurrentSkillAttack()) != null) {
            float multiplier = attack.attack.movement_speed();
            this.f_108618_.f_108566_ *= multiplier;
            this.f_108618_.f_108567_ *= multiplier;
            this.f_108583_ = 0;
        }
    }
}

