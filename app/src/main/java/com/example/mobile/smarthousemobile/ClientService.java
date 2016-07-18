package com.example.mobile.smarthousemobile;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;


public class ClientService extends Service {
    public SocketClient socket_client;
    public static LampSensor lamp_sensor;
    public static WeatherSensor weather_sensor;

    public ClientService() {}

    @Override
    public void onCreate() {
        System.out.println("Service has been started");

        super.onCreate();

        socket_client = new SocketClient();

        Context context = getApplicationContext();
        lamp_sensor = new LampSensor(context);
        weather_sensor = new WeatherSensor(context);

        socket_client.register_sensor(lamp_sensor);
        socket_client.register_sensor(weather_sensor);

        socket_client.start();

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    socket_client.stop();

                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    socket_client.start();
                }
            }
        }, intentFilter);
    }

    @Override
    public void onDestroy() {
        System.out.println("Stopping the service...");
        socket_client.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
