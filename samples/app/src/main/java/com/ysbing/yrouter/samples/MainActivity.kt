package com.ysbing.yrouter.samples

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ysbing.yrouter.api.YRouterApi
import com.ysbing.yrouter.sampleslibrary1.Library1Activity
import com.ysbing.yrouter.sampleslibrary2.Library2Activity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        library1Activity.setOnClickListener {
            startActivity(Intent(this, Library1Activity::class.java))
        }
        library2Activity.setOnClickListener {
            startActivity(Intent(this, Library2Activity::class.java))
        }
    }

    companion object {
        @YRouterApi
        fun sum(num1: Int, num2: Int): Int {
            return num1 + num2
        }
    }

    class MainActivityInnerClass {
        @YRouterApi
        var lala: Float = 3.14f

        @YRouterApi
        fun lolo(d: Double) {
        }
    }
}