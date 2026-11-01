package vn.linkmap

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class LinkMapWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        update(context,(context.applicationContext as LinkMapApplication).store.bookmarks.value.size)
    }
    companion object {
        fun update(context: Context, count: Int) {
            val manager=AppWidgetManager.getInstance(context)
            val ids=manager.getAppWidgetIds(ComponentName(context,LinkMapWidget::class.java))
            if(ids.isEmpty()) return
            val views=RemoteViews(context.packageName,R.layout.linkmap_widget)
            views.setTextViewText(R.id.widget_title,"LinkMap · $count mục")
            fun action(name: String, code:Int)=PendingIntent.getActivity(context,code,Intent(context,MainActivity::class.java).setAction(name),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_add,action("vn.linkmap.ADD",1))
            views.setOnClickPendingIntent(R.id.widget_search,action("vn.linkmap.SEARCH",2))
            manager.updateAppWidget(ids,views)
        }
    }
}
