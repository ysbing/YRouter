package com.ysbing.yrouter.test.java;

import com.ysbing.yrouter.api.YRouterApi;

/**
 * 实现一个未指定类型的泛型系统接口
 */
@YRouterApi
public class Java08<T> implements Comparable<T> {

    @Override
    public int compareTo(T o) {
        return 0;
    }
}
