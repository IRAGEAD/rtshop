/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.ai.goal.Goal
 *  net.minecraft.world.entity.ai.goal.Goal$Flag
 */
package net.spell_engine.entity.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.spell_engine.api.spell.summon.SummonBehaviour;
import net.spell_engine.entity.SummonedEntity;
import net.spell_engine.internals.casting.MobCastController;

public class SpellCastGoal
extends Goal {
    private final MobCastController controller;

    public SpellCastGoal(SummonedEntity entity, SummonBehaviour.Action.SpellCast config) {
        this.controller = new MobCastController(entity, config);
        this.m_7021_(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean m_183429_() {
        return true;
    }

    public boolean m_8036_() {
        return this.controller.canBegin();
    }

    public void m_8056_() {
        this.controller.begin();
    }

    public boolean m_8045_() {
        return this.controller.shouldContinue();
    }

    public void m_8041_() {
        this.controller.end();
    }

    public void m_8037_() {
        this.controller.tick();
    }
}

