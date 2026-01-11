package ibm.controller;

import java.nio.ByteBuffer;

import static java.lang.Math.abs;

public class PressureBuffer {

    PressurePoint[] points;
    final int SIZE;
    private int i = 0;
    private int back = 0 ;
    private int length = 0;

    public PressureBuffer(int SIZE) {
        this.SIZE = SIZE;
        points = new PressurePoint[SIZE];
    }

    public void append(PressurePoint point) {
        points[i] = point;
        i = ++i % SIZE;

        if (length < SIZE) {
            length++;
        } else {
            back = ++back % SIZE;
        }
    }

    public void appendAsBytes(ByteBuffer bb) {

        bb.flip();

        while(bb.remaining() > 0) {

            append(new PressurePoint(bb.getDouble(), bb.getDouble()));
        }

    }

    public PressurePoint popBack() {

        if (length == 0) {
            throw new IndexOutOfBoundsException();
        }

        var a = points[back];

        back = ++back % SIZE;
        length--;

        return a;

    }

    public PressurePoint[] getSegment(int size) {

        size = abs(size);

        if (size > length) {
            size = length;
        }

        PressurePoint[] segment = new PressurePoint[size];

        if (i >= size) {

            System.arraycopy(points, i-size, segment, 0, size);

        } else {

            System.arraycopy(points, SIZE-(size-i), segment, 0, size-i);
            System.arraycopy(points, size-i, segment, size-i, i);

        }

        return segment;

    }

    public int length() {
        return length;
    }




    public record PressurePoint(double p, double t) {};
}
