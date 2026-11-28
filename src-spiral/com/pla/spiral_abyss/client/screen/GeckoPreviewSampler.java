/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.logging.LogUtils
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  org.joml.Matrix3f
 *  org.joml.Matrix3fc
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  software.bernie.geckolib.cache.object.BakedGeoModel
 *  software.bernie.geckolib.cache.object.GeoBone
 *  software.bernie.geckolib.constant.DataTickets
 *  software.bernie.geckolib.core.animatable.GeoAnimatable
 *  software.bernie.geckolib.core.animatable.model.CoreGeoModel
 *  software.bernie.geckolib.core.animation.AnimatableManager
 *  software.bernie.geckolib.core.animation.Animation
 *  software.bernie.geckolib.core.animation.AnimationController
 *  software.bernie.geckolib.core.animation.AnimationProcessor
 *  software.bernie.geckolib.core.animation.AnimationState
 *  software.bernie.geckolib.core.animation.RawAnimation
 *  software.bernie.geckolib.core.molang.MolangParser
 *  software.bernie.geckolib.core.object.DataTicket
 *  software.bernie.geckolib.core.object.PlayState
 *  software.bernie.geckolib.core.state.BoneSnapshot
 *  software.bernie.geckolib.model.GeoModel
 *  software.bernie.geckolib.model.data.EntityModelData
 *  software.bernie.geckolib.renderer.GeoEntityRenderer
 *  software.bernie.geckolib.renderer.GeoRenderer
 */
package com.pla.spiral_abyss.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.encounter.data.BossOptions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.molang.MolangParser;
import software.bernie.geckolib.core.object.DataTicket;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

final class GeckoPreviewSampler {
    private static final Set<Failure> failed = new HashSet<Failure>();

    GeckoPreviewSampler() {
    }

    static boolean supports(LivingEntity entity) {
        return entity instanceof GeoAnimatable && Minecraft.m_91087_().m_91290_().m_114382_((Entity)entity) instanceof GeoEntityRenderer;
    }

    static boolean render(LivingEntity entity, long seed, PoseStack pose, MultiBufferSource buffers, int light) {
        return GeckoPreviewSampler.render(entity, seed, BossOptions.PreviewAnimation.DEFAULT, pose, buffers, light);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean render(LivingEntity entity, long seed, BossOptions.PreviewAnimation preview, PoseStack pose, MultiBufferSource buffers, int light) {
        boolean bl;
        RenderType type;
        double sample;
        String clip;
        BakedGeoModel baked;
        GeoModel model;
        boolean reload;
        AnimationProcessor processor;
        Field querySupplier;
        HashSet originalQueries;
        ArrayList<SavedField> queryState;
        ArrayList<SavedField> boneState;
        ArrayList<SavedField> rendererState;
        GeoAnimatable animated;
        GeoEntityRenderer renderer;
        Failure failure;
        block44: {
            int n;
            List<String> choices;
            block43: {
                failure = new Failure(entity.getClass(), preview);
                if (failed.contains(failure)) {
                    return false;
                }
                renderer = (GeoEntityRenderer)Minecraft.m_91087_().m_91290_().m_114382_((Entity)entity);
                animated = (GeoAnimatable)entity;
                rendererState = new ArrayList<SavedField>();
                boneState = new ArrayList<SavedField>();
                queryState = new ArrayList<SavedField>();
                originalQueries = new HashSet(MolangParser.VARIABLES.keySet());
                querySupplier = null;
                processor = null;
                reload = false;
                pose.m_85836_();
                querySupplier = Class.forName("software.bernie.geckolib.core.molang.LazyVariable").getDeclaredField("valueSupplier");
                querySupplier.setAccessible(true);
                for (Object variable : MolangParser.VARIABLES.values()) {
                    queryState.add(new SavedField(variable, querySupplier, querySupplier.get(variable)));
                }
                for (String name : List.of("animatable", "entityRenderTranslations", "modelRenderTranslations")) {
                    Field field = GeoEntityRenderer.class.getDeclaredField(name);
                    field.setAccessible(true);
                    rendererState.add(new SavedField(renderer, field, GeckoPreviewSampler.copy(field.get(renderer))));
                    if (!name.equals("animatable")) continue;
                    field.set(renderer, entity);
                }
                model = renderer.getGeoModel();
                baked = model.getBakedModel(model.getModelResource(animated, (GeoRenderer)renderer));
                processor = model.getAnimationProcessor();
                reload = processor.reloadAnimations;
                for (Object e : processor.getRegisteredBones()) {
                    GeoBone bone = (GeoBone)e;
                    for (Field field : GeoBone.class.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()) && field.getType() != Matrix4f.class && field.getType() != Matrix3f.class || !field.getType().isPrimitive() && field.getType() != Boolean.class && field.getType() != Matrix4f.class && field.getType() != Matrix3f.class) continue;
                        field.setAccessible(true);
                        boneState.add(new SavedField(bone, field, GeckoPreviewSampler.copy(field.get(bone))));
                    }
                    GeckoPreviewSampler.reset(bone);
                }
                List<String> list = choices = preview.configured() ? List.of(preview.name()) : GeckoPreviewSampler.clips(model, animated);
                if (!choices.isEmpty()) break block43;
                boolean bl2 = false;
                for (SavedField field : boneState) {
                    field.restore();
                }
                for (SavedField field : rendererState) {
                    field.restore();
                }
                for (SavedField field : queryState) {
                    field.restore();
                }
                if (querySupplier != null) {
                    for (Map.Entry entry : MolangParser.VARIABLES.entrySet()) {
                        if (originalQueries.contains(entry.getKey())) continue;
                        new SavedField(entry.getValue(), querySupplier, () -> 0.0).restore();
                    }
                }
                if (processor != null) {
                    processor.reloadAnimations = reload;
                }
                pose.m_85849_();
                return bl2;
            }
            Random random = new Random(seed);
            clip = choices.get(random.nextInt(choices.size()));
            Animation animation = model.getAnimation(animated, clip);
            if (animation == null) {
                throw new IllegalArgumentException("Unknown preview animation: " + clip);
            }
            sample = preview.configured() ? Math.min(preview.timeSeconds() * 20.0, Math.max(0.0, animation.length() - 0.001)) : Math.max(1.0, Math.min(160.0, animation.length()) * (0.2 + random.nextDouble() * 0.5));
            RawAnimation raw = RawAnimation.begin().thenLoop(clip);
            AnimatableManager manager = new AnimatableManager(animated);
            manager.getAnimationControllers().clear();
            AnimationController controller = new AnimationController(animated, "abyss_preview", 0, state -> {
                state.setAnimation(raw);
                return PlayState.CONTINUE;
            });
            controller.setSoundKeyframeHandler(event -> {});
            controller.setParticleKeyframeHandler(event -> {});
            controller.setCustomInstructionKeyframeHandler(event -> {});
            manager.addController(controller);
            AnimationState state2 = new AnimationState(animated, 0.0f, 0.0f, 0.0f, false);
            state2.setData(DataTickets.ENTITY, (Object)entity);
            state2.setData(DataTickets.ENTITY_MODEL_DATA, (Object)new EntityModelData(false, false, 0.0f, 0.0f));
            state2.setData(DataTickets.TICK, (Object)sample);
            model.addAdditionalStateData(animated, renderer.getInstanceId((Entity)entity), (key, value) -> state2.setData((DataTicket)key, value));
            processor.reloadAnimations = false;
            double[] dArray = new double[]{0.0, 0.0, sample};
            int n2 = dArray.length;
            for (n = 0; n < n2; n += 1) {
                double time;
                state2.animationTick = time = dArray[n];
                processor.preAnimationSetup(animated, time);
                processor.tickAnimation(animated, (CoreGeoModel)model, manager, time, state2, false);
            }
            model.setCustomAnimations(animated, renderer.getInstanceId((Entity)entity), state2);
            ResourceLocation texture = renderer.m_5478_((Entity)entity);
            type = renderer.getRenderType(animated, texture, buffers, 0.0f);
            if (type != null) break block44;
            n = 0;
            for (SavedField field : boneState) {
                field.restore();
            }
            for (SavedField field : rendererState) {
                field.restore();
            }
            for (SavedField field : queryState) {
                field.restore();
            }
            if (querySupplier != null) {
                for (Map.Entry entry : MolangParser.VARIABLES.entrySet()) {
                    if (originalQueries.contains(entry.getKey())) continue;
                    new SavedField(entry.getValue(), querySupplier, () -> 0.0).restore();
                }
            }
            if (processor != null) {
                processor.reloadAnimations = reload;
            }
            pose.m_85849_();
            return n != 0;
        }
        try {
            GeoRenderer mesh = GeckoPreviewSampler.previewRenderer(model, animated, renderer, preview.configured());
            mesh.reRender(baked, pose, buffers, animated, type, buffers.m_6299_(type), 0.0f, light, OverlayTexture.f_118083_, 1.0f, 1.0f, 1.0f, 1.0f);
            LogUtils.getLogger().debug("Abyss preview {} samples {} at {} ticks", new Object[]{entity.m_6095_(), clip, sample});
            bl = true;
        }
        catch (LinkageError | ReflectiveOperationException | RuntimeException exception) {
            boolean bl3;
            try {
                failed.add(failure);
                LogUtils.getLogger().warn("Optional Abyss animation preview unavailable for {}", (Object)entity.m_6095_(), (Object)exception);
                bl3 = false;
            }
            catch (Throwable throwable) {
                for (SavedField field : boneState) {
                    field.restore();
                }
                for (SavedField field : rendererState) {
                    field.restore();
                }
                for (SavedField field : queryState) {
                    field.restore();
                }
                if (querySupplier != null) {
                    for (Map.Entry entry : MolangParser.VARIABLES.entrySet()) {
                        if (originalQueries.contains(entry.getKey())) continue;
                        new SavedField(entry.getValue(), querySupplier, () -> 0.0).restore();
                    }
                }
                if (processor != null) {
                    processor.reloadAnimations = reload;
                }
                pose.m_85849_();
                throw throwable;
            }
            for (SavedField savedField : boneState) {
                savedField.restore();
            }
            for (SavedField savedField : rendererState) {
                savedField.restore();
            }
            for (SavedField savedField : queryState) {
                savedField.restore();
            }
            if (querySupplier != null) {
                for (Map.Entry entry : MolangParser.VARIABLES.entrySet()) {
                    if (originalQueries.contains(entry.getKey())) continue;
                    new SavedField(entry.getValue(), querySupplier, () -> 0.0).restore();
                }
            }
            if (processor != null) {
                processor.reloadAnimations = reload;
            }
            pose.m_85849_();
            return bl3;
        }
        for (SavedField field : boneState) {
            field.restore();
        }
        for (SavedField field : rendererState) {
            field.restore();
        }
        for (SavedField field : queryState) {
            field.restore();
        }
        if (querySupplier != null) {
            for (Map.Entry entry : MolangParser.VARIABLES.entrySet()) {
                if (originalQueries.contains(entry.getKey())) continue;
                new SavedField(entry.getValue(), querySupplier, () -> 0.0).restore();
            }
        }
        if (processor != null) {
            processor.reloadAnimations = reload;
        }
        pose.m_85849_();
        return bl;
    }

    private static GeoRenderer previewRenderer(GeoModel model, GeoAnimatable entity, GeoEntityRenderer renderer, boolean meshOnly) {
        if (meshOnly) {
            return new PreviewMesh(model, entity);
        }
        return renderer;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static List<String> clips(GeoModel model, GeoAnimatable entity) {
        ResourceLocation path = model.getAnimationResource(entity);
        Optional resource = Minecraft.m_91087_().m_91098_().m_213713_(path);
        if (resource.isEmpty()) {
            return List.of();
        }
        try (BufferedReader reader = ((Resource)resource.get()).m_215508_();){
            JsonObject animations = JsonParser.parseReader((Reader)reader).getAsJsonObject().getAsJsonObject("animations");
            if (animations == null) {
                List<String> list = List.of();
                return list;
            }
            ArrayList<String> names = new ArrayList<String>();
            for (String name : animations.keySet()) {
                Animation animation;
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("death") || lower.contains("die") || lower.contains("spawn") || lower.contains("intro") || lower.contains("cinematic") || lower.contains("transform") || lower.contains("transition") || lower.contains("entrance") || lower.contains("layer") || lower.contains("overlay") || lower.contains("hurt") || lower.contains("stun") || lower.contains("knocked") || lower.contains("dive") || lower.contains("sand_move") || lower.contains("_old") || lower.contains("hit") || lower.contains("sleep") || lower.contains("frozen") || (animation = model.getAnimation(entity, name)) == null || !(animation.length() > 4.0) || !Double.isFinite(animation.length())) continue;
                names.add(name);
            }
            Collections.sort(names);
            ArrayList<String> arrayList = names;
            return arrayList;
        }
        catch (IOException exception) {
            return List.of();
        }
    }

    private static void reset(GeoBone bone) {
        BoneSnapshot initial = bone.getInitialSnapshot();
        bone.setRotX(initial.getRotX());
        bone.setRotY(initial.getRotY());
        bone.setRotZ(initial.getRotZ());
        bone.setPosX(initial.getOffsetX());
        bone.setPosY(initial.getOffsetY());
        bone.setPosZ(initial.getOffsetZ());
        bone.setScaleX(initial.getScaleX());
        bone.setScaleY(initial.getScaleY());
        bone.setScaleZ(initial.getScaleZ());
        bone.setHidden(Boolean.TRUE.equals(bone.shouldNeverRender()));
        bone.resetStateChanges();
    }

    private static Object copy(Object value) {
        if (value instanceof Matrix4f) {
            Matrix4f matrix = (Matrix4f)value;
            return new Matrix4f((Matrix4fc)matrix);
        }
        if (value instanceof Matrix3f) {
            Matrix3f matrix = (Matrix3f)value;
            return new Matrix3f((Matrix3fc)matrix);
        }
        return value;
    }

    private record Failure(Class<?> type, BossOptions.PreviewAnimation animation) {
    }

    private record SavedField(Object owner, Field field, Object value) {
        void restore() {
            try {
                Object object = this.value;
                if (object instanceof Matrix4f) {
                    Matrix4f matrix = (Matrix4f)object;
                    ((Matrix4f)this.field.get(this.owner)).set((Matrix4fc)matrix);
                } else {
                    object = this.value;
                    if (object instanceof Matrix3f) {
                        Matrix3f matrix = (Matrix3f)object;
                        ((Matrix3f)this.field.get(this.owner)).set((Matrix3fc)matrix);
                    } else {
                        this.field.set(this.owner, this.value);
                    }
                }
            }
            catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }

    private record PreviewMesh(GeoModel model, GeoAnimatable entity) implements GeoRenderer<GeoAnimatable>
    {
        public GeoModel getGeoModel() {
            return this.model;
        }

        public GeoAnimatable getAnimatable() {
            return this.entity;
        }

        public void fireCompileRenderLayersEvent() {
        }

        public boolean firePreRenderEvent(PoseStack pose, BakedGeoModel model, MultiBufferSource buffers, float tick, int light) {
            return true;
        }

        public void firePostRenderEvent(PoseStack pose, BakedGeoModel model, MultiBufferSource buffers, float tick, int light) {
        }

        public void updateAnimatedTextureFrame(GeoAnimatable entity) {
        }
    }
}

