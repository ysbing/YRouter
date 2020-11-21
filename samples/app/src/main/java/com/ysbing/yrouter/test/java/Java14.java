package com.ysbing.yrouter.test.java;

import com.ysbing.yrouter.api.YRouterApi;

/**
 * 抽象类实现接口
 */
@YRouterApi
abstract class Java14 implements Java14InterfaceTest {
    @Override
    public void a() {
    }
}

interface Java14InterfaceTest {
    void a();
}