/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses;

import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class BossTargetFinder {
    public static <T extends Entity> List<T> getEntitiesInHorizontalBox(Class<T> clazz, Level level, Vec3 start, Vec2 direction, float length, float width, float height, Predicate<T> predicate) {
        direction = direction.m_165902_();
        Vec3 sideOffset = new Vec3((double)(direction.f_82470_ * width / 2.0f), 0.0, (double)(direction.f_82471_ * width / 2.0f)).m_82524_(1.5707964f);
        Vec3 lengthOffset = new Vec3((double)(direction.f_82470_ * length), 0.0, (double)(direction.f_82471_ * length));
        Vec3 p1 = start.m_82549_(sideOffset);
        Vec3 p2 = start.m_82549_(sideOffset.m_82548_());
        Vec3 p3 = start.m_82549_(lengthOffset);
        Vec3 p4 = start.m_82549_(lengthOffset);
        Vec3 p5 = start.m_82549_(sideOffset).m_82520_(0.0, (double)height, 0.0);
        Vec3 p6 = start.m_82549_(sideOffset.m_82548_()).m_82520_(0.0, (double)height, 0.0);
        Vec3 p7 = start.m_82549_(lengthOffset).m_82520_(0.0, (double)height, 0.0);
        Vec3 p8 = start.m_82549_(lengthOffset).m_82520_(0.0, (double)height, 0.0);
        double minX = Math.min(p1.f_82479_, Math.min(p2.f_82479_, Math.min(p3.f_82479_, Math.min(p4.f_82479_, Math.min(p5.f_82479_, Math.min(p6.f_82479_, Math.min(p7.f_82479_, p8.f_82479_)))))));
        double minY = Math.min(p1.f_82480_, Math.min(p2.f_82480_, Math.min(p3.f_82480_, Math.min(p4.f_82480_, Math.min(p5.f_82480_, Math.min(p6.f_82480_, Math.min(p7.f_82480_, p8.f_82480_)))))));
        double minZ = Math.min(p1.f_82481_, Math.min(p2.f_82481_, Math.min(p3.f_82481_, Math.min(p4.f_82481_, Math.min(p5.f_82481_, Math.min(p6.f_82481_, Math.min(p7.f_82481_, p8.f_82481_)))))));
        double maxX = Math.max(p1.f_82479_, Math.max(p2.f_82479_, Math.max(p3.f_82479_, Math.max(p4.f_82479_, Math.max(p5.f_82479_, Math.max(p6.f_82479_, Math.max(p7.f_82479_, p8.f_82479_)))))));
        double maxY = Math.max(p1.f_82480_, Math.max(p2.f_82480_, Math.max(p3.f_82480_, Math.max(p4.f_82480_, Math.max(p5.f_82480_, Math.max(p6.f_82480_, Math.max(p7.f_82480_, p8.f_82480_)))))));
        double maxZ = Math.max(p1.f_82481_, Math.max(p2.f_82481_, Math.max(p3.f_82481_, Math.max(p4.f_82481_, Math.max(p5.f_82481_, Math.max(p6.f_82481_, Math.max(p7.f_82481_, p8.f_82481_)))))));
        AABB searchBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        List list = level.m_6443_(clazz, searchBox, predicate);
        Iterator iterator = list.iterator();
        double angle = Math.atan2(direction.f_82471_, direction.f_82470_);
        while (iterator.hasNext()) {
            Entity entity = (Entity)iterator.next();
            Vec3 pos = entity.m_20182_();
            Vec3 horizontal = pos.m_82542_(1.0, 0.0, 1.0).m_82546_(start.m_82542_(1.0, 0.0, 1.0));
            Vec3 rotated = horizontal.m_82524_((float)angle);
            if (!(rotated.f_82479_ > (double)length || rotated.f_82479_ < 0.0 || pos.f_82480_ > start.f_82480_ + (double)height || pos.f_82480_ < start.f_82480_) && !(Math.abs(rotated.f_82481_) > (double)(width / 2.0f))) continue;
            iterator.remove();
        }
        return list;
    }

    public static <T extends Entity> List<T> getEntitiesInCylinder(Class<T> clazz, Level level, Vec3 cylinderStart, float cylinderHeight, float cylinderRadius) {
        return BossTargetFinder.getEntitiesInCylinder(clazz, level, cylinderStart, cylinderHeight, cylinderRadius, v -> true);
    }

    public static <T extends Entity> List<T> getEntitiesInCylinder(Class<T> clazz, Level level, Vec3 cylinderStart, float cylinderHeight, float cylinderRadius, Predicate<T> predicate) {
        AABB box = new AABB((double)(-cylinderRadius), 0.0, (double)(-cylinderRadius), (double)cylinderRadius, (double)cylinderHeight, (double)cylinderRadius).m_82383_(cylinderStart);
        List entities = level.m_6443_(clazz, box, entity -> {
            Vec3 entityPos = entity.m_20182_();
            return BossTargetFinder.isPointInCylinder(entityPos, cylinderStart, cylinderHeight, cylinderRadius) && predicate.test(entity);
        });
        return entities;
    }

    public static boolean isPointInCylinder(Vec3 point, Vec3 cylinderStart, float cylinderHeight, float cylinderRadius) {
        Vec3 entityPos = point;
        Vec3 b = entityPos.m_82546_(cylinderStart);
        double horizontalRadius = Math.sqrt(b.f_82479_ * b.f_82479_ + b.f_82481_ * b.f_82481_);
        double h = b.f_82480_;
        return horizontalRadius <= (double)cylinderRadius && h >= 0.0 && h <= (double)cylinderHeight;
    }

    public static <T extends Entity> List<T> getEntitiesInSphere(Class<T> clazz, Level level, Vec3 center, float radius) {
        return BossTargetFinder.getEntitiesInSphere(clazz, level, center, radius, v -> true);
    }

    public static <T extends Entity> List<T> getEntitiesInSphere(Class<T> clazz, Level level, Vec3 center, float radius, Predicate<T> predicate) {
        AABB box = new AABB((double)(-radius), (double)(-radius), (double)(-radius), (double)radius, (double)radius, (double)radius).m_82383_(center);
        List entities = level.m_6443_(clazz, box, entity -> {
            Vec3 entityPos = entity.m_20182_();
            Vec3 b = entityPos.m_82546_(center);
            double d = b.m_82553_();
            return d <= (double)radius && predicate.test(entity);
        });
        return entities;
    }

    public static <T extends Entity> List<T> getEntitiesInArc(Class<T> clazz, Level level, Vec3 start, Vec2 direction, float halfAngle, float cylinderHeight, float cylinderRadius) {
        return BossTargetFinder.getEntitiesInArc(clazz, level, start, direction, halfAngle, cylinderHeight, cylinderRadius, v -> true);
    }

    public static <T extends Entity> List<T> getEntitiesInArc(Class<T> clazz, Level level, Vec3 start, Vec2 direction, float angle, float cylinderHeight, float cylinderRadius, Predicate<T> predicate) {
        List<T> inCylinder = BossTargetFinder.getEntitiesInCylinder(clazz, level, start, cylinderHeight, cylinderRadius, predicate);
        Iterator<T> entities = inCylinder.iterator();
        while (entities.hasNext()) {
            Vec3 dir;
            Entity next = (Entity)entities.next();
            Vec3 position = next.m_20182_();
            Vec3 horizontal = position.m_82546_(start).m_82542_(1.0, 0.0, 1.0).m_82541_();
            double angleBetween = FDMathUtil.angleBetweenVectors((Vec3)horizontal, (Vec3)(dir = new Vec3((double)direction.f_82470_, 0.0, (double)direction.f_82471_)));
            if (!(angleBetween > (double)(angle / 2.0f))) continue;
            entities.remove();
        }
        return inCylinder;
    }
}

