/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraftforge.fml.ModList
 */
package dev.dyslexic.cinematic.client.guide;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.client.compat.jei.JeiRecipeViewer;
import dev.dyslexic.cinematic.client.guide.RecipeViewer;
import javax.annotation.Nullable;
import net.minecraftforge.fml.ModList;

public final class RecipeViewers {
    private static boolean failed;

    private RecipeViewers() {
    }

    @Nullable
    public static RecipeViewer get() {
        if (failed || !ModList.get().isLoaded("jei")) {
            return null;
        }
        try {
            return JeiRecipeViewer.current();
        }
        catch (LinkageError | RuntimeException e) {
            failed = true;
            Cinematic.LOGGER.warn("[Cinematic] JEI integration unavailable; the scenes go on without the field guide", e);
            return null;
        }
    }
}

