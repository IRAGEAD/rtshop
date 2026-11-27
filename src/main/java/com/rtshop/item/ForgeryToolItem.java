package com.rtshop.item;

import com.rtshop.stock.Forgery;
import com.rtshop.stock.Stocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 히든 직업의 도구 (3.76.0): 위조 도구 꾸러미(위조자) · 감정 돋보기(조사관). 한 손에 도구, 다른 손에 증서를 들고 우클릭한다.
 * 할 일은 stock/Forgery 가 한다. 직업이 없으면 쓰지 못한다 (무엇을 하는 도구인지는 설명에 적혀 있다).
 */
public class ForgeryToolItem extends Item {
    public static final int KIT = 0, LOUPE = 1;
    private final int kind;

    public ForgeryToolItem(Properties props, int kind) {
        super(props);
        this.kind = kind;
    }

    public int kind() {
        return kind;
    }

    /** 3.78 살펴보기: 히든 직업의 도구도 처음엔 무엇인지 모른다 (화면 쪽에서 이름을 감춘다). */
    @Override
    public Component getName(ItemStack stack) {
        if (com.rtshop.story.Examine.hideName(stack)) return com.rtshop.story.Examine.unknownName();
        return super.getName(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(tool);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(tool);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        MutableComponent msg;
        boolean good;
        if (kind == KIT) {
            int r = Forgery.forge(sp, other);
            good = r == Forgery.OK;
            msg = switch (r) {
                case Forgery.OK -> Component.translatable("rtshop.forge.done", Component.translatable("rtshop.forge.tier." + Forgery.tier(Forgery.lastQualityForTest)),
                        Forgery.forgeLeft(sp));
                case Forgery.NOT_JOB -> Component.translatable("rtshop.forge.not_job");
                case Forgery.NO_TEMPLATE -> Component.translatable("rtshop.forge.no_template");
                case Forgery.BAD_TEMPLATE -> Component.translatable("rtshop.forge.bad_template");
                case Forgery.EXPOSED_TEMPLATE -> Component.translatable("rtshop.forge.exposed_template");
                case Forgery.LIMIT -> Component.translatable("rtshop.forge.limit");
                case Forgery.NO_PAPER -> Component.translatable("rtshop.forge.no_paper");
                case Forgery.NO_INK -> Component.translatable("rtshop.forge.no_ink");
                case Forgery.NO_WAX -> Component.translatable("rtshop.forge.no_wax");
                case Forgery.FULL -> Component.translatable("rtshop.forge.full");
                default -> Component.translatable("rtshop.forge.off");
            };
            if (good) {
                level.playSound(null, sp.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.8F, 1.1F);
                level.playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.9F, 0.9F);
            }
        } else {
            int r = Forgery.appraise(sp, other);
            good = r == Forgery.FOUND;
            msg = switch (r) {
                case Forgery.FOUND -> {
                    MutableComponent m = Component.translatable("rtshop.appraise.found", Component.translatable("rtshop.appraise.flaw." + Math.max(0, Forgery.lastFlawForTest)));
                    long got = Forgery.lastFineForTest[2];
                    if (got > 0) m.append(" ").append(Component.translatable("rtshop.appraise.reward", Stocks.num(got)));
                    yield m;
                }
                case Forgery.CLEAN -> Component.translatable("rtshop.appraise.clean");
                case Forgery.AGAIN -> Component.translatable("rtshop.appraise.again");
                case Forgery.NOT_JOB -> Component.translatable("rtshop.appraise.not_job");
                case Forgery.NO_TEMPLATE -> Component.translatable("rtshop.appraise.no_cert");
                case Forgery.EXPOSED_TEMPLATE -> Component.translatable("rtshop.appraise.already");
                default -> Component.translatable("rtshop.forge.off");
            };
            level.playSound(null, sp.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
            if (good) {
                level.playSound(null, sp.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7F, 0.7F);
                sp.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, sp.getX(), sp.getEyeY() - 0.3, sp.getZ(), 8, 0.25, 0.15, 0.25, 0.05);
            }
        }
        com.rtshop.notice.Notice.send(sp, msg.withStyle(good ? ChatFormatting.GOLD : ChatFormatting.YELLOW), true);
        sp.getCooldowns().addCooldown(this, 15);
        return InteractionResultHolder.consume(tool);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tips, TooltipFlag flag) {
        String base = kind == KIT ? "item.rtshop.forger_kit" : "item.rtshop.appraisal_loupe";
        tips.add(Component.translatable(base + ".tip1").withStyle(ChatFormatting.GRAY));
        tips.add(Component.translatable(base + ".tip2").withStyle(ChatFormatting.DARK_GREEN));
    }
}
