package com.example.mobile.smarthousemobile;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Implementation of App Widget functionality.
 */
public class NewAppWidget extends AppWidgetProvider {
    static private Intent client_intent;
    class SendStateRunnable implements Runnable {
        boolean _state;

        public SendStateRunnable(boolean state) {
            _state = state;
        }

        @Override
        public void run() {
            System.out.println("Sending state...");

            try {
                Socket sock = new Socket("flid.ddns.net", 10101);
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);

                System.out.println("Connected, sending state request...");
                out.println("{\"sensor\": \"nrf24l01\", \"type\": \"set_state\", \"node_id\": 1, \"state\": {\"power_on\": " + (_state?"1":"0") + "}}");
                out.flush();
                sock.close();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        System.out.println("onReceive: " + intent.getAction());

        if (intent.getAction().equals(LampSensor.LAMP_MODE_SWITCH_ACTION)) {
            System.out.println("CLICK!!!");
            Thread th = new Thread(
                new SendStateRunnable(!ClientService.lamp_sensor.isLampOn)
            );
            th.start();
            return;
        }

        super.onReceive(context, intent);
    }

    public static PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, NewAppWidget.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        System.out.println("Updating...");
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        views.setOnClickPendingIntent(
            R.id.imageButton,
            getPendingSelfIntent(context, LampSensor.LAMP_MODE_SWITCH_ACTION)
        );
        ComponentName thisWidget = new ComponentName(context, NewAppWidget.class);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        System.out.println("Initializing...");
        super.onEnabled(context);

        System.out.println("Launching the service...");
        // Update the widgets via the service
        Intent service_intent = new Intent(context.getApplicationContext(), ClientService.class);
        context.startService(service_intent);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        Intent service_intent = new Intent(context.getApplicationContext(), ClientService.class);
        context.stopService(service_intent);
        super.onDisabled(context);
    }
}

