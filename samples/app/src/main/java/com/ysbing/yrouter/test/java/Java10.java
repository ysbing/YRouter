package com.ysbing.yrouter.test.java;

import android.app.Activity;

import androidx.fragment.app.Fragment;

import com.ysbing.yrouter.api.YRouterApi;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 实现一个通配符泛型系统接口
 */
@YRouterApi
public class Java10 implements Comparator<List<Map<? super Activity, Fragment>>> {
    @Override
    public int compare(List<Map<? super Activity, Fragment>> o1, List<Map<? super Activity, Fragment>> o2) {
        return 0;
    }
}
