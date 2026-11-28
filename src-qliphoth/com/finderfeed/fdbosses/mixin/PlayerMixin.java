/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.finderfeed.fdbosses.mixin;

import com.finderfeed.fdbosses.content.entities.malkuth_boss.malkuth_chain.MalkuthChainEntity;
import com.finderfeed.fdbosses.content.items.chesed.PhaseSphereHandler;
import com.finderfeed.fdbosses.content.util.Undismountable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Player.class})
public class PlayerMixin {
    @Inject(method={"wantsToStopRiding"}, at={@At(value="HEAD")}, cancellable=true)
    public void wantsToStopRiding(CallbackInfoReturnable<Boolean> cir) {
        block4: {
            block3: {
                Entity vehicle;
                block2: {
                    vehicle = ((Player)this).m_20202_();
                    if (!(vehicle instanceof MalkuthChainEntity)) break block2;
                    MalkuthChainEntity malkuthChainEntity = (MalkuthChainEntity)vehicle;
                    break block3;
                }
                if (!(vehicle instanceof Undismountable)) break block4;
            }
            cir.setReturnValue((Object)false);
        }
    }

    @Inject(method={"tick"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/entity/player/Player;isSpectator()Z", ordinal=1)})
    public void tick(CallbackInfo ci) {
        PhaseSphereHandler.onChesedItemUse((Player)this);
    }
}

