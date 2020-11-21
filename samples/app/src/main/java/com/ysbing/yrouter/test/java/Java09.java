package com.ysbing.yrouter.test.java;

import androidx.fragment.app.Fragment;

import com.ysbing.yrouter.api.YRouterApi;

import java.util.Comparator;

/**
 * 实现一个指定类型的泛型系统接口
 */
@YRouterApi
public class Java09 implements Comparator<Fragment> {
    @Override
    public int compare(Fragment o1, Fragment o2) {
        return 0;
    }
}
