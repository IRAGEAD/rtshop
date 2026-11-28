/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.animation.AnimationDefinition
 *  net.minecraft.client.animation.KeyframeAnimations
 *  net.minecraft.client.model.HierarchicalModel
 *  net.minecraft.client.model.geom.ModelPart
 *  org.joml.Vector3f
 */
package com.pla.spiral_abyss.client.renderer;

import com.pla.spiral_abyss.entity.LootChestEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

public abstract class LootChestModel
extends HierarchicalModel<LootChestEntity> {
    private final ModelPart root;
    private final AnimationDefinition spawn;
    private final AnimationDefinition idle;
    private final AnimationDefinition open;
    private final Vector3f scratch = new Vector3f();

    protected LootChestModel(ModelPart root, AnimationDefinition spawn, AnimationDefinition idle, AnimationDefinition open) {
        this.root = root;
        this.spawn = spawn;
        this.idle = idle;
        this.open = open;
        root.m_171324_((String)"chest").m_171324_((String)"hitbox").f_104207_ = false;
    }

    public ModelPart m_142109_() {
        return this.root;
    }

    public void setupAnim(LootChestEntity entity, float limbSwing, float amount, float age, float yaw, float pitch) {
        AnimationDefinition clip;
        this.root.m_171331_().forEach(ModelPart::m_233569_);
        long millis = (long)(Math.max(0.0f, (float)(entity.m_9236_().m_46467_() - entity.phaseStart()) + age - (float)entity.f_19797_) * 50.0f);
        switch (entity.phase()) {
            case 4: {
                AnimationDefinition animationDefinition = this.spawn;
                break;
            }
            case 0: {
                AnimationDefinition animationDefinition = this.idle;
                break;
            }
            default: {
                AnimationDefinition animationDefinition = clip = this.open;
            }
        }
        if (entity.phase() == 2 || entity.phase() == 3) {
            millis = Math.round(this.open.f_232255_() * 1000.0f);
        } else if (!clip.f_232256_()) {
            millis = Math.min(millis, (long)Math.round(clip.f_232255_() * 1000.0f));
        }
        KeyframeAnimations.m_232319_((HierarchicalModel)this, (AnimationDefinition)clip, (long)millis, (float)1.0f, (Vector3f)this.scratch);
    }
}

