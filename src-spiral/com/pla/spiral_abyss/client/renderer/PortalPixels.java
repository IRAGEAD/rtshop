/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss.client.renderer;

final class PortalPixels {
    static final int WIDTH = 64;
    static final int HEIGHT = 72;
    static final int FRAMES = 64;
    private static final double TAU = Math.PI * 2;
    private static final int[] PALETTE = new int[]{199458, 465469, 798307, 1199245, 1535413, 2461651, 3783148, 6939647, 10941439};
    private static final int[] OUTLINE_X = new int[]{8, 56, 56, 64, 64, 56, 56, 40, 40, 24, 24, 8, 8, 0, 0, 8};
    private static final int[] OUTLINE_Y = new int[]{0, 0, 8, 8, 56, 56, 64, 64, 72, 72, 64, 64, 56, 56, 8, 8};
    private static final double[] BORDER_DISTANCE = new double[4608];
    private static final double[] BORDER_PHASE = new double[4608];

    private PortalPixels() {
    }

    static int pixel(int x, int y, int state, int frame) {
        double width;
        if (!PortalPixels.inside(x, y)) {
            return 0;
        }
        double px = ((double)x + 0.5 - 32.0) / 32.0;
        double py = (35.2 - (double)y - 0.5) / 32.0;
        double radius = Math.sqrt(px * px + py * py);
        double angle = Math.atan2(py, px);
        double time = (double)Math.floorMod(frame, 64) * (Math.PI * 2) / 64.0;
        double distance = BORDER_DISTANCE[y * 64 + x];
        double flow = BORDER_PHASE[y * 64 + x] - time;
        if (state == 0) {
            return PortalPixels.idle(x, y, distance, flow, time);
        }
        double rolling = 0.5 + 0.5 * Math.sin(flow * 4.0 + Math.sin(flow * 3.0) * 0.7);
        double ripple = Math.sin(flow * 7.0 + time * 2.0) * 0.7;
        boolean active = state > 0;
        int shade = 0;
        if (active) {
            double spiral = 0.5 + 0.5 * Math.sin(angle * 4.0 + radius * 8.0 - time * (double)(state == 2 ? 8 : 4) + Math.sin(angle * 3.0 - time * 3.0) * 0.55);
            double cloud = 0.5 + 0.5 * Math.sin(angle * 9.0 + radius * 15.0 + time * 2.0);
            double well = Math.max(0.0, Math.min(1.0, (radius - 0.12) / 0.75));
            shade = (int)(well * (1.1 + Math.pow(spiral, 3.0) * (state == 2 ? 4.5 : 3.5) + cloud * 0.65));
        }
        if (distance < (width = (active ? 3.2 : 1.4) + rolling * (active ? 6.2 : 3.8) + ripple)) {
            int rim;
            double band = distance / width;
            int n = band < 0.18 ? 4 : (band < 0.52 ? 7 : (rim = band < 0.78 ? 5 : 3));
            shade = Math.max(shade, rim += rolling > 0.68 ? 1 : (rolling < 0.2 ? -2 : 0));
        } else if (!active) {
            int grain = Math.floorMod(x * 73 + y * 151, 43);
            if (distance < width + 1.5 && grain == 0 && rolling > 0.55) {
                shade = 5;
            } else {
                return 0;
            }
        }
        if (shade >= 3 && shade <= 6 && Math.floorMod(x * 13 + y * 7, 11) == 0) {
            --shade;
        }
        int rgb = PALETTE[Math.max(0, Math.min(PALETTE.length - 1, shade))];
        return 0xFF000000 | (rgb & 0xFF) << 16 | rgb & 0xFF00 | rgb >> 16;
    }

    private static int idle(int x, int y, double distance, double flow, double time) {
        int shade;
        int alpha;
        double head = 0.5 + 0.5 * Math.sin(flow * 3.0 + Math.sin(flow * 2.0) * 0.8);
        double eddy = 0.5 + 0.5 * Math.sin(flow * 7.0 - distance * 0.48 + Math.sin(flow * 3.0 + time * 2.0));
        double width = 3.8 + head * 7.8 + Math.sin(flow * 9.0 + time * 2.0) * 0.85;
        if (distance >= width || distance > 12.0) {
            return 0;
        }
        double band = distance / width;
        double curl = 0.5 + 0.5 * Math.sin(flow * 5.0 - distance * 0.72 + time * 2.0);
        if (band < 0.16) {
            alpha = 150;
            shade = 5;
        } else if (band < 0.4) {
            alpha = head > 0.58 ? 235 : 185;
            shade = head > 0.55 ? 8 : 6;
        } else if (band < 0.7) {
            alpha = 140;
            shade = eddy > 0.45 ? 6 : 4;
        } else {
            alpha = curl > 0.55 ? 105 : 55;
            int n = shade = eddy > 0.62 ? 5 : 3;
        }
        if (head < 0.22 && band > 0.55 && eddy < 0.38) {
            return 0;
        }
        if (Math.floorMod(x * 13 + y * 7, 11) == 0 && shade > 3) {
            --shade;
        }
        int rgb = PALETTE[shade];
        return alpha << 24 | (rgb & 0xFF) << 16 | rgb & 0xFF00 | rgb >> 16;
    }

    private static boolean inside(int x, int y) {
        if (y < 8) {
            return x >= 8 && x < 56;
        }
        if (y < 56) {
            return true;
        }
        if (y < 64) {
            return x >= 8 && x < 56;
        }
        return x >= 24 && x < 40;
    }

    static {
        double perimeter = 0.0;
        for (int i = 0; i < OUTLINE_X.length; ++i) {
            int next = (i + 1) % OUTLINE_X.length;
            perimeter += Math.hypot(OUTLINE_X[next] - OUTLINE_X[i], OUTLINE_Y[next] - OUTLINE_Y[i]);
        }
        for (int y = 0; y < 72; ++y) {
            for (int x = 0; x < 64; ++x) {
                double best = Double.POSITIVE_INFINITY;
                double along = 0.0;
                double selected = 0.0;
                for (int i = 0; i < OUTLINE_X.length; ++i) {
                    int next = (i + 1) % OUTLINE_X.length;
                    double dx = OUTLINE_X[next] - OUTLINE_X[i];
                    double dy = OUTLINE_Y[next] - OUTLINE_Y[i];
                    double length = Math.hypot(dx, dy);
                    double t = Math.max(0.0, Math.min(1.0, (((double)x + 0.5 - (double)OUTLINE_X[i]) * dx + ((double)y + 0.5 - (double)OUTLINE_Y[i]) * dy) / (length * length)));
                    double distance = Math.hypot((double)x + 0.5 - (double)OUTLINE_X[i] - t * dx, (double)y + 0.5 - (double)OUTLINE_Y[i] - t * dy);
                    if (distance < best) {
                        best = distance;
                        selected = along + t * length;
                    }
                    along += length;
                }
                PortalPixels.BORDER_DISTANCE[y * 64 + x] = best;
                PortalPixels.BORDER_PHASE[y * 64 + x] = selected / perimeter * (Math.PI * 2);
            }
        }
    }
}

