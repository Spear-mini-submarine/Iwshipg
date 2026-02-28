package cc.deya.iwshipg.init;

import cc.deya.iwshipg.Iwshipg;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Iwshipg.MODID);

    public static final RegistryObject<CreativeModeTab> CREATE_TAB = CREATIVE_MODE_TABS.register("creative_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.TEST_ARTILLERY.get()))
                    .title(Component.translatable("item_group." + Iwshipg.MODID + ".creative_tab")) // 标题
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.TEST_ARTILLERY.get());
                        output.accept(ModItems.HANDLE_ARTILLERY_ITEM.get());
                        output.accept(ModItems.HANDLE_AUTO_ARTILLERY_ITEM.get());
                        output.accept(ModItems.HANDLE_AA_ARTILLERY_ITEM.get());
                        output.accept(ModItems.HANDLE_GUN_ITEM.get());
                        output.accept(ModItems.HANDLE_AUTO_GUN_ITEM.get());
                        output.accept(ModItems.HANDLE_TORPRDO_TUBE_ITEM.get());
                        output.accept(ModItems.HANDLE_RAPID_CANNON_ITEM.get());
                        output.accept(ModItems.HANDLE_RAPID_DOUBLE_CANNON_ITEM.get());
                        output.accept(ModItems.HANDLE_RACK_ITEM.get());
                        output.accept(ModItems.ENGINE_BOOT.get());

                        output.accept(ModBlocks.HOMING_CANNON_TORPEDO.get());

                    })
                    .build());

    public static void init(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
