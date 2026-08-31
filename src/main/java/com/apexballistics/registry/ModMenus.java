package com.apexballistics.registry;

import com.apexballistics.ApexBallistics;
import com.apexballistics.menu.LauncherMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ApexBallistics.MOD_ID);

    public static final RegistryObject<MenuType<LauncherMenu>> LAUNCHER =
            MENUS.register("launcher", () -> IForgeMenuType.create(LauncherMenu::new));

    private ModMenus() {
    }
}
