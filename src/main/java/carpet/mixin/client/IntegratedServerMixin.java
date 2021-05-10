package carpet.mixin.client;

import carpet.CarpetServer;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {
    @Inject(method = "setupServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/integrated/IntegratedServer;createWorlds(Ljava/lang/String;Ljava/lang/String;JLnet/minecraft/world/level/LevelGeneratorType;Ljava/lang/String;)V"))
    private void onSetupServerIntegrated(CallbackInfoReturnable<Boolean> cir) {
        CarpetServer.getInstance().onServerLoaded();
    }
}
