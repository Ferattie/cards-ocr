package org.me;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class BeetleParser {
    private Point cp = new Point(0, 0, Direction.LEFT);

    private ArrayList<Blob> blobs = new ArrayList<>();

    private Point startPoint = new Point();

    private int minX = 0, minY = 0, maxX = 0, maxY = 0;
    private boolean detected = false;

    private BufferedImage image;

    private Stack<Point> uncheckedPoints = new Stack<>();
    private List<Point> circuit = new ArrayList<>();
    private boolean invert = false;

    public void parse(BufferedImage image) {
        parse(image, false, false);
    }

    public void parse(BufferedImage image, boolean invert) {
        parse(image, false, invert);
    }

    public void parse(BufferedImage image, boolean stepByStep, boolean invert) {
        init(image, invert);

        // Движемся от левого края вправо пока не встретим белый пиксель
        cp = new Point(-1, /*START_LINE*/0, Direction.RIGHT);

        if (!stepByStep) {
            while (hasNext()) {
                next();
            }
        }
    }

    public boolean hasNext() {
        return cp.x <= image.getWidth() + 1 && cp.y <= image.getHeight() + 1;
    }

    public void next() {
        ImageWrapper image = new ImageWrapper(this.image, invert);
        int objColor = invert ? 0xff : 0;
        if (detected) {
            int p = image.getRGB(cp.x, cp.y);
            uncheckedPoints.remove(cp.getCopy());

            if ((p & 0xff) != objColor) {
                moveRight();
            } else {
                moveLeft();
            }

            circuit.add(cp.getCopy());
            uncheckedPoints.remove(cp.getCopy());

            updatePoints(cp.x, cp.y, image);

            maxX = Math.max(maxX, cp.x);
            maxY = Math.max(maxY, cp.y);

            p = image.getRGB(cp);
            if ((p & 0xff) == objColor) {
                minX = Math.min(minX, cp.x);
                minY = Math.min(minY, cp.y);
            }

            if (cp.equals(startPoint)) {
                if (!uncheckedPoints.isEmpty()) {
                    Point newStartPoint = uncheckedPoints.pop();

                    cp.x = startPoint.x = newStartPoint.x;
                    cp.y = startPoint.y = newStartPoint.y;
                    cp.direction = newStartPoint.direction;
                } else {
                    blobs.add(new Blob(minX, minY, maxX - minX, maxY - minY, circuit));
                    startPoint = new Point();
                    circuit = new ArrayList<>();

                    // Сканирование начинаем с правого верхнего угла найденной блоба
                    cp.x = maxX;
                    cp.y = minY;
                    detected = false;
                    cp.direction = Direction.RIGHT;
                }
            }
        } else {
            // Если дошли до края изображения, переносим курсор на следующую строку
            if (cp.x >= this.image.getWidth() - 1) {
                cp.y++;
                cp.x = -1;
            } else {
                int p1 = (image.getRGB(cp.x, cp.y) & 0xff);
                doMove();
                Blob blob = blobContains(cp);
                if (blob == null) {
                    detected = (image.getRGB(cp.x, cp.y) & 0xff) != p1;
                    if (detected) {
                        circuit.add(cp.getCopy());
                    }

                    maxX = minX = cp.x;
                    maxY = minY = cp.y;
                } else {
                    cp.x += blob.getWidth();
                }
            }
            startPoint = new Point(cp);
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    public Point getCurrentPoint() {
        return cp;
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public List<Blob> getBlobs() {
        return blobs;
    }

    public Stack<Point> getUncheckedPoints() {
        return uncheckedPoints;
    }

    public List<Point> getCircuit() {
        return circuit;
    }

    private void init(BufferedImage image, boolean invert) {
        this.image = image;
        this.blobs = new ArrayList<>();
        this.detected = false;
        this.uncheckedPoints = new Stack<>();
        this.circuit = new ArrayList<>();
        this.invert = invert;
    }

    private Blob blobContains(Point point) {
        for (Blob blob : getBlobs()) {
            if (blob.contains(point)) {
                return blob;
            }
        }
        return null;
    }

    private void moveLeft() {
        switch (cp.direction) {
            case UP: cp.direction = Direction.LEFT; break;
            case DOWN: cp.direction = Direction.RIGHT; break;
            case LEFT: cp.direction = Direction.DOWN; break;
            case RIGHT: cp.direction = Direction.UP; break;
        }
        doMove();
    }

    private void moveRight() {
        switch (cp.direction) {
            case UP: cp.direction = Direction.RIGHT; break;
            case DOWN: cp.direction = Direction.LEFT; break;
            case LEFT: cp.direction = Direction.UP; break;
            case RIGHT: cp.direction = Direction.DOWN; break;
        }
        doMove();
    }

    private void doMove() {
        switch (cp.direction) {
            case UP: cp.y--; break;
            case RIGHT: cp.x++; break;
            case DOWN: cp.y++; break;
            case LEFT: cp.x--; break;
        }
    }

    private void updatePoints(int x, int y, ImageWrapper img) {
        int objColor = invert ? 0xff : 0;
        Point topLeft = new Point(x - 1, y - 1, Direction.LEFT);
        Point bottomLeft = new Point(x - 1, y + 1, Direction.DOWN);
        Point topRight = new Point(x + 1, y - 1, Direction.UP);
        Point bottomRight = new Point(x + 1, y + 1, Direction.RIGHT);

        Point[] cornerPoints = new Point[] {
                topLeft, bottomLeft, bottomRight, topRight
        };

        for (Point corner : cornerPoints) {
            if ((img.getRGB(corner) & 0xff) == objColor && !circuit.contains(corner) && !uncheckedPoints.contains(corner)) {
                if (!isCircuitPoint(corner, img)) {
                    uncheckedPoints.push(topLeft);
                }
            }
        }

    }

    private boolean isCircuitPoint(Point point, ImageWrapper img) {
        return isCircuitPoint(point.x, point.y, img);
    }

    private boolean isCircuitPoint(int x, int y, ImageWrapper img) {
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if (img.getRGB(i, j) == 0) return false;
            }
        }
        return true;
    }
}
