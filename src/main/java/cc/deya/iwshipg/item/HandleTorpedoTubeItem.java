package cc.deya.iwshipg.item;

import cc.deya.iwshipg.inventory.HandleTorpedoMenu;
import com.cainiao1053.cbcmoreshells.munitions.big_cannon.AbstractCannonTorpedoProjectile;
import com.cainiao1053.cbcmoreshells.munitions.big_cannon.TorpedoProjectileBlock;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import rbasamoyai.createbigcannons.index.CBCSoundEvents;

public class HandleTorpedoTubeItem extends Item {

    public HandleTorpedoTubeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // --- 打开五联装专用 GUI ---
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("鱼雷发射管");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                        return new HandleTorpedoMenu(id, inv, stack);
                    }
                }, buf -> buf.writeItem(stack));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        } else {
            // --- 执行扇形齐射 ---
            if (!player.getCooldowns().isOnCooldown(this)) {
                this.fire(level, player, stack);
            }
            return InteractionResultHolder.consume(stack);
        }
    }

    public void fire(Level level, Player player, ItemStack tubeStack) {
        if (level.isClientSide) return;

        ItemStackHandler handler = new ItemStackHandler(10);
        CompoundTag tag = tubeStack.getOrCreateTag();
        if (tag.contains("Inventory")) {
            handler.deserializeNBT(tag.getCompound("Inventory"));
        }

        boolean firedAny = false;
        // 扇形偏移角度：-10, -5, 0, 5, 10
        float[] angles = {-10f, -5f, 0f, 5f, 10f};

        // 1. 遍历待发架 (0-4 号槽位)
        for (int i = 0; i < 5; i++) {
            ItemStack ammo = handler.getStackInSlot(i);
            if (!ammo.isEmpty() && ammo.getItem() instanceof BlockItem bi &&
                    bi.getBlock() instanceof TorpedoProjectileBlock<?> torpedoBlock) {

                AbstractCannonTorpedoProjectile projectile = torpedoBlock.getProjectile(level, ammo);
                if (projectile != null) {
                    // --- 计算扇形发射向量 ---
                    Vec3 look = player.getLookAngle();
                    // 在 Y 轴上进行旋转偏转
                    Vec3 shootVec = look.yRot(angles[i] * (float) Math.PI / 180f);
                    Vec3 spawnPos = player.getEyePosition().add(shootVec.scale(1.5));

                    projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                    projectile.setChargePower(1.0F); // 鱼雷初始推进力

                    // 检查是否有曳光
                    // 使用 TorpedoProjectileBlock 提供的工具方法从鱼雷物品中提取曳光物品
                    ItemStack tracerStack = TorpedoProjectileBlock.getTracerFromItemStack(ammo);

                    if (!tracerStack.isEmpty()) {
                        // 这里传入的是 ItemStack 实例，而不是 true
                        projectile.setTracer(tracerStack);
                    }

                    // 射击：初速取鱼雷自带参数，极低散布（0.001F）
                    projectile.shoot(shootVec.x, shootVec.y, shootVec.z, projectile.getTorpedoSpeed(), 0.001F);
                    projectile.setOwner(player);

                    level.addFreshEntity(projectile);

                    // 消耗该槽位
                    handler.setStackInSlot(i, ItemStack.EMPTY);
                    firedAny = true;

                    // 发射粒子效果
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, spawnPos.x, spawnPos.y, spawnPos.z, 5, 0.1, 0.1, 0.1, 0.05);
                    }
                }
            }
        }

        // 2. 核心逻辑：自动推弹上膛
        if (firedAny) {
            boolean reloaded = false;
            for (int i = 0; i < 5; i++) {
                ItemStack reserve = handler.getStackInSlot(i + 5);
                if (!reserve.isEmpty()) {
                    handler.setStackInSlot(i, reserve.copy()); // 推到上行
                    handler.setStackInSlot(i + 5, ItemStack.EMPTY); // 清空下行
                    reloaded = true;
                }
            }

            // 保存 NBT
            tag.put("Inventory", handler.serializeNBT());

            // 3. 播放音效
            // 发射声
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    CBCSoundEvents.FIRE_AUTOCANNON.getMainEvent(), SoundSource.PLAYERS, 2.0F, 0.7F);

            // 如果发生了推弹，播一个“咔哒”上膛声
            if (reloaded) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0F, 1.5F);
            }

            // 设置冷却：齐射后需要较长时间整备
            player.getCooldowns().addCooldown(this, 80);
        } else {
            player.displayClientMessage(Component.literal("§c弹舱清空"), true);
        }
    }
}