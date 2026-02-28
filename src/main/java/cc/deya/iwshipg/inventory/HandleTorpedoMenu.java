package cc.deya.iwshipg.inventory;

import cc.deya.iwshipg.init.ModMenus;
import com.cainiao1053.cbcmoreshells.munitions.big_cannon.TorpedoProjectileBlock;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class HandleTorpedoMenu extends AbstractContainerMenu {
    private final ItemStack tubeStack;

    // 10个槽位：0-4 为待发架，5-9 为备用架
    private final ItemStackHandler itemHandler = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            // 当 GUI 内物品改变时，实时保存到物品 NBT
            if (tubeStack != null && !tubeStack.isEmpty()) {
                tubeStack.getOrCreateTag().put("Inventory", serializeNBT());
            }
        }
    };

    public HandleTorpedoMenu(int id, Inventory inv, ItemStack stack) {
        super(ModMenus.TORPEDO_TUBE_MENU.get(), id);
        this.tubeStack = stack;

        // 加载现有弹药数据
        if (stack.hasTag() && stack.getTag().contains("Inventory")) {
            itemHandler.deserializeNBT(stack.getTag().getCompound("Inventory"));
        }

        // --- 第一行：待发槽位 (0-4) ---
        // 渲染坐标参考标准：起始 x=44, y=20
        for (int i = 0; i < 5; i++) {
            this.addSlot(new TorpedoSlot(itemHandler, i, 44 + i * 18, 20));
        }

        // --- 第二行：预备槽位 (5-9) ---
        // 渲染坐标参考标准：起始 x=44, y=45
        for (int i = 0; i < 5; i++) {
            this.addSlot(new TorpedoSlot(itemHandler, i + 5, 44 + i * 18, 45));
        }

        // 添加玩家背包和快捷栏
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    @Override
    public boolean stillValid(Player player) {
        // 只要玩家手里还拿着这个发射器，GUI 就有效
        return player.getMainHandItem() == this.tubeStack || player.getOffhandItem() == this.tubeStack;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // 如果点击的是鱼雷管里的 10 个槽位 (0-9)
            if (index < 10) {
                // 尝试移入玩家背包 (10-45)
                if (!this.moveItemStackTo(itemstack1, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 如果点击的是玩家背包里的物品
                // 首先判断是不是鱼雷，如果不是则不允许 Shift 填入
                if (itemstack1.getItem() instanceof BlockItem bi && bi.getBlock() instanceof TorpedoProjectileBlock) {
                    // 尝试移入鱼雷管的 10 个槽位
                    if (!this.moveItemStackTo(itemstack1, 0, 10, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY; // 非鱼雷物品不执行快速移动
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    /**
     * 自定义鱼雷专用槽位类
     */
    private static class TorpedoSlot extends SlotItemHandler {
        public TorpedoSlot(ItemStackHandler itemHandler, int index, int x, int y) {
            super(itemHandler, index, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            // 核心判定：只有鱼雷块物品可以放入
            return stack.getItem() instanceof BlockItem bi &&
                    bi.getBlock() instanceof TorpedoProjectileBlock;
        }

        @Override
        public int getMaxStackSize() {
            return 1; // 强制每个槽位只能放一个，模拟真实的发射架
        }

        @Override
        public int getMaxStackSize(@NotNull ItemStack stack) {
            return 1;
        }
    }
}