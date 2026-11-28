/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.simple_screen.SimpleFDScreen
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.network.chat.Component
 *  org.joml.Vector2f
 */
package com.finderfeed.fdbosses.client.boss_screen;

import com.finderfeed.fdbosses.client.BossRenderUtil;
import com.finderfeed.fdlib.systems.simple_screen.SimpleFDScreen;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

public class SubscribersScreen
extends SimpleFDScreen {
    private List<String> subscribers;
    private Component subscribersName;

    public SubscribersScreen(Component subscribersName, List<String> subscribers) {
        this.subscribers = subscribers;
        this.subscribersName = subscribersName;
    }

    public void m_88315_(GuiGraphics graphics, int mx, int my, float pticks) {
        super.m_88315_(graphics, mx, my, pticks);
        this.m_280273_(graphics);
        BossRenderUtil.renderBossScreenTooltip(graphics.m_280168_(), this.relX - 7.0f - 1.0f, this.relY - 7.0f - 1.0f, this.getScreenWidth(), this.getScreenHeight());
        float xt = this.relX + this.getScreenWidth() / 2.0f;
        float yt = this.relY + 5.0f;
        FDRenderUtil.renderCenteredText((GuiGraphics)graphics, (float)xt, (float)yt, (float)2.0f, (boolean)true, (String)this.subscribersName.getString(), (int)14324834);
        for (int i = 0; i < this.subscribers.size(); ++i) {
            String subscriber = this.subscribers.get(i);
            float x = this.relX + this.getScreenWidth() / 2.0f;
            Objects.requireNonNull(this.f_96547_);
            float y = this.relY + 30.0f + (float)(i * (9 + 1));
            FDRenderUtil.renderCenteredText((GuiGraphics)graphics, (float)x, (float)y, (float)1.0f, (boolean)false, (String)subscriber, (int)14324834);
        }
        Vector2f v = this.getAnchor(0.5f, 0.0f);
        FDRenderUtil.renderFullyCenteredText((GuiGraphics)graphics, (float)v.x, (float)(v.y + 15.0f), (int)250, (float)1.25f, (boolean)true, (int)14324834, (Component)Component.m_237115_((String)"fdbosses.word.not_updated"));
    }

    public float getScreenWidth() {
        return 150.0f;
    }

    public float getScreenHeight() {
        return 200.0f;
    }
}

