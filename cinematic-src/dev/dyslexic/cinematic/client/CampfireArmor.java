/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.AbstractClientPlayer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.Sheets
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.item.ArmorItem
 *  net.minecraft.world.item.DyeableLeatherItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.armortrim.ArmorTrim
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.ForgeHooksClient
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.client.LegMesh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;

final class CampfireArmor {
    private static final LegMesh INNER = new LegMesh(0, 16, 0.5f, 32.0f, 0.25f);
    private static final LegMesh OUTER = new LegMesh(0, 16, 1.0f, 32.0f, 0.25f);

    private CampfireArmor() {
    }

    static void drawOnModel(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player, float knee, int light) {
        CampfireArmor.draw(player, buffers, (mesh, consumer, red, green, blue, boots) -> {
            if (boots) {
                mesh.drawShinOnModel(pose, consumer, knee, light, red, green, blue);
            } else {
                mesh.drawBentOnModel(pose, consumer, knee, light, red, green, blue);
            }
        });
    }

    static void drawInScene(PoseStack pose, MultiBufferSource buffers, Vec3 camera, AbstractClientPlayer player, boolean right, Vec3 hip, Vec3 knee, Vec3 foot, Vec3 outward, int light) {
        CampfireArmor.draw(player, buffers, (mesh, consumer, red, green, blue, boots) -> {
            if (boots) {
                mesh.drawShinInScene(pose, consumer, camera, knee, foot, outward, light, red, green, blue);
            } else {
                mesh.drawBentInScene(pose, consumer, camera, hip, knee, foot, outward, light, red, green, blue);
            }
        });
    }

    private static void draw(AbstractClientPlayer player, MultiBufferSource buffers, Geometry geometry) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            int n;
            ArmorItem armor;
            ItemStack stack = player.m_6844_(slot);
            Item item = stack.m_41720_();
            if (!(item instanceof ArmorItem) || (armor = (ArmorItem)item).m_40402_() != slot) continue;
            boolean inner = slot == EquipmentSlot.LEGS;
            LegMesh mesh = inner ? INNER : OUTER;
            String suffix = armor instanceof DyeableLeatherItem ? "overlay" : null;
            String base = "textures/models/armor/" + armor.m_40401_().m_6082_() + "_layer_" + (inner ? 2 : 1) + ".png";
            ResourceLocation texture = new ResourceLocation(ForgeHooksClient.getArmorTexture((Entity)player, (ItemStack)stack, (String)base, (EquipmentSlot)slot, null));
            if (armor instanceof DyeableLeatherItem) {
                DyeableLeatherItem dyeable = (DyeableLeatherItem)armor;
                n = dyeable.m_41121_(stack);
            } else {
                n = 0xFFFFFF;
            }
            int color = n;
            geometry.draw(mesh, buffers.m_6299_(RenderType.m_110431_((ResourceLocation)texture)), (float)(color >> 16 & 0xFF) / 255.0f, (float)(color >> 8 & 0xFF) / 255.0f, (float)(color & 0xFF) / 255.0f, !inner);
            if (suffix != null) {
                String overlay = "textures/models/armor/" + armor.m_40401_().m_6082_() + "_layer_" + (inner ? 2 : 1) + "_overlay.png";
                ResourceLocation over = new ResourceLocation(ForgeHooksClient.getArmorTexture((Entity)player, (ItemStack)stack, (String)overlay, (EquipmentSlot)slot, (String)suffix));
                geometry.draw(mesh, buffers.m_6299_(RenderType.m_110431_((ResourceLocation)over)), 1.0f, 1.0f, 1.0f, !inner);
            }
            ArmorTrim.m_266285_((RegistryAccess)player.m_9236_().m_9598_(), (ItemStack)stack).ifPresent(trim -> {
                ResourceLocation trimTexture = inner ? trim.m_267774_(armor.m_40401_()) : trim.m_267606_(armor.m_40401_());
                VertexConsumer trimBuffer = Minecraft.m_91087_().m_91304_().m_119428_(Sheets.f_265912_).m_118316_(trimTexture).m_118381_(buffers.m_6299_(Sheets.m_266442_()));
                geometry.draw(mesh, trimBuffer, 1.0f, 1.0f, 1.0f, !inner);
            });
            if (!stack.m_41790_()) continue;
            geometry.draw(mesh, buffers.m_6299_(RenderType.m_110484_()), 1.0f, 1.0f, 1.0f, !inner);
        }
    }

    @FunctionalInterface
    private static interface Geometry {
        public void draw(LegMesh var1, VertexConsumer var2, float var3, float var4, float var5, boolean var6);
    }
}

