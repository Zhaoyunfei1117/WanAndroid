/*
 * Copyright 2017 JessYan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.hegj.wandroid.app

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import butterknife.ButterKnife
import com.billy.android.swipe.*
import com.billy.android.swipe.SwipeConsumer.DIRECTION_LEFT
import com.billy.android.swipe.listener.SimpleSwipeListener
import com.jess.arms.base.delegate.AppLifecycles
import com.jess.arms.integration.cache.IntelligentCache
import com.jess.arms.utils.ArmsUtils
import com.kingja.loadsir.callback.SuccessCallback
import com.kingja.loadsir.core.LoadSir
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.tencent.bugly.Bugly
import com.tencent.bugly.crashreport.CrashReport.UserStrategy
import com.tencent.mmkv.MMKV
import me.hegj.wandroid.BuildConfig
import me.hegj.wandroid.app.utils.HttpUtils
import me.hegj.wandroid.app.utils.sliding.MyActivitySlidingBackConsumer
import me.hegj.wandroid.app.weight.loadCallBack.EmptyCallback
import me.hegj.wandroid.app.weight.loadCallBack.ErrorCallback
import me.hegj.wandroid.app.weight.loadCallBack.LoadingCallback
import me.hegj.wandroid.mvp.ui.activity.MainActivity
import me.hegj.wandroid.mvp.ui.activity.start.SplashActivity


/**
 * ================================================
 * 展示 [AppLifecycles] 的用法
 * Created by JessYan on 04/09/2017 17:12
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
class AppLifecyclesImpl : AppLifecycles {

    override fun attachBaseContext(base: Context) {
        MultiDex.install(base)  //这里比 onCreate 先执行,常用于 MultiDex 初始化,插件化框架的初始化
    }

    override fun onCreate(application: Application) {
        //初始化 SmartSwipeBack
        SmartSwipeBack.activityBack(application, { activity ->
            MyActivitySlidingBackConsumer(activity)
                    .setRelativeMoveFactor(0.5f)
                    .setScrimColor(-0x80000000)
                    .setShadowColor(-0x80000000)
                    .setShadowSize(SmartSwipe.dp2px(20, application))
                    .setEdgeSize(SmartSwipe.dp2px(20, application))
                    .enableDirection(DIRECTION_LEFT)
                    .addListener(object : SimpleSwipeListener() {
                        override fun onSwipeOpened(wrapper: SmartSwipeWrapper?, consumer: SwipeConsumer?, direction: Int) {
                            activity.finish()
                            activity.overridePendingTransition(R.anim.anim_none, R.anim.anim_none)
                        }
                    })
        }, {
            it !is MainActivity || it is SplashActivity//禁止主Activity滑动返回
        })

        //初始化MMKV
        MMKV.initialize(application.filesDir.absolutePath + "/mmkv")

        if (LeakCanary.isInAnalyzerProcess(application)) {
            return
        }
        ButterKnife.setDebug(BuildConfig.LOG_DEBUG)
        ArmsUtils.obtainAppComponentFromContext(application).extras()
                .put(IntelligentCache.getKeyOfKeep(RefWatcher::class.java.name), if (BuildConfig.USE_CANARY) LeakCanary.install(application) else RefWatcher.DISABLED)

        //界面加载管理 初始化
        LoadSir.beginBuilder()
                .addCallback(LoadingCallback())//加载
                .addCallback(ErrorCallback())//错误
                .addCallback(EmptyCallback())//空
                .setDefaultCallback(SuccessCallback::class.java)//设置默认加载状态页
                .commit()

        //初始化Bugly
        val context = application.applicationContext
        // 获取当前包名
        val packageName = context.packageName
        // 获取当前进程名
        val processName = HttpUtils.getProcessName(android.os.Process.myPid())
        // 设置是否为上报进程
        val strategy = UserStrategy(context)
        strategy.isUploadProcess = processName == null || processName == packageName
        // 初始化Bugly
        Bugly.init(context, "5a5f6366fc", BuildConfig.DEBUG)
    }

    override fun onTerminate(application: Application) {

    }
}
