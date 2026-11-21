package com.rtshop.net;

import net.minecraft.network.FriendlyByteBuf;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** 긴 글(JSON)을 압축해서 보낸다 (클라이언트 -> 서버 패킷은 32KB 까지라 뽑기 목록 같은 큰 설정을 그대로 못 보냄). */
public final class Packed {
    private Packed() {
    }

    private static final int MAX = 8 * 1024 * 1024;

    public static void write(FriendlyByteBuf buf, String s) {
        byte[] raw = s.getBytes(StandardCharsets.UTF_8);
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        d.setInput(raw);
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, raw.length / 4));
        byte[] chunk = new byte[8192];
        while (!d.finished()) {
            int n = d.deflate(chunk);
            out.write(chunk, 0, n);
        }
        d.end();
        buf.writeVarInt(raw.length);
        buf.writeByteArray(out.toByteArray());
    }

    public static String read(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        if (len < 0 || len > MAX) {
            throw new IllegalArgumentException("too large: " + len);
        }
        byte[] packed = buf.readByteArray(MAX);
        Inflater inf = new Inflater();
        inf.setInput(packed);
        byte[] raw = new byte[len];
        try {
            int got = 0;
            while (got < len && !inf.finished()) {
                int n = inf.inflate(raw, got, len - got);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) {
                    break;
                }
                got += n;
            }
        } catch (DataFormatException e) {
            throw new IllegalArgumentException(e);
        } finally {
            inf.end();
        }
        return new String(raw, StandardCharsets.UTF_8);
    }
}
