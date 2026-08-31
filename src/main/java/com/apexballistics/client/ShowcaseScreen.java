package com.apexballistics.client;

import com.apexballistics.item.MissileItem;
import com.apexballistics.item.MissileKind;
import com.apexballistics.item.MissileSpecification;
import com.apexballistics.menu.ShowcaseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ShowcaseScreen extends AbstractContainerScreen<ShowcaseMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "apexballistics", "textures/gui/missile_showcase.png");

    public ShowcaseScreen(ShowcaseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 220;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0.0f, 0.0f, imageWidth, imageHeight, 512, 512);
        ItemStack stack = menu.displayed();
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, leftPos + 172, topPos + 70);
            graphics.renderItemDecorations(font, stack, leftPos + 172, topPos + 70);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 20, 14, 0xC8E4F4, false);
        ItemStack stack = menu.displayed();
        if (stack.isEmpty() || !(stack.getItem() instanceof MissileItem missileItem)) {
            graphics.drawString(font, Component.translatable("screen.apexballistics.showcase.empty"),
                    20, 48, 0x7DB5CC, false);
            return;
        }
        MissileKind kind = missileItem.kind();
        MissileSpecification spec = MissileSpecification.fromStack(stack, kind);
        graphics.drawString(font, kind.displayName(), 20, 48, 0xE8F4FF, false);
        graphics.drawString(font, Component.translatable("tooltip.apexballistics.guidance",
                spec.guidance().getSerializedName()), 20, 68, 0x70E0A0, false);
        graphics.drawString(font, Component.translatable("tooltip.apexballistics.payload",
                spec.payload().getSerializedName()), 20, 84, 0xF0C85A, false);
        graphics.drawString(font, Component.translatable("tooltip.apexballistics.fuse",
                spec.fuse().getSerializedName()), 20, 100, 0xD8E4EA, false);
        graphics.drawString(font, Component.translatable("tooltip.apexballistics.reliability",
                (int) (spec.reliability() * 100)), 20, 116, 0xD8E4EA, false);
    }
}
