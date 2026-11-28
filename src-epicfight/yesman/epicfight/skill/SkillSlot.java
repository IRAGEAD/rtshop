/*
 * Decompiled with CFR 0.152.
 */
package yesman.epicfight.skill;

import yesman.epicfight.api.utils.ExtendableEnum;
import yesman.epicfight.api.utils.ExtendableEnumManager;
import yesman.epicfight.skill.SkillCategory;

public interface SkillSlot
extends ExtendableEnum {
    public static final ExtendableEnumManager<SkillSlot> ENUM_MANAGER = new ExtendableEnumManager("skill_slot");

    public SkillCategory category();
}

