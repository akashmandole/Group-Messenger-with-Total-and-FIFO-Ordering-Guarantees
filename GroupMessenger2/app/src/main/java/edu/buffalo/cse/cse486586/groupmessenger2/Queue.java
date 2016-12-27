package edu.buffalo.cse.cse486586.groupmessenger2;

import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

public class Queue<E> extends LinkedBlockingQueue {
    @Override
    public Object take() throws InterruptedException {

        Object obj = peek();

        if (obj instanceof Message) {
            Log.v("Msg removed from Q : ",((Message) obj).getMessage());
        }
        return super.take();
    }
}
