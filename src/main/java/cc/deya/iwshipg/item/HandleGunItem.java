package cc.deya.iwshipg.item;

import cc.deya.iwshipg.inventory.HandleArtilleryMenu;
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

public class HandleGunItem extends Item {
    private static final int FIRE_RATE = 20; //1秒一发
    private static final float MUZZLE_VELOCITY = 8F; // 步枪弹初速更高，更平直

    public HandleGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("步枪供弹具");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                        return new HandleArtilleryMenu(id, inv, stack);
                    }
                }, buf -> buf.writeItem(stack));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        } else {
            if (!player.getCooldowns().isOnCooldown(this)) {
                this.fire(level, player, stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player) {
            int elapsed = 72000 - count;
            // 步枪通常不是全自动，但这里保留低频率连发手感
            if (elapsed > 0 && elapsed % FIRE_RATE == 0) {
                if (!player.getCooldowns().isOnCooldown(this)) {
                    this.fire(level, player, stack);
                }
            }
        }
    }

    public void fire(Level level, Player player, ItemStack gunStack) {
        if (level.isClientSide) return;

        ItemStackHandler handler = new ItemStackHandler(2);
        CompoundTag tag = gunStack.getOrCreateTag();
        if (tag.contains("Inventory")) {
            handler.deserializeNBT(tag.getCompound("Inventory"));
        }

        // 核心修改：只读取并消耗 0 号槽位
        ItemStack activeAmmo = handler.getStackInSlot(0);

        if (!activeAmmo.isEmpty() && activeAmmo.getItem() instanceof AutocannonAmmoItem round) {
            AbstractAutocannonProjectile projectile = round.getAutocannonProjectile(activeAmmo, level);
            if (projectile != null) {
                Vec3 look = player.getLookAngle();
                // 枪管较长，生成点稍微靠前
                Vec3 spawnPos = player.getEyePosition().add(look.scale(1.2));

                projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                projectile.setChargePower(8.0F);//毁伤倍率
                projectile.setLifetime(180);   // 给它约 9 秒的飞行时间

                // 自动判断曳光
                projectile.setTracer(round.isTracer(activeAmmo));

                // 高精度射击
                projectile.shoot(look.x, look.y, look.z, MUZZLE_VELOCITY, 0.005F);
                projectile.setOwner(player);
                level.addFreshEntity(projectile);

                // 消耗 0 号位弹药
                activeAmmo.shrink(1);
                tag.put("Inventory", handler.serializeNBT());

                // 步枪音效：高音调，更清脆
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        CBCSoundEvents.FIRE_AUTOCANNON.getMainEvent(), SoundSource.PLAYERS, 1.8F, 1.6F);

                player.getCooldowns().addCooldown(this, FIRE_RATE);
            }
        } else {
            // 如果 0 号位空了，即使 1 号位有东西也不会打火
            player.displayClientMessage(Component.literal("§7弹匣清空"), true);
            player.stopUsingItem();
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }
}