package cc.deya.iwshipg.inventory;

import cc.deya.iwshipg.init.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class HandleArtilleryMenu extends AbstractContainerMenu {

    private final ItemStack artilleryStack;

    // 构造函数 A：供客户端使用（通过 FriendlyByteBuf 接收数据）
    public HandleArtilleryMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        // 这里通过 extraData 读取我们在 Item 类里 writeItem 进去的数据
        // 或者简单点，直接获取玩家手持物品
        this(containerId, inv, inv.player.getMainHandItem());
    }    // 两个格子的处理器：0是炮弹，1是药包

    // 构造函数 B：核心构造函数（真正干活的）
    public HandleArtilleryMenu(int containerId, Inventory inv, ItemStack stack) {
        super(ModMenus.HANDLE_ARTILLERY_MENU.get(), containerId);
        this.artilleryStack = stack;

        if (stack.hasTag() && stack.getTag().contains("Inventory")) {
            itemHandler.deserializeNBT(stack.getTag().getCompound("Inventory"));
        }

        // --- 核心修改：通过类名判断是否为自动火炮 ---
        // 所需64的都放这
        boolean isAuto = stack.getItem() instanceof cc.deya.iwshipg.item.HandleAutoArtilleryItem
                || stack.getItem() instanceof cc.deya.iwshipg.item.HandleAutoGunItem;

        // 1. 添加自定义格位
        // 传入 isAuto 变量
        this.addSlot(new SingleItemSlot(itemHandler, 0, 56, 35, isAuto));

        // 药包位通常也是可以堆叠的（为了计算动力），如果不希望药包位也受限，
        // 药包位保持原样即可，或者也改用类似的逻辑。
        this.addSlot(new SlotItemHandler(itemHandler, 1, 116, 35) {
            @Override
            public int getMaxStackSize() {
                return 64; // 药包位统一允许堆叠，方便计算动力
            }
        });
        // 2. 添加玩家背包格位 (必须有，否则玩家没法从包里拿东西放进去)
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            // 这里调用了下方的 saveToNBT 方法
            saveToNBT();
        }
    };

    @Override
    public boolean stillValid(Player player) {
        return true; // 简单处理：只要玩家拿着这个物品就有效
    }

    // --- 必须实现的 Shift 点击逻辑 ---
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        // 如果点击的格子不是空的
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // index 0 和 1 是我们的炮弹位和药包位
            if (index < 2) {
                // 如果是从“炮”里往外拿：尝试放入玩家背包 (格位 2 到 38)
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 如果是从玩家背包往“炮”里放：尝试放入格位 0 到 2 (不含2)
                if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
                    return ItemStack.EMPTY;
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

    // 玩家背包格位的辅助方法 (略，通常是标准的双循环逻辑)
    private void addPlayerInventory(Inventory playerInventory) {
        // 玩家主背包：3 行 x 9 列
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                // 这里的坐标 (8, 84) 是适配原版贴图的标准起始点
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        // 玩家快捷栏：1 行 x 9 列
        for (int col = 0; col < 9; ++col) {
            // 坐标 142 是快捷栏的标准高度
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private void saveToNBT() {
        // 确保这里用的是 artilleryStack
        if (!artilleryStack.isEmpty()) {
            artilleryStack.getOrCreateTag().put("Inventory", itemHandler.serializeNBT());
        }
    }

    //限制0号格子物品上限=1 auto=64
    private static class SingleItemSlot extends SlotItemHandler {
        private final boolean isAuto; // 是否为自动火炮模式

        public SingleItemSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition, boolean isAuto) {
            super(itemHandler, index, xPosition, yPosition);
            this.isAuto = isAuto;
        }

        @Override
        public int getMaxStackSize() {
            return isAuto ? 64 : 1; // 自动炮允许 64，普通炮限制 1
        }

        @Override
        public int getMaxStackSize(@NotNull ItemStack stack) {
            // 这里也要同步修改，返回物品本身的最大堆叠或我们的限制
            return isAuto ? stack.getMaxStackSize() : 1;
        }

        @Override
        public void set(@NotNull ItemStack stack) {
            // 如果不是自动炮且数量大于 1，才截断
            if (!isAuto && !stack.isEmpty() && stack.getCount() > 1) {
                super.set(stack.copyWithCount(1));
            } else {
                super.set(stack);
            }
        }
    }




}