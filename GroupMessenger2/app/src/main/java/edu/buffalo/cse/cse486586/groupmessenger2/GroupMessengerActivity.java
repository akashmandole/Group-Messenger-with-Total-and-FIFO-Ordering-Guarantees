package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.PriorityQueue;

public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";

    static final int TIMEOUT = 2000;
    static final int SERVER_PORT = 10000;

    static final String url = "content://edu.buffalo.cse.cse486586.groupmessenger2.provider";
    static final Uri p_uri = Uri.parse(url);

    int sequence = 0;
    int deliveredSequence = 0;

    String myPort;

    Queue<Message> messagesToSend;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        myPort = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask(myPort).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        messagesToSend = new Queue <Message>();

       /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText1 = (EditText) findViewById(R.id.editText1);

        Button sendButton = (Button)findViewById(R.id.button4);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Message newMessage = new Message();
                newMessage.setPort(Integer.parseInt(myPort));
                newMessage.setMessage(editText1.getText().toString());
                newMessage.setSequence(sequence);
                sequence++;
                newMessage.setPriority(-1);
                newMessage.setDelivered(false);
                try {
                    messagesToSend.put(newMessage);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                editText1.setText("");
            }
        });
        ClientTask client_task = new ClientTask(messagesToSend);
        client_task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        int port;

        public ServerTask(String port) {
            this.port = Integer.parseInt(port);
        }

        @Override
        protected Void doInBackground(ServerSocket... params) {
            int proposedPriority = 0;
            PriorityQueue<Message> receivedMessages = new PriorityQueue<Message>(50, new MessageComparator());
            ServerSocket server_socket = params[0];


            while(true) {
                try {
                    Socket serverListener = server_socket.accept();
                    serverListener.setSoTimeout(TIMEOUT);

                    Message messageReceived = readMessage(serverListener);

                    if(messageReceived.getMessage() == "ApplicationCrashed") {
                        serverListener.close();
                        continue;
                    }

                    if(messageReceived.delivered) {

                        int receivedPriority = messageReceived.priority;
                        proposedPriority = receivedPriority >= proposedPriority ? receivedPriority + 1 : proposedPriority;

                        Message messageInQueue = null;
                        boolean messageFoundInQueue = false;

                        for (Iterator <Message> iterator = receivedMessages.iterator(); iterator.hasNext();){
                            Message message = iterator.next();

                            int seq = message.getSequence();
                            int port = message.getPort();
                            int rSeq = messageReceived.getSequence();
                            int rPort = messageReceived.getPort();

                            boolean seqMatch = seq == rSeq ? true : false;
                            boolean portMatch = port == rPort ? true : false;

                            if (seqMatch && portMatch) {
                                messageInQueue = message;
                                messageFoundInQueue = true;
                                break;
                            }

                        }

                        if(messageFoundInQueue) {
                            receivedMessages.remove(messageInQueue);
                            receivedMessages.add(messageReceived);
                        }

                        while((receivedMessages.size() > 0)) {
                            Message message = receivedMessages.peek();
                            if(message.delivered) {

                                ContentValues keyValueToInsert = new ContentValues();

                                keyValueToInsert.put("key", Integer.toString(deliveredSequence++));
                                keyValueToInsert.put("value", message.message);

                                getContentResolver().insert(p_uri, keyValueToInsert);

                                publishProgress(message.message);
                                Log.v("message recieved : ", message.message);

                                receivedMessages.poll();
                            } else {
                                try {

                                    Message applicationCrashedMessage = new Message();
                                    applicationCrashedMessage.setPort(54432);
                                    applicationCrashedMessage.setMessage("ApplicationCrashed");
                                    applicationCrashedMessage.setPriority(1);
                                    applicationCrashedMessage.setSequence(1);
                                    applicationCrashedMessage.setDelivered(false);

                                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (message.port*2));
                                    socket.setSoTimeout(TIMEOUT);

                                    writeMessage(socket,applicationCrashedMessage);

                                    socket.close();
                                    break;
                                } catch (Exception e) {
                                    receivedMessages.poll();
                                }
                            }
                        }
                    } else {

                        messageReceived.setPriority(proposedPriority);
                        receivedMessages.add(messageReceived);
                        proposedPriority++;

                        writeMessage(serverListener, messageReceived);

                    }

                    serverListener.close();

                }catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();

                }
            }

        }

        private Message readMessage (Socket serverListener) throws IOException, ClassNotFoundException{
            ObjectInputStream oin = new ObjectInputStream(serverListener.getInputStream());
            Message messageReceived = (Message)oin.readObject();
            return messageReceived;
        }

        private void writeMessage(Socket serverListener, Message messageReceived) throws IOException {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(serverListener.getOutputStream());
            objectOutputStream.writeObject(messageReceived);
            objectOutputStream.close();
        }

        @Override
        protected void onProgressUpdate(String... msg) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String message = msg[0].trim();
            TextView textView1 = (TextView) findViewById(R.id.textView1);
            textView1.append(message + " " + (deliveredSequence -1) + "\t\n");

            Log.v("message recieved : ", message);
        }
    }

    private class ClientTask extends AsyncTask<String, String, Void> {

        private Queue <Message> messagesToSend;
        private int THREAD_SLEEP = 300;

        public ClientTask(Queue <Message> messagesToSend) {
            this.messagesToSend = messagesToSend;
        }

        @Override
        protected Void doInBackground(String... params)
        {
            while(true) {

                if(GroupMessengerActivity.this.messagesToSend.size() > 0) {
                    Message newMessage = null;

                    try {
                        newMessage = (Message) GroupMessengerActivity.this.messagesToSend.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    sendMessagesToAllAVD(newMessage,true);

                    newMessage.delivered = true;

                    sendMessagesToAllAVD(newMessage,false);
                }
                else {
                    try {
                        Thread.sleep(THREAD_SLEEP);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }

        @Override
        protected void onProgressUpdate(String... msg) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = msg[0].trim();
            TextView textView1 = (TextView) findViewById(R.id.textView1);
            textView1.append(strReceived + "\t\n");
            Log.v("message recieved : ", strReceived);
        }

        private void sendMessagesToAllAVD(Message msgToSend, boolean receive) {

            // Sending message to AVD 0
            sendMessageToAVD(msgToSend,REMOTE_PORT0, receive);

            // Sending message to AVD 1
            sendMessageToAVD(msgToSend,REMOTE_PORT1, receive);

            // Sending message to AVD 2
            sendMessageToAVD(msgToSend,REMOTE_PORT2, receive);

            // Sending message to AVD 3
            sendMessageToAVD(msgToSend,REMOTE_PORT3, receive);

            // Sending message to AVD 4
            sendMessageToAVD(msgToSend,REMOTE_PORT4, receive);

        }

        private void sendMessageToAVD(Message msgToSend, String port, boolean receive) {
            try
            {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port));
                socket.setSoTimeout(TIMEOUT);

                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectOutputStream.writeObject(msgToSend);
                objectOutputStream.flush();

                if (receive == true) {

                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                    Message received_msg = (Message)objectInputStream.readObject();

                    if(received_msg.getPriority() > msgToSend.getPriority()) {
                        msgToSend.setPriority(received_msg.getPriority());
                    }
                }

                objectOutputStream.close();
                socket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            } catch (Exception e) {
                Log.e(TAG, "The following AVD was crashed : " + port);
            }
        }

    }
}