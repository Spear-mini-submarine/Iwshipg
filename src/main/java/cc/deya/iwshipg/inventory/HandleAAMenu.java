package cc.deya.iwshipg.inventory;

import cc.deya.iwshipg.init.ModMenus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class HandleAAMenu extends AbstractContainerMenu {
    private final ItemStack aaStack;
    private final ItemStackHandler itemHandler = new ItemStackHandler(16) {
        @Override
        protected void onContentsChanged(int slot) {
            // 当槽位内容变化时，自动将数据同步到物品 NBT
            CompoundTag tag = aaStack.getOrCreateTag();
            tag.put("Inventory", serializeNBT());
        }
    };

    public HandleAAMenu(int id, Inventory inv, ItemStack stack) {
        super(ModMenus.AA_ARTILLERY_MENU.get(), id);
        this.aaStack = stack;

        // 从 NBT 加载现有物品
        if (stack.hasTag() && stack.getTag().contains("Inventory")) {
            itemHandler.deserializeNBT(stack.getTag().getCompound("Inventory"));
        }

        // --- 绘制 2x2 的 2x2 矩阵槽位 ---
        // 外部循环控制 4 个大组 (Alpha, Bravo, Charlie, Delta)
        for (int group = 0; group < 4; group++) {
            int groupX = (group % 2) * 50; // 组间横向间距
            int groupY = (group / 2) * 50; // 组间纵向间距

            // 内部循环控制每组内的 4 个格子
            for (int i = 0; i < 4; i++) {
                int slotX = (i % 2) * 18; // 格子横向间距
                int slotY = (i / 2) * 18; // 格子纵向间距

                // 起始偏移坐标 (40, 20)，必须与 Screen 中的 renderBg 坐标完全对齐
                this.addSlot(new SlotItemHandler(itemHandler, group * 4 + i, 40 + groupX + slotX, 20 + groupY + slotY));
            }
        }

        // --- 绘制玩家背包 (位于下方) ---
        int startY = 118; // 增加间距防止重叠
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, startY + row * 18));
            }
        }

        // --- 绘制快捷栏 ---
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, startY + 58));
        }
    }

    // 核心方法：处理 Shift+点击 快速移动物品
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // 0-15 是机炮槽位，16-51 是玩家背包
            if (index < 16) {
                // 从机炮移向背包 (倒序填充)
                if (!this.moveItemStackTo(itemstack1, 16, 52, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从背包移向机炮
                if (itemstack1.getItem() instanceof rbasamoyai.createbigcannons.munitions.autocannon.AutocannonAmmoItem) {
                    if (!this.moveItemStackTo(itemstack1, 0, 16, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // 如果不是机炮弹药，尝试在背包内部（快捷栏 <-> 仓库）移动
                    if (index < 43) { // 在仓库里点击
                        if (!this.moveItemStackTo(itemstack1, 43, 52, false)) return ItemStack.EMPTY;
                    } else { // 在快捷栏点击
                        if (!this.moveItemStackTo(itemstack1, 16, 43, false)) return ItemStack.EMPTY;
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            // 如果数量没变，说明没能成功移动（比如目标槽位已满）
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem() == this.aaStack || player.getOffhandItem() == this.aaStack;
    }
}