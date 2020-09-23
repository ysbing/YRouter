package com.ysbing.yrouter.samples;

import android.content.Context;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.ysbing.yrouter.api.YRouterApi;

public class JavaTest extends Fragment {
    @YRouterApi
    public String a = "JavaTest";
    @YRouterApi
    public static int num1 = 100;
    @YRouterApi
    public static int num2 = 200;

    @YRouterApi
    public char mmmmmmmmm = 0;
    @YRouterApi
    public short kkkkkk = 0;
    @YRouterApi
    public byte bbbbbbbbb =0;

    public static class InnerClass1 extends JavaTest {

        @YRouterApi
        public InnerClass1(String a) {
        }

        @YRouterApi
        public static void f111(Context context) {
            Toast.makeText(context, "这里是JavaTest的第一个内部类", Toast.LENGTH_SHORT).show();
        }
    }

    public static class InnerClass2 {
        @YRouterApi
        public void f222(FragmentActivity context) {
            Toast.makeText(context, "这里是JavaTest的第二个内部类", Toast.LENGTH_SHORT).show();
        }
    }
}