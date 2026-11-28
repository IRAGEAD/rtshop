/*
 * Decompiled with CFR 0.152.
 */
package dev.dyslexic.cinematic.client;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;

final class LayoutFiles {
    private LayoutFiles() {
    }

    static void write(Path target, String json) throws IOException {
        Files.createDirectories(target.getParent(), new FileAttribute[0]);
        Path temp = target.resolveSibling(String.valueOf(target.getFileName()) + ".tmp");
        Files.writeString(temp, (CharSequence)json, new OpenOption[0]);
        int attempt = 0;
        while (true) {
            try {
                try {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                }
                catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            }
            catch (AccessDeniedException e) {
                if (attempt == 3) {
                    throw e;
                }
                try {
                    Thread.sleep(20L << attempt);
                }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while replacing layout file", interrupted);
                }
                ++attempt;
                continue;
            }
            break;
        }
    }
}

