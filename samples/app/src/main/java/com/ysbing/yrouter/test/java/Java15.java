package com.ysbing.yrouter.test.java;

import com.ysbing.yrouter.api.YRouterApi;

/**
 * 父类实现接口类，直接继承，父类不开发API
 */
@YRouterApi
class Java15 extends Java15Parent implements Java15InterfaceTest {
}

class Java15Parent {
    public void a() {
    }
}

@YRouterApi
interface Java15InterfaceTest {
    void a();
}