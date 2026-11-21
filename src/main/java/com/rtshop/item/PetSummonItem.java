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
        if (e instanceof Mob m) {
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.SPAWN_EGG, null, null);
            m.setPersistenceRequired();
        }
        if (e instanceof TamableAnimal t) {
            t.tame(p);
            t.setOrderedToSit(false);
        }
        level.addFreshEntityWithPassengers(e);
        return e;
    }

    private static InteractionResult use(ServerPlayer p, ItemStack stack, Vec3 at) {
        Entity e = summon(p, stack, at);
        if (e == null) {
            p.sendSystemMessage(Component.translatable("rtshop.gacha.pet_fail").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        p.sendSystemMessage(Component.translatable("rtshop.gacha.pet_summoned", e.getDisplayName()).withStyle(ChatFormatting.GREEN));
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
