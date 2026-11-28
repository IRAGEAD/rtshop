package com.rtshop.item;

import com.rtshop.gacha.Gacha;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

/** 펫 소환권: 쓰면 그 자리에 펫이 나에게 길들여진 채로 나타난다. NBT "entity" = 엔티티 ID. */
public class PetSummonItem extends Item {
    public PetSummonItem(Properties props) {
        super(props);
    }

    public static String entityOf(ItemStack s) {
        return s.hasTag() ? s.getTag().getString("entity") : "";
    }

    @Override
    public Component getName(ItemStack stack) {
        String id = entityOf(stack);
        if (id.isEmpty()) {
            return super.getName(stack);
        }
        return Component.translatable("item.rtshop.pet_summon.named", Gacha.petName(id));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.rtshop.pet_summon.tip").withStyle(ChatFormatting.GRAY));
    }

    /** 펫을 소환해 주인으로 길들인다. 성공하면 그 엔티티, 실패하면 null. */
    /** 3.84: 뽑기로 부른 펫에 붙이는 태그. */
    public static final String PET_TAG = "rtshop_pet";

    public static Entity summon(ServerPlayer p, ItemStack stack, Vec3 at) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(entityOf(stack)));
        if (type == null || !(p.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity e = type.create(level);
        if (e == null) {
            return null;
        }
        e.moveTo(at.x, at.y, at.z, p.getYRot() + 180, 0);
        e.addTag(PET_TAG);   // 3.84: 뽑기로 부른 펫 — 마을의 생물 금지(land/ZoneCreatures)에서 빠진다
        if (e instanceof Mob m) {
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.SPAWN_EGG, null, null);
            m.setPersistenceRequired();
        }
        if (e instanceof TamableAnimal t) {
            t.tame(p);
            t.setOrderedToSit(false);
        }
        level.addFreshEntityWithPassengers(e);
        if (!(e instanceof TamableAnimal)) {
            tameOther(e, p);
        }
        return e;
    }

    /** 탈것 (말 종류, Fantasy Mounts 의 FantasyBeast 등): 그 모드의 길들이기 메서드를 찾아 부른다. */
    public static boolean tameOther(Entity e, ServerPlayer p) {
        if (e instanceof net.minecraft.world.entity.animal.horse.AbstractHorse h) {
            h.tameWithName(p);
            return true;
        }
        for (String name : new String[]{"tameBy", "tameWithName", "setTamedBy", "tame", "giveTo"}) {
            for (java.lang.reflect.Method m : e.getClass().getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(ServerPlayer.class)) {
                    try {
                        m.invoke(e, p);
                        return true;
                    } catch (Exception ex) {
                        com.rtshop.RtShop.LOGGER.warn("[rtshop] {} 을(를) 길들이지 못했습니다: {}", e.getType(), ex.toString());
                    }
                }
            }
        }
        return false;
    }

    private static InteractionResult use(ServerPlayer p, ItemStack stack, Vec3 at) {
        Entity e = summon(p, stack, at);
        if (e == null) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.gacha.pet_fail").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.gacha.pet_summoned", e.getDisplayName()).withStyle(ChatFormatting.GREEN));
        if (!p.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide() || !(ctx.getPlayer() instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        return use(sp, ctx.getItemInHand(), Vec3.atBottomCenterOf(pos));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(s);
        }
        Vec3 at = sp.position().add(sp.getLookAngle().multiply(1.5, 0, 1.5));
        InteractionResult r = use(sp, s, at);
        return r == InteractionResult.FAIL ? InteractionResultHolder.fail(s) : InteractionResultHolder.consume(s);
    }
}
