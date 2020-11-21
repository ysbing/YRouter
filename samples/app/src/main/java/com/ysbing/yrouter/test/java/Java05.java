package com.ysbing.yrouter.test.java;

import android.app.Activity;
import android.content.Context;

import androidx.fragment.app.Fragment;

import com.ysbing.yrouter.api.YRouterApi;

/**
 * 开放方法或变量
 */
@YRouterApi
public class Java05 {
    public Java05(Activity a) {
    }

    private Java05(Fragment a, String b, String c) {
    }

    protected Java05(Fragment a, String b, String c, int d) {
    }

    Java05(Fragment a, String b, int c) {
    }

    public int a;
    @YRouterApi
    public float b;

    public void c() {
    }

    @YRouterApi
    public Activity d(Double num, Context context) {
        return null;
    }
}
