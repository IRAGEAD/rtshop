package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** 컴파일용 자리표시 (Cinematic Interactions 의 실제 클래스가 실행 때 쓰인다; jar 에 들어가지 않음). */
public final class EnchantFx {
    public static final ResourceLocation GLOW = null;

    private EnchantFx() {
    }

    public static void circle(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 center, double radius, double angle, CirclePalette palette, float brightness) {
    }

    public static void circleBillboard(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 center, double radius, double angle, CirclePalette palette, float brightness) {
    }

    public static void billboard(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 center, double radius, double angle, ResourceLocation texture, float r, float g, float b) {
    }

    public static void beam(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 base, double height, double width, double spin, float r, float g, float b) {
    }

    public static void stream(Vec3 from, Vec3 to, int count) {
    }

    public static void burst(Vec3 at, double strength) {
    }

    public static void sparkle(Vec3 at, double radius) {
    }

    public static void gather(Vec3 at, double radius) {
    }

    public enum CirclePalette {
        VIOLET, RED, LAPIS
    }
}
