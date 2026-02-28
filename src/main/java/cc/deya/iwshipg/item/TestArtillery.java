package cc.deya.iwshipg.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import rbasamoyai.createbigcannons.index.CBCEntityTypes;
import rbasamoyai.createbigcannons.index.CBCItems;
import rbasamoyai.createbigcannons.munitions.big_cannon.ap_shell.APShellProjectile;

public class TestArtillery extends Item {

    public static final float COMMON_SPEED = 8.0F;

    public TestArtillery(Properties pProperties) {
        super(pProperties);
    }

    public static void fireCBCAP(ServerLevel serverLevel, Player player) {
        // 1. 直接通过 CBCEntityTypes 获取类型 (使用 .get())
        // 注意：这里需要确保你的环境里 Registrate 依赖已生效，否则编译器会不认识 get()
        APShellProjectile ap = new APShellProjectile(CBCEntityTypes.AP_SHELL.get(), serverLevel);

        // 2. 直接通过 CBCItems 获取引信并设置 (使用 .get())
        // .getDefaultInstance() 返回的就是 ItemStack
        ap.setFuze(CBCItems.IMPACT_FUZE.get().getDefaultInstance());


        // 4. 计算位置与方向
        Vec3 view = player.getViewVector(1.0F);
        Vec3 pos = player.getEyePosition(1.0F).add(view.scale(0.5));
        ap.setPos(pos.x, pos.y, pos.z);

        // 5. 发射
        ap.shoot(view.x, view.y, view.z, COMMON_SPEED, 0.0F);

        // 6. 生成
        serverLevel.addFreshEntity(ap);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (!level.isClientSide()) {
            fireCBCAP((ServerLevel) level, player);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}