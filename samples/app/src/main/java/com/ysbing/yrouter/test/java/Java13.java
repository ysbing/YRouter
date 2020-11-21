package com.ysbing.yrouter.test.java;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.ysbing.yrouter.api.YRouterApi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * 继承一个通配符泛型系统类
 */
@YRouterApi
public class Java13 extends ArrayList<Set<? extends FragmentActivity>> {
    @Override
    public boolean addAll(@NonNull Collection<? extends Set<? extends FragmentActivity>> c) {
        return super.addAll(c);
    }
}
