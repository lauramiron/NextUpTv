package io.github.lauramiron.nextuptv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import io.github.lauramiron.nextuptv.ui.AppEntry

class AppSource {
    fun loadApps(context: Context): List<AppEntry> {
        val pm = context.packageManager;
        val myPkg = context.packageName;

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        };

        val results: List<ResolveInfo> = pm.queryIntentActivities(intent, 0).filter {
            it.activityInfo.packageName != myPkg };

        return results.map { ri: ResolveInfo ->
            val pkg = ri.activityInfo.packageName;
            val label = ri.loadLabel(pm)?.toString().orEmpty();
            val icon = runCatching { ri.loadIcon(pm) }.getOrNull();

            val component = ComponentName(pkg, ri.activityInfo.name);
            val leanback = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
//                component = component;
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            };

            val launch = leanback.takeIf { it.resolveActivity(pm) != null }
                ?: pm.getLaunchIntentForPackage(pkg);

            AppEntry(label, pkg, icon, launch);
        }.sortedBy { it.label.lowercase() }
    }
}