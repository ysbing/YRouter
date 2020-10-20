package com.ysbing.yrouter.mock;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;

/**
 * 获取Context对象的助手
 *
 * @author ysbing
 */
public class ContextHelper {

    public static Context getAppContext() {
        return getApplication().getApplicationContext();
    }

    public static Application getApplication() {
        return ActivityThread.currentActivityThread().getApplication();
    }
}
