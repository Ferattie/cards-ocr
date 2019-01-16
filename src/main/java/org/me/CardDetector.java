package org.me;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
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

    private final List<Holder> templates;

    public CardDetector(ApplicationContext context) throws IOException {

        Resource[] files = context.getResources("templates/*.png");

        templates = Arrays.stream(files).map(resource -> {
            try {
                String filename = resource.getFilename();
                InputStream in = resource.getInputStream();
                return new Holder(filename, ImageIO.read(in));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
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

}
