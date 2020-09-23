package com.ysbing.yrouter.sampleslibrary2

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ysbing.yrouter.samples.InterfaceTest
import com.ysbing.yrouter.samples.JavaTest
import com.ysbing.yrouter.samples.KotlinObjectTest
import com.ysbing.yrouter.samples.MainActivity

class Library2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(
            this,
            MainActivity.sum(JavaTest.num1, KotlinObjectTest.f1).toString(),
            Toast.LENGTH_SHORT
        ).show()
    }
}