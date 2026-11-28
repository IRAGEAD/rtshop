/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.RenderTarget
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.Lighting
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  com.mojang.logging.LogUtils
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.EntityRenderDispatcher
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.world.entity.AnimationState
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.boss.enderdragon.EnderDragon
 *  net.minecraft.world.entity.monster.warden.Warden
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.lwjgl.opengl.GL11
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.client.screen.BannerPortrait;
import com.pla.spiral_abyss.client.screen.OptionalGeoPreview;
import com.pla.spiral_abyss.encounter.MobSpawnData;
import com.pla.spiral_abyss.encounter.data.BossOptions;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.lwjgl.opengl.GL11;

final class BannerBossRenderer {
    private static final TagKey<EntityType<?>> PREVIEW_UPRIGHT_NORMALS = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "preview_upright_normals"));
    private static final TagKey<EntityType<?>> PREVIEW_ADULT_AGE_TICKS = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "preview_adult_age_ticks"));
    private static final TagKey<EntityType<?>> PREVIEW_NATIVE_SCALE = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "preview_native_scale"));
    private final Map<ResourceLocation, LivingEntity> entities = new HashMap<ResourceLocation, LivingEntity>();
    private final Map<ResourceLocation, TextureTarget> portraits = new HashMap<ResourceLocation, TextureTarget>();
    private final Map<ResourceLocation, BossOptions.PreviewAnimation> animations = new HashMap<ResourceLocation, BossOptions.PreviewAnimation>();
    private final Map<Integer, BannerPortrait.Crop> crops = new HashMap<Integer, BannerPortrait.Crop>();
    private final Set<ResourceLocation> unavailable = new HashSet<ResourceLocation>();
    private final long cycleSeed;
    private final Random poses = new Random(0L);

    BannerBossRenderer(long cycleSeed) {
        this.cycleSeed = cycleSeed;
    }

    static void initializeModel(LivingEntity entity) {
        if (entity.m_6095_().m_204039_(PREVIEW_ADULT_AGE_TICKS)) {
            MobSpawnData.apply(entity, MobSpawnData.parse("{AgeTicks:2400000,AgingDisabled:1b}"));
        }
        if (entity.m_6095_().m_204039_(PREVIEW_NATIVE_SCALE)) {
            MobSpawnData.apply(entity, MobSpawnData.parse("{Scale:3.0f,Ancient:1b}"));
        }
        if (!entity.m_6095_().m_204039_(PREVIEW_UPRIGHT_NORMALS)) {
            return;
        }
        try {
            Vec3 up = new Vec3(0.0, 1.0, 0.0);
            entity.getClass().getField("normal").set(entity, up);
            entity.getClass().getField("lastNormal").set(entity, up);
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot initialize tagged preview surface normals", exception);
        }
    }

    Component name(ResourceLocation id) {
        EntityType<?> type = AbyssRegistries.ENTITY_TYPES.getValue(id);
        return type == null ? Component.m_237113_((String)id.toString()) : type.m_20676_();
    }

    void clear() {
        int framebuffer = GL11.glGetInteger((int)36006);
        int readFramebuffer = GL11.glGetInteger((int)36010);
        try {
            this.portraits.values().forEach(RenderTarget::m_83930_);
        }
        finally {
            GlStateManager._glBindFramebuffer((int)36160, (int)framebuffer);
            GlStateManager._glBindFramebuffer((int)36008, (int)readFramebuffer);
        }
        this.portraits.clear();
        this.crops.clear();
        this.entities.clear();
        this.animations.clear();
        this.unavailable.clear();
    }

    void render(GuiGraphics graphics, ResourceLocation id, int x, int y, int width, int height) {
        this.render(graphics, id, BossOptions.PreviewAnimation.DEFAULT, x, y, width, height);
    }

    void render(GuiGraphics graphics, ResourceLocation id, BossOptions.PreviewAnimation animation, int x, int y, int width, int height) {
        if (width < 1 || height < 1) {
            return;
        }
        if (!animation.equals(this.animations.put(id, animation))) {
            TextureTarget oldPortrait = this.portraits.remove(id);
            if (oldPortrait != null) {
                this.destroyPortrait(oldPortrait);
            }
            this.unavailable.remove(id);
        }
        Minecraft client = Minecraft.m_91087_();
        if (client.f_91073_ == null || this.unavailable.contains(id)) {
            this.fallback(graphics, id, x, y, width, height);
            return;
        }
        LivingEntity entity = this.entities.get(id);
        if (entity == null || entity.m_9236_() != client.f_91073_) {
            TextureTarget oldPortrait = this.portraits.remove(id);
            if (oldPortrait != null) {
                this.destroyPortrait(oldPortrait);
            }
            try {
                Entity created;
                EntityType<?> type = AbyssRegistries.ENTITY_TYPES.getValue(id);
                Entity entity2 = created = type == null ? null : type.m_20615_((Level)client.f_91073_);
                if (!(created instanceof LivingEntity)) {
                    this.unavailable.add(id);
                    this.fallback(graphics, id, x, y, width, height);
                    return;
                }
                LivingEntity living = (LivingEntity)created;
                long seed = this.cycleSeed;
                for (char c : id.toString().toCharArray()) {
                    seed = (seed ^ (long)c) * 1099511628211L;
                }
                this.poses.setSeed(seed);
                living.m_217043_().m_188584_(seed);
                living.m_20084_(new UUID(seed, seed ^ 0xFFFFFFFFFFFFFFFFL));
                BannerBossRenderer.initializeModel(living);
                this.samplePose(living);
                entity = living;
                this.entities.put(id, living);
            }
            catch (LinkageError | RuntimeException exception) {
                this.unavailable.add(id);
                LogUtils.getLogger().warn("Cannot create Abyss banner model {}", (Object)id, (Object)exception);
                this.fallback(graphics, id, x, y, width, height);
                return;
            }
        }
        TextureTarget portrait = this.portraits.get(id);
        try {
            if (portrait == null) {
                graphics.m_280262_();
                for (int attempt = 0; attempt < 10; ++attempt) {
                    portrait = this.capture(graphics, entity, animation, (float)Math.pow(0.55, attempt));
                    BannerPortrait.Crop crop = this.crops.get(portrait.m_83975_());
                    if (attempt == 9 || crop.x() > 0 && crop.y() > 0 && crop.x() + crop.width() < portrait.f_83915_ && crop.y() + crop.height() < portrait.f_83916_) break;
                    this.destroyPortrait(portrait);
                }
                this.portraits.put(id, portrait);
            }
            BannerPortrait.draw(graphics, portrait, this.crops.get(portrait.m_83975_()), x, y, width, height);
        }
        catch (LinkageError | RuntimeException exception) {
            this.unavailable.add(id);
            LogUtils.getLogger().warn("Cannot render Abyss banner model {}", (Object)id, (Object)exception);
        }
        if (this.unavailable.contains(id)) {
            this.fallback(graphics, id, x, y, width, height);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private TextureTarget capture(GuiGraphics graphics, LivingEntity entity, BossOptions.PreviewAnimation animation, float zoom) {
        Minecraft client = Minecraft.m_91087_();
        EntityRenderDispatcher dispatcher = client.m_91290_();
        Quaternionf previousCamera = new Quaternionf((Quaternionfc)dispatcher.m_253208_());
        BannerPortrait.RenderState previous = new BannerPortrait.RenderState();
        TextureTarget target = null;
        boolean success = false;
        try {
            TextureTarget textureTarget;
            int pixelWidth = 512;
            int pixelHeight = 512;
            target = new TextureTarget(pixelWidth, pixelHeight, true, Minecraft.f_91002_);
            target.m_83936_(9729);
            target.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
            RenderSystem.disableScissor();
            RenderSystem.depthMask((boolean)true);
            target.m_83954_(Minecraft.f_91002_);
            target.m_83947_(true);
            RenderSystem.setProjectionMatrix((Matrix4f)new Matrix4f().setOrtho(0.0f, (float)pixelWidth, (float)pixelHeight, 0.0f, 1000.0f, 21000.0f), (VertexSorting)VertexSorting.f_276633_);
            RenderSystem.getModelViewStack().m_85836_();
            try {
                RenderSystem.getModelViewStack().m_166856_();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask((boolean)true);
                RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                Lighting.m_166384_();
                dispatcher.m_252923_(new Quaternionf());
                boolean dragon = entity instanceof EnderDragon;
                float modelWidth = dragon ? 17.0f : Math.max(0.9f, entity.m_20205_() * 1.8f);
                float modelHeight = dragon ? 9.0f : Math.max(1.0f, entity.m_20206_() * 1.22f);
                float scale = zoom * 0.65f * Math.min(((float)pixelWidth - 24.0f) / modelWidth, ((float)pixelHeight - 24.0f) / modelHeight);
                PoseStack modelPose = new PoseStack();
                modelPose.m_252880_((float)pixelWidth / 2.0f, (float)pixelHeight * (dragon || OptionalGeoPreview.supports(entity, animation) ? 0.54f : 0.82f), -11000.0f);
                modelPose.m_85841_(scale, scale, -scale);
                modelPose.m_252781_(new Quaternionf().rotateZ((float)Math.PI).rotateY((float)Math.toRadians(dragon ? 150.0 : -16.0)));
                RenderSystem.runAsFancy(() -> {
                    if (!OptionalGeoPreview.render(entity, entity.m_20148_().getMostSignificantBits(), animation, modelPose, (MultiBufferSource)graphics.m_280091_(), 0xF000F0)) {
                        dispatcher.m_114382_((Entity)entity).m_7392_((Entity)entity, 0.0f, 1.0f, modelPose, (MultiBufferSource)graphics.m_280091_(), 0xF000F0);
                    }
                });
                graphics.m_280262_();
                this.crops.put(target.m_83975_(), BannerPortrait.finish(target));
                success = true;
                textureTarget = target;
            }
            catch (Throwable throwable) {
                try {
                    if (!success) {
                        graphics.m_280262_();
                    }
                }
                finally {
                    RenderSystem.getModelViewStack().m_85849_();
                    RenderSystem.applyModelViewMatrix();
                }
                throw throwable;
            }
            try {
                if (!success) {
                    graphics.m_280262_();
                }
            }
            finally {
                RenderSystem.getModelViewStack().m_85849_();
                RenderSystem.applyModelViewMatrix();
            }
            return textureTarget;
        }
        finally {
            try {
                if (!success && target != null) {
                    target.m_83930_();
                }
            }
            finally {
                dispatcher.m_252923_(previousCamera);
                Lighting.m_84931_();
                previous.restore();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void destroyPortrait(TextureTarget portrait) {
        this.crops.remove(portrait.m_83975_());
        int framebuffer = GL11.glGetInteger((int)36006);
        int readFramebuffer = GL11.glGetInteger((int)36010);
        try {
            portrait.m_83930_();
        }
        finally {
            GlStateManager._glBindFramebuffer((int)36160, (int)framebuffer);
            GlStateManager._glBindFramebuffer((int)36008, (int)readFramebuffer);
        }
    }

    Bounds contentBounds(ResourceLocation id, int x, int y, int width, int height) {
        BannerPortrait.Crop crop;
        TextureTarget target = this.portraits.get(id);
        BannerPortrait.Crop crop2 = crop = target == null ? null : this.crops.get(target.m_83975_());
        if (crop == null) {
            return new Bounds(x, y, width, height);
        }
        float scale = Math.min((float)width / (float)crop.width(), (float)height / (float)crop.height());
        int fittedWidth = Math.round((float)crop.width() * scale);
        int fittedHeight = Math.round((float)crop.height() * scale);
        return new Bounds(x + (width - fittedWidth) / 2, y + (height - fittedHeight) / 2, fittedWidth, fittedHeight);
    }

    private void samplePose(LivingEntity entity) {
        entity.f_19797_ = 50 + this.poses.nextInt(100);
        entity.m_146922_(180.0f);
        entity.f_20884_ = 180.0f;
        entity.f_20883_ = 180.0f;
        entity.f_19859_ = 180.0f;
        entity.f_20885_ = entity.f_20886_ = 168.0f + this.poses.nextFloat() * 24.0f;
        entity.m_146926_(-8.0f + this.poses.nextFloat() * 12.0f);
        entity.f_19860_ = entity.m_146909_();
        entity.f_20921_ = entity.f_20920_ = 0.22f + this.poses.nextFloat() * 0.52f;
        int count = 5 + this.poses.nextInt(16);
        for (int step = 0; step < count; ++step) {
            entity.f_267362_.m_267566_(0.65f + this.poses.nextFloat() * 0.3f, 1.0f);
        }
        if (entity instanceof Mob) {
            Mob mob = (Mob)entity;
            mob.m_21561_(true);
        }
        if (entity instanceof EnderDragon) {
            EnderDragon dragon = (EnderDragon)entity;
            dragon.f_31082_ = dragon.f_31081_ = 0.08f + this.poses.nextFloat() * 0.42f;
            dragon.f_31093_ = 63;
            for (int i = 0; i < dragon.f_31092_.length; ++i) {
                dragon.f_31092_[i][0] = 180.0 + Math.sin((double)i * 0.16) * 7.0;
                dragon.f_31092_[i][1] = Math.sin((double)i * 0.12) * 0.35;
                dragon.f_31092_[i][2] = 0.0;
            }
        } else if (entity instanceof Warden) {
            Warden warden = (Warden)entity;
            if (this.poses.nextBoolean()) {
                warden.f_219312_.m_216977_(entity.f_19797_ - 16 - this.poses.nextInt(20));
            } else {
                warden.f_219313_.m_216977_(entity.f_19797_ - 3 - this.poses.nextInt(5));
            }
        } else {
            this.sampleExposedAnimation(entity);
        }
        this.sampleLibraryAnimation(entity);
    }

    private void sampleLibraryAnimation(LivingEntity entity) {
        for (String api : List.of("com.github.alexthe666.citadel.animation.IAnimatedEntity", "com.github.L_Ender.lionfishapi.server.animation.IAnimatedEntity")) {
            try {
                Class<?> animated = Class.forName(api, false, entity.getClass().getClassLoader());
                if (!animated.isInstance(entity)) continue;
                Object[] animations = (Object[])animated.getMethod("getAnimations", new Class[0]).invoke((Object)entity, new Object[0]);
                Object none = animated.getField("NO_ANIMATION").get(null);
                ArrayList<Object> choices = new ArrayList<Object>();
                for (Object animation : animations) {
                    if (animation == null || animation == none || (Integer)animation.getClass().getMethod("getDuration", new Class[0]).invoke(animation, new Object[0]) <= 4) continue;
                    choices.add(animation);
                }
                if (choices.isEmpty()) {
                    return;
                }
                Object animation = choices.get(this.poses.nextInt(choices.size()));
                int duration = (Integer)animation.getClass().getMethod("getDuration", new Class[0]).invoke(animation, new Object[0]);
                animated.getMethod("setAnimation", animation.getClass()).invoke((Object)entity, animation);
                animated.getMethod("setAnimationTick", Integer.TYPE).invoke((Object)entity, Math.max(1, Math.round((float)duration * (0.2f + this.poses.nextFloat() * 0.5f))));
                return;
            }
            catch (ClassNotFoundException animated) {
            }
            catch (LinkageError | ReflectiveOperationException | RuntimeException exception) {
                LogUtils.getLogger().debug("Cannot sample optional Abyss banner animation {}", (Object)api, (Object)exception);
            }
        }
    }

    private void sampleExposedAnimation(LivingEntity entity) {
        ArrayList<AnimationState> candidates = new ArrayList<AnimationState>();
        Field[] fields = entity.getClass().getFields();
        Arrays.sort(fields, Comparator.comparing(field -> field.getDeclaringClass().getName() + ":" + field.getName()));
        for (Field field2 : fields) {
            String name;
            if (Modifier.isStatic(field2.getModifiers()) || field2.getType() != AnimationState.class || !(name = field2.getName().toLowerCase(Locale.ROOT)).contains("attack") && !name.contains("roar") && !name.contains("swing") && !name.contains("walk") && !name.contains("idle")) continue;
            try {
                Object object = field2.get(entity);
                if (!(object instanceof AnimationState)) continue;
                AnimationState animation = (AnimationState)object;
                candidates.add(animation);
            }
            catch (IllegalAccessException illegalAccessException) {
                // empty catch block
            }
        }
        if (!candidates.isEmpty()) {
            ((AnimationState)candidates.get(this.poses.nextInt(candidates.size()))).m_216977_(entity.f_19797_ - 4 - this.poses.nextInt(12));
        }
    }

    private void fallback(GuiGraphics graphics, ResourceLocation id, int x, int y, int width, int height) {
        Font font = Minecraft.m_91087_().f_91062_;
        List lines = font.m_92923_((FormattedText)this.name(id), Math.max(1, width - 8));
        int n = lines.size();
        Objects.requireNonNull(font);
        int visibleLines = Math.min(n, Math.max(1, height / 9));
        Objects.requireNonNull(font);
        int top = y + (height - visibleLines * 9) / 2;
        for (int i = 0; i < visibleLines; ++i) {
            FormattedCharSequence formattedCharSequence = (FormattedCharSequence)lines.get(i);
            int n2 = x + width / 2;
            Objects.requireNonNull(font);
            graphics.m_280364_(font, formattedCharSequence, n2, top + i * 9, -1450812);
        }
    }

    record Bounds(int x, int y, int width, int height) {
    }
}

