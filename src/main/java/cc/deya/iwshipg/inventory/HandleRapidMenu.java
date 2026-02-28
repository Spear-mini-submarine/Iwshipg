package cc.deya.iwshipg.inventory;

import cc.deya.iwshipg.init.ModMenus;
import cc.deya.iwshipg.item.HandleRackItem;
import com.cainiao1053.cbcmoreshells.munitions.dual_cannon.DualCannonProjectileBlock;
import com.cainiao1053.cbcmoreshells.munitions.racked_projectile.RackedProjectileBlock;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class HandleRapidMenu extends AbstractContainerMenu {
    private final ItemStack gunStack;
    private final ItemStackHandler itemHandler = new ItemStackHandler(16);

    public HandleRapidMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, buf.readItem());
    }

    public HandleRapidMenu(int id, Inventory playerInv, ItemStack gunStack) {
        super(ModMenus.RAPID_CANNON_MENU.get(), id);
        this.gunStack = gunStack;

        if (gunStack.hasTag() && gunStack.getTag().contains("Inventory")) {
            itemHandler.deserializeNBT(gunStack.getTag().getCompound("Inventory"));
        }

        int startX = 44;
        int startY = 18;
        int groupOffsetX = 54;
        int groupOffsetY = 54;
        int slotSize = 18;

        for (int group = 0; group < 4; group++) {
            for (int i = 0; i < 4; i++) {
                int slotIndex = group * 4 + i;
                int x = startX + (group % 2 * groupOffsetX) + (i % 2 * slotSize);
                int y = startY + (group / 2 * groupOffsetY) + (i / 2 * slotSize);

                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        if (stack.getItem() instanceof BlockItem bi) {
                            // 兼容逻辑：细炮/双联装接受 DualCannonProjectileBlock，挂架接受 RackedProjectileBlock
                            return bi.getBlock() instanceof DualCannonProjectileBlock ||
                                    bi.getBlock() instanceof RackedProjectileBlock;
                        }
                        return false;
                    }

                    @Override
                    public int getMaxStackSize() {
                        // --- 动态上限逻辑 ---
                        // 如果手里拿的是挂架项（HandleRackItem），上限设为 1
                        // 否则（速射炮或双联装），上限设为 16
                        if (gunStack.getItem() instanceof HandleRackItem) {
                            return 1;
                        }
                        return 16;
                    }

                    @Override
                    public int getMaxStackSize(@NotNull ItemStack stack) {
                        // 保持与上面一致
                        return this.getMaxStackSize();
                    }

                    @Override
                    public void setChanged() {
                        super.setChanged();
                        saveToNbt();
                    }
                });
            }
        }
        addPlayerInventory(playerInv);
    }

    private void saveToNbt() {
        gunStack.getOrCreateTag().put("Inventory", itemHandler.serializeNBT());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // 容器槽位区间 [0, 15]
            if (index < 16) {
                if (!this.moveItemStackTo(itemstack1, 16, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从背包往挂架里移：执行合法性与数量检查
                if (itemstack1.getItem() instanceof BlockItem bi &&
                        (bi.getBlock() instanceof DualCannonProjectileBlock || bi.getBlock() instanceof RackedProjectileBlock)) {

                    // 注意：moveItemStackTo 会尝试填满槽位，但由于我们改了 Slot 的 getMaxStackSize，
                    // 它现在只会给每个空格子塞 1 个。
                    if (!this.moveItemStackTo(itemstack1, 0, 16, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem() == gunStack || player.getOffhandItem() == gunStack;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int inventoryStartY = 138;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, inventoryStartY + i * 18));
            }
        }
        int hotbarY = inventoryStartY + 58;
        for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, hotbarY));
        }
    }
}