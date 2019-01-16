package org.me;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class TemplateMaker {
    public static void main(String[] args) throws Exception {
        Path root = Paths.get("C:\\Develop\\test\\java_test_task\\templates");

        Files.list(root).forEach(root1 -> {
            try {
                makeMap(root1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        System.out.println();
    }

    private static void makeMap(Path root) throws IOException {
        int[][] matrix = new int[256][256];
        for (int x = 0; x < 256; x++) {
            Arrays.fill(matrix[x], 0);
        }

        AtomicInteger w = new AtomicInteger();
        AtomicInteger h = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();
        Files.list(root).forEach(file -> {
            try {
                BufferedImage image = ImageIO.read(file.toFile());
                for (int x = 0; x < image.getWidth(); x++)
                    for (int y = 0; y < image.getHeight(); y++)
                        if ((image.getRGB(x, y) & 0xff) == 0) {
                            matrix[x][y]++;
                            max.set(Math.max(max.get(), matrix[x][y]));

                            w.set(Math.max(w.get(), x));
                            h.set(Math.max(h.get(), y));
                        }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        BufferedImage res = new BufferedImage(w.get(), h.get(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = res.createGraphics();


        for (int x = 0; x < w.get(); x++) {
            for (int y = 0; y < h.get(); y++) {

                int p = (int) (matrix[x][y] / max.doubleValue() * 255);

                g.setColor(new Color(p, p, p));
                g.drawLine(x, y, x, y);
            }
        }
        ImageIO.write(res, "PNG", new File(root.getFileName() + "_res.png"));
    }
}
