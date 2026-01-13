package data;

import java.nio.ByteBuffer;

import static java.lang.Math.abs;

public class ContinuousSignalBuffer {

    PressurePoint[] points;
    final int SIZE;
    private int i = 0;
    private int back = 0;
    private int length = 0;

    public ContinuousSignalBuffer(int SIZE) {
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

        while (bb.remaining() > 0) {

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

    public PressurePoint[] getSegment(int size, int delay) {

        size = abs(size);

        if (size > length) {
            size = length;
        }

        PressurePoint[] segment = new PressurePoint[size];

        int offset = (i + SIZE - delay) % SIZE;


        System.out.println(offset + ", "+ length);
        System.out.println(back + ", back");

        if (offset == 0) {

            System.arraycopy(points, 0, segment, 0, size);
            System.out.println("test3");

        } else {
            if (size > offset) {

                System.out.println(size + " size");
                System.arraycopy(points, SIZE + offset - size, segment, 0, size - offset);
                System.arraycopy(points, 0, segment, size - offset, offset);


            } else {
                System.out.println("test1");
                System.arraycopy(points, offset - size, segment, 0, size);

            }
        }

        return segment;

    }

    public PressurePoint[] getBackSegment(int size) {
        return getSegment(size, length);

    }

    public int length() {
        return length;
    }

}