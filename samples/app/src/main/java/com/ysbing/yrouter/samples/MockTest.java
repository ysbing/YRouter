package com.ysbing.yrouter.samples;

import android.widget.Toast;

import com.ysbing.yrouter.api.mock.YRouterMockClass;
import com.ysbing.yrouter.api.mock.YRouterMockValue;
import com.ysbing.yrouter.mock.ContextHelper;
import com.ysbing.yrouter.sampleslibrary2.Library2Api;

@YRouterMockClass(Library2Api.class)
public class MockTest {

    @YRouterMockValue("e22222222222")
    private String rrrrrrrr = "11啦啦";

    @YRouterMockValue("w222222222")
    int erewrere(float lala) {
        return 222222;
    }

    @YRouterMockValue("voidTest")
    void voidTest(String str) {
        Toast.makeText(ContextHelper.getAppContext(), str, Toast.LENGTH_SHORT).show();
    }
}