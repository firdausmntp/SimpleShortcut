package com.josski.simpleshortcut.widget

import android.content.Intent
import android.widget.RemoteViewsService

class ShortcutWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ShortcutRemoteViewsFactory(applicationContext)
    }
}