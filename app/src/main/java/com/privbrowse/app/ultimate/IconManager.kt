package com.privbrowse.app.ultimate

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconManager {
    private val aliases=mapOf("Default" to "com.privbrowse.app.ui.LauncherDefault","Tools" to "com.privbrowse.app.ui.LauncherTools","Home" to "com.privbrowse.app.ui.LauncherHome")
    fun set(context:Context,name:String){val pm=context.packageManager;aliases.forEach{(label,cn)->pm.setComponentEnabledSetting(ComponentName(context,cn),if(label==name)PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP)}}
}
