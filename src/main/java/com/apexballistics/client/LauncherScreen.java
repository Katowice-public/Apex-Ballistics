package com.apexballistics.client;

import com.apexballistics.block.LauncherType;
import com.apexballistics.menu.LauncherMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class LauncherScreen extends AbstractContainerScreen<LauncherMenu> {
    public LauncherScreen(LauncherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.apexballistics.launch"),
                        button -> sendButton(0))
                .bounds(leftPos + 28, topPos + 174, 92, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.apexballistics.eject"),
                        button -> sendButton(1))
                .bounds(leftPos + 136, topPos + 174, 92, 20)
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
                "textures/gui/launcher_" + menu.launcherType().name().toLowerCase() + ".png");
        graphics.blit(panel, leftPos, topPos, 0.0f, 0.0f,
                imageWidth, imageHeight, 512, 512);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int accent = accentColor(menu.launcherType());
        graphics.drawString(font, title, 18, 14, accent, false);
        graphics.drawString(font, Component.translatable("screen.apexballistics.type",
                menu.launcherType().name()), 20, 42, 0xD8E4EA, false);
        graphics.drawString(font, Component.translatable("screen.apexballistics.magazine",
                menu.loaded(), menu.capacity()), 20, 60, 0xD8E4EA, false);
        graphics.drawString(font, Component.translatable("screen.apexballistics.integrity",
                menu.integrity()), 20, 78,
                menu.integrity() < 25 ? 0xFF4B3E : 0x70E0A0, false);
        graphics.drawString(font, Component.translatable("screen.apexballistics.cooldown",
                menu.cooldown()), 20, 96, 0xD8E4EA, false);
        graphics.drawString(font, Component.translatable("screen.apexballistics.emp",
                menu.empTicks()), 20, 114,
                menu.empTicks() > 0 ? 0xFF9D42 : 0x7DB5CC, false);
        graphics.drawString(font, Component.translatable("screen.apexballistics.airburst",
                menu.airburstHeight()), 20, 132, 0xF0C85A, false);
        Component target = menu.hasTarget()
                ? Component.translatable("screen.apexballistics.target",
                menu.targetX(), menu.targetZ())
                : Component.translatable("screen.apexballistics.no_target");
        graphics.drawString(font, target, 20, 150, accent, false);
    }

    private static int accentColor(LauncherType type) {
        return switch (type) {
            case SILO -> 0xEF544F;
            case TUBE -> 0x4F9EEB;
            case PAD -> 0x77C95E;
            case SAM_BATTERY -> 0xEBC84F;
            case MOBILE -> 0xA7C56A;
            case VLS -> 0xB47DE8;
        };
    }
}
