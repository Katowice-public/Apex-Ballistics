package com.apexballistics.client;

import com.apexballistics.menu.SirenMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class SirenScreen extends AbstractContainerScreen<SirenMenu> {
    public SirenScreen(SirenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(
                Component.translatable("screen.apexballistics.siren.power"),
                button -> sendButton(0))
                .bounds(leftPos + 14, topPos + 172, 66, 20)
                .build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.apexballistics.siren.auto"),
                button -> sendButton(1))
                .bounds(leftPos + 86, topPos + 172, 76, 20)
                .build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.apexballistics.siren.sound"),
                button -> sendButton(2))
                .bounds(leftPos + 168, topPos + 172, 74, 20)
                .build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.apexballistics.siren.test"),
                button -> sendButton(3))
                .bounds(leftPos + 154, topPos + 42, 90, 18)
                .build());
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation panel = ResourceLocation.fromNamespaceAndPath("apexballistics",
                menu.sirenType().guiTexture());
        graphics.blit(panel, leftPos, topPos, 0.0f, 0.0f, imageWidth, imageHeight, 512, 512);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 20, 14, 0xE8D8C0, false);
        graphics.drawString(font, Component.translatable("screen.apexballistics.siren.power")
                .append(": ")
                .append(onOff(menu.powered())), 20, 48, menu.powered() ? 0x70E0A0 : 0xD8E4EA, false);
        graphics.drawString(font, Component.translatable("screen.apexballistics.siren.auto")
                .append(": ")
                .append(onOff(menu.autoAlert())), 20, 66, menu.autoAlert() ? 0xF0C85A : 0xD8E4EA, false);
        graphics.drawString(font, Component.translatable("screen.apexballistics.siren.sound")
                .append(": ")
                .append(onOff(menu.soundEnabled())), 20, 84, menu.soundEnabled() ? 0x70E0A0 : 0xFF4B3E, false);
        graphics.drawString(font, menu.linked()
                        ? Component.translatable("screen.apexballistics.siren.linked", "OK")
                        : Component.translatable("screen.apexballistics.siren.unlinked"),
                20, 108, menu.linked() ? 0x70E0A0 : 0xFF9D42, false);
        graphics.drawString(font, menu.sounding()
                        ? Component.translatable("screen.apexballistics.siren.sounding")
                        : Component.translatable("screen.apexballistics.siren.silent"),
                20, 132, menu.sounding() ? 0xEF544F : 0x7DB5CC, false);
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value
                ? "tooltip.apexballistics.active"
                : "tooltip.apexballistics.inactive");
    }
}
