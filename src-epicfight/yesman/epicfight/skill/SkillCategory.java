/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package yesman.epicfight.skill;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.utils.ExtendableEnum;
import yesman.epicfight.api.utils.ExtendableEnumManager;
import yesman.epicfight.main.EpicFightMod;

public interface SkillCategory
extends ExtendableEnum {
    public static final ResourceLocation DEFAULT_BOOK_ICON = EpicFightMod.identifier("skillbook");
    public static final ExtendableEnumManager<SkillCategory> ENUM_MANAGER = new ExtendableEnumManager("skill_category");

    public boolean shouldSave();

    public boolean shouldSynchronize();

    public boolean learnable();

    default public ResourceLocation bookIcon() {
        return DEFAULT_BOOK_ICON;
    }
}

