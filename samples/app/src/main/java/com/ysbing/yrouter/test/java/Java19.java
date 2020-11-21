package com.ysbing.yrouter.test.java;

import android.app.Activity;

import java.lang.ref.SoftReference;

/**
 * 泛型构造函数
 * TODO: 泛型构造函数暂不支持，没法获取父类构造函数需要的具体类型
 */
public class Java19 extends SoftReference<Activity> {

    public Java19(Activity referent) {
        super(referent);
    }
}