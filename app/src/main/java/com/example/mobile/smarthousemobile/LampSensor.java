package com.example.mobile.smarthousemobile;


import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import org.json.JSONException;
import org.json.JSONObject;

public class LampSensor extends BaseSensor {
    public static boolean isOnline = false;
    public static boolean isLampOn = false;

    public static String LAMP_MODE_SWITCH_ACTION = "LampModeSwitch";

    public LampSensor(Context context) {
        super(context);
    }

    public int get_node_id() {
        return 1;
    }

    public String get_register_command() {
        return "{\"sensor\": \"nrf24l01\", \"type\": \"register\", \"msg_stream\": \"1\"}";
    }

    public String get_state_command() {
        return "{\"sensor\": \"nrf24l01\", \"type\": \"get_state\", \"node_id\": 1}";
    }

    public void handle_state(JSONObject state) throws JSONException {
        isOnline = state.getBoolean("is_online");

        int isOn = 0;
        if (!state.isNull("state")) {
            isOn=state.getJSONObject("state").getInt("power_on");
        }

        int resource;

        if (!isOnline) {
            resource = R.drawable.lamp_icon_disabled;
        }
        else if (isOn == 1) {
            resource =  R.drawable.lamp_icon_on;
        }
        else {
            resource =  R.drawable.lamp_icon_off;
        }

        RemoteViews views = new RemoteViews(
                _context.getPackageName(),
                R.layout.new_app_widget
        );
        views.setImageViewResource(R.id.imageButton, resource);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(_context);
        ComponentName thisWidget = new ComponentName(_context, NewAppWidget.class);

        views.setOnClickPendingIntent(
                R.id.imageButton,
                NewAppWidget.getPendingSelfIntent(_context, LAMP_MODE_SWITCH_ACTION)
        );

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(thisWidget, views);
        isLampOn = isOn == 1;
    }
}
