package cc.deya.iwshipg.item;

import cc.deya.iwshipg.inventory.HandleArtilleryMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import rbasamoyai.createbigcannons.index.CBCSoundEvents;
import rbasamoyai.createbigcannons.munitions.autocannon.AbstractAutocannonProjectile;

public class HandleArtilleryItem extends Item {
    public HandleArtilleryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // --- Shift + 右键：打开 GUI ---
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("装填弹膛");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                        return new HandleArtilleryMenu(id, inv, stack);
                    }
                }, buf -> buf.writeItem(stack));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        } else {
            // --- 纯右键：开火 ---
            if (!level.isClientSide) {
                this.fire(level, player, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
    }

    protected void performLaunch(Level level, Player player, ItemStack shellStack, ItemStack powderStack) {
        if (level.isClientSide) return;

        Item shellItem = shellStack.getItem();
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();

        // --- 分支 A: 处理机炮弹药 (AutocannonAmmoItem) ---
        if (shellItem instanceof rbasamoyai.createbigcannons.munitions.autocannon.AutocannonAmmoItem round) {
            AbstractAutocannonProjectile projectile = round.getAutocannonProjectile(shellStack, level);
            if (projectile != null) {
                projectile.setPos(eyePos.add(look.scale(0.5)));
                projectile.setChargePower(4.0F);
                projectile.setTracer(round.isTracer(shellStack));
                projectile.setLifetime(100);
                projectile.shoot(look.x, look.y, look.z, 3.5F, 0.2F); // 速度 3.5，散布 0.2
                projectile.setOwner(player);
                level.addFreshEntity(projectile);

                // 播放机炮音效
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        CBCSoundEvents.FIRE_AUTOCANNON.getMainEvent(), SoundSource.PLAYERS, 2.0F, 1.0F);
                return; // 结束执行
            }
        }

        // --- 分支 B: 处理巨炮弹药 (ProjectileBlock) ---
        if (shellItem instanceof net.minecraft.world.item.BlockItem blockItem &&
                blockItem.getBlock() instanceof rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlock<?> projBlock) {

            Entity projectile = projBlock.getProjectile(level, shellStack);
            if (projectile != null) {
                projectile.setPos(eyePos.x + look.x * 0.5, eyePos.y + look.y * 0.5, eyePos.z + look.z * 0.5);
                float speed = Math.max(1.0F, powderStack.getCount() * 2.0F);
                projectile.setDeltaMovement(look.scale(speed));
                if (projectile instanceof net.minecraft.world.entity.projectile.Projectile p) p.setOwner(player);
                level.addFreshEntity(projectile);

                // 播放大炮音效
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.8F);
            }
        }
    }

    public void fire(Level level, Player player, ItemStack artilleryStack) {
        if (level.isClientSide) return;

        if (artilleryStack.hasTag() && artilleryStack.getTag().contains("Inventory")) {
            ItemStackHandler handler = new ItemStackHandler(2);
            handler.deserializeNBT(artilleryStack.getTag().getCompound("Inventory"));

            ItemStack shell = handler.getStackInSlot(0);
            ItemStack powder = handler.getStackInSlot(1);

            // 核心修改：只要有炮弹就能开火
            if (!shell.isEmpty()) {
                int finalPower = 0;

                if (!powder.isEmpty()) {
                    String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(powder.getItem()).toString();

                    if (itemId.equals("createbigcannons:powder_charge")) {
                        finalPower = powder.getCount(); // 只有 CBC 药包按数量翻倍
                    } else if (itemId.equals("createbigcannons:big_cartridge")) {
                        if (powder.hasTag() && powder.getTag().contains("Power")) {
                            finalPower = powder.getTag().getInt("Power"); // 药筒读取 NBT
                        }
                    } else {
                        finalPower = 1; // --- 修复：杂物无论堆叠多少，只给 1 点动力 ---
                    }
                }

                finalPower = Math.min(finalPower, 32);

                // 构造一个临时的副本用于传递数值，确保即使 powder 为空，performLaunch 也能拿到数量为 0 的对象
                ItemStack powderToPass = powder.isEmpty() ? ItemStack.EMPTY.copy() : powder.copy();
                powderToPass.setCount(finalPower);

                // 执行发射：此时 finalPower 为 0，performLaunch 应只计算基础方向向量
                performLaunch(level, player, shell, powderToPass);

                // 清理并保存
                handler.setStackInSlot(0, ItemStack.EMPTY);
                handler.setStackInSlot(1, ItemStack.EMPTY);
                artilleryStack.getTag().put("Inventory", handler.serializeNBT());

                player.displayClientMessage(Component.literal("§c "), true);
            } else {
                player.displayClientMessage(Component.literal("§e弹膛未装填炮弹！"), true);
            }
        }
    }
}
