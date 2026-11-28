/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.Util
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance$Attenuation
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.client.portal;

import com.pla.spiral_abyss.block.entity.AbyssPortalBlockEntity;
import com.pla.spiral_abyss.client.platform.AbyssClientEvents;
import com.pla.spiral_abyss.client.screen.FloorSelectionScreen;
import com.pla.spiral_abyss.init.BlockInit;
import com.pla.spiral_abyss.init.SoundInit;
import com.pla.spiral_abyss.network.AbyssNetwork;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PortalEntryAnimation {
    private static final long DURATION_MILLIS = 4200L;
    private static EntryScreen current;

    private PortalEntryAnimation() {
    }

    public static void open(AbyssNetwork.OpenFloors state) {
        AbyssPortalBlockEntity portal;
        Minecraft client = Minecraft.m_91087_();
        if (client.f_91074_ == null || client.f_91073_ == null) {
            return;
        }
        if (state.inside() || state.entryPortal() == null) {
            FloorSelectionScreen.open(state);
            return;
        }
        if (!client.f_91073_.m_46472_().equals(Level.f_46428_)) {
            return;
        }
        BlockEntity blockEntity = client.f_91073_.m_7702_(state.entryPortal());
        if (!(blockEntity instanceof AbyssPortalBlockEntity) || !(portal = (AbyssPortalBlockEntity)blockEntity).frame().active((Level)client.f_91073_)) {
            AbyssNetwork.select(state.token(), 0);
            return;
        }
        EntryScreen screen = new EntryScreen(state, portal);
        client.m_91152_((Screen)screen);
        current = screen;
        screen.playSound();
    }

    public static boolean active() {
        return current != null && Minecraft.m_91087_().f_91080_ == current && Minecraft.m_91087_().f_91073_ == PortalEntryAnimation.current.level && Minecraft.m_91087_().f_91074_ == PortalEntryAnimation.current.player;
    }

    public static float intensity(AbyssPortalBlockEntity portal, float partialTick) {
        if (!PortalEntryAnimation.active() || !portal.m_58899_().equals((Object)PortalEntryAnimation.current.state.entryPortal())) {
            return 0.0f;
        }
        return PortalEntryAnimation.smooth((float)current.elapsed() / 850.0f);
    }

    public static CameraPose camera() {
        return PortalEntryAnimation.active() ? current.cameraPose() : null;
    }

    public static void closeForReset() {
        EntryScreen screen = current;
        if (screen == null) {
            return;
        }
        screen.handedOff = true;
        screen.stopSound();
        current = null;
        if (Minecraft.m_91087_().f_91080_ == screen) {
            Minecraft.m_91087_().m_91152_(null);
        }
    }

    public static void logout(AbyssClientEvents.ClientPlayerNetworkEvent.LoggingOut event) {
        PortalEntryAnimation.closeForReset();
    }

    public static void hud(AbyssClientEvents.RenderGuiEvent.Pre event) {
        if (PortalEntryAnimation.active()) {
            event.setCanceled(true);
        }
    }

    public static void hand(AbyssClientEvents.RenderHandEvent event) {
        if (PortalEntryAnimation.active()) {
            event.setCanceled(true);
        }
    }

    public static void outline(AbyssClientEvents.RenderHighlightEvent.Block event) {
        Minecraft client = Minecraft.m_91087_();
        if (PortalEntryAnimation.active() || client.f_91073_ != null && client.f_91073_.m_8055_(event.getTarget().m_82425_()).m_60713_((Block)BlockInit.ABYSS_PORTAL.get())) {
            event.setCanceled(true);
        }
    }

    private static float smooth(float value) {
        float t = Mth.m_14036_((float)value, (float)0.0f, (float)1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static final class EntryScreen
    extends Screen {
        private final AbyssNetwork.OpenFloors state;
        private final ClientLevel level;
        private final LocalPlayer player;
        private final UUID portalId;
        private final long started = Util.m_137550_();
        private final Vec3 initialCamera;
        private final Vec3 eye;
        private final Vec3 target;
        private final Vec3 outward;
        private final float initialYaw;
        private final float initialPitch;
        private boolean handedOff;
        private SoundInstance entrySound;

        private EntryScreen(AbyssNetwork.OpenFloors state, AbyssPortalBlockEntity portal) {
            super((Component)Component.m_237115_((String)"screen.spiral_abyss.entering"));
            this.state = state;
            Minecraft client = Minecraft.m_91087_();
            this.level = client.f_91073_;
            this.player = client.f_91074_;
            this.portalId = portal.portalId();
            Camera camera = client.f_91063_.m_109153_();
            this.initialCamera = camera.m_90583_();
            this.initialYaw = camera.m_90590_();
            this.initialPitch = camera.m_90589_();
            this.eye = this.player.m_146892_();
            this.target = Vec3.m_82528_((Vec3i)portal.frame().base()).m_82520_(0.5, 2.8, 0.5);
            Vec3 normal = Vec3.m_82528_((Vec3i)portal.frame().front().m_122436_());
            double side = -this.player.m_20154_().m_82526_(normal);
            if (Math.abs(side) < 0.35) {
                side = this.player.m_20182_().m_82546_(this.target).m_82526_(normal);
            }
            this.outward = normal.m_82490_(side < 0.0 ? -1.0 : 1.0);
        }

        private long elapsed() {
            return Math.max(0L, Util.m_137550_() - this.started);
        }

        private void playSound() {
            this.entrySound = new SimpleSoundInstance(((SoundEvent)SoundInit.PORTAL_ENTRY.get()).m_11660_(), SoundSource.BLOCKS, 0.8f, 1.0f, SoundInstance.m_235150_(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
            Minecraft.m_91087_().m_91106_().m_120367_(this.entrySound);
        }

        private void stopSound() {
            if (this.entrySound == null) {
                return;
            }
            Minecraft.m_91087_().m_91106_().m_120399_(this.entrySound);
            this.entrySound = null;
        }

        private boolean valid() {
            AbyssPortalBlockEntity portal;
            BlockEntity blockEntity;
            Minecraft client = Minecraft.m_91087_();
            return client.f_91073_ == this.level && client.f_91074_ == this.player && this.player.m_6084_() && !this.player.m_5833_() && this.player.f_20916_ == 0 && this.player.m_20238_(Vec3.m_82512_((Vec3i)this.state.entryPortal())) <= 100.0 && this.level.m_46805_(this.state.entryPortal()) && (blockEntity = this.level.m_7702_(this.state.entryPortal())) instanceof AbyssPortalBlockEntity && (portal = (AbyssPortalBlockEntity)blockEntity).portalId().equals(this.portalId) && portal.frame().active((Level)this.level);
        }

        public void m_86600_() {
            if (!this.valid()) {
                this.m_7379_();
                return;
            }
            if (this.elapsed() >= 4200L) {
                this.finish();
            }
        }

        private CameraPose cameraPose() {
            float pullBack = PortalEntryAnimation.smooth((float)this.elapsed() / 650.0f);
            float approach = PortalEntryAnimation.smooth((float)(this.elapsed() - 650L) / 3200.0f);
            double distance = Mth.m_14139_((double)approach, (double)8.0, (double)3.8);
            Vec3 side = new Vec3(this.outward.f_82481_, 0.0, -this.outward.f_82479_).m_82490_(0.35);
            Vec3 desired = this.eye.m_82549_(this.outward.m_82490_(distance)).m_82549_(side).m_82520_(0.0, Mth.m_14139_((double)approach, (double)0.55, (double)0.15), 0.0);
            desired = this.initialCamera.m_165921_(desired, (double)pullBack);
            Vec3 position = this.clipCamera(desired);
            Vec3 look = this.target.m_82546_(position);
            float yaw = (float)(Math.toDegrees(Math.atan2(look.f_82481_, look.f_82479_)) - 90.0);
            float pitch = (float)(-Math.toDegrees(Math.atan2(look.f_82480_, look.m_165924_())));
            return new CameraPose(position, this.initialYaw + Mth.m_14177_((float)(yaw - this.initialYaw)) * pullBack, Mth.m_14179_((float)pullBack, (float)this.initialPitch, (float)pitch));
        }

        private Vec3 clipCamera(Vec3 desired) {
            Vec3 ray = desired.m_82546_(this.eye);
            double length = ray.m_82553_();
            if (length < 0.001) {
                return this.eye;
            }
            double allowed = length;
            for (int corner = 0; corner < 8; ++corner) {
                Vec3 offset = new Vec3((corner & 1) == 0 ? -0.15 : 0.15, (corner & 2) == 0 ? -0.15 : 0.15, (corner & 4) == 0 ? -0.15 : 0.15);
                BlockHitResult hit = this.level.m_45547_(new ClipContext(this.eye.m_82549_(offset), desired.m_82549_(offset), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, (Entity)this.player));
                if (hit.m_6662_() == HitResult.Type.MISS) continue;
                allowed = Math.min(allowed, Math.max(0.0, hit.m_82450_().m_82554_(this.eye.m_82549_(offset)) - 0.2));
            }
            return this.eye.m_82549_(ray.m_82490_(allowed / length));
        }

        private void finish() {
            if (!this.valid()) {
                this.m_7379_();
                return;
            }
            this.handedOff = true;
            this.stopSound();
            current = null;
            long remaining = this.state.resetRemainingMillis() < 0L ? this.state.resetRemainingMillis() : Math.max(0L, this.state.resetRemainingMillis() - this.elapsed());
            FloorSelectionScreen.open(new AbyssNetwork.OpenFloors(this.state.token(), this.state.claimed(), this.state.unlocked(), this.state.selectable(), this.state.visible(), this.state.inside(), remaining, this.state.floors(), null, this.state.endlessWave(), this.state.sections()));
        }

        public boolean m_7933_(int key, int scan, int modifiers) {
            if (key == 32 || key == 257 || key == 335) {
                this.finish();
                return true;
            }
            return super.m_7933_(key, scan, modifiers);
        }

        public void m_7379_() {
            this.f_96541_.m_91152_(null);
        }

        public void m_7861_() {
            this.stopSound();
            if (current == this) {
                current = null;
            }
            if (!this.handedOff && Minecraft.m_91087_().m_91403_() != null) {
                AbyssNetwork.select(this.state.token(), 0);
            }
        }

        public boolean m_7043_() {
            return false;
        }

        public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bar = Math.round((float)this.f_96544_ * 0.085f * PortalEntryAnimation.smooth((float)this.elapsed() / 350.0f));
            graphics.m_280509_(0, 0, this.f_96543_, bar, -16446696);
            graphics.m_280509_(0, this.f_96544_ - bar, this.f_96543_, this.f_96544_, -16446696);
            graphics.m_280653_(this.f_96547_, (Component)Component.m_237115_((String)"screen.spiral_abyss.entry_controls"), this.f_96543_ / 2, this.f_96544_ - Math.max(12, bar / 2 + 4), 12046051);
            int fade = Math.round(255.0f * PortalEntryAnimation.smooth((float)(this.elapsed() - 3850L) / 350.0f));
            if (fade > 0) {
                graphics.m_280509_(0, 0, this.f_96543_, this.f_96544_, fade << 24 | 0x30817);
            }
        }
    }

    public record CameraPose(Vec3 position, float yaw, float pitch) {
    }
}

