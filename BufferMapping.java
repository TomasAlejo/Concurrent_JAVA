package main;


public final class BufferMapping {

    public enum Operation { ADD, REMOVE }


    private final String bufferName;
    private final Operation op;

    public BufferMapping(String bufferName, Operation op) {
        this.bufferName = bufferName;
        this.op = op;
    }

    public String getBufferName() { return bufferName; }
    public Operation getOperation() { return op; }
}