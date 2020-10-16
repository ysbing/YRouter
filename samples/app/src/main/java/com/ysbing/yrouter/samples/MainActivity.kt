package com.ysbing.yrouter.samples

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ysbing.yrouter.api.YRouterApi
import com.ysbing.yrouter.sampleslibrary2.Library2Api
import kotlinx.android.synthetic.main.activity_main.*

@YRouterApi
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        library1Activity.setOnClickListener {
            //startActivity(Intent(this, Library1Activity::class.java))
            Library2Api().voidTest("hello")
        }
        library2Activity.setOnClickListener {
            val nn = Library2Api().w222222222(55f).toString() + Library2Api().e22222222222
            Toast.makeText(this, nn, Toast.LENGTH_SHORT).show()
            Log.i("WWWWWWWW", nn)
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