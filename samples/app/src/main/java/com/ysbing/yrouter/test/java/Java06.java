package com.ysbing.yrouter.test.java;

import android.app.Activity;
import android.content.Context;

import androidx.fragment.app.Fragment;

import com.ysbing.yrouter.api.YRouterApi;
import com.ysbing.yrouter.api.YRouterSkip;

/**
 * 开放类内所有方法和变量
 */
@YRouterApi
public class Java06 {
    private Java06() {
    }

    public Java06(Fragment fragment) {
    }

    @YRouterSkip
    public int a;
    public float b;

    @YRouterSkip
    public void c() {
    }

    public Activity d(Double num, Context context) {
        return null;
    }
}
