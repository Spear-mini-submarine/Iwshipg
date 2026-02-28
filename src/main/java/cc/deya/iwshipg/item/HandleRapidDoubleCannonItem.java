package cc.deya.iwshipg.item;

import cc.deya.iwshipg.inventory.HandleRapidMenu;
import com.cainiao1053.cbcmoreshells.munitions.dual_cannon.AbstractDualCannonProjectile;
import com.cainiao1053.cbcmoreshells.munitions.dual_cannon.DualCannonProjectileBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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

public class HandleRapidDoubleCannonItem extends Item {

    public HandleRapidDoubleCannonItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (id, inv, p) -> new HandleRapidMenu(id, inv, stack),
                        Component.literal("Double-Barreled Cannon")
                ), buf -> buf.writeItem(stack));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            if (!level.isClientSide) {
                fireDouble(level, player, stack);
            }
            return InteractionResultHolder.consume(stack);
        }
    }

    private void fireDouble(Level level, Player player, ItemStack gun) {
        ItemStackHandler handler = new ItemStackHandler(16);
        CompoundTag tag = gun.getOrCreateTag();
        if (tag.contains("Inventory")) {
            handler.deserializeNBT(tag.getCompound("Inventory"));
        }

        int currentGroup = tag.getInt("ShotPointer");
        int shotsFiredThisTime = 0;

        // 双联装逻辑：尝试在当前组（及后续组）中寻找最多 2 发弹药
        for (int g = 0; g < 4 && shotsFiredThisTime < 2; g++) {
            int targetGroup = (currentGroup + g) % 4;
            int groupStartIndex = targetGroup * 4;

            for (int i = 0; i < 4 && shotsFiredThisTime < 2; i++) {
                int slotIndex = groupStartIndex + i;
                ItemStack ammo = handler.getStackInSlot(slotIndex);

                if (!ammo.isEmpty() && ammo.getItem() instanceof BlockItem bi
                        && bi.getBlock() instanceof DualCannonProjectileBlock<?> projBlock) {

                    AbstractDualCannonProjectile projectile = projBlock.getProjectile(level, ammo);
                    if (projectile != null) {
                        // shotsFiredThisTime 为 0 时偏左，为 1 时偏右
                        double sideOffset = (shotsFiredThisTime == 0) ? -0.5 : 0.5;
                        setupAndShootDouble(level, player, projectile, ammo, sideOffset);

                        handler.extractItem(slotIndex, 1, false);
                        shotsFiredThisTime++;
                    }
                }
            }
        }

        if (shotsFiredThisTime > 0) {
            // 每次齐射后指针依然步进，保证供弹循环
            tag.putInt("ShotPointer", (currentGroup + 1) % 4);
            tag.put("Inventory", handler.serializeNBT());

            // 齐射 CD 稍长一点点 (15 tick = 0.75秒)
            player.getCooldowns().addCooldown(this, 15);

            // 播放你之前找到的专属音效 (双联装建议声音大一点)
            playRandomFireSound(level, player);
        } else {
            player.displayClientMessage(Component.literal("§c弹舱清空"), true);
            player.getCooldowns().addCooldown(this, 40);
        }
    }

    private void setupAndShootDouble(Level level, Player player, AbstractDualCannonProjectile projectile, ItemStack ammo, double sideOffset) {
        Vec3 look = player.getLookAngle();

        // 计算侧向偏移向量 (利用叉积求出玩家右手方向)
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up).normalize();
        Vec3 spawnPos = player.getEyePosition()
                .add(look.scale(0.8)) // 向前一点
                .add(right.scale(sideOffset)); // 左右偏移

        projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        projectile.setOwner(player);

        // 威力强化
        projectile.setDurabilityModifier(1.3f);
        projectile.setLifetime(400);

        // 双联装通常散布会稍微大一点点以增加覆盖面
        float enhancedVel = projectile.getInitVel();
        float spread = 0.3f;

        projectile.shoot(look.x, look.y, look.z, enhancedVel, spread);
        projectile.setProjectileMass(projectile.getMaximumMass() * 1.5f);

        level.addFreshEntity(projectile);
    }

    private void playRandomFireSound(Level level, Player player) {
        // 随机调用 dual_cannon_1 到 3
        int idx = level.random.nextInt(3) + 1;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cbcmoreshells", "dual_cannon_" + idx)),
                SoundSource.PLAYERS, 2.5F, 0.9F + level.random.nextFloat() * 0.2F);
    }
}