package com.rtshop.client;

import com.rtshop.net.LandInfoPacket;
import com.rtshop.net.JobEditorPacket;
import com.rtshop.net.JobOpenPacket;
import com.rtshop.net.QuestEditorPacket;
import com.rtshop.net.QuestOpenPacket;
import com.rtshop.shop.ShopData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 클라이언트 전용 진입점 (패킷 핸들러가 DistExecutor 로만 호출). */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void setBalance(long wallet, long bank) {
        ClientEconomy.wallet = wallet;
        ClientEconomy.bank = bank;
    }

    public static void openShop(ShopData data) {
        Minecraft.getInstance().setScreen(new ShopScreen(data));
    }

    public static void openBank(double lossMin, double lossMax) {
        Minecraft.getInstance().setScreen(new BankScreen(lossMin, lossMax));
    }

    public static void onResult(Component message, boolean success) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ShopScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof BankScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof QuestEditScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof JobScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof JobEditScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof PickerScreen p && p.parent instanceof JobEditScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof PickerScreen p && p.parent instanceof QuestEditScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof GachaScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof GachaEditScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof PickerScreen p && p.parent instanceof GachaEditScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof AttendanceScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof AttendanceEditScreen screen) {
            screen.showResult(message, success);
        } else if (mc.screen instanceof PickerScreen p && p.parent instanceof AttendanceEditScreen screen) {
            screen.showResult(message, success);
        } else if (mc.player != null) {
            mc.player.displayClientMessage(message, true);
        }
    }

    /** 퀘스트 창을 열거나, 이미 열려 있으면 내용만 새로 고친다. */
    public static void openQuest(QuestOpenPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof QuestScreen screen) {
            screen.update(packet);
        } else {
            mc.setScreen(new QuestScreen(packet));
        }
    }

    /** 퀘스트 편집기를 열거나, 이미 열려 있으면 내용만 새로 고친다. */
    public static void openQuestEditor(QuestEditorPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof QuestEditScreen screen) {
            screen.update(packet);
        } else if (mc.screen instanceof PickerScreen p && p.parent instanceof QuestEditScreen screen) {
            screen.update(packet);
        } else {
            mc.setScreen(new QuestEditScreen(packet));
        }
    }

    /** 직업 화면을 열거나, 이미 열려 있으면 내용만 새로 고친다. */
    public static void openJobs(JobOpenPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof JobScreen screen) {
            screen.update(packet);
        } else {
            mc.setScreen(new JobScreen(packet));
        }
    }

    /** 직업 편집기를 열거나, 이미 열려 있으면 내용만 새로 고친다. */
    public static void openJobEditor(JobEditorPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof JobEditScreen screen) {
            screen.update(packet.json);
        } else if (mc.screen instanceof PickerScreen p && p.parent instanceof JobEditScreen screen) {
            screen.update(packet.json);
        } else {
            mc.setScreen(new JobEditScreen(packet.json));
        }
    }

    /** 출석 창 / 출석 편집기를 열거나, 이미 열려 있으면 내용만 새로 고친다. */
    public static void openAttendance(com.rtshop.net.AttendancePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (packet.mode == com.rtshop.net.AttendancePacket.EDITOR) {
            if (mc.screen instanceof AttendanceEditScreen screen) {
                screen.update(packet.json);
            } else if (mc.screen instanceof PickerScreen p && p.parent instanceof AttendanceEditScreen screen) {
                screen.update(packet.json);
            } else {
                mc.setScreen(new AttendanceEditScreen(packet.json));
            }
            return;
        }
        if (mc.screen instanceof AttendanceScreen screen) {
            screen.update(packet.json);
        } else {
            mc.setScreen(new AttendanceScreen(packet.json));
        }
    }

    /** 뽑기 창 / 결과 / 편집기 / 자동 분류 / 모의 뽑기. */
    public static void openGacha(com.rtshop.net.GachaPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        GachaEditScreen editor = mc.screen instanceof GachaEditScreen ge ? ge
                : (mc.screen instanceof PickerScreen p && p.parent instanceof GachaEditScreen ge2 ? ge2 : null);
        switch (packet.mode) {
            case com.rtshop.net.GachaPacket.EDITOR -> {
                if (editor != null) {
                    editor.update(packet.json);
                } else {
                    mc.setScreen(new GachaEditScreen(packet.json));
                }
            }
            case com.rtshop.net.GachaPacket.AUTOFILL -> {
                if (editor != null) {
                    editor.autofill(packet.json);
                }
            }
            case com.rtshop.net.GachaPacket.SIM -> {
                if (editor != null) {
                    editor.simulated(packet.json);
                }
            }
            default -> {
                boolean result = packet.mode == com.rtshop.net.GachaPacket.RESULT;
                if (mc.screen instanceof GachaScreen screen) {
                    screen.update(packet.json, result);
                } else {
                    mc.setScreen(new GachaScreen(packet.json));
                    if (result && mc.screen instanceof GachaScreen screen) {
                        screen.update(packet.json, true);
                    }
                }
            }
        }
    }

    public static void openHudEditor() {
        Minecraft.getInstance().setScreen(new HudEditScreen());
    }

    /** 토지 설정 화면을 열거나, 이미 열려 있으면 내용만 새로 고친다. */
    public static void openLand(List<LandInfoPacket.View> views, int currentId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof LandScreen screen) {
            screen.update(views, currentId);
        } else {
            mc.setScreen(new LandScreen(views, currentId));
        }
    }

    /** 방금 연 보관함이 토지 안에 있다는 서버의 알림 (PIN 버튼 표시용). */
    public static void containerInfo(int containerId, BlockPos pos, boolean canSet, boolean hasPin) {
        ClientLand.containerId = containerId;
        ClientLand.containerPos = pos;
        ClientLand.canSetPin = canSet;
        ClientLand.hasPin = hasPin;
    }

    /** PIN 으로 잠긴 보관함을 열려고 했을 때: PIN 입력 화면. */
    public static void openPinEntry(BlockPos pos) {
        Minecraft.getInstance().setScreen(new PinScreen(pos, PinScreen.Mode.ENTER, false));
    }
}
