package org.me;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.FileSystemUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableConfigurationProperties
public class App implements ApplicationRunner {

    private static Logger LOG = LoggerFactory.getLogger(App.class);

    private boolean debug = false;
    private Path debugFolder = null;

    private final CardDetector cardDetector = CardDetector.getInstance();

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> nonOptionalArgs = args.getNonOptionArgs();
        if (nonOptionalArgs.isEmpty()) {
            LOG.error("A source folder doesn't specified.");
            return;
        }

        File path = new File(nonOptionalArgs.get(0));
        if (!path.exists()) {
            LOG.error("The specified source folder \"{}\" doesn't exists.", path.getPath());
            return;
        }

        if (!path.isDirectory()) {
            LOG.error("The specified source folder \"{}\" is not a directory", path.getPath());
            return;
        }

        List<String> debugOption = args.getOptionValues("debug");
        try {
            this.debug = (debugOption != null && !debugOption.isEmpty()) && Boolean.valueOf(args.getOptionValues("debug").get(0));

            if (this.debug) {
                debugFolder = Paths.get(path.getPath() + File.separator + "debug");
                FileSystemUtils.deleteRecursively(debugFolder);
                Files.createDirectory(debugFolder);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Files.list(Paths.get(path.getPath())).filter(Files::isRegularFile).forEach(this::parseImage);
    }

    private void parseImage(Path path) {
        String result = "";
        try {
            Path fileDebugPath = debug ? Files.createDirectories(Paths.get(debugFolder + File.separator + path.getFileName())) : null;
            Path listFolder = Paths.get(debugFolder + File.separator + "list");
            if (debug & Files.notExists(listFolder)) {
                listFolder = Files.createDirectory(listFolder);
            }

            // Считываем изображение из файла
            BufferedImage image = ImageIO.read(path.toFile());
            if (debug) {
                ImageIO.write(image, "PNG", new File(fileDebugPath + File.separator + "origin.png"));
            }

            // Известно, что сканированию подлежит только центральная часть изображения.
            // Для сокращения вычислений выделим среднюю часть
            image = extractMiddlePart(image);
            if (debug) {
                ImageIO.write(image, "PNG", new File(fileDebugPath + File.separator + "step1_extract_workspace.png"));
            }

            // Преобразуем в градации серого
            convertToGrayscale(image);
            if (debug) {
                ImageIO.write(image, "PNG", new File(fileDebugPath + File.separator + "step2_gray_scale.png"));
            }

            // Бинаризуем изображение для поиска контуров
            binarise(image, 150);
            if (debug) {
                ImageIO.write(image, "PNG", new File(fileDebugPath + File.separator + "step3_binary.png"));
            }

            BeetleParser parser = new BeetleParser();

            // Ищем возможные контуры карт с помощью модифицированного алгоритма жука. Стандартный алгоритм предполагает,
            // что объект имеют черный цвет, а фон - белый. В нашем случае мы ищем белые карты на черном фоне,
            // поэтому указываем флаг, что поиска производится при инвертированных цветах.
            parser.parse(image, true);

            // Получаем список областей изображений, где потенциально может содержаться карта. Отфильтровываем
            // области размером менее 10х10 пикселя, т.к. эти области заведомо не содержат изображения карты
            List<Blob> blobs = parser.getBlobs().stream().filter(blob -> blob.w > 10 && blob.h > 10).collect(Collectors.toList());

            // Определяем какие из найденных объектов попадают под определение "карта".
            // Объект может считаться картой, если большая часть его контура расположена вдоль на гранях прямоугольника
            // ограничивающего объект
            List<Blob> cards = blobs.stream().filter(blob -> {
                List<Point> circuit = blob.getCircuit();
                long c = circuit.stream().filter(point -> isNear(point, blob.x, blob.y, blob.getWidth(), blob.getHeight())).count();
                double p = (double)c / circuit.size();
                return p > .7;
            }).collect(Collectors.toList());

            int blobCounter = 0;
            for (Blob card : cards) {
                BufferedImage subImage = image.getSubimage(card.x, card.y, card.w, card.h);
                if (debug) {
                    ImageIO.write(subImage, "PNG", new File(fileDebugPath + File.separator + "step4_part_" + blobCounter + ".png"));
                }

                // Для каждой потенциальной карты необходимо провести анализ достоинства и масти карты
                // Воспользуемся тем же алгоритмом жука, но в этот раз фон будет белым, а объекты черными.
                parser.parse(subImage);

                // Получаем список объектов карты и отфильтровываем области менее 5х5 пикселей, т.к.
                // они заведомо не содержат признаков карты и не интересны для анализа
                List<Blob> parts = parser.getBlobs().stream().filter(blob -> blob.w > 5 && blob.h > 5).collect(Collectors.toList());

                int partCounter = 0;
                for (Blob part : parts) {
                    BufferedImage partSubimage = parser.getImage().getSubimage(part.x, part.y, part.w, part.h);
                    if (debug) {
                        ImageIO.write(partSubimage, "PNG", new File(fileDebugPath + File.separator + "step4_part_" + blobCounter + "_" + (partCounter++) + ".png"));
                    }
                    //ImageIO.write(partSubimage, "PNG", new File(listFolder + File.separator + path.getFileName() + "_step4_part_" + blobCounter + "_" + (partCounter++) + ".png"));
                    String partName = cardDetector.detect(partSubimage);
                    if (partName != null && !partName.contains("small")) {
                        result += partName.substring(0, 1);
                    }
                }
                blobCounter++;
            }


            System.out.println(path.getFileName() + " - " + result);
        } catch (Exception e) {
            System.out.println(path.getFileName() + " - Error");
        }

    }

    private boolean isNear(Point point, int x, int y, int width, int height) {
        boolean onTop = Math.abs(point.y - y) < 1;
        boolean onLeft = Math.abs(point.x - x) < 1;
        boolean onRight = Math.abs(point.x - x) >= width - 1;
        boolean onBottom = Math.abs(point.y - y) >= height - 1;

        return onTop || onBottom || onLeft || onRight;
    }

    private BufferedImage extractMiddlePart(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();

        return source.getSubimage(0, (int)(height * .4), width, (int)(height * .3));
    }

    private void convertToGrayscale(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = img.getRGB(x, y);

                int a = (p>>24) & 0xff;
                int r = (p>>16) & 0xff;
                int g = (p>>8) & 0xff;
                int b = p & 0xff;

                int avg = (r + g + b) / 3;

                p = (a<<24) | (avg<<16) | (avg<<8) | avg;

                img.setRGB(x, y, p);
            }
        }
    }

    private void binarise(BufferedImage img, int threshold) {
        int width = img.getWidth();
        int height = img.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = img.getRGB(x, y);
                int avg = p & 0xff;

                p = avg <= threshold ? 0 : 0xffffff;

                img.setRGB(x, y, p);
            }
        }
    }
}