package com.example.mobile.smarthousemobile;


import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.StringBuilderPrinter;
import android.widget.RemoteViews;

import org.json.JSONException;
import org.json.JSONObject;


public class WeatherSensor extends BaseSensor {
    public static boolean isOnline = false;
    public static int temperature = -273;
    public static int humidity = -1;

    public static String LAMP_MODE_SWITCH_ACTION = "LampModeSwitch";

    public WeatherSensor(Context context) {
        super(context);
    }

    public int get_node_id() {
        return 2;
    }

    public void handle_state(JSONObject state) throws JSONException {
        isOnline = state.getBoolean("is_online");

        if (!state.isNull("state")) {
            JSONObject state_data = state.getJSONObject("state");
            temperature=state_data.getInt("temperature");
            humidity=state_data.getInt("humidity");
        }

        RemoteViews views = new RemoteViews(
                _context.getPackageName(),
                R.layout.new_app_widget
        );
        views.setTextViewText(
                R.id.temperatureLabel,
                String.format("%d C", temperature)
        );
        views.setTextViewText(
                R.id.humidityLabel,
                String.format("%d%%", humidity)
        );

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(_context);
        ComponentName thisWidget = new ComponentName(_context, NewAppWidget.class);

        views.setOnClickPendingIntent(
                R.id.imageButton,
                NewAppWidget.getPendingSelfIntent(_context, LAMP_MODE_SWITCH_ACTION)
        );

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(thisWidget, views);

    }
}
