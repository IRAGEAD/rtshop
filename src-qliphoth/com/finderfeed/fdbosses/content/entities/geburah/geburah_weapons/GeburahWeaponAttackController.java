/*
 * Decompiled with CFR 0.152.
 */
package com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons;

import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.geburah_weapons.GeburahWeaponAttack;

public class GeburahWeaponAttackController {
    private GeburahEntity geburah;
    private GeburahWeaponAttack currentAttack;

    public GeburahWeaponAttackController(GeburahEntity geburah) {
        this.geburah = geburah;
    }

    public void tick() {
        if (this.currentAttack != null) {
            this.currentAttack.tick();
            if (this.currentAttack.hasEnded()) {
                this.currentAttack.onAttackEnd();
                this.currentAttack = null;
            }
        }
    }

    public void setCurrentAttack(GeburahWeaponAttack geburahWeaponAttack, boolean forceChange) {
        if (this.currentAttack == null || forceChange || this.currentAttack.canBeChanged()) {
            if (this.currentAttack != null) {
                this.currentAttack.onAttackEnd();
            }
            geburahWeaponAttack.onAttackStart();
            this.currentAttack = geburahWeaponAttack;
        }
    }

    public void stopAttack() {
        if (this.currentAttack != null) {
            this.currentAttack.onAttackEnd();
            this.currentAttack = null;
        }
    }

    public boolean isAttacking() {
        return this.currentAttack != null;
    }

    public GeburahWeaponAttack getCurrentAttack() {
        return this.currentAttack;
    }
}

