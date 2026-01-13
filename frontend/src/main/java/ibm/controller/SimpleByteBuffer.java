package ibm.controller;

public class SimpleByteBuffer {

    byte[] bytes;
    int head = 0;
    int size;

    public SimpleByteBuffer(int size) {
        this.size = size;
        bytes = new byte[size];
    }

    public void append(byte[] segment) {
        int l = segment.length;

        if (l + head > size) {
          //  System.out.println("head sum: "+ (l+head));
            throw new IndexOutOfBoundsException();
        }

        System.arraycopy(segment, 0, bytes, head, 3);
        head+=l;

    }

    public byte[] concat() { //this also removes the int tag at the front
        byte[] ret = new byte[head-4];

        System.arraycopy(bytes, 4, ret, 0, head-4);

        return ret;

    }

    public void reset() {
        bytes = new byte[size];
    }
}
