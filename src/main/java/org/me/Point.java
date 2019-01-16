package org.me;

import java.util.Objects;

public class Point {
    int x, y;
    Direction direction = Direction.RIGHT;

    public Point() {
    }

    public Point(Point source) {
        this(source.x, source.y, source.direction);
    }

    public Point(int x, int y, Direction direction) {
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

    public Point getCopy() {
        return new Point(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point freePoint = (Point) o;
        return x == freePoint.x &&
                y == freePoint.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Point{" +
                "x=" + x +
                ", y=" + y +
                ", direction=" + direction +
                '}';
    }
}
