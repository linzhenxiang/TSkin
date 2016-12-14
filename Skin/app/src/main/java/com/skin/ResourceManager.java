package com.skin;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;

import com.skin.attr.SkinParam;
import com.skin.utils.SkinLog;
import com.skin.vector.VectorDrawableCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;


/**
 * Created by zhy on 15/9/22.
 */
public class ResourceManager {
    private static final String TYPE_DRAWABLE = "drawable";
    private static final String TYPE_COLOR = "color";

    private Resources mResources;
    //插件包名
    private String mPluginPackageName;
    //
    private String mSuffix;
    //皮肤颜色值，色值切换
    private int mSuffixColor = Color.TRANSPARENT;
    private SkinParam skinParam;
    private boolean isUserPlugin = false;


    public ResourceManager(Resources res, String pluginPackageName, int color, SkinParam param, boolean usePlugin) {
        SkinLog.e("init_________ResourceManager");
        isUserPlugin = usePlugin;
        mResources = res;
        mPluginPackageName = pluginPackageName;
        skinParam = param;
        if (color != Color.TRANSPARENT) {
            mSuffixColor = color;
        }
    }

    /**
     * @author:Lzx
     * @date:2016/12/14 0014
     * @time:下午 4:48
     * @todo: 是否 使用插件包换肤
     */
    public boolean isUserPlugin() {
        return isUserPlugin;
    }

    /**
     * @author:Lzx
     * @date:2016/12/14 0014
     * @time:下午 3:53
     * @todo: 根据xml 名称获取资源图片
     */
    public Drawable getDrawableByName(String name) {
        try {
            if (TextUtils.isEmpty(name))
                return null;
            SkinLog.e("name = " + name + " , " + mPluginPackageName);
            Drawable drawable = mResources.getDrawable(mResources.getIdentifier(name, TYPE_DRAWABLE, mPluginPackageName));
            //对颜色背景着色
            if (mSuffixColor != Color.TRANSPARENT && drawable != null && drawable instanceof GradientDrawable) {
                ((GradientDrawable) drawable).setColors(new int[]{mSuffixColor, Color.TRANSPARENT});
            }
            return drawable;
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    /**
     * Switch
     * * @param name
     *
     * @return 获取switch 开关的背景图片
     */
    public Drawable[] getSwitchDrawableByName(String name) {
        Drawable[] drawables = new Drawable[2];
        if (!name.contains("?") || mSuffixColor == Color.TRANSPARENT || skinParam == null)
            return null;

        String[] names = name.split("\\?");
        LayerDrawable[] layerDrawables = new LayerDrawable[names.length];
        for (int i = 0; i < names.length; i++) {
            XmlPullParser xmlPullParser = mResources.getXml(mResources.getIdentifier(names[i], TYPE_DRAWABLE, mPluginPackageName));
            try {
                LayerDrawable layerDrawable = (LayerDrawable) LayerDrawable.createFromXml(mResources, xmlPullParser);
                if (i % 2 == 1) {//checked
                    Drawable dwCheck = layerDrawable.findDrawableByLayerId(i == 1 ? skinParam.mSwitchThumb : skinParam.mSwitchTrack);
                    if (dwCheck instanceof GradientDrawable) {
                        if (i == 3) {
                            ((GradientDrawable) dwCheck).setColor(Color.argb(120, Color.red(mSuffixColor), Color.green(mSuffixColor), Color.blue(mSuffixColor)));
                        } else {
                            ((GradientDrawable) dwCheck).setColor(mSuffixColor);
                        }
                        layerDrawables[i] = layerDrawable;
                    }
                } else {
                    layerDrawables[i] = layerDrawable;
                }
            } catch (XmlPullParserException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        for (int i = 0; i < drawables.length; i++) {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_checked}, layerDrawables[i * 2 + 1]);
            stateListDrawable.addState(new int[]{}, layerDrawables[i * 2]);

            drawables[i] = stateListDrawable;
        }
        return drawables;
    }


    /**
     * @author:Lzx
     * @date:2016/12/14 0014
     * @time:下午 4:13
     * @todo: 获取 vector 图片
     */
    public Drawable getDrawableVectorXmlByName(String name) throws Resources.NotFoundException {
        try {
            int resId = mResources.getIdentifier(name, TYPE_DRAWABLE, mPluginPackageName);
            VectorDrawableCompat drawableCompat = VectorDrawableCompat.create(mResources, resId, Resources.getSystem().newTheme());
            if (drawableCompat == null || mSuffixColor == Color.TRANSPARENT)
                return null;
            Drawable.ConstantState constantState = drawableCompat.getConstantState();
            if (constantState instanceof VectorDrawableCompat.VectorDrawableCompatState) {
                VectorDrawableCompat.VPathRenderer vPathRenderer = ((VectorDrawableCompat.VectorDrawableCompatState) constantState).getVPathRenderer();
                vPathRenderer.setStorkColor(mSuffixColor);
            }
            return drawableCompat.getCurrent();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            return null;
        }

    }

    public int getColor(String name) throws Resources.NotFoundException {
        if (TextUtils.isEmpty(name))
            return -1;
        SkinLog.e("name = " + name);
        return isUserPlugin() ? mResources.getColor(mResources.getIdentifier(name, TYPE_COLOR, mPluginPackageName)) : mSuffixColor;
    }


    /**
     * @author:Lzx
     * @date:2016/12/14 0014
     * @time:下午 4:58
     * @todo: TextView color seletor
     */
    public ColorStateList getColorStateList(String name) {
        try {
            if (TextUtils.isEmpty(name))
                return null;
            SkinLog.e("name = " + name);
            ColorStateList colorStateList = mResources.getColorStateList(mResources.getIdentifier(name, TYPE_COLOR, mPluginPackageName));

            if (!isUserPlugin &&mSuffixColor!=Color.TRANSPARENT) {

                int color = colorStateList.getColorForState(new int[]{},Color.BLACK);
                colorStateList = new ColorStateList(new int[][]{{android.R.attr.state_pressed},{}}, new int[]{mSuffixColor,color});
            }
            return colorStateList;

        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            return null;
        }

    }


}
