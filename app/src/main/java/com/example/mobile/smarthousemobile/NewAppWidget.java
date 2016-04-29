package com.example.mobile.smarthousemobile;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
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
    static private AppWidgetManager _manager;
    static private int[] _ids;
    static private Context _context;
    static boolean isLampOnline = false;
    static boolean isLampOn = false;

    public static String YOUR_AWESOME_ACTION = "YourAwesomeAction";
    Intent _intent;



    class ThreadRunnable implements Runnable {
        @Override
        public void run() {
            System.out.println("Client init");

            Socket receiver;
            BufferedReader in;
            PrintWriter out;

            receiver = null;
            in = null;
            out = null;

            while (true) {
                receiver = null;
                //Create socket connection
                try {
                    receiver = new Socket("flid.ddns.net", 10101);
                    out = new PrintWriter(receiver.getOutputStream(), true);
                    in = new BufferedReader(
                            new InputStreamReader(receiver.getInputStream())
                    );

                    System.out.println("Connected, sending request...");

                    out.println("{\"sensor\": \"nrf24l01\", \"type\": \"register\", \"msg_stream\": \"1\"}");
                    out.flush();
                    out.println("{\"sensor\": \"nrf24l01\", \"type\": \"get_state\", \"node_id\": 1}");
                    out.flush();

                    System.out.println("Listening...");

                    while(true) {
                        String response = in.readLine();
                        if (response == null) break;
                        System.out.println("Response received");
                        System.out.println(response);

                        JSONObject obj = new JSONObject(response);

                        boolean isOnline = obj.getBoolean("is_online");

                        int isOn = 0;
                        if (!obj.isNull("state")) {
                            isOn=obj.getJSONObject("state").getInt("power_on");
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

                        for (int appWidgetId : _ids) {
                            RemoteViews views = new RemoteViews(_context.getPackageName(), R.layout.new_app_widget);
                            views.setImageViewResource(R.id.imageButton, resource);

                            // Instruct the widget manager to update the widget
                            _manager.updateAppWidget(appWidgetId, views);
                        }
                        isLampOnline = isOnline;
                        isLampOn = isOn == 1;

                    }
                }
                catch (InterruptedIOException e) {
                    System.out.println("Client has been interrupted, exitting...");
                    return;
                }
                catch (UnknownHostException e) {
                    System.out.println("Unknown host");
                }
                catch  (IOException e) {
                    System.out.println("I/O Error");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("Client disconnected, retrying...");
                try {
                    Thread.sleep(1000);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    };
    private static ThreadRunnable thread_listener;
    private static Thread client_thread = null;

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


    public void startClient() {
        System.out.println("Starting the client");

        if (client_thread != null) {
            System.out.println("Already started");
            return;
        }
        thread_listener = new ThreadRunnable();

        client_thread = new Thread(thread_listener);
        client_thread.start();
    }

    public void stopClient() {
        System.out.println("Stopping the client");
        client_thread.interrupt();
        client_thread = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);

        if (intent.getAction().equals(YOUR_AWESOME_ACTION)) {
            System.out.println("CLICK!!!");
            Thread th = new Thread(
                new SendStateRunnable(!isLampOn)
            );
            th.start();

        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        _manager = appWidgetManager;
        _ids = appWidgetIds;
        _context = context;

        System.out.println("Updating...");

        _intent = new Intent(context, NewAppWidget.class);
        _intent.setAction(YOUR_AWESOME_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, _intent, 0);
        RemoteViews views = new RemoteViews(_context.getPackageName(), R.layout.new_app_widget);
        views.setOnClickPendingIntent(R.id.imageButton, pendingIntent);
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        startClient();


    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        stopClient();
    }
}

