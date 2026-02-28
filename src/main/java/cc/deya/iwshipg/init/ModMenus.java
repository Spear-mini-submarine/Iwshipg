package cc.deya.iwshipg.init;

import cc.deya.iwshipg.Iwshipg;
import cc.deya.iwshipg.inventory.HandleAAMenu;
import cc.deya.iwshipg.inventory.HandleArtilleryMenu;
import cc.deya.iwshipg.inventory.HandleRapidMenu;
import cc.deya.iwshipg.inventory.HandleTorpedoMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Iwshipg.MODID);
    public static final RegistryObject<MenuType<HandleAAMenu>> AA_ARTILLERY_MENU =
            MENUS.register("aa_artillery_menu", () -> IForgeMenuType.create((windowId, inv, data) -> {
                // 从缓冲区读回物品，传给 Menu 的构造函数
                ItemStack stack = data.readItem();
                return new HandleAAMenu(windowId, inv, stack);
            }));    // 注册我们的火炮控制器菜单
    //鱼雷发射器
    public static final RegistryObject<MenuType<HandleTorpedoMenu>> TORPEDO_TUBE_MENU =
            MENUS.register("torpedo_tube_menu", () -> IForgeMenuType.create((windowId, inv, data) -> {
                ItemStack stack = data.readItem();
                return new HandleTorpedoMenu(windowId, inv, stack);
            }));    public static final RegistryObject<MenuType<HandleArtilleryMenu>> HANDLE_ARTILLERY_MENU =
            MENUS.register("handle_artillery_menu", () -> IForgeMenuType.create(HandleArtilleryMenu::new));

    public static void init(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }



    public static final RegistryObject<MenuType<HandleRapidMenu>> RAPID_CANNON_MENU =
            MENUS.register("rapid_cannon_menu",
                    () -> IForgeMenuType.create(HandleRapidMenu::new));


}