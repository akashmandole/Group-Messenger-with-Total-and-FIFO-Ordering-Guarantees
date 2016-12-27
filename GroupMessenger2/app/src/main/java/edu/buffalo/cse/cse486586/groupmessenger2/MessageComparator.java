package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

public class MessageComparator implements Comparator<Message> {
    @Override
    public int compare(Message message1, Message message2)
    {
        if(message1.priority > message2.priority) {
            return 1;
        } else if (message1.priority < message2.priority) {
            return -1;
        } else {
            if(message1.port > message2.port) {
                return  1;
            } else {
                return -1;
            }
        }
    }
}