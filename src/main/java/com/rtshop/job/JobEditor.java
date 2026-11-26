package com.rtshop.job;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import com.rtshop.net.JobEditorPacket;
import com.rtshop.net.ResultPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/** 게임 안 직업 편집기의 서버 쪽 (OP 전용): 받은 설정을 검사하고 jobs.json 에 쓴 뒤 바로 적용한다. */
public final class JobEditor {
    private JobEditor() {
    }

    public static void open(ServerPlayer p) {
        if (!com.rtshop.util.Perm.op(p)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.qedit.no_permission"));
            return;
        }
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new JobEditorPacket(JobConfig.get().toJson().toString()));
    }

    /** 저장. 성공하면 null, 실패하면 이유. */
    public static String save(String json) {
        JobConfig c;
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            c = JobConfig.fromJson(o);
        } catch (Exception e) {
            return "저장하지 못했습니다: " + e.getMessage();
        }
        try {
            c.save();
        } catch (Exception e) {
            return "파일에 쓰지 못했습니다: " + e.getMessage();
        }
        JobConfig.reload();
        return null;
    }

    public static void handleSave(ServerPlayer p, String json) {
        if (!com.rtshop.util.Perm.op(p)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.qedit.no_permission"));
            return;
        }
        String err = save(json);
        if (err == null) {
            for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
                Jobs.applyAttributes(o);
            }
            RtShop.LOGGER.info("[rtshop] {} 이(가) 직업 설정을 고쳤습니다", p.getGameProfile().getName());
        }
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new JobEditorPacket(JobConfig.get().toJson().toString()));
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ResultPacket(
                err == null ? Component.translatable("rtshop.jedit.saved") : Component.literal(com.rtshop.story.StoryText.saveError(p, err)), err == null));
    }
}
