package com.example.mobile.smarthousemobile;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;


public abstract class BaseSensor {

    public abstract void handle_state(JSONObject state) throws JSONException;
    public abstract int get_node_id();

    protected Context _context;

    public BaseSensor(Context context) {
        _context = context;
    }

    public String get_register_command() {
        return String.format(
            "{\"sensor\": \"nrf24l01\", \"type\": \"register\", \"msg_stream\": \"%d\"}",
            get_node_id()
        );
    }

    public String get_state_command() {
        return String.format(
            "{\"sensor\": \"nrf24l01\", \"type\": \"get_state\", \"node_id\": %d}",
            get_node_id()
        );
    }
}
