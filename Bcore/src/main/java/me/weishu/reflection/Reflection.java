package me.weishu.reflection;

import android.content.Context;
import java.lang.reflect.Method;

/**
 * Native Java reflection bypass unsealing hidden Android APIs.
 * Embeds FreeReflection directly into Bcore to eliminate deprecated JCenter dependency.
 */
public class Reflection {
    public static int unseal(Context context) {
        try {
            Method forName = Class.class.getDeclaredMethod("forName", String.class);
            Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);

            Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
            Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null);
            Method setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});

            Object sVmRuntime = getRuntime.invoke(null);
            setHiddenApiExemptions.invoke(sVmRuntime, new Object[]{new String[]{"L"}});
            return 0;
        } catch (Throwable e) {
            return -1;
        }
    }
}
