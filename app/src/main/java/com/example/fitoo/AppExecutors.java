package com.example.fitoo;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {

    private static volatile AppExecutors INSTANCE;

    private final ExecutorService io;
    private final Executor main;

    private AppExecutors() {
        io = Executors.newFixedThreadPool(2);
        Handler handler = new Handler(Looper.getMainLooper());
        main = handler::post;
    }

    public static AppExecutors get() {
        if (INSTANCE == null) {
            synchronized (AppExecutors.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppExecutors();
                }
            }
        }
        return INSTANCE;
    }

    public ExecutorService io() {
        return io;
    }

    public Executor main() {
        return main;
    }
}

