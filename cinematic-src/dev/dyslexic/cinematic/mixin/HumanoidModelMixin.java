/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.HumanoidModel
 *  net.minecraft.client.model.PlayerModel
 *  net.minecraft.client.model.geom.ModelPart
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.AnvilStation;
import dev.dyslexic.cinematic.client.ClientMenus;
import dev.dyslexic.cinematic.client.ClientPayloads;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.ShelfBooks;
import dev.dyslexic.cinematic.client.StonecutterStation;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={HumanoidModel.class})
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Shadow
    @Final
    public ModelPart f_102811_;
    @Shadow
    @Final
    public ModelPart f_102812_;
    @Shadow
    @Final
    public ModelPart f_102813_;
    @Shadow
    @Final
    public ModelPart f_102814_;
    @Shadow
    @Final
    public ModelPart f_102810_;
    @Shadow
    @Final
    public ModelPart f_102808_;
    @Unique
    private static final float CINEMATIC$LEAN = 0.45f;
    @Unique
    private boolean cinematic$leaned;

    @Inject(method={"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"}, at={@At(value="TAIL")})
    private void cinematic$stationPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (this.cinematic$leaned) {
            this.f_102810_.f_104202_ = 0.0f;
            this.f_102808_.f_104202_ = 0.0f;
            this.cinematic$leaned = false;
        }
        if (!(entity instanceof Player)) {
            return;
        }
        StationKind kind = ClientPayloads.atStation(entity.m_19879_());
        if (kind == null) {
            return;
        }
        float t = ageInTicks * 0.09f + (float)entity.m_19879_();
        double move = ClientMenus.sinceMove(entity.m_19879_());
        switch (kind) {
            case ANVIL: {
                this.f_102812_.f_104203_ = -1.05f + Mth.m_14031_((float)(t + 2.1f)) * 0.04f;
                this.f_102812_.f_104204_ = 0.28f;
                this.f_102812_.f_104205_ = -0.06f;
                boolean forging = move < 2.05;
                this.f_102811_.f_104203_ = forging ? AnvilStation.armPitch(move) : -0.98f + Mth.m_14031_((float)t) * 0.05f;
                this.f_102811_.f_104204_ = forging ? -0.35f : -0.2f;
                this.f_102811_.f_104205_ = 0.08f;
                break;
            }
            case CAMPFIRE: {
                this.f_102813_.f_104203_ = -2.1f;
                this.f_102813_.f_104204_ = 0.12f;
                this.f_102813_.f_104205_ = 0.05f;
                this.f_102813_.f_104207_ = false;
                HumanoidModelMixin humanoidModelMixin = this;
                if (humanoidModelMixin instanceof PlayerModel) {
                    PlayerModel playerModel = (PlayerModel)humanoidModelMixin;
                    playerModel.f_103377_.f_104207_ = false;
                }
                this.f_102814_.f_104203_ = -1.45f;
                this.f_102814_.f_104204_ = -0.22f;
                this.f_102814_.f_104205_ = 0.0f;
                this.cinematic$leanBack();
                this.f_102811_.f_104203_ = -1.5f + Mth.m_14031_((float)(t * 0.6f)) * 0.03f;
                this.f_102811_.f_104204_ = -0.3f;
                this.f_102811_.f_104205_ = 0.05f;
                this.f_102812_.f_104203_ = 0.4f;
                this.f_102812_.f_104204_ = 0.0f;
                this.f_102812_.f_104205_ = -0.25f;
                break;
            }
            case LOOM: {
                float throwing = Mth.m_14031_((float)(t * 2.2f)) * 0.2f;
                this.f_102811_.f_104203_ = -1.0f + Mth.m_14031_((float)(t * 1.1f)) * 0.04f;
                this.f_102811_.f_104204_ = -0.35f + throwing;
                this.f_102811_.f_104205_ = 0.05f;
                this.f_102812_.f_104203_ = -1.0f + Mth.m_14031_((float)(t * 1.1f + 1.3f)) * 0.04f;
                this.f_102812_.f_104204_ = 0.35f + throwing;
                this.f_102812_.f_104205_ = -0.05f;
                break;
            }
            case GRINDSTONE: {
                float stroke = move < 2.4 ? GrindstoneStation.armSway(move) : 0.0f;
                this.f_102811_.f_104203_ = -1.15f + Mth.m_14031_((float)(t * 1.1f)) * 0.03f;
                this.f_102811_.f_104204_ = -0.32f + stroke;
                this.f_102811_.f_104205_ = 0.05f;
                this.f_102812_.f_104203_ = -1.2f + Mth.m_14031_((float)(t * 1.1f + 1.3f)) * 0.03f;
                this.f_102812_.f_104204_ = 0.3f + stroke;
                this.f_102812_.f_104205_ = -0.05f;
                break;
            }
            case STONECUTTER: {
                float[] fArray;
                if (move < 1.05) {
                    fArray = StonecutterStation.armTurns(move);
                } else {
                    float[] fArray2 = new float[2];
                    fArray2[0] = -0.2f;
                    fArray = fArray2;
                    fArray2[1] = -0.12f;
                }
                float[] turns = fArray;
                this.f_102812_.f_104203_ = -1.0f + Mth.m_14031_((float)(t + 2.1f)) * 0.04f;
                this.f_102812_.f_104204_ = turns[0];
                this.f_102812_.f_104205_ = -0.06f;
                this.f_102811_.f_104203_ = -0.98f + Mth.m_14031_((float)t) * 0.05f;
                this.f_102811_.f_104204_ = turns[1];
                this.f_102811_.f_104205_ = 0.06f;
                break;
            }
            case FURNACE: 
            case SMOKER: 
            case BREWING: {
                this.f_102811_.f_104203_ = -1.32f + Mth.m_14031_((float)(t * 1.3f)) * 0.12f;
                this.f_102811_.f_104204_ = -0.12f + Mth.m_14031_((float)(t * 0.8f)) * 0.08f;
                this.f_102811_.f_104205_ = 0.04f;
                this.f_102812_.f_104203_ = -1.6f + Mth.m_14031_((float)(t + 2.1f)) * 0.03f;
                this.f_102812_.f_104204_ = 0.32f;
                this.f_102812_.f_104205_ = -0.05f;
                break;
            }
            case CHISELED_BOOKSHELF: {
                if (ShelfBooks.reading(entity.m_19879_())) {
                    this.f_102811_.f_104203_ = -1.05f + Mth.m_14031_((float)(t * 0.6f)) * 0.02f;
                    this.f_102811_.f_104204_ = -0.42f;
                    this.f_102811_.f_104205_ = 0.05f;
                    this.f_102812_.f_104203_ = -1.05f + Mth.m_14031_((float)(t * 0.6f + 1.4f)) * 0.02f;
                    this.f_102812_.f_104204_ = 0.42f;
                    this.f_102812_.f_104205_ = -0.05f;
                    break;
                }
                this.f_102811_.f_104203_ = -1.4f + Mth.m_14031_((float)(t * 0.9f)) * 0.1f;
                this.f_102811_.f_104204_ = -0.1f + Mth.m_14031_((float)(t * 0.55f)) * 0.18f;
                this.f_102811_.f_104205_ = 0.04f;
                this.f_102812_.f_104203_ = -0.55f;
                this.f_102812_.f_104204_ = 0.1f;
                this.f_102812_.f_104205_ = -0.1f;
                break;
            }
            case ENCHANTING: 
            case BEACON: {
                this.f_102811_.f_104203_ = -1.75f + Mth.m_14031_((float)(t * 0.7f)) * 0.06f;
                this.f_102811_.f_104204_ = -0.32f + Mth.m_14031_((float)(t * 0.5f)) * 0.05f;
                this.f_102811_.f_104205_ = 0.18f;
                this.f_102812_.f_104203_ = -1.75f + Mth.m_14031_((float)(t * 0.7f + 1.4f)) * 0.06f;
                this.f_102812_.f_104204_ = 0.32f;
                this.f_102812_.f_104205_ = -0.18f;
                break;
            }
            default: {
                this.f_102811_.f_104203_ = -0.98f + Mth.m_14031_((float)t) * 0.07f;
                this.f_102811_.f_104204_ = -0.16f + Mth.m_14031_((float)(t * 0.7f)) * 0.05f;
                this.f_102811_.f_104205_ = 0.06f;
                this.f_102812_.f_104203_ = -0.98f + Mth.m_14031_((float)(t + 2.1f)) * 0.05f;
                this.f_102812_.f_104204_ = 0.16f;
                this.f_102812_.f_104205_ = -0.06f;
            }
        }
    }

    @Unique
    private void cinematic$leanBack() {
        float cos = Mth.m_14089_((float)0.45f);
        float sin = Mth.m_14031_((float)0.45f);
        this.f_102810_.f_104203_ = -0.45f;
        this.f_102810_.f_104201_ = 12.0f - 12.0f * cos;
        this.f_102810_.f_104202_ = 12.0f * sin;
        this.f_102808_.f_104201_ = this.f_102810_.f_104201_;
        this.f_102808_.f_104202_ = this.f_102810_.f_104202_;
        this.f_102811_.f_104201_ = 12.0f - 10.0f * cos;
        this.f_102811_.f_104202_ = 10.0f * sin;
        this.f_102812_.f_104201_ = this.f_102811_.f_104201_;
        this.f_102812_.f_104202_ = this.f_102811_.f_104202_;
        this.cinematic$leaned = true;
    }
}

