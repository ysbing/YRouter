package com.ysbing.yrouter.test.java;

import android.app.Activity;
import android.app.Fragment;

import com.ysbing.yrouter.api.YRouterApi;

/**
 * 继承系统类，系统类实现了接口
 */
@YRouterApi
class Java16 extends Fragment implements Java16InterfaceTest {
}

@YRouterApi
interface Java16InterfaceTest {
    Activity getActivity();
}