package cc.deya.iwshipg.init;

import cc.deya.iwshipg.Iwshipg;
import cc.deya.iwshipg.item.*;
import cc.deya.iwshipg.item.custom.EngineBoot;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Iwshipg.MODID);

    public static final RegistryObject<Item> TEST_ARTILLERY;
    public static final RegistryObject<Item> HANDLE_ARTILLERY_ITEM;
    public static final RegistryObject<Item> HANDLE_AUTO_ARTILLERY_ITEM;
    public static final RegistryObject<Item> HANDLE_AA_ARTILLERY_ITEM;
    public static final RegistryObject<Item> HANDLE_GUN_ITEM;
    public static final RegistryObject<Item> HANDLE_AUTO_GUN_ITEM;
    public static final RegistryObject<Item> HANDLE_RAPID_CANNON_ITEM;
    public static final RegistryObject<Item> HANDLE_RAPID_DOUBLE_CANNON_ITEM;
    public static final RegistryObject<Item> HANDLE_TORPRDO_TUBE_ITEM;
    public static final RegistryObject<Item> HANDLE_RACK_ITEM;
    public static final RegistryObject<Item> ENGINE_BOOT;

    static {
        TEST_ARTILLERY = ITEMS.register("test_artillery", () -> new TestArtillery(new Item.Properties().stacksTo(1)));
        HANDLE_ARTILLERY_ITEM = ITEMS.register("handle_artillery", () -> new HandleArtilleryItem(new Item.Properties().stacksTo(1)));
        HANDLE_AUTO_ARTILLERY_ITEM = ITEMS.register("handle_auto_artillery", () -> new HandleAutoArtilleryItem(new Item.Properties().stacksTo(1)));
        HANDLE_AA_ARTILLERY_ITEM = ITEMS.register("handle_aa_artillery", () -> new HandleAAArtilleryItem(new Item.Properties().stacksTo(1)));
        HANDLE_GUN_ITEM = ITEMS.register("handle_gun_artillery", () -> new HandleGunItem(new Item.Properties().stacksTo(1)));
        HANDLE_AUTO_GUN_ITEM = ITEMS.register("handle_auto_gun_artillery", () -> new HandleAutoGunItem(new Item.Properties().stacksTo(1)));
        HANDLE_RAPID_CANNON_ITEM = ITEMS.register("handle_rapid_cannon", () -> new HandleRapidCannonItem(new Item.Properties().stacksTo(1)));
        HANDLE_RAPID_DOUBLE_CANNON_ITEM = ITEMS.register("handle_rapid_double_cannon", () -> new HandleRapidDoubleCannonItem(new Item.Properties().stacksTo(1)));
        HANDLE_RACK_ITEM = ITEMS.register("handle_rack", () -> new HandleRackItem(new Item.Properties().stacksTo(1)));
        HANDLE_TORPRDO_TUBE_ITEM = ITEMS.register("handle_torpedo_tube", () -> new HandleTorpedoTubeItem(new Item.Properties().stacksTo(1)));

        ENGINE_BOOT = ITEMS.register("engine_boot", () -> new EngineBoot(new Item.Properties().stacksTo(1)));


    }

    public static void init(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }


}
