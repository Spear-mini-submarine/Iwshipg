package cc.deya.iwshipg.cannon.homing_cannon_torpedo;

import com.cainiao1053.cbcmoreshells.CBCMSBlocks;
import com.cainiao1053.cbcmoreshells.index.CBCMSMunitionPropertiesHandlers;
import com.cainiao1053.cbcmoreshells.munitions.big_cannon.cannon_torpedo.CannonTorpedoProjectile;
import com.cainiao1053.cbcmoreshells.munitions.big_cannon.config.TorpedoProjectilePropertiesComponent;
import com.cainiao1053.cbcmoreshells.munitions.big_cannon.config.TorpedoProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import rbasamoyai.createbigcannons.munitions.big_cannon.config.BigCannonFuzePropertiesComponent;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class HomingCannonTorpedoProjectile extends CannonTorpedoProjectile {
    private static final float MAX_TURN_ANGLE_PER_TICK = 5.0F; // 每tick最大转向角度（度）
    private static final double HOMING_RANGE = 30.0D; // 跟踪搜索范围（方块）
    private static final double HOMING_SPEED_MULTIPLIER = 1D; // 跟踪时速度加成

    // 跟踪目标相关
    private UUID targetUUID;
    private Entity cachedTarget;
    private int noTargetTicks; // 无目标时的计数，超过阈值停止跟踪
    private int tickNoWater;

    public HomingCannonTorpedoProjectile(EntityType<? extends HomingCannonTorpedoProjectile> type, Level level) {
        super(type, level);
        this.targetUUID = null;
        this.cachedTarget = null;
        this.noTargetTicks = 0;
        this.tickNoWater = 0;
    }

    @Override
    public void tick() {
        // 先执行父类基础逻辑（水中tick计数、引信校验等）
        super.tick();

        // 仅在服务端且未过载、未销毁,已存在超过40tick且入水超过35tick时执行跟踪逻辑
        if (!this.level().isClientSide && !this.tooManyCharges && !this.isRemoved() && this.tickCount > 40 && this.tickInWater >= 35) {
            if (!this.isInValidWater() && this.tickInWater >= 35) {
                this.tickNoWater++;

                //如果有35tick脱离了水体则自爆
                if (this.tickNoWater >= 35) {
                    this.detonate(this.position());
                    this.removeNextTick = true;
                }
            }

            Entity target = this.getValidTarget();
            if (target != null) {
                this.noTargetTicks = 0;
                this.homingSteer(target);
            } else {
                this.noTargetTicks++;
                if (this.noTargetTicks > 60) {
                    this.resetTarget();
                }
            }
        }
    }

    /**
     * 检查鱼雷是否在有效水体中（防止飞出水体）
     */
    private boolean isInValidWater() {
        BlockPos currentPos = this.blockPosition();
        FluidState fluidState = this.level().getFluidState(currentPos);

        if (fluidState.isEmpty()) {
            return false;
        }

        BlockPos belowPos = currentPos.below();
        FluidState belowFluid = this.level().getFluidState(belowPos);
        return !belowFluid.isEmpty();
    }

    /**
     * 获取有效跟踪目标（优先缓存目标，无则搜索）
     */
    private Entity getValidTarget() {
        // 1. 检查缓存目标是否有效
        if (this.isValidTarget(this.cachedTarget)) {
            return this.cachedTarget;
        }

        // 2. 通过UUID重新获取目标
        if (this.targetUUID != null) {
            Entity target = ((ServerLevel) this.level()).getEntity(this.targetUUID);
            if (this.isValidTarget(target)) {
                this.cachedTarget = target;
                return target;
            }
            this.targetUUID = null;
        }

        // 3. 搜索范围内的敌方目标
        return this.searchNearbyTargets();
    }

    /**
     * 搜索范围内的有效敌方目标
     */
    private Entity searchNearbyTargets() {
        AABB searchBox = this.getBoundingBox().inflate(HOMING_RANGE);
        List<Player> potentialTargets = this.level().getEntitiesOfClass(
                Player.class,
                searchBox,
                this::isValidTarget
        );

        // 优先选择最近的目标
        Optional<Player> nearestTarget = potentialTargets.stream()
                .min((e1, e2) -> {
                    double d1 = e1.distanceToSqr(this);
                    double d2 = e2.distanceToSqr(this);
                    return Double.compare(d1, d2);
                });

        if (nearestTarget.isPresent()) {
            LivingEntity target = nearestTarget.get();
            this.targetUUID = target.getUUID();
            this.cachedTarget = target;
            return target;
        }

        return null;
    }

    /**
     * 判断实体是否为有效跟踪目标（友方判断+存活+在水中）
     */
    private boolean isValidTarget(Entity entity) {
        // 排除无效实体
        if (entity == null || entity.isRemoved() || !entity.isAlive() || entity.equals(this)) {
            return false;
        }

        // 友方判断：通过团队系统判断是否为友方
        Team thisTeam = this.getTeam();
        Team targetTeam = entity.getTeam();
        if (thisTeam != null && targetTeam != null && thisTeam.isAlliedTo(targetTeam)) {
            return false; // 友方不跟踪
        }

        // 目标不能是发射者
        Entity owner = this.getOwner();
        return owner == null || !owner.equals(entity);
    }

    /**
     * 跟踪转向逻辑（限制每tick转向角度）
     */
    private void homingSteer(Entity target) {
        Vec3 currentVelocity = this.getDeltaMovement();
        Vec3 targetPos = target.getEyePosition(1.0F);
        Vec3 torpedoPos = this.position();
        Vec3 desiredDir = targetPos.subtract(torpedoPos).normalize();

        Vec3 currentDir = currentVelocity.normalize();
        double angleRadians = Math.acos(Math.min(1.0, Math.max(-1.0, currentDir.dot(desiredDir))));
        float angleDegrees = (float) Math.toDegrees(angleRadians);

        Vec3 newDir;
        if (angleDegrees > MAX_TURN_ANGLE_PER_TICK) {
            // 球面插值，限制转向角度
            newDir = this.slerp(currentDir, desiredDir, MAX_TURN_ANGLE_PER_TICK / angleDegrees);
        } else {
            newDir = desiredDir;
        }

        double currentSpeed = currentVelocity.length();
        double newSpeed = currentSpeed * HOMING_SPEED_MULTIPLIER;
        Vec3 newVelocity = newDir.scale(newSpeed);

        this.setDeltaMovement(newVelocity);
        this.hasImpulse = true;

        this.onTickRotate();
    }

    /**
     * 球面插值（SLERP）：平滑转向，防止瞬间掉头
     */
    private Vec3 slerp(Vec3 start, Vec3 end, double t) {
        double dot = Math.min(1.0, Math.max(-1.0, start.dot(end)));
        double theta = Math.acos(dot) * t;
        Vec3 relativeVec = end.subtract(start.scale(dot)).normalize();
        return start.scale(Math.cos(theta)).add(relativeVec.scale(Math.sin(theta)));
    }

    /**
     * 重置跟踪目标
     */
    private void resetTarget() {
        this.targetUUID = null;
        this.cachedTarget = null;
        this.noTargetTicks = 0;
    }

    @Override
    protected void detonate(Position position) {
        super.detonate(position);
        if (this.level().isClientSide) return;

        float explosivePower = this.getAllProperties().explosion().explosivePower();
        if (this.getTickInWater() > 20) {
            explosivePower *= 4.0F;
        }
        AABB explosionBounds = this.getBoundingBox().inflate(explosivePower);
        List<LivingEntity> entities = this.level().getEntitiesOfClass(
                LivingEntity.class,
                explosionBounds,
                entity -> entity != null && entity.isAlive()
        );
        entities.forEach(entity -> {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 1, 10));
            entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 1, 10));
        });
    }

    @Nonnull
    @Override
    protected BigCannonFuzePropertiesComponent getFuzeProperties() {
        return this.getAllProperties().fuze();
    }

    @Nonnull
    @Override
    protected TorpedoProjectilePropertiesComponent getBigCannonProjectileProperties() {
        return this.getAllProperties().torpedoProperties();
    }

    @Override
    protected TorpedoProperties getAllProperties() {
        return CBCMSMunitionPropertiesHandlers.TORPEDO_PROJECTILE.getPropertiesOf(this);
    }

    @Override
    public BlockState getRenderedBlockState() {
        return CBCMSBlocks.CANNON_TORPEDO.getDefaultState().setValue(BlockStateProperties.FACING, Direction.NORTH);
    }

    // 序列化跟踪目标数据（存档/同步）
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.targetUUID != null) {
            tag.putUUID("HomingTarget", this.targetUUID);
        }
        tag.putInt("NoTargetTicks", this.noTargetTicks);
    }

    // 反序列化跟踪目标数据
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("HomingTarget")) {
            this.targetUUID = tag.getUUID("HomingTarget");
        }
        this.noTargetTicks = tag.getInt("NoTargetTicks");
    }
}
