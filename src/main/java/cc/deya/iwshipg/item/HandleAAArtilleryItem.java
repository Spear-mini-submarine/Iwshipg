package cc.deya.iwshipg.item;

import cc.deya.iwshipg.inventory.HandleAAMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
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
import rbasamoyai.createbigcannons.munitions.autocannon.AutocannonAmmoItem;

public class HandleAAArtilleryItem extends Item {

    // --- 配置参数 ---
    private static final int FIRE_RATE = 4;        // 连发间隔 (ticks)
    private static final float MUZZLE_VELOCITY = 6F; // 初始速度
    private static final float BARREL_SPACING = 0.5F; // 2x2 阵型间距

    public HandleAAArtilleryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // --- 打开 16 槽位 AA 专用 GUI ---
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("四联机炮");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                        return new HandleAAMenu(id, inv, stack);
                    }
                }, buf -> buf.writeItem(stack));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        } else {
            // --- 开启全自动开火模式 ---
            if (!player.getCooldowns().isOnCooldown(this)) {
                this.fire(level, player, stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
    }

    // 全自动连发处理

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        // 先判断是不是玩家在用，因为 LivingEntity 范围更广
        if (entity instanceof Player player) {
            int elapsed = 72000 - remainingUseDuration;
            if (elapsed > 0 && elapsed % FIRE_RATE == 0) {
                if (!player.getCooldowns().isOnCooldown(this)) {
                    this.fire(level, player, stack);
                }
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    public void fire(Level level, Player player, ItemStack aaStack) {
        if (level.isClientSide) return;

        // 1. 获取弹药库数据 (16 槽位)
        ItemStackHandler handler = new ItemStackHandler(16);
        CompoundTag tag = aaStack.getOrCreateTag();
        if (tag.contains("Inventory")) {
            handler.deserializeNBT(tag.getCompound("Inventory"));
        }

        // 2. 计算 2x2 矩阵偏移
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < 0.01) right = look.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 up = right.cross(look).normalize();

        Vec3[] muzzleOffsets = {
                up.scale(BARREL_SPACING).add(right.scale(-BARREL_SPACING)), // 左上 (Group 0)
                up.scale(BARREL_SPACING).add(right.scale(BARREL_SPACING)),  // 右上 (Group 1)
                up.scale(-BARREL_SPACING).add(right.scale(-BARREL_SPACING)),// 左下 (Group 2)
                up.scale(-BARREL_SPACING).add(right.scale(BARREL_SPACING))  // 右下 (Group 3)
        };

        boolean anyFired = false;

        // 3. 遍历四个炮组执行发射
        for (int g = 0; g < 4; g++) {
            for (int i = 0; i < 4; i++) {
                int slotIndex = g * 4 + i;
                ItemStack ammo = handler.getStackInSlot(slotIndex);

                if (!ammo.isEmpty() && ammo.getItem() instanceof AutocannonAmmoItem round) {
                    // 调用 CBC 核心黑箱实例化实体
                    AbstractAutocannonProjectile projectile = round.getAutocannonProjectile(ammo, level);

                    if (projectile != null) {
                        // 物理定位：眼睛位置 + 视线延伸 + 阵型偏移
                        Vec3 spawnPos = player.getEyePosition().add(look.scale(1.2)).add(muzzleOffsets[g]);
                        projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

                        // CBC 属性强制补丁 (确保防空性能)
                        projectile.setChargePower(6.0F);//毁伤倍率
                        projectile.setLifetime(180);   // 给它约 9 秒的飞行时间
                        projectile.setTracer(round.isTracer(ammo));    // 检查曳光

                        // 执行射击
                        projectile.shoot(look.x, look.y, look.z, MUZZLE_VELOCITY, 0.06F);
                        projectile.setOwner(player);

                        level.addFreshEntity(projectile);
                        ammo.shrink(1);
                        anyFired = true;
                        break; // 当前炮管发射成功，寻找下一个炮管组
                    }
                }
            }
        }

        // 4. 发射后处理
        if (anyFired) {
            // 保存 16 槽位状态
            tag.put("Inventory", handler.serializeNBT());

            // 播放四联装机炮音效 (稍微压低音调增加厚重感)
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    CBCSoundEvents.FIRE_AUTOCANNON.getMainEvent(), SoundSource.PLAYERS, 3.5F, 0.95F);

            // 设置冷却
            player.getCooldowns().addCooldown(this, FIRE_RATE);
        } else {
            player.displayClientMessage(Component.literal("§c弹药仓已空！"), true);
            player.stopUsingItem();
        }
    }
}