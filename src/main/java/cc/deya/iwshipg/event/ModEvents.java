package cc.deya.iwshipg.event;

import cc.deya.iwshipg.Iwshipg;
import cc.deya.iwshipg.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Iwshipg.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level level = player.level();
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        // 检查是否穿着动力靴
        boolean hasBoots = boots.is(ModItems.ENGINE_BOOT.get());

        // 逻辑判定：脚下 1.5 格内是否有水 (判定范围稍大以维持飞行状态)
        BlockPos pos = player.blockPosition();
        boolean isAboveWater = level.getBlockState(pos).is(Blocks.WATER) ||
                level.getBlockState(pos.below()).is(Blocks.WATER);

        if (hasBoots && isAboveWater && !player.isShiftKeyDown()) {
            // --- 1. 开启伪飞行模式 ---
            player.getAbilities().mayfly = true;

            // 如果玩家不在飞行，强制让他飞起来（离地感）
            if (!player.getAbilities().flying) {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }

            if (level.isClientSide) {
                // 1. 停止腿部摆动动画
                // walkAnimation 负责处理肢体移动的幅度
                player.walkAnimation.setSpeed(0f);
                player.walkAnimation.position(0f);

            }

            // --- 2. 物理手感调整 ---
            if (level.isClientSide) {
                handleEngineFlightPhysics(player);
            }

            // --- 3. 高度锁定：防止飞到天上去 ---
            // 如果离水面太高（比如超过 1.2 格），强制压低 Y 轴
            double waterY = pos.getY() + 1.0;
            if (player.getY() > waterY + 0.2) {
                Vec3 m = player.getDeltaMovement();
                player.setDeltaMovement(m.x, -0.1, m.z);
            }
        } else {
            // --- 4. 状态恢复 ---
            // 如果离开水面或脱掉靴子，且不是创造模式，关闭飞行
            if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    private static void handleEngineFlightPhysics(Player player) {
        Vec3 motion = player.getDeltaMovement();

        // --- 数值定义区 ---
        double maxSpeed = 1.4;      // 28格每秒 / 20 ticks = 1.4
        double accel = 0.015;        // 较低的加速度，产生缓慢推背感
        double friction = 1.095;    // 极高惯性：每 tick 只保留 99.2% 的速度
        // -----------------

        // 1. 处理玩家输入（加速）
        if (player.zza != 0 || player.xxa != 0) {
            Vec3 look = player.getLookAngle();
            // 仅取水平方向的朝向
            Vec3 dir = new Vec3(look.x, 0, look.z).normalize();

            // 只有在按住 W/S 的时候注入加速度
            // 注意：player.zza 正值代表前进
            double nextX = motion.x + dir.x * accel * player.zza;
            double nextZ = motion.z + dir.z * accel * player.zza;

            // 限制最大速度
            double currentSpeed = Math.sqrt(nextX * nextX + nextZ * nextZ);
            if (currentSpeed > maxSpeed) {
                double ratio = maxSpeed / currentSpeed;
                nextX *= ratio;
                nextZ *= ratio;
            }

            player.setDeltaMovement(nextX, motion.y, nextZ);
        } else {
            // 2. 处理惯性滑行
            // 当玩家松开按键，我们手动接管减速过程，覆盖原生的飞行阻力
            if (motion.horizontalDistance() > 0.01) {
                // 强行保持极长距离的滑行
                player.setDeltaMovement(motion.x * friction, motion.y, motion.z * friction);
                // 阻止原生阻力介入
                player.hasImpulse = true;
            }
        }
    }
}