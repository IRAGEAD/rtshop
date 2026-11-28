package com.rtshop.job;

import com.rtshop.RtShop;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 직업 패시브 중 전투·채굴·낚시·식사 쪽 (속성 수정치는 Jobs.applyAttributes, 농사·광석은 FarmLootModifier, 판매가는 ShopServer). */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class JobPassives {
    private JobPassives() {
    }

    /** 이 공격에 쓴 무기 종류: 주문이면 magic, 투사체면 손에 든 발사 무기(없으면 활), 아니면 주 손 무기. */
    public static String attackType(ServerPlayer attacker, net.minecraft.world.damagesource.DamageSource source) {
        JobConfig cfg = JobConfig.get();
        if (WeaponTypes.isMagic(source)) {
            return WeaponTypes.MAGIC;
        }
        String main = cfg.weaponType(attacker.getMainHandItem());
        if (source.is(DamageTypeTags.IS_PROJECTILE) && source.getDirectEntity() != attacker) {
            if (!cfg.isMelee(main) && !WeaponTypes.NONE.equals(main) && !WeaponTypes.VANILLA.equals(main)) {
                return main;
            }
            String off = cfg.weaponType(attacker.getOffhandItem());
            if (!cfg.isMelee(off) && !WeaponTypes.NONE.equals(off) && !WeaponTypes.FIST.equals(off) && !WeaponTypes.VANILLA.equals(off)) {
                return off;
            }
            // 3.84: 바닐라 활 · 석궁으로 쏜 것은 직업 보정 없음
            if (vanillaLauncher(attacker)) return WeaponTypes.VANILLA;
            return source.getDirectEntity() instanceof AbstractArrow ? "bow" : main;
        }
        return main;
    }

    /** 손에 든 발사 무기(활 · 석궁 · 삼지창 …)가 바닐라인가 (3.84: 바닐라 무기에는 직업 패시브가 없다). */
    public static boolean vanillaLauncher(ServerPlayer p) {
        if (!JobConfig.get().vanillaNoWeaponPassives) return false;
        ItemStack main = p.getMainHandItem(), off = p.getOffhandItem();
        ItemStack launcher = main.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem || main.getItem() instanceof net.minecraft.world.item.TridentItem ? main
                : off.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem || off.getItem() instanceof net.minecraft.world.item.TridentItem ? off : ItemStack.EMPTY;
        return JobConfig.vanillaItem(launcher);
    }

    /** 공격자 쪽 피해 배율 (직업의 무기 상성). */
    public static double outgoing(ServerPlayer attacker, String weaponType) {
        return Math.max(0.05, 1.0 + Jobs.weapon(attacker, weaponType)[0]);
    }

    /** 궁수 거리 보너스: 5칸부터 늘어나 distanceFull 칸에서 최대(distanceBonus). */
    public static double distanceMultiplier(ServerPlayer shooter, double distance) {
        double max = Jobs.stat(shooter, "distanceBonus");
        if (max <= 0) {
            return 1.0;
        }
        double full = Math.max(6, Jobs.stat(shooter, "distanceFull"));
        double t = Math.max(0, Math.min(1, (distance - 5.0) / (full - 5.0)));
        return 1.0 + max * t;
    }

    /**
     * 탱커 방패 막기: 근접 공격자를 밀쳐 내고, 막은 피해의 일부를 되돌려 준다.
     * 되돌리는 피해는 가시 피해로 줘서 또 막히거나 서로 되돌려 주는 고리가 생기지 않게 한다.
     */
    public static void shieldResponse(ServerPlayer tank, LivingEntity attacker, float blocked) {
        double kb = Jobs.stat(tank, "shieldKnockback");
        if (kb > 0) {
            attacker.knockback(kb, tank.getX() - attacker.getX(), tank.getZ() - attacker.getZ());
            attacker.hurtMarked = true;
        }
        double reflect = Jobs.stat(tank, "shieldReflect");
        if (reflect > 0 && blocked > 0) {
            attacker.hurt(tank.damageSources().thorns(tank), (float) (blocked * reflect));
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(net.minecraftforge.event.entity.living.ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer tank) || event.isCanceled()) {
            return;
        }
        // 근접 공격만 (화살 등 투사체를 막은 경우는 쏜 사람을 밀치지 않는다)
        if (event.getDamageSource().getDirectEntity() instanceof LivingEntity attacker && attacker != tank
                && !event.getDamageSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
                && !event.getDamageSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            shieldResponse(tank, attacker, event.getBlockedDamage());
        }
    }

    /** 받는 피해 배율. */
    public static double incoming(ServerPlayer victim) {
        return Math.max(0.0, 1.0 - Jobs.stat(victim, "damageTaken"));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        float amount = event.getAmount();
        Entity src = event.getSource().getEntity();
        boolean thorns = event.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS);
        if (src instanceof ServerPlayer attacker && src != event.getEntity() && !thorns) {
            String at = attackType(attacker, event.getSource());
            amount *= (float) outgoing(attacker, at);
            if (event.getSource().is(DamageTypeTags.IS_PROJECTILE) && event.getSource().getDirectEntity() != attacker && !WeaponTypes.VANILLA.equals(at)) {
                amount *= (float) distanceMultiplier(attacker, attacker.distanceTo(event.getEntity()));
            }
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            amount *= (float) incoming(victim);
        }
        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onCrit(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !"sword".equals(JobConfig.get().weaponType(sp.getMainHandItem()))) {
            return;
        }
        double bonus = Jobs.stat(sp, "critDamage");
        if (bonus > 0 && (event.isVanillaCritical() || event.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW)) {
            event.setDamageModifier(event.getDamageModifier() * (float) (1.0 + bonus));
        }
    }

    /** 궁수: 일정 확률로 쏜 화살을 돌려받는다 (돌려받은 화살은 땅에서 다시 주울 수 없게 해서 복사되지 않게). */
    @SubscribeEvent
    public static void onArrow(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof ServerPlayer sp) || arrow.pickup != AbstractArrow.Pickup.ALLOWED || sp.isCreative()) {
            return;
        }
        double chance = Jobs.stat(sp, "arrowSave");
        if (chance <= 0 || vanillaLauncher(sp) || sp.getRandom().nextDouble() >= chance) {   // 3.84: 바닐라 활 · 석궁은 화살을 아끼지 않는다
            return;
        }
        ItemStack back;
        if (arrow instanceof SpectralArrow) {
            back = new ItemStack(Items.SPECTRAL_ARROW);
        } else if (arrow.getClass() == Arrow.class && ((Arrow) arrow).getColor() == -1) {
            back = new ItemStack(Items.ARROW);
        } else {
            return; // 물약 화살·모드 화살은 그대로 둔다
        }
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        if (!sp.getInventory().add(back)) {
            sp.drop(back, false);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            double bonus = Jobs.stat(sp, "mineSpeed");
            if (bonus > 0) {
                event.setNewSpeed(event.getNewSpeed() * (float) (1.0 + bonus));
            }
        } else if (event.getEntity().level().isClientSide()) {
            // 채굴 속도는 클라이언트도 같이 계산해야 블록이 깨지는 모습이 맞는다
            double bonus = ClientJobCache.mineSpeed;
            if (bonus > 0) {
                event.setNewSpeed(event.getNewSpeed() * (float) (1.0 + bonus));
            }
        }
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (com.rtshop.rules.FoodScale.mixed()) {
            return;   // 3.84: 요리사 보너스는 음식 값에 이미 들어 있다 (rules/FoodScale)
        }
        ItemStack used = event.getItem();
        FoodProperties food = used.getFoodProperties(sp);
        double bonus = Jobs.stat(sp, "foodBonus");
        if (food == null || bonus <= 0) {
            return;
        }
        int extra = (int) Math.max(1, Math.round(food.getNutrition() * bonus));
        sp.getFoodData().eat(extra, food.getSaturationModifier());
    }

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        double chance = Jobs.stat(sp, "extraFish");
        if (chance <= 0 || sp.getRandom().nextDouble() >= chance) {
            return;
        }
        for (ItemStack s : event.getDrops()) {
            if (s.is(net.minecraft.tags.ItemTags.FISHES)) {
                ItemStack extra = s.copy();
                extra.setCount(1);
                if (!sp.getInventory().add(extra)) {
                    sp.drop(extra, false);
                }
                return;
            }
        }
    }

    /** 광석인가 (광부의 추가 드롭 대상). */
    public static boolean isOre(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Tags.Blocks.ORES);
    }
}
