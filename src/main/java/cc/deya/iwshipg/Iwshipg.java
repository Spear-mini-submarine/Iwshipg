package cc.deya.iwshipg;

import cc.deya.iwshipg.init.*;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Iwshipg.MODID)
public class Iwshipg {
    public static final String MODID = "iwshipg";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);

    public Iwshipg() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        REGISTRATE.registerEventListeners(modEventBus);
        ModBlocks.register();
        ModEntityTypes.register();
        ModItems.init(modEventBus);
        //ModBlockEntity.init(modEventBus);
        ModCreativeTabs.init(modEventBus);
        ModMenus.init(modEventBus);

    }
}