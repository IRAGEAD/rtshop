package com.rtshop.item;

import com.rtshop.stock.StockConfig;
import com.rtshop.stock.StockData;
import com.rtshop.stock.Stocks;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 조합 증서 (3.72.0): 조합 · 주 수 · 일련번호 · 발행일 · 배당 도장. 손으로 건네거나 플레이어 상점에 올릴 수 있고,
 * 은행원(증권 창구)에게 가져가면 그날 시세로 되판다. 장부(StockData)에 적힌 것만 통한다. 우클릭하면 시세판을 본다.
 * 조합 이름(한국어 · 영어)은 발행할 때 증서에 함께 적어 두므로 서버 설정이 없는 화면에서도 읽힌다.
 */
public class StockCertificateItem extends Item {
    public static final String TAG = "rtshop_stock";

    public StockCertificateItem(Properties props) {
        super(props);
    }

    public static ItemStack create(UUID id, StockConfig.Union u, StockData.Cert cert) {
        ItemStack stack = new ItemStack(ModItems.STOCK_CERTIFICATE.get());
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putString("union", u.id);
        t.putString("name", u.name);
        t.putString("nameEn", u.nameEn);
        t.putInt("color", u.color);
        t.putInt("shares", cert.shares);
        t.putLong("no", cert.no);
        t.putLong("issued", cert.issuedDay);
        t.putLong("price", cert.price);
        stack.getOrCreateTag().put(TAG, t);
        return stack;
    }

    @Nullable
    public static UUID id(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t != null && t.hasUUID("id") ? t.getUUID("id") : null;
    }

    public static String union(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t == null ? "" : t.getString("union");
    }

    public static int shares(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t == null ? 0 : Math.max(0, t.getInt("shares"));
    }

    public static long no(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t == null ? 0 : t.getLong("no");
    }

    /**
     * 위조 판명 도장 (3.76.0): 거래소나 조사관이 가짜임을 밝혀낸 증서. 값은 드러낸 쪽 (조사관 이름, "" = 거래소).
     * 이 도장이 없으면 가짜도 진짜와 똑같이 보인다 — 위조인지는 서버의 기록(StockData.forged)만 안다.
     */
    @Nullable
    public static String exposed(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t != null && t.contains("exposed") ? t.getString("exposed") : null;
    }

    public static void markExposed(ItemStack stack, String by) {
        CompoundTag t = stack.getTagElement(TAG);
        if (t != null) {
            t.putString("exposed", by == null ? "" : by);
            t.remove("paidDay");
        }
    }

    /** 배당 도장 (받은 날). */
    public static void stamp(ItemStack stack, long day) {
        CompoundTag t = stack.getTagElement(TAG);
        if (t != null) t.putLong("paidDay", day);
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        if (t != null && t.contains("exposed")) {
            String ko = t.getString("name"), en = t.getString("nameEn").isEmpty() ? ko : t.getString("nameEn");
            return Component.translatable("item.rtshop.stock_certificate.fake_named", ko, en, t.getInt("shares"));
        }
        if (t != null && t.getInt("shares") > 0) {
            // 언어 파일이 이름 둘 중 하나를 고른다 (ko "%1$s 증서 · %3$s주", en "%2$s certificate · %3$s shares"): 화면 쪽 코드가 필요 없다
            String ko = t.getString("name"), en = t.getString("nameEn").isEmpty() ? ko : t.getString("nameEn");
            return Component.translatable("item.rtshop.stock_certificate.named", ko, en, t.getInt("shares"));
        }
        return super.getName(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t != null && t.contains("paidDay") && !t.contains("exposed");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tips, TooltipFlag flag) {
        CompoundTag t = stack.getTagElement(TAG);
        if (t == null) {
            tips.add(Component.translatable("item.rtshop.stock_certificate.blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tips.add(Component.translatable("item.rtshop.stock_certificate.no", String.format("%06d", t.getLong("no"))).withStyle(ChatFormatting.GRAY));
        tips.add(Component.translatable("item.rtshop.stock_certificate.issued", Stocks.date(t.getLong("issued")), String.format("%,d", t.getLong("price")))
                .withStyle(ChatFormatting.GRAY));
        if (t.contains("exposed")) {
            String by = t.getString("exposed");
            tips.add((by.isEmpty() ? Component.translatable("item.rtshop.stock_certificate.exposed_exchange")
                    : Component.translatable("item.rtshop.stock_certificate.exposed", by)).withStyle(ChatFormatting.RED));
            return;
        }
        if (t.contains("paidDay")) {
            tips.add(Component.translatable("item.rtshop.stock_certificate.paid", Stocks.date(t.getLong("paidDay"))).withStyle(ChatFormatting.GOLD));
        }
        tips.add(Component.translatable("item.rtshop.stock_certificate.tip1").withStyle(ChatFormatting.DARK_GREEN));
        tips.add(Component.translatable("item.rtshop.stock_certificate.tip2").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            Stocks.open(sp, false, null, null, true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
