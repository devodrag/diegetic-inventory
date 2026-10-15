package net.diegetic.inventory.mixin;

import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Отключает проверку лицензионной сессии Mojang для локального мира (Open to LAN),
 * чтобы в среде разработки и по локальной сети можно было тестировать мультиплеер
 * без ошибки "Invalid session".
 */
@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {

    @Inject(method = "setupServer", at = @At("RETURN"))
    private void onSetupServer(CallbackInfoReturnable<Boolean> cir) {
        ((IntegratedServer) (Object) this).setOnlineMode(false);
    }

    @Inject(method = "openToLan", at = @At("HEAD"))
    private void onOpenToLan(GameMode gameMode, boolean cheatsAllowed, int port, CallbackInfoReturnable<Boolean> cir) {
        ((IntegratedServer) (Object) this).setOnlineMode(false);
    }
}
