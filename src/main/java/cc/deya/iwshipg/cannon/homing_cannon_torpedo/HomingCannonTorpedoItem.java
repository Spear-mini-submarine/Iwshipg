package cc.deya.iwshipg.cannon.homing_cannon_torpedo;

import com.cainiao1053.cbcmoreshells.base.CBCMSTooltip;
import com.cainiao1053.cbcmoreshells.index.CBCMSMunitionPropertiesHandlers;
import com.cainiao1053.cbcmoreshells.munitions.big_cannon.FuzedTorpedoProjectileBlockItem;
import com.cainiao1053.cbcmoreshells.munitions.big_cannon.config.TorpedoProperties;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Components;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

import static cc.deya.iwshipg.init.ModEntityTypes.HOMING_CANNON_TORPEDO;

public class HomingCannonTorpedoItem extends FuzedTorpedoProjectileBlockItem {
    public HomingCannonTorpedoItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static void appendTorpedoInfo(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag, float torpSpeed, float buoyancyFactor, float lifetime) {
        if (Screen.hasShiftDown()) {
            TooltipHelper.Palette palette = CBCMSTooltip.getPalette(level, stack);
            String key1 = stack.getDescriptionId() + ".tooltip.torpInfo";
            tooltip.add(Components.translatable(key1).withStyle(ChatFormatting.GRAY));
            tooltip.addAll(TooltipHelper.cutStringTextComponent(I18n.get(key1 + ".main", String.format("%.1f", (double) torpSpeed * 38.8), buoyancyFactor, String.format("%.1f", lifetime * torpSpeed), 40, 10), palette.primary(), palette.highlight(), 1));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TorpedoProperties properties = CBCMSMunitionPropertiesHandlers.TORPEDO_PROJECTILE.getPropertiesOf(HOMING_CANNON_TORPEDO.get());
        HomingCannonTorpedoItem.appendTorpedoInfo(stack, level, tooltip, flag, properties.torpedoProperties().torpedoSpeed(), properties.torpedoProperties().buoyancyFactor(), properties.lifetime());
    }
}
