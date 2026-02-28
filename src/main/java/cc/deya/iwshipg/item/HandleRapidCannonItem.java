package cc.deya.iwshipg.item;

import cc.deya.iwshipg.inventory.HandleRapidMenu;
import com.cainiao1053.cbcmoreshells.munitions.dual_cannon.AbstractDualCannonProjectile;
import com.cainiao1053.cbcmoreshells.munitions.dual_cannon.DualCannonProjectileBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

public class HandleRapidCannonItem extends Item {

    public HandleRapidCannonItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    // 右键打开 GUI，蹲下右键进入发射模式
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // 潜行右键打开 GUI
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (id, inv, p) -> new HandleRapidMenu(id, inv, stack),
                        Component.literal("Rapid Cannon")
                ), buf -> buf.writeItem(stack));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            // 正常右键直接发射（单次）
            if (!level.isClientSide) {
                // 直接调用射击函数
                fire(level, player, stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
    }


    private void fire(Level level, Player player, ItemStack gun) {
        ItemStackHandler handler = new ItemStackHandler(16);
        CompoundTag tag = gun.getOrCreateTag();
        if (tag.contains("Inventory")) {
            handler.deserializeNBT(tag.getCompound("Inventory"));
        }

        // 1. 获取轮询指针 (0=Alpha, 1=Bravo, 2=Charlie, 3=Delta)
        int currentGroup = tag.getInt("ShotPointer");
        boolean shotFired = false;

        // 2. 轮询四个弹仓组
        for (int g = 0; g < 4; g++) {
            int targetGroup = (currentGroup + g) % 4;
            int groupStartIndex = targetGroup * 4;

            // 3. 检查当前组内的 4 个格子
            for (int i = 0; i < 4; i++) {
                int slotIndex = groupStartIndex + i;
                ItemStack ammo = handler.getStackInSlot(slotIndex);

                if (!ammo.isEmpty() && ammo.getItem() instanceof BlockItem bi
                        && bi.getBlock() instanceof DualCannonProjectileBlock<?> projBlock) {

                    // 4. 利用解析出的方块类生成实体 (自动处理曳光/引信)
                    AbstractDualCannonProjectile projectile = projBlock.getProjectile(level, ammo);
                    if (projectile != null) {
                        setupAndShoot(level, player, projectile, ammo);

                        // 消耗弹药并更新指针
                        handler.extractItem(slotIndex, 1, false);
                        tag.putInt("ShotPointer", (targetGroup + 1) % 4);
                        shotFired = true;
                        break;
                    }
                }
            }
            if (shotFired) break;
        }
        player.getCooldowns().addCooldown(this, 20);
        if (shotFired) {
            tag.put("Inventory", handler.serializeNBT());
            // 射击成功后的 CD (20 tick = 1秒)
            player.getCooldowns().addCooldown(this, 12);
        } else {
            // 空仓提示 (仅发送一次)
            player.displayClientMessage(Component.literal("§c弹舱清空"), true);
            player.getCooldowns().addCooldown(this, 40);
        }
    }

    private void setupAndShoot(Level level, Player player, AbstractDualCannonProjectile projectile, ItemStack ammo) {
        Vec3 look = player.getLookAngle();
        // 设置位置在玩家前方一点
        projectile.setPos(player.getEyePosition().add(look.scale(0.5)));
        projectile.setOwner(player);

        float materialBonus = 1.3f;
        projectile.setDurabilityModifier(materialBonus);

        // 弹药存续时间强行
        projectile.setLifetime(400);
        float baseVel = projectile.getInitVel(); // 基础初速
        float baseSpread = projectile.getProjectileSpread(); // 基础散布


        // 增加初速 (1.0x) 并 降低散布 (0.2x)
        float enhancedVel = baseVel;
        float preciseSpread = baseSpread * 0.2f;

        // 执行发射：初速和散布由弹药方块预设决定
        projectile.shoot(look.x, look.y, look.z, enhancedVel, preciseSpread);
        //疑似质量检查或是穿透力
        projectile.setProjectileMass(projectile.getMaximumMass() * 1.5f);
        level.addFreshEntity(projectile);

        // 播放机炮射击音效
        // 使用较高的 pitch (1.5f)
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.8F);
    }
}