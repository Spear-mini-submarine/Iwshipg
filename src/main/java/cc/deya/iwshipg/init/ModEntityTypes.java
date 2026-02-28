package cc.deya.iwshipg.init;

import cc.deya.iwshipg.cannon.homing_cannon_torpedo.HomingCannonTorpedoProjectile;
import com.cainiao1053.cbcmoreshells.index.CBCMSMunitionPropertiesHandlers;
import com.cainiao1053.cbcmoreshells.munitions.big_cannon.AbstractCannonTorpedoProjectile;
import com.cainiao1053.cbcmoreshells.munitions.big_cannon.CannonTorpedoProjectileRenderer;
import com.cainiao1053.cbcmoreshells.munitions.dual_cannon.AbstractDualCannonProjectile;
import com.cainiao1053.cbcmoreshells.munitions.dual_cannon.DualCannonProjectileRenderer;
import com.cainiao1053.cbcmoreshells.munitions.racked_projectile.AbstractRackedProjectile;
import com.cainiao1053.cbcmoreshells.munitions.racked_projectile.RackedProjectileRenderer;
import com.tterrag.registrate.util.entry.EntityEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import rbasamoyai.createbigcannons.multiloader.EntityTypeConfigurator;
import rbasamoyai.createbigcannons.munitions.autocannon.AbstractAutocannonProjectile;
import rbasamoyai.createbigcannons.munitions.autocannon.AutocannonProjectileRenderer;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.BigCannonProjectileRenderer;
import rbasamoyai.createbigcannons.munitions.config.MunitionPropertiesHandler;
import rbasamoyai.createbigcannons.munitions.config.PropertiesTypeHandler;
import rbasamoyai.ritchiesprojectilelib.RPLTags;

import java.util.function.Consumer;

import static cc.deya.iwshipg.Iwshipg.REGISTRATE;

public class ModEntityTypes {
    public static final EntityEntry<HomingCannonTorpedoProjectile> HOMING_CANNON_TORPEDO = torpedoProjectile(
            "homing_cannon_torpedo",          // 注册ID（小写+下划线）
            HomingCannonTorpedoProjectile::new,  // 你的鱼雷实体类
            "Homing Cannon Torpedo",          // 英文显示名
            CBCMSMunitionPropertiesHandlers.TORPEDO_PROJECTILE  // 属性配置
    );


    private static <T extends AbstractBigCannonProjectile> EntityEntry<T>
    cannonProjectile(String id, EntityType.EntityFactory<T> factory, PropertiesTypeHandler<EntityType<?>, ?> handler) {
        return REGISTRATE
                .entity(id, factory, MobCategory.MISC)
                .properties(cannonProperties())
                .renderer(() -> BigCannonProjectileRenderer::new)
                .tag(RPLTags.PRECISE_MOTION)
                .onRegister(type -> MunitionPropertiesHandler.registerProjectileHandler(type, handler))
                .register();
    }

    private static <T extends AbstractBigCannonProjectile> EntityEntry<T>
    cannonProjectile(String id, EntityType.EntityFactory<T> factory, String enUSdiffLang, PropertiesTypeHandler<EntityType<?>, ?> handler) {
        return REGISTRATE
                .entity(id, factory, MobCategory.MISC)
                .properties(cannonProperties())
                .renderer(() -> BigCannonProjectileRenderer::new)
                .tag(RPLTags.PRECISE_MOTION)
                .onRegister(type -> MunitionPropertiesHandler.registerProjectileHandler(type, handler))
                .register();
    }

    private static <T extends AbstractCannonTorpedoProjectile> EntityEntry<T>
    torpedoProjectile(String id, EntityType.EntityFactory<T> factory, String enUSdiffLang, PropertiesTypeHandler<EntityType<?>, ?> handler) {
        return REGISTRATE
                .entity(id, factory, MobCategory.MISC)
                .properties(cannonProperties())
                .renderer(() -> CannonTorpedoProjectileRenderer::new)
                .tag(RPLTags.PRECISE_MOTION)
                .onRegister(type -> MunitionPropertiesHandler.registerProjectileHandler(type, handler))
                .register();
    }

    private static <T extends AbstractRackedProjectile> EntityEntry<T>
    rackedProjectile(String id, EntityType.EntityFactory<T> factory, String enUSdiffLang, PropertiesTypeHandler<EntityType<?>, ?> handler) {
        return REGISTRATE
                .entity(id, factory, MobCategory.MISC)
                .properties(cannonProperties())
                .renderer(() -> RackedProjectileRenderer::new)
                .tag(RPLTags.PRECISE_MOTION)
                .onRegister(type -> MunitionPropertiesHandler.registerProjectileHandler(type, handler))
                .register();
    }

    private static <T extends AbstractDualCannonProjectile> EntityEntry<T>
    dualCannonProjectile(String id, EntityType.EntityFactory<T> factory, String enUSdiffLang, PropertiesTypeHandler<EntityType<?>, ?> handler) {
        return REGISTRATE
                .entity(id, factory, MobCategory.MISC)
                .properties(cannonProperties())
                .renderer(() -> DualCannonProjectileRenderer::new)
                .tag(RPLTags.PRECISE_MOTION)
                .onRegister(type -> MunitionPropertiesHandler.registerProjectileHandler(type, handler))
                .register();
    }

    private static <T> NonNullConsumer<T> configure(Consumer<EntityTypeConfigurator> cons) {
        return b -> cons.accept(EntityTypeConfigurator.of(b));
    }

    private static <T> NonNullConsumer<T> autocannonProperties() {
        return configure(c -> c.size(0.2f, 0.2f)
                .fireImmune()
                .updateInterval(1)
                .updateVelocity(false) // Mixin ServerEntity to not track motion
                .trackingRange(16));
    }

    private static <T extends AbstractAutocannonProjectile> EntityEntry<T>
    autocannonProjectile(String id, EntityType.EntityFactory<T> factory, PropertiesTypeHandler<EntityType<?>, ?> handler) {
        return REGISTRATE
                .entity(id, factory, MobCategory.MISC)
                .properties(autocannonProperties())
                .renderer(() -> AutocannonProjectileRenderer::new)
                .tag(RPLTags.PRECISE_MOTION)
                .onRegister(type -> MunitionPropertiesHandler.registerProjectileHandler(type, handler))
                .register();
    }

    private static <T extends AbstractAutocannonProjectile> EntityEntry<T>
    autocannonProjectile(String id, EntityType.EntityFactory<T> factory, String enUSdiffLang, PropertiesTypeHandler<EntityType<?>, ?> handler) {
        return REGISTRATE
                .entity(id, factory, MobCategory.MISC)
                .properties(autocannonProperties())
                .renderer(() -> AutocannonProjectileRenderer::new)
                .lang(enUSdiffLang)
                .tag(RPLTags.PRECISE_MOTION)
                .onRegister(type -> MunitionPropertiesHandler.registerProjectileHandler(type, handler))
                .register();
    }

    private static <T> NonNullConsumer<T> cannonProperties() {
        return configure(c -> c.size(0.8f, 0.8f)
                .fireImmune()
                .updateInterval(1)
                .updateVelocity(false) // Ditto
                .trackingRange(16));
    }

    private static <T> NonNullConsumer<T> shrapnel() {
        return configure(c -> c.size(0.8f, 0.8f)
                .fireImmune()
                .updateInterval(1)
                .updateVelocity(true)
                .trackingRange(16));
    }

    public static void register() {
    }
}
