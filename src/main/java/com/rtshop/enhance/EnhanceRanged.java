package com.rtshop.enhance;

import com.rtshop.RtShop;
import com.rtshop.job.WeaponTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 원거리 · 마법 무기의 강화 (3.65.0). 근접 무기는 공격력 속성이 오르지만, 활 · 쇠뇌 · 총은 쏜 것의 피해가, 지팡이 · 완드는 주문 피해가 오른다.
 * 오르는 비율은 근접 무기와 같은 표(enhance.json 의 weaponBonus, +7 = 28%, +11 = 62%).
 * <ul>
 *   <li>쏜 것(화살 · 총알 등)이 세상에 나올 때, 쏜 사람이 든 원거리 무기의 강화 단계를 그것에 적어 둔다 (나중에 무기를 바꿔 들어도 그 단계).</li>
 *   <li>맞힐 때: 주문 피해면 든 지팡이 · 완드의 단계, 아니면 맞힌 것에 적힌 단계만큼 피해를 키운다. 주문에는 활의 단계를 쓰지 않는다.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class EnhanceRanged {
    private EnhanceRanged() {
    }

    /** 쏜 것에 적는 강화 단계. */
    public static final String TAG = "rtshop_enh_lv";

    /** 손에 든 (주 손 먼저) 그 종류 무기의 강화 단계. 없으면 0. */
    public static int heldLevel(Player p, Enhance.Kind kind) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (!s.isEmpty() && Enhance.kind(s) == kind) {
                return Enhance.level(s);
            }
        }
        return 0;
    }

    public static double multiplier(int lv) {
        if (lv <= 0) {
            return 1.0;
        }
        return 1.0 + Enhance.bonusPct(EnhanceConfig.get(), Enhance.Kind.RANGED, lv) / 100.0;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || e.loadedFromDisk() || !(e.getEntity() instanceof Projectile pr) || pr instanceof ThrownTrident) {
            return;
        }
        if (!(pr.getOwner() instanceof ServerPlayer sp)) {
            return;
        }
        int lv = heldLevel(sp, Enhance.Kind.RANGED);
        if (lv > 0) {
            pr.getPersistentData().putInt(TAG, lv);
        }
    }

    /** 이 피해에 붙는 원거리 · 마법 강화 단계 (점검에서도 쓴다). */
    public static int levelFor(ServerPlayer attacker, DamageSource source) {
        if (WeaponTypes.isMagic(source)) {
            return heldLevel(attacker, Enhance.Kind.MAGIC);
        }
        Entity direct = source.getDirectEntity();
        if (direct != null && direct != attacker && direct.getPersistentData().contains(TAG)) {
            return direct.getPersistentData().getInt(TAG);
        }
        return 0;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        if (e.getEntity().level().isClientSide() || !(e.getSource().getEntity() instanceof ServerPlayer sp) || sp == e.getEntity()) {
            return;
        }
        int lv = levelFor(sp, e.getSource());
        if (lv > 0) {
            e.setAmount((float) (e.getAmount() * multiplier(lv)));
        }
    }
}
