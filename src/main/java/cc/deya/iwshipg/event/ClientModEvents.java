package cc.deya.iwshipg.event;

import cc.deya.iwshipg.Iwshipg;
import cc.deya.iwshipg.client.HandleAAScreen;
import cc.deya.iwshipg.client.HandleArtilleryScreen;
import cc.deya.iwshipg.client.HandleRapidScreen;
import cc.deya.iwshipg.client.HandleTorpedoScreen;
import cc.deya.iwshipg.init.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Iwshipg.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 将 MenuType 和 Screen 类绑定
            MenuScreens.register(ModMenus.HANDLE_ARTILLERY_MENU.get(), HandleArtilleryScreen::new);
            MenuScreens.register(ModMenus.AA_ARTILLERY_MENU.get(), HandleAAScreen::new);
            MenuScreens.register(ModMenus.TORPEDO_TUBE_MENU.get(), HandleTorpedoScreen::new);
            MenuScreens.register(ModMenus.RAPID_CANNON_MENU.get(), HandleRapidScreen::new);
        });
    }
}