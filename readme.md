
![](https://github.com/ysbing/YRouter/wiki/assets/video_start.gif)
右键动图新标签页打开效果更佳

## 一、简介

YRouter是一款性能0损耗的Android模块路由。

## 二、框架特性
* 性能0损耗
* 数据模拟，支持模块化开发后的数据模拟
* 反射优化，所有用到反射的地方都可以干掉了

## 三、安装

根目录的build.gradle：
``` gradle
buildscript {
    repositories {
        jcenter()
        google()
    }
    dependencies {
        classpath 'com.ysbing.yrouter:YRouter-gradle-plugin:1.1.0'
    }
}
```
在需要使用的模块应用插件
``` gradle
apply plugin: 'YRouter'
```
或
``` gradle
plugins {
    id 'YRouter'
}
```
## 四、生成索引jar
在需要开放的类、方法、变量的前面加入注解 **@YRouterApi**
``` java
public class JavaTest {
    @YRouterApi
    public String a = "JavaTest";

    public static class InnerClass1 extends JavaTest {
        @YRouterApi
        public InnerClass1(String a) {
        }

        @YRouterApi
        public static void f111(Context context) {
            Toast.makeText(context, "这里是JavaTest的第一个内部类", Toast.LENGTH_SHORT).show();
        }
    }
}
```

执行app的任务yrouter，如下图：

![](https://github.com/ysbing/YRouter/wiki/assets/img_yrouter_task.png)

执行完毕在app工程的build目录下有一个yrouter文件夹，把yrouter文件夹里的.jar文件拿出来

![](https://github.com/ysbing/YRouter/wiki/assets/img_yrouter_task.png)

这个jar文件就是我们的api了，拿着这个jar文件随意调用即可，我们尝试把它放到library1工程

![](https://github.com/ysbing/YRouter/wiki/assets/img_yrouter_dependencies.png)

在library1工程里，就可以随意整个app工程开放的方法或变量了

![](https://github.com/ysbing/YRouter/wiki/assets/img_class_call.png)

## 五、进阶
进阶的内容较为复杂，仅仅简单使用的话上面足够了
* [数据模拟](mock.md)
* [反射优化](reflection.md)
