package cc.deya.iwshipg.cannon.homing_cannon_torpedo;

import cc.deya.iwshipg.init.ModEntityTypes;
import com.cainiao1053.cbcmoreshells.munitions.big_cannon.GeneralCannonTorpedoBlock;
import net.minecraft.world.entity.EntityType;
import rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers;

public class HomingCannonTorpedoBlock extends GeneralCannonTorpedoBlock<HomingCannonTorpedoProjectile> {
    public HomingCannonTorpedoBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isBaseFuze() {
        return CBCMunitionPropertiesHandlers.COMMON_SHELL_BIG_CANNON_PROJECTILE.getPropertiesOf(this.getAssociatedEntityType()).fuze().baseFuze();
    }

    @Override
    public EntityType<? extends HomingCannonTorpedoProjectile> getAssociatedEntityType() {
        return ModEntityTypes.HOMING_CANNON_TORPEDO.get();
    }
}
