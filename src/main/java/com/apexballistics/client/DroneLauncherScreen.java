package com.apexballistics.client;

import com.apexballistics.menu.DroneLauncherMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class DroneLauncherScreen extends AbstractContainerScreen<DroneLauncherMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "apexballistics", "textures/gui/drone_launcher.png");

    public DroneLauncherScreen(DroneLauncherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 220;
        inventoryLabelY = 106;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.apexballistics.launch"),
                        button -> sendButton(0))
                .bounds(leftPos + 128, topPos + 42, 110, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.apexballistics.eject"),
                        button -> sendButton(1))
                .bounds(leftPos + 128, topPos + 66, 110, 20)
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
        graphics.blit(TEXTURE, leftPos, topPos, 0.0f, 0.0f, imageWidth, imageHeight, 512, 512);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 20, 12, 0xE8D8C0, false);
        graphics.drawString(font, Component.translatable("screen.apexballistics.integrity", menu.integrity()),
                128, 90, menu.integrity() < 25 ? 0xFF4B3E : 0x70E0A0, false);
        graphics.drawString(font, Component.translatable("screen.apexballistics.cooldown", menu.cooldown()),
                128, 102, 0xD8E4EA, false);
        graphics.drawString(font, Component.translatable("screen.apexballistics.emp", menu.empTicks()),
                20, 88, menu.empTicks() > 0 ? 0xFF9D42 : 0x7DB5CC, false);
        Component target = menu.hasTarget()
                ? Component.translatable("screen.apexballistics.target", menu.targetX(), menu.targetZ())
                : Component.translatable("screen.apexballistics.no_target");
        graphics.drawString(font, target, 20, 74, 0xF0C85A, false);
    }
}
