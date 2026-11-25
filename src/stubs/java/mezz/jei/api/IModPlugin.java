package mezz.jei.api;

import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;

/** 컴파일용 자리표시 (JEI 의 실제 클래스가 실행 때 쓰인다; jar 에 들어가지 않음). */
public interface IModPlugin {
    ResourceLocation getPluginUid();

    default void registerGuiHandlers(IGuiHandlerRegistration registration) {
    }
}
