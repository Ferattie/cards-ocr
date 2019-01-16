package org.me;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class CardDetector {

    private static class Holder {
        private String filename;
        private BufferedImage image;

        public Holder(String filename, BufferedImage image) {
            this.filename = filename;
            this.image = image;
        }

        public String getFilename() {
            return filename;
        }

        public BufferedImage getImage() {
            return image;
        }
    }

    private static final CardDetector INSTANCE = new CardDetector();

    private final Holder[] templates;

    private CardDetector() {
        File[] files = getResourceFolderFiles("templates");
        templates = new Holder[files.length];
        for (int i = 0; i < files.length; i++) {
            try {
                templates[i] = new Holder(files[i].getName(), ImageIO.read(files[i]));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static CardDetector getInstance() {
        return INSTANCE;
    }

    public String detect(BufferedImage origin) {
        String result = null;
        for (Holder holder : templates) {
            BufferedImage template = holder.getImage();
            int width = Math.min(template.getWidth(), origin.getWidth());
            int height = Math.min(template.getHeight(), origin.getHeight());

            // Изображение и шаблын должны быть соразмерны
            double widthDiff = Math.abs(1 - (double)template.getWidth() / origin.getWidth());
            double heightDiff = Math.abs(1 - (double)template.getHeight() / origin.getHeight());

            // Если изображения отличаются более чем на 10%, пропускаем
            if (widthDiff > .1 || heightDiff > .1) {
                continue;
            }

            int expected = 0;
            int total = 0;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int p = origin.getRGB(x, y);
                    int t = template.getRGB(x, y);

                    // Если точка черная, т.е. принадлежит объекту
                    if ((p & 0xff) == 0) {
                        // Если вероятность нахождения точки по заданным координатам > 0
                        if ((t & 0xff) > 0) {
                            // Увеличиваем счетчик ожидаемых совпадений
                            expected ++;
                        }
                        // Увеличиваем счетчик точек объекта
                        total ++;
                    }
                }
            }

            // Если 95% точек объекта ожидаемы, то считаем, что совпадение найдено
            if ((double)expected / total > .95) {
                result = holder.getFilename();
                break;
            }
        }
        return result;
    }

    private static File[] getResourceFolderFiles (String folder) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(folder);
        String path = url.getPath();
        return new File(path).listFiles();
    }
}
