package com.ysbing.yrouter.test.java;

import com.ysbing.yrouter.api.YRouterApi;

/**
 * 内部类
 */
public class Java17 {
    private long a = 10L;

    private void b() {
    }

    private class Java17Inner1 {
        @YRouterApi
        private class Java17Inner1_1 {
            private String a;

            public void b() {
            }
        }
    }

    @YRouterApi
    private class Java17Inner2 {
        private int a;
        private Float b;
    }
}
