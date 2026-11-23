package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec3;

/** 컴파일용 자리표시 (Cinematic Interactions 의 실제 클래스가 실행 때 쓰인다; jar 에 들어가지 않음). */
public abstract class MenuStation<M extends AbstractContainerMenu> extends ClientStation {
    protected double denyAt = -10.0;

    protected MenuStation(BlockPos pos, Direction side, Class<M> menuType, int inputs) {
        super(pos, side);
    }

    public M menu() {
        return null;
    }

    protected void deny() {
    }

    protected double denyShake() {
        return 0;
    }

    @Override
    public void click(int button, boolean shift) {
    }

    @Override
    public boolean busy() {
        return false;
    }

    protected abstract void act(int button, boolean shift);

    @Override
    public void onEnd() {
    }

    public abstract void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light);

    protected abstract Vec3 restPoint(int slot);

    protected boolean outro() {
        return false;
    }
}
