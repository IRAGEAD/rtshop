/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.registries.DeferredRegister
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSin;
import com.finderfeed.fdbosses.content.entities.geburah.sins.instances.CrystalsOfSinSin;
import com.finderfeed.fdbosses.content.entities.geburah.sins.instances.JumpSin;
import com.finderfeed.fdbosses.content.entities.geburah.sins.instances.KillEntitySin;
import com.finderfeed.fdbosses.content.entities.geburah.sins.instances.MoveClockwiseSin;
import com.finderfeed.fdbosses.content.entities.geburah.sins.instances.PressedTooManyButtonsSin;
import com.finderfeed.fdbosses.content.entities.geburah.sins.instances.StayStillSin;
import com.finderfeed.fdbosses.init.BossRegistries;
import java.util.function.Supplier;
import net.minecraftforge.registries.DeferredRegister;

public class GeburahSins {
    public static final DeferredRegister<PlayerSin> SINS = DeferredRegister.create(BossRegistries.PLAYER_SIN, (String)"fdbosses");
    public static final Supplier<JumpSin> JUMPING_SIN = SINS.register("jumping_sin", JumpSin::new);
    public static final Supplier<MoveClockwiseSin> MOVE_CLOCKWISE_SIN = SINS.register("move_clockwise_sin", MoveClockwiseSin::new);
    public static final Supplier<MoveClockwiseSin> MOVE_COUNTERCLOCKWISE_SIN = SINS.register("move_counterclockwise_sin", MoveClockwiseSin::new);
    public static final Supplier<PressedTooManyButtonsSin> PRESSED_TOO_MANY_BUTTONS_SIN = SINS.register("pressed_too_many_buttons", PressedTooManyButtonsSin::new);
    public static final Supplier<CrystalsOfSinSin> CRYSTAL_OF_SIN = SINS.register("crystal_of_sin", CrystalsOfSinSin::new);
    public static final Supplier<KillEntitySin> KILL_ENTITY_SIN = SINS.register("kill_entity_sin", KillEntitySin::new);
    public static final Supplier<StayStillSin> STAY_STILL_SIN = SINS.register("stay_still_sin", StayStillSin::new);
}

