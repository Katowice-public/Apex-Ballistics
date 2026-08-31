package com.apexballistics.client;

import com.apexballistics.item.WeaponPerks;
import com.apexballistics.menu.PerkWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class PerkWorkbenchScreen extends AbstractContainerScreen<PerkWorkbenchMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "apexballistics", "textures/gui/perk_workbench.png");

    public PerkWorkbenchScreen(PerkWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 220;
        inventoryLabelY = 106;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.apexballistics.perk.apply"),
                        button -> sendButton(0))
                .bounds(leftPos + 128, topPos + 46, 110, 20)
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
        ItemStack weapon = menu.weapon();
        if (weapon.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.apexballistics.perk.empty"),
                    128, 80, 0x7DB5CC, false);
            return;
        }
        WeaponPerks perks = WeaponPerks.fromStack(weapon);
        graphics.drawString(font, Component.translatable("tooltip.apexballistics.perk.range",
                perks.range(), WeaponPerks.MAX), 20, 72, 0x70E0A0, false);
        graphics.drawString(font, Component.translatable("tooltip.apexballistics.perk.damage",
                perks.damage(), WeaponPerks.MAX), 20, 86, 0xFF4B3E, false);
        graphics.drawString(font, Component.translatable("tooltip.apexballistics.perk.accuracy",
                perks.accuracy(), WeaponPerks.MAX), 128, 80, 0xA0E070, false);
        graphics.drawString(font, Component.translatable("tooltip.apexballistics.perk.speed",
                perks.speed(), WeaponPerks.MAX), 128, 94, 0xF0C85A, false);
    }
}
