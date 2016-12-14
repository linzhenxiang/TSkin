package com.skin;

import android.app.Application;

/**
 * Created by Administrator on 2016/12/14 0014.
 */
public class SkinApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SkinManager.getInstance().init(this, null);
    }
}
