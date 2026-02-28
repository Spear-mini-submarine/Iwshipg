package cc.deya.iwshipg.item.custom;

import cc.deya.iwshipg.init.ModArmorMaterials;
import net.minecraft.world.item.ArmorItem;

public class EngineBoot extends ArmorItem {
    public EngineBoot(Properties properties) {
        // 使用自定义材质，指定为靴子位置
        super(ModArmorMaterials.ENGINE, Type.BOOTS, properties);
    }


}