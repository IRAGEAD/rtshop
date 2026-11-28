/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder
 *  net.minecraft.world.entity.Entity
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.internals.casting;

import java.util.List;
import net.minecraft.core.Holder;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.client.casting.ClientCastController;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCastInteractor;
import net.spell_engine.internals.cost.SpellCooldownManager;
import net.spell_engine.internals.delivery.arrow.ArrowShootContext;
import net.spell_engine.internals.delivery.melee.Melee;
import org.jetbrains.annotations.Nullable;

public class SpellCaster {

    public static interface Client
    extends Player {
        public ClientCastController getCastController();

        default public List<net.minecraft.world.entity.Entity> getCurrentTargets() {
            return this.getCastController().currentTargets();
        }

        @Nullable
        default public SpellCast.Progress getSpellCastProgress() {
            return this.getCastController().progress();
        }

        default public void cancelSpellCast() {
            this.getCastController().cancelSpellCast();
        }

        public void onAttacksAvailable(List<Melee.Attack> var1);

        public Melee.ActiveAttack getCurrentSkillAttack();
    }

    public static interface Player
    extends Entity {
        public SpellCastInteractor getInteractor();

        public SpellCooldownManager getCooldownManager();

        @Override
        @Nullable
        default public SpellCast.Process getSpellCastProcess() {
            return this.getInteractor().process();
        }

        public void setArrowShootContext(ArrowShootContext var1);

        public ArrowShootContext getArrowShootContext();

        public void setMeleeSkillAttack(Melee.ActiveAttack var1);

        public float getExtraSlipperiness();

        public void setActiveMeleeSkill(Holder<Spell> var1);

        public Holder<Spell> getActiveMeleeSkill();
    }

    public static interface Entity {
        @Nullable
        public SpellCast.Process getSpellCastProcess();

        @Nullable
        default public Spell getCastedSpell() {
            SpellCast.Process process = this.getSpellCastProcess();
            return process != null ? (Spell)process.spell().m_203334_() : null;
        }

        default public float getCastingSpeed() {
            SpellCast.Process process = this.getSpellCastProcess();
            return process != null ? process.speed() : 1.0f;
        }

        default public boolean isCastingSpell() {
            return this.getSpellCastProcess() != null;
        }

        @Nullable
        default public Spell.Target.Beam getBeam() {
            Spell spell = this.getCastedSpell();
            if (spell != null && spell.target != null && spell.target.type == Spell.Target.Type.BEAM) {
                return spell.target.beam;
            }
            return null;
        }

        default public boolean isBeaming() {
            return this.getBeam() != null;
        }
    }
}

