package com.ysbing.yrouter.test.java;

import androidx.fragment.app.FragmentActivity;

import com.ysbing.yrouter.api.YRouterApi;

import java.util.ArrayList;

/**
 * 继承一个带泛型的系统类
 */
@YRouterApi
public class Java12 extends ArrayList<FragmentActivity> {
    @Override
    public boolean add(FragmentActivity fragmentActivity) {
        return super.add(fragmentActivity);
    }
}
