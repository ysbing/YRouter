package com.ysbing.yrouter.test.java;

import androidx.annotation.NonNull;

import com.ysbing.yrouter.api.YRouterApi;

import java.io.File;

/**
 * 继承一个系统类必须调super
 */
public class Java11 extends File {

    public Java11(@NonNull String pathname) {
        super((File) null, (String) null);
    }

    @YRouterApi
    public void a() {
    }
}
