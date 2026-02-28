package cc.deya.iwshipg.mixin.flywheel;

import com.jozufozu.flywheel.config.BackendType;
import com.jozufozu.flywheel.config.FlwConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * idea 环境专用,编译时记得在iwshipg.mixins.json排除
 */
@Mixin(value = FlwConfig.class, remap = false)
public class FlwConfigMixin {
    @Inject(
            method = "getBackendType",
            at = @At("HEAD"),
            cancellable = true
    )
    private void getBackendType(CallbackInfoReturnable<BackendType> cir) {
        cir.setReturnValue(BackendType.INSTANCING);
    }
}
