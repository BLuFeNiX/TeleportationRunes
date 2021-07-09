package net.blufenix.common;

import net.blufenix.teleportationrunes.Config;
import net.blufenix.teleportationrunes.TeleportationRunes;

import java.util.logging.Logger;

public class Log {

    private static class LazyHolder {
        public static Logger logger = TeleportationRunes.getInstance().getLogger();
    }

    public static void d(String format, Object... args) {
        if (Config.debug) {
            LazyHolder.logger.info(String.format(format, args));
        }
    }

    public static void e(String format, Object... args) {
        int lastIdx = args.length-1;
        if (args.length > 0 && args[lastIdx] instanceof Throwable) {
            Object[] tmpArgs = new Object[lastIdx];
            System.arraycopy(args, 0, tmpArgs, 0, lastIdx);
            LazyHolder.logger.warning(String.format(format, tmpArgs));
            LazyHolder.logger.warning("ERROR: "+((Throwable)args[lastIdx]).getMessage());
            ((Throwable)args[lastIdx]).printStackTrace();
        } else {
            LazyHolder.logger.warning(String.format(format, args));
        }
    }

}
