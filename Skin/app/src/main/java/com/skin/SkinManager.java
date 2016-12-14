package com.skin;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;


import com.skin.attr.SkinAttrSupport;
import com.skin.attr.SkinParam;
import com.skin.attr.SkinView;
import com.skin.callback.ISkinChangingCallback;
import com.skin.utils.SkinLog;
import com.skin.utils.PrefUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhy on 15/9/22.
 */
public class SkinManager {
    private Context mContext;
    private Resources mResources;
    private ResourceManager mResourceManager;
    private PrefUtils mPrefUtils;

    private boolean usePlugin;

    private String mSuffix = "";
    private int mSuffixColor = Color.TRANSPARENT;
    private String mCurPluginPath;
    private String mCurPluginPkg;
    private SkinParam skinParam;


    private List<Activity> mActivities = new ArrayList<Activity>();

    private SkinManager() {
    }

    public static SkinManager getInstance() {
        return SingletonHolder.sInstance;
    }

    public Resources getResources() {
        return mContext.getResources();
    }

    public void init(Context context, SkinParam param) {
        mContext = context.getApplicationContext();
        mPrefUtils = new PrefUtils(mContext);

        String skinPluginPath = mPrefUtils.getPluginPath();
        String skinPluginPkg = mPrefUtils.getPluginPkgName();
        mSuffix = mPrefUtils.getSuffix();
        mSuffixColor = mPrefUtils.getSuffixColor();
        skinParam = param;
        if (!validPluginParams(skinPluginPath, skinPluginPkg))
            return;
        try {
            loadPlugin(skinPluginPath, skinPluginPkg, mSuffixColor);
            mCurPluginPath = skinPluginPath;
            mCurPluginPkg = skinPluginPkg;
        } catch (Exception e) {
            mPrefUtils.clear();
            e.printStackTrace();
        }
    }

    private PackageInfo getPackageInfo(String skinPluginPath) {
        PackageManager pm = mContext.getPackageManager();
        return pm.getPackageArchiveInfo(skinPluginPath, PackageManager.GET_ACTIVITIES);
    }

    private void loadPlugin(String skinPath, String skinPkgName, int color) throws Exception {
        AssetManager assetManager = AssetManager.class.newInstance();
        Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
        addAssetPath.invoke(assetManager, skinPath);

        Resources superRes = mContext.getResources();
        mResources = new Resources(assetManager, superRes.getDisplayMetrics(), superRes.getConfiguration());
        mResourceManager = new ResourceManager(mResources, skinPkgName, color, null, usePlugin);
        usePlugin = true;
    }

    private boolean validPluginParams(String skinPath, String skinPkgName) {
        if (TextUtils.isEmpty(skinPath) || TextUtils.isEmpty(skinPkgName)) {
            return false;
        }

        File file = new File(skinPath);
        if (!file.exists())
            return false;

        PackageInfo info = getPackageInfo(skinPath);
        if (!info.packageName.equals(skinPkgName))
            return false;
        return true;
    }

    private void checkPluginParamsThrow(String skinPath, String skinPkgName) {
        if (!validPluginParams(skinPath, skinPkgName)) {
            throw new IllegalArgumentException("skinPluginPath or skinPkgName not valid ! ");
        }
    }

    public void removeAnySkin() {
        SkinLog.e("removeAnySkin");
        clearPluginInfo();
        notifyChangedListeners();
    }

    public boolean needChangeSkin() {
        return usePlugin || !TextUtils.isEmpty(mSuffix);
    }

    public ResourceManager getResourceManager() {
        if (!usePlugin) {
            mResourceManager = new ResourceManager(mContext.getResources(), mContext.getPackageName(), mSuffixColor, skinParam,usePlugin);
        }
        return mResourceManager;
    }


    /**
    * @author:Lzx
    * @date:2016/12/14 0014
    * @time:下午 4:38
    * @todo: 纯色换肤
    */
    public void changeSkinColor(int color) {
        clearPluginInfo();//clear before
        mSuffixColor = color;
        mPrefUtils.putPluginSuffixColor(color);
        notifyChangedListeners();
    }

    /**
    * @author:Lzx
    * @date:2016/12/14 0014
    * @time:下午 4:38
    * @todo: 清除插件信息
    */
    private void clearPluginInfo() {
        mCurPluginPath = null;
        mCurPluginPkg = null;
        usePlugin = false;
        mSuffix = null;
        mSuffixColor = Color.TRANSPARENT;
        mPrefUtils.clear();
    }


    /**
    * @author:Lzx
    * @date:2016/12/14 0014
    * @time:下午 4:39
    * @todo: 更新插件信息
    */
    private void updatePluginInfo(String skinPluginPath, String pkgName, int color) {
        mPrefUtils.putPluginPath(skinPluginPath);
        mPrefUtils.putPluginPkg(pkgName);
        mPrefUtils.putPluginSuffixColor(color);
        mCurPluginPkg = pkgName;
        mCurPluginPath = skinPluginPath;
        mSuffixColor = color;
    }

     /**
     * @author:Lzx
     * @date:2016/12/14 0014
     * @time:下午 4:38
     * @todo: 插件换肤
     */
    public void changeSkin(final String skinPluginPath, final String skinPluginPkg, ISkinChangingCallback callback) {
        changeSkin(skinPluginPath, skinPluginPkg, Color.TRANSPARENT, callback);
    }

    /**
     * 根据suffix选择插件内某套皮肤，默认为""
     *
     * @param skinPluginPath
     * @param skinPluginPkg
     * @param suffixColor
     * @param callback
     */
    public void changeSkin(final String skinPluginPath, final String skinPluginPkg, final int suffixColor, ISkinChangingCallback callback) {
        SkinLog.e("changeSkin = " + skinPluginPath + " , " + skinPluginPkg);
        if (callback == null)
            callback = ISkinChangingCallback.DEFAULT_SKIN_CHANGING_CALLBACK;
        final ISkinChangingCallback skinChangingCallback = callback;

        skinChangingCallback.onStart();

        try {
            checkPluginParamsThrow(skinPluginPath, skinPluginPkg);
        } catch (IllegalArgumentException e) {
            skinChangingCallback.onError(new RuntimeException("checkPlugin occur error"));
            return;
        }

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    loadPlugin(skinPluginPath, skinPluginPkg, suffixColor);
                    return 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    return 0;
                }

            }

            @Override
            protected void onPostExecute(Integer res) {
                if (res == 0) {
                    skinChangingCallback.onError(new RuntimeException("loadPlugin occur error"));
                    return;
                }
                try {
                    updatePluginInfo(skinPluginPath, skinPluginPkg, suffixColor);
                    notifyChangedListeners();
                    skinChangingCallback.onComplete();
                } catch (Exception e) {
                    e.printStackTrace();
                    skinChangingCallback.onError(e);
                }

            }
        }.execute();
    }

    public void apply(Activity activity) {
        List<SkinView> skinViews = SkinAttrSupport.getSkinViews(activity);
        if (skinViews == null) return;
        for (SkinView skinView : skinViews) {
            skinView.apply();
        }
    }

    public void register(final Activity activity) {
        mActivities.add(activity);
        activity.findViewById(android.R.id.content).post(new Runnable() {
            @Override
            public void run() {
                apply(activity);

            }
        });

    }

    public void unregister(Activity activity) {
        mActivities.remove(activity);
    }

    public void notifyChangedListeners() {

        for (Activity activity : mActivities) {
            apply(activity);
        }
    }

    /**
     * apply for dynamic construct view
     *
     * @param view
     */
    public void injectSkin(View view) {
        List<SkinView> skinViews = new ArrayList<SkinView>();
        SkinAttrSupport.addSkinViews(view, skinViews);
        for (SkinView skinView : skinViews) {
            skinView.apply();
        }
    }

    private static class SingletonHolder {
        static SkinManager sInstance = new SkinManager();
    }


}
