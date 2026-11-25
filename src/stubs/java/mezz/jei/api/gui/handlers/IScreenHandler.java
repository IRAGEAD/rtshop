package mezz.jei.api.gui.handlers;

import net.minecraft.client.gui.screens.Screen;

/** 컴파일용 자리표시 (JEI 의 실제 클래스가 실행 때 쓰인다; jar 에 들어가지 않음). */
@FunctionalInterface
public interface IScreenHandler<T extends Screen> {
    IGuiProperties apply(T guiScreen);
}
