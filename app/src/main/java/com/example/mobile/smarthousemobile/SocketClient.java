package com.example.mobile.smarthousemobile;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;


public class SocketClient {
    private final static int SOL_TCP = 6;
    private final static int TCP_KEEPIDLE = 4;
    private final static int TCP_KEEPINTVL = 5;
    private final static int TCP_KEEPCNT = 6;

    private Socket read_socket;
    private BufferedReader read_socket_in;
    private PrintWriter read_socket_out;
    private Thread client_thread;
    HashMap<Integer, BaseSensor> registered_sensors;

    public SocketClient() {
        registered_sensors = new HashMap<Integer, BaseSensor>();
    }

    protected void setKeepaliveSocketOptions(Socket socket, int idleTimeout, int interval, int count) throws Exception{
        try {
            socket.setKeepAlive(true);
            try {
                Field socketImplField = Class.forName("java.net.Socket").getDeclaredField("impl");
                socketImplField.setAccessible(true);
                if(socketImplField != null) {
                    Object plainSocketImpl = socketImplField.get(socket);
                    Field fileDescriptorField = Class.forName("java.net.SocketImpl").getDeclaredField("fd");
                    if(fileDescriptorField != null) {
                        fileDescriptorField.setAccessible(true);
                        FileDescriptor fileDescriptor = (FileDescriptor)fileDescriptorField.get(plainSocketImpl);
                        Class libCoreClass = Class.forName("libcore.io.Libcore");
                        Field osField = libCoreClass.getDeclaredField("os");
                        osField.setAccessible(true);
                        Object libcoreOs = osField.get(libCoreClass);
                        Method setSocketOptsMethod = Class.forName("libcore.io.ForwardingOs").getDeclaredMethod("setsockoptInt", FileDescriptor.class, int.class, int.class, int.class);
                        if(setSocketOptsMethod != null) {
                            setSocketOptsMethod.invoke(libcoreOs, fileDescriptor, SOL_TCP, TCP_KEEPIDLE, idleTimeout);
                            setSocketOptsMethod.invoke(libcoreOs, fileDescriptor, SOL_TCP, TCP_KEEPINTVL, interval);
                            setSocketOptsMethod.invoke(libcoreOs, fileDescriptor, SOL_TCP, TCP_KEEPCNT, count);
                        }
                    }
                }
            }
            catch (Exception reflectionException) {}
        } catch (SocketException e) {}
    }


    class ThreadRunner implements Runnable {
        @Override
        public void run() {
            System.out.println("SocketClient init");

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    System.out.println("Setting sockets...");
                    read_socket = new Socket("flid.ddns.net", 10101);

                    setKeepaliveSocketOptions(read_socket, 30, 10, 5);

                    read_socket_out = new PrintWriter(read_socket.getOutputStream(), true);
                    read_socket_in = new BufferedReader(
                        new InputStreamReader(read_socket.getInputStream())
                    );


                    for (BaseSensor sensor : registered_sensors.values()) {
                        read_socket_out.println(sensor.get_register_command());
                        read_socket_out.flush();
                        read_socket_out.println(sensor.get_state_command());
                        read_socket_out.flush();
                    }

                    System.out.println("Listening...");

                    while(true) {
                        String response;
                        response = read_socket_in.readLine();
                        if (response == null) break;
                        System.out.println("Response received");
                        System.out.println(response);

                        JSONObject state = new JSONObject(response);

                        int node_id = state.getInt("msg_stream");

                        if (!registered_sensors.containsKey(node_id)) {
                            System.out.println("Unexpected node_id" + Integer.toString(node_id));
                            continue;
                        }
                        registered_sensors.get(node_id).handle_state(state);
                    }
                }
                catch (InterruptedIOException e) {
                    System.out.println("Timeout");
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
                catch (InterruptedException e) {
                    break;
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            System.out.println("Server went down.");
        }
    };

    void register_sensor(BaseSensor sensor) {
        System.out.println("Registering sensor " + Integer.toString(sensor.get_node_id()) + "...");
        registered_sensors.put(sensor.get_node_id(), sensor);
    }

    public void start() {
        System.out.println("Starting the client");

        if (client_thread != null) {
            System.out.println("Already started");
            return;
        }

        client_thread = new Thread(new ThreadRunner());
        client_thread.start();
    }

    public void stop() {
        System.out.println("Stopping the client");

        client_thread.interrupt();
        client_thread = null;

        try {
            read_socket.close();
            read_socket_in.close();
            read_socket_out.close();
            read_socket = null;
        }
        catch (Exception e) {
            System.out.println("Error while closing socket");
            e.printStackTrace();
        }
    }
}
