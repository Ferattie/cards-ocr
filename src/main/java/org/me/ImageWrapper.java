package org.me;

import java.awt.image.BufferedImage;

class ImageWrapper {
    private BufferedImage image;
    private boolean invert;

    public ImageWrapper(BufferedImage image, boolean invert) {
        this.image = image;
        this.invert = invert;
    }

    public int getRGB(Point point) {
        return getRGB(point.x, point.y);
    }

    public int getRGB(int x, int y) {
        if (x < 0 || x >= image.getWidth()) {
            return invert ? 0 : 0xff;
        }
        if (y < 0 || y >= image.getHeight()) {
            return invert ? 0 : 0xff;
        }
        return image.getRGB(x, y);
    }
}
