package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;


class Message implements Serializable {
    int port;
    String message;
    int sequence;
    int priority;
    int proposedPriority;
    boolean delivered;

    public Message() {

    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public int getPriority() {
        return priority;
    }

    public int getProposedPriority() { return proposedPriority;}

    public void setProposedPriority(int proposedPriority) { this.proposedPriority = proposedPriority;}

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

}