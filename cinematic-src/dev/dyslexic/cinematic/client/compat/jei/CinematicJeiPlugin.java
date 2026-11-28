/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  mezz.jei.api.IModPlugin
 *  mezz.jei.api.JeiPlugin
 *  mezz.jei.api.runtime.IJeiRuntime
 *  net.minecraft.resources.ResourceLocation
 */
package dev.dyslexic.cinematic.client.compat.jei;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.client.compat.jei.JeiRecipeViewer;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class CinematicJeiPlugin
implements IModPlugin {
    public ResourceLocation getPluginUid() {
        return Cinematic.id("field_guide");
    }

    public void onRuntimeAvailable(IJeiRuntime runtime) {
        JeiRecipeViewer.available(runtime);
    }

    public void onRuntimeUnavailable() {
        JeiRecipeViewer.available(null);
    }
}

