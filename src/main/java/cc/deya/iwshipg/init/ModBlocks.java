package cc.deya.iwshipg.init;

import cc.deya.iwshipg.cannon.homing_cannon_torpedo.HomingCannonTorpedoBlock;
import cc.deya.iwshipg.cannon.homing_cannon_torpedo.HomingCannonTorpedoItem;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import rbasamoyai.createbigcannons.CBCTags;
import rbasamoyai.createbigcannons.datagen.assets.CBCBuilderTransformers;

import static cc.deya.iwshipg.Iwshipg.REGISTRATE;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

public class ModBlocks {
    public static final BlockEntry<HomingCannonTorpedoBlock> HOMING_CANNON_TORPEDO = REGISTRATE
            .block("homing_cannon_torpedo", HomingCannonTorpedoBlock::new)
            .transform(shell(MapColor.COLOR_RED))
            .transform(axeOrPickaxe())
            .transform(CBCBuilderTransformers.projectile("projectile/homing_cannon_torpedo"))
            .transform(CBCBuilderTransformers.safeNbt())
            .loot(CBCBuilderTransformers.shellLoot())
            .lang("Homing Cannon Torpedo")
            .item(HomingCannonTorpedoItem::new)
            .tag(CBCTags.CBCItemTags.BIG_CANNON_PROJECTILES)
            .build()
            .register();

    private static <T extends Block, P> NonNullUnaryOperator<BlockBuilder<T, P>> shell(MapColor color) {
        return b -> b.addLayer(() -> RenderType::solid)
                .properties(p -> p.mapColor(color))
                .properties(p -> p.strength(2.0f, 3.0f))
                .properties(p -> p.sound(SoundType.STONE));
    }

    public static void register() {
    }
}
