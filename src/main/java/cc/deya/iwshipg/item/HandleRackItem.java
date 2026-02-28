package cc.deya.iwshipg.item;

import cc.deya.iwshipg.inventory.HandleRapidMenu;
import com.cainiao1053.cbcmoreshells.munitions.racked_projectile.AbstractRackedProjectile;
import com.cainiao1053.cbcmoreshells.munitions.racked_projectile.RackedProjectileBlock;
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
import org.jetbrains.annotations.NotNull;

public class HandleRackItem extends Item {

    public HandleRackItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                // 打开挂架管理界面
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (id, inv, p) -> new HandleRapidMenu(id, inv, stack), // 这里可复用之前的 16 格界面，或定制为 8 格
                        Component.literal("Weapon Rack")
                ), buf -> buf.writeItem(stack));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            if (!level.isClientSide) {
                fireRack(level, player, stack);
            }
            return InteractionResultHolder.consume(stack);
        }
    }

    private void fireRack(Level level, Player player, ItemStack gun) {
        ItemStackHandler handler = new ItemStackHandler(16);
        CompoundTag tag = gun.getOrCreateTag();
        if (tag.contains("Inventory")) {
            handler.deserializeNBT(tag.getCompound("Inventory"));
        }

        boolean shotFired = false;
        // 挂架通常按顺序发射
        for (int i = 0; i < 16; i++) {
            ItemStack ammo = handler.getStackInSlot(i);

            if (!ammo.isEmpty() && ammo.getItem() instanceof BlockItem bi
                    && bi.getBlock() instanceof RackedProjectileBlock<?> rackBlock) {

                // 生成挂架弹药实体
                AbstractRackedProjectile projectile = rackBlock.getProjectile(level, ammo);
                if (projectile != null) {
                    setupAndLaunch(level, player, projectile, ammo);

                    handler.extractItem(i, 1, false);
                    shotFired = true;
                    break;
                }
            }
        }

        if (shotFired) {
            tag.put("Inventory", handler.serializeNBT());
            // 挂架发射 CD  (0.1秒)
            player.getCooldowns().addCooldown(this, 2);
        } else {
            player.displayClientMessage(Component.literal("§6挂架已空"), true);
        }
    }

    private void setupAndLaunch(Level level, Player player, AbstractRackedProjectile projectile, ItemStack ammo) {
        Vec3 look = player.getLookAngle();
        // 从玩家头顶略高处发射，模拟挂载位置
        projectile.setPos(player.getX(), player.getEyeY() + 0.5, player.getZ());
        projectile.setOwner(player);

        // 区分火箭弹与航空炸弹的初速：
        // 火箭弹需要高初速，炸弹则主要是顺着惯性落下
        float launchVel = 2.0f;
        projectile.shoot(look.x, look.y, look.z, launchVel, 0.05f);

        level.addFreshEntity(projectile);
        projectile.setLifetime(400);

        // 播放你之前看到的 rocket_launch 音效！
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cbcmoreshells", "rocket_launch")),
                SoundSource.PLAYERS, 3.0F, 1.0F);
    }
}