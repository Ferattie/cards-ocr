package org.me;

import java.util.List;

public class Blob {

    int x, y, w, h;

    private List<Point> circuit;

    public Blob(int x, int y, int w, int h, List<Point> circuit) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;

        this.circuit = circuit;
    }

    public boolean contains(Point point) {
        return point.x >= x && point.x <= x + w &&
                point.y >= y && point.y <= y + h;
    }

    public List<Point> getCircuit() {
        return circuit;
    }

    public int getWidth() {
        return circuit.stream().mapToInt(point -> point.x).max().getAsInt() - x;
    }

    public int getHeight() {
        return circuit.stream().mapToInt(point -> point.y).max().getAsInt() - y;
    }
}
