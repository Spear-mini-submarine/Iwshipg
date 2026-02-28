package cc.deya.iwshipg.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

public class HandleAutoArtilleryItem extends HandleArtilleryItem {

    public HandleAutoArtilleryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 如果按住 Shift，依然调用父类的“打开 GUI”逻辑
        if (player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        } else {
            // 纯右键：增加冷却判断
            if (!level.isClientSide) {
                // 检查冷却：这里 100 代表 100 ticks (5秒)，你可以按需修改
                if (!player.getCooldowns().isOnCooldown(this)) {
                    this.fire(level, player, stack);
                    player.getCooldowns().addCooldown(this, 100);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
    }

    @Override
    public void fire(Level level, Player player, ItemStack artilleryStack) {
        if (level.isClientSide) return;

        CompoundTag tag = artilleryStack.getOrCreateTag();
        ItemStackHandler handler = new ItemStackHandler(2);
        if (tag.contains("Inventory")) {
            handler.deserializeNBT(tag.getCompound("Inventory"));
        }

        ItemStack shell = handler.getStackInSlot(0);
        ItemStack powder = handler.getStackInSlot(1);

        // 判定：只要有炮弹就能发射
        if (!shell.isEmpty()) {
            // 动力逻辑计算
            int finalPower = 1;
            if (!powder.isEmpty()) {
                String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(powder.getItem()).toString();
                if (itemId.equals("createbigcannons:powder_charge")) {
                    finalPower = 2;
                } else if (itemId.equals("createbigcannons:big_cartridge")) {
                    if (powder.hasTag() && powder.getTag().contains("Power")) {
                        finalPower = powder.getTag().getInt("Power");
                    }
                }
            }
            finalPower = Math.min(finalPower, 32);

            // 构造传递给父类的 ItemStack（只用于传递数值）
            ItemStack powderToPass = powder.copy();
            powderToPass.setCount(finalPower);

            // 【关键点】这里现在可以成功调用父类的 protected 方法了
            this.performLaunch(level, player, shell, powderToPass);

            // --- 自动减 1 逻辑 ---
            shell.shrink(1); // 炮弹消耗 1
            if (!powder.isEmpty()) {
                powder.shrink(1); // 药包/药筒消耗 1
            }

            // 保存修改后的物品到 NBT
            handler.setStackInSlot(0, shell);
            handler.setStackInSlot(1, powder);
            tag.put("Inventory", handler.serializeNBT());

        } else {
            player.displayClientMessage(Component.literal("§e弹匣清空"), true);
        }
    }
}