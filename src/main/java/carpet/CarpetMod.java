package carpet;

import carpet.helpers.StackTraceDeobfuscator;
import carpet.pubsub.PubSubManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class CarpetMod {
    private static final Logger LOGGER = LogManager.getLogger("CarpetMod");
    private static final CompletableFuture<StackTraceDeobfuscator> DEOBFUSCATOR = StackTraceDeobfuscator.loadDefault();
    private static CarpetMod instance;

    public static final Random rand = new Random();
    public static final PubSubManager PUBSUB = new PubSubManager();
    public static ThreadLocal<Boolean> playerInventoryStacking = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private CarpetMod() {
        instance = this;
    }

    public static CarpetMod getInstance() {
        return instance;
    }

    @Nullable
    public static StackTraceDeobfuscator getDeobfuscator(boolean block) {
        if (!DEOBFUSCATOR.isDone() && !block) return null;
        if (DEOBFUSCATOR.isCompletedExceptionally() || DEOBFUSCATOR.isCancelled()) return null;
        try {
            return DEOBFUSCATOR.join();
        } catch (RuntimeException e) {
            LOGGER.debug(e);
            return null;
        }
    }
}
