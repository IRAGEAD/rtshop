/*
 * Decompiled with CFR 0.152.
 */
package dev.dyslexic.cinematic.client;

interface LayoutEditor {
    public boolean editing();

    public boolean visible();

    public boolean over(double var1, double var3);

    public boolean click(double var1, double var3, int var5);

    public boolean drag(double var1, double var3, int var5);

    public boolean scroll(double var1, double var3, double var5);

    public boolean keyPressed(int var1);

    public void release();
}

