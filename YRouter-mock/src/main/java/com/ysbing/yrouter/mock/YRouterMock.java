package com.ysbing.yrouter.mock;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ysbing.yrouter.api.mock.YRouterMockBean;
import com.ysbing.yrouter.api.mock.YRouterMockPair;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YRouterMock {
    private static final Map<String, String> sCassMap = new HashMap<String, String>() {{
        put("kotlin.Boolean", "java.lang.Boolean");
        put("kotlin.Char", "java.lang.Character");
        put("kotlin.Byte", "java.lang.Byte");
        put("kotlin.Short", "java.lang.Short");
        put("kotlin.Int", "java.lang.Integer");
        put("kotlin.Float", "java.lang.Float");
        put("kotlin.Long", "java.lang.Long");
        put("kotlin.Double", "java.lang.Double");
        put("kotlin.String", "java.lang.String");
        put("kotlin.Number", "java.lang.Number");

        put("kotlin.Error", "java.lang.Error");
        put("kotlin.Throwable", "java.lang.Throwable");
        put("kotlin.Exception", "java.lang.Exception");
        put("kotlin.RuntimeException", "java.lang.RuntimeException");
        put("kotlin.IllegalArgumentException", "java.lang.IllegalArgumentException");
        put("kotlin.IllegalStateException", "java.lang.IllegalStateException");
        put("kotlin.IndexOutOfBoundsException", "java.lang.IndexOutOfBoundsException");
        put("kotlin.UnsupportedOperationException", "java.lang.UnsupportedOperationException");
        put("kotlin.ArithmeticException", "java.lang.ArithmeticException");
        put("kotlin.NumberFormatException", "java.lang.NumberFormatException");
        put("kotlin.NullPointerException", "java.lang.NullPointerException");
        put("kotlin.ClassCastException", "java.lang.ClassCastException");
        put("kotlin.AssertionError", "java.lang.AssertionError");
        put("kotlin.NoSuchElementException", "java.lang.NoSuchElementException");
        put("kotlin.ConcurrentModificationException", "java.util.ConcurrentModificationException");

        put("kotlin.Comparator", "java.util.Comparator");
        put("kotlin.collections.Iterable", "java.lang.Iterable");
        put("kotlin.collections.Collection", "java.util.Collection");
        put("kotlin.collections.List", "java.util.List");
        put("kotlin.collections.Set", "java.util.Set");
        put("kotlin.collections.Map", "java.util.Map");
    }};

    private static List<YRouterMockBean> mockClass = null;

    public static Object fieldMock(
            String className,
            String name,
            String retType
    ) {
        Object ret = getFieldRet(className, name);
        if (ret != null) {
            return ret;
        }
        return getDefaultRet(retType);
    }

    public static Object methodMock(
            String className,
            String name,
            String retType,
            YRouterMockPair... args
    ) {
        Object ret = getMethodRet(className, name, args);
        if (ret != null) {
            return ret;
        }
        return getDefaultRet(retType);
    }

    public static void methodVoidMock(String className, String name, YRouterMockPair... args) {
        getMethodRet(className, name, args);
    }

    private static Object getFieldRet(
            String className,
            String name
    ) {
        List<YRouterMockBean> mockClass = getMockClassArray();
        if (mockClass != null && !mockClass.isEmpty()) {
            for (YRouterMockBean mock : mockClass) {
                if (mock.targetClass.equals(className)) {
                    if (mock.type == YRouterMockBean.TYPE.FIELD && mock.targetName.equals(name)) {
                        try {
                            Class<?> clazz = getClass(mock.className);
                            Field field = clazz.getDeclaredField(mock.name);
                            field.setAccessible(true);
                            return field.get(clazz.newInstance());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Object getMethodRet(
            String className,
            String name,
            YRouterMockPair... args
    ) {
        List<YRouterMockBean> mockClass = getMockClassArray();
        if (mockClass != null && !mockClass.isEmpty()) {
            for (YRouterMockBean mock : mockClass) {
                if (mock.targetClass.equals(className)) {
                    if (mock.type == YRouterMockBean.TYPE.METHOD && mock.targetName.equals(name)) {
                        try {
                            Class<?> clazz = Class.forName(mock.className);
                            List<Class<?>> argsType = new ArrayList<>();
                            List<Object> argsParams = new ArrayList<>();
                            for (YRouterMockPair arg : args) {
                                argsType.add(getClass(arg.className));
                                argsParams.add(arg.args);
                            }
                            Class<?>[] argsTypeArray = argsType.toArray(new Class[0]);
                            Method method = clazz.getDeclaredMethod(mock.name, Arrays.copyOf(argsTypeArray, argsTypeArray.length));
                            method.setAccessible(true);
                            Object[] argsParamsArray = argsParams.toArray(new Object[0]);
                            return method.invoke(clazz.newInstance(), Arrays.copyOf(argsParamsArray, argsParamsArray.length));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Object getDefaultRet(String className) {
        for (Map.Entry<String, String> classMap : sCassMap.entrySet()) {
            if (className.startsWith(classMap.getKey())) {
                className = classMap.getValue();
                break;
            }
        }
        Object returnValue = null;
        switch (className) {
            case "boolean":
            case "java.lang.Boolean":
                returnValue = false;
                break;
            case "char":
            case "java.lang.Character":
                returnValue = '0';
                break;
            case "byte":
            case "short":
            case "int":
            case "java.lang.Byte":
            case "java.lang.Short":
            case "java.lang.Integer":
                returnValue = 0;
                break;
            case "float":
            case "double":
            case "java.lang.Float":
            case "java.lang.Double":
                returnValue = 0f;
                break;
            case "long":
            case "java.lang.Long":
                returnValue = 0L;
                break;
            case "java.lang.String":
                returnValue = "\"\"";
                break;
            default:
                break;
        }
        return returnValue;
    }

    private static List<YRouterMockBean> getMockClassArray() {
        if (mockClass != null) {
            return mockClass;
        }
        try {
            Class<?> mockConfigClass = Class.forName("com.ysbing.yrouter.mock.YRouterMockConfig");
            Field configField = mockConfigClass.getDeclaredField("config");
            String config = (String) configField.get(null);
            mockClass = new Gson().fromJson(config, new TypeToken<List<YRouterMockBean>>() {
            }.getType());
            return mockClass;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Class<?> getClass(String className) throws ClassNotFoundException {
        for (Map.Entry<String, String> classMap : sCassMap.entrySet()) {
            if (className.startsWith(classMap.getKey())) {
                className = classMap.getValue();
                break;
            }
        }
        Class<?> returnValue;
        switch (className) {
            case "boolean" :
            case "java.lang.Boolean" :
                returnValue = boolean.class;
                break;
            case "char":
            case "java.lang.Character":
                returnValue = char.class;
                break;
            case "byte":
            case "java.lang.Byte":
                returnValue = byte.class;
                break;
            case "short":
            case "java.lang.Short":
                returnValue = short.class;
                break;
            case "int":
            case "java.lang.Integer":
                returnValue = int.class;
                break;
            case "float":
            case "java.lang.Float":
                returnValue = float.class;
                break;
            case "double":
            case "java.lang.Double":
                returnValue = double.class;
                break;
            case "long":
            case "java.lang.Long":
                returnValue = long.class;
                break;
            case "java.lang.String":
                returnValue = String.class;
                break;
            default:
                returnValue = Class.forName(className);
                break;
        }
        return returnValue;
    }
}