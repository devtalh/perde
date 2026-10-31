package io.github.devtalh.perde.client.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Test process only: silence Chromium before any page or browser is created. */
@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "net.dimaskama.mcef.impl.MCEFApiImpl", remap = false)
public abstract class McefMuteMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lme/friwi/jcefmaven/CefAppBuilder;addJcefArgs([Ljava/lang/String;)V"), index = 0)
    private String[] muteTestAudio(String[] arguments) {
        String[] muted = java.util.Arrays.copyOf(arguments, arguments.length + 1);
        muted[arguments.length] = "--mute-audio";
        org.slf4j.LoggerFactory.getLogger("perde-f2-probe").info("Test Chromium audio: --mute-audio");
        return muted;
    }
}
