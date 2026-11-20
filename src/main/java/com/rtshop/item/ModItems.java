package com.rtshop.item;

import com.rtshop.RtShop;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {
    }

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RtShop.MODID);

    /** Gold 수표: 우클릭하면 적힌 금액이 소지금에 들어온다. */
    public static final RegistryObject<Item> GOLD_CHECK = ITEMS.register("gold_check",
            () -> new CheckItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> FIRST_LAND_DEED = ITEMS.register("first_land_deed",
            () -> new DeedItem(DeedItem.Type.FIRST, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LAND_DEED = ITEMS.register("land_deed",
            () -> new DeedItem(DeedItem.Type.NORMAL, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> LARGE_LAND_DEED = ITEMS.register("large_land_deed",
            () -> new DeedItem(DeedItem.Type.LARGE, new Item.Properties().stacksTo(16)));
}
