package com.rtshop.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

/**
 * Epic Fight Nightfall 3.4.2 의 크래시 방지 (소환된 검 등 스킬 효과):
 * Nightfall 은 예전 Entity Culling 의 dev.tr7zw.entityculling.versionless.access.Cullable 을 찾는데,
 * 모드팩의 Entity Culling 1.12 에서는 이 클래스가 dev.tr7zw.entityculling.access.Cullable 로 옮겨져 NoClassDefFoundError 로 게임이 튕겼다.
 * Nightfall 의 연동 함수를 새 위치의 같은 기능으로 대신 처리한다 (없으면 조용히 건너뜀).
 */
@Pseudo
@Mixin(targets = "com.hm.efn.util.CullableUtil", remap = false)
public abstract class NightfallCullMixin {
    private static Class<?> rtshop$type;
    private static boolean rtshop$looked;

    private static Object rtshop$cullable(Entity e) {
        if (!rtshop$looked) {
            rtshop$looked = true;
            for (String n : new String[]{"dev.tr7zw.entityculling.access.Cullable", "dev.tr7zw.entityculling.versionless.access.Cullable"}) {
                try {
                    rtshop$type = Class.forName(n, false, Entity.class.getClassLoader());
                    break;
                } catch (Throwable ignored) {
                    try {
                        rtshop$type = Class.forName(n);
                        break;
                    } catch (Throwable ignored2) {
                    }
                }
            }
        }
        return rtshop$type != null && rtshop$type.isInstance(e) ? e : null;
    }

    private static Object rtshop$call(Entity e, String name, Object arg) {
        Object c = rtshop$cullable(e);
        if (c == null) return null;
        try {
            if (arg == null) {
                Method m = rtshop$type.getMethod(name);
                return m.invoke(c);
            }
            Method m = rtshop$type.getMethod(name, boolean.class);
            return m.invoke(c, arg);
        } catch (Throwable t) {
            return null;
        }
    }

    @Inject(method = "isCulled", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void rtshop$isCulled(Entity e, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(Boolean.TRUE.equals(rtshop$call(e, "isCulled", null)));
    }

    @Inject(method = "isForcedVisible", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void rtshop$isForcedVisible(Entity e, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(Boolean.TRUE.equals(rtshop$call(e, "isForcedVisible", null)));
    }

    @Inject(method = "setCulled", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void rtshop$setCulled(Entity e, boolean v, CallbackInfo ci) {
        rtshop$call(e, "setCulled", v);
        ci.cancel();
    }

    @Inject(method = "setTimeout", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void rtshop$setTimeout(Entity e, CallbackInfo ci) {
        rtshop$call(e, "setTimeout", null);
        ci.cancel();
    }

    @Inject(method = "setAlwaysVisible", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void rtshop$setAlwaysVisible(Entity e, CallbackInfo ci) {
        rtshop$call(e, "setCulled", false);
        rtshop$call(e, "setOutOfCamera", false);
        rtshop$call(e, "setTimeout", null);
        ci.cancel();
    }
}
