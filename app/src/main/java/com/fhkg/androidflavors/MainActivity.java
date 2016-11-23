package com.fhkg.androidflavors;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class MainActivity extends Activity {
    private static final String CREATE_SHORTCUT_ACTION = "com.android.launcher.action.INSTALL_SHORTCUT";

    private static final String DROP_SHORTCUT_ACTION = "com.android.launcher.action.UNINSTALL_SHORTCUT";

    private static final String PREFERENCE_KEY_SHORTCUT_EXISTS = "IsShortCutExists";
    // 获取默认的SharedPreferences
    SharedPreferences sharedPreferences ;

    // 从SharedPreferences获取是否存在快捷方式 若不存在返回false 程序第一次进来肯定返回false
    boolean exists ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        exists = sharedPreferences.getBoolean(PREFERENCE_KEY_SHORTCUT_EXISTS, false);
        //创建桌面快捷方式
        //若第一次启动则创建,下次启动则不创建
        if (!exists) {
            setUpShortCut();
        }
    }

    /**
     * 创建桌面快捷方式
     */
    private void setUpShortCut() {

        Intent intent = new Intent(CREATE_SHORTCUT_ACTION);

        // 设置快捷方式图片
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_bank_card));

        // 设置快捷方式名称
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "xinran");

        // 设置是否允许重复创建快捷方式 false表示不允许
        intent.putExtra("duplicate", false);



        // 设置快捷方式要打开的intent

        // 第一种方法创建快捷方式要打开的目标intent
        Intent targetIntent = new Intent();
        // 设置应用程序卸载时同时也删除桌面快捷方式
        targetIntent.setAction(Intent.ACTION_MAIN);
        targetIntent.addCategory("android.intent.category.LAUNCHER");

        ComponentName componentName = new ComponentName(getPackageName(), this.getClass().getName());
        targetIntent.setComponent(componentName);


        // 第二种方法创建快捷方式要打开的目标intent
        /*
         * Intent
         * targetIntent=getPackageManager().getLaunchIntentForPackage(getPackageName
         * ());
         */
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, targetIntent);

        // 发送广播
        sendBroadcast(intent);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREFERENCE_KEY_SHORTCUT_EXISTS, true);
        editor.commit();

    }

    /**
     * 删除桌面快捷方式
     */
    private void tearDownShortCut() {

        Intent intent = new Intent(DROP_SHORTCUT_ACTION);
        // 指定要删除的shortcut名称
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "sina");

        String appClass = getPackageName() + "." + this.getLocalClassName();

        ComponentName component = new ComponentName(getPackageName(), appClass);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,new Intent().setAction(Intent.ACTION_MAIN).setComponent(component));
        sendBroadcast(intent);

    }
}
