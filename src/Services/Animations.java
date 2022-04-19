package Services;

import java.util.Random;

/**
 * Animation for v2g
 * Image viewer ImageGlass is recommended (Won't work without proper auto refresh)
 */
public class Animations {

    static Bitmap bitmap = new Bitmap(256, 256, "Car_Animations.ppm", false);

    static Bitmap.Colors colors = bitmap.new Colors();
    static Bitmap.Pattern backgroundColor = colors.merge(colors.grey(), colors.white(), "gradientH=auto");
    static Bitmap.Outline wheelGripColor = bitmap.new Outline(2, colors.merge(colors.dark_grey(), colors.black(), "checkerboard>"));
    static Bitmap.Outline bodySideColor = bitmap.new Outline(2, colors.merge(colors.light_grey(), colors.black(), "checkerboard>"));
    static Bitmap.Pattern windowColor = colors.merge(colors.blue(), colors.white(), "gradientH=auto");

    public static void appear() {

        // Background
        Bitmap.Pattern backgroundColor = colors.merge(colors.grey(), colors.white(), "gradientH=auto");
        bitmap.fillWin(backgroundColor);
        bitmap.fillArea(1, 100, bitmap.canvas_width, 101, colors.dark_grey());
        // Animation with loops
        for (int radius = 1; radius < 60; radius += 2) {
            // Body
            bitmap.shapes.circle(bitmap.canvas_width / 2, 100, radius, colors.black(), bodySideColor);
            if (radius >= 40) {
                // Wheels
                bitmap.shapes.circle(70, 50, radius - 37, colors.black(), wheelGripColor);
                bitmap.shapes.circle(190, 50, radius - 37, colors.black(), wheelGripColor);
                // Window
                bitmap.shapes.circle(bitmap.canvas_width / 2 + 25, 120, radius - 40, windowColor, bodySideColor);
            }
            // Render each 150ms = 6.6 fps
            render_delayed(bitmap, 150);
        }


        // Antenna
        for (int i = 0; i < 15; i++) {
            bitmap.lineV(150, 150 + i * 2, bitmap.canvas_width / 2);
            render_delayed(bitmap, 150);
        }

        // Blink
        for (int radius = 1; radius < 8; radius++) {
            bitmap.shapes.circle(bitmap.canvas_width / 2, 180, radius, colors.black());
            render_delayed(bitmap, 150);
        }
    }

    public static void drive() {

        Random rand = new Random();
        double treeSize;
        int yTree;

        // Drive n*8 frames
        for (int i = 0; i < 4; i++) {
            treeSize = 1 + rand.nextDouble();
            yTree = 100 - rand.nextInt(3);
            for (int radius = 1; radius < 8; radius++) {
                // Background
                bitmap.fillWin(backgroundColor);
                bitmap.lineH(100, colors.dark_grey(), 2);
                // Tree
                bitmap.shapes.tree(275 - 38 * radius, yTree, treeSize);
                // Blink
                bitmap.shapes.circle(bitmap.canvas_width / 2, 180, 7, colors.black());
                bitmap.lineV(150, 180, bitmap.canvas_width / 2);
                bitmap.shapes.circle(bitmap.canvas_width / 2, 180, 7 - radius, colors.random());
                // Body
                bitmap.shapes.circle(bitmap.canvas_width / 2, 100, 59, colors.black(), bodySideColor);
                // Wheels (again), but they do vrmm vrmm
                ((Bitmap.Pattern) wheelGripColor.color_provider).shiftPattern = !((Bitmap.Pattern) wheelGripColor.color_provider).shiftPattern;
                bitmap.shapes.circle(70, 50, 22, colors.black(), wheelGripColor);
                bitmap.shapes.circle(190, 50, 22, colors.black(), wheelGripColor);
                // Dust
                bitmap.changePixel(42, 40, radius % 2 == 0 ? backgroundColor : colors.patternFromColor(colors.dark_grey()));
                bitmap.changePixel(41, 40, radius % 2 == 0 ? backgroundColor : colors.patternFromColor(colors.dark_grey()));
                bitmap.changePixel(39, 43, radius % 2 != 0 ? backgroundColor : colors.patternFromColor(colors.dark_grey()));
                bitmap.changePixel(38, 43, radius % 2 != 0 ? backgroundColor : colors.patternFromColor(colors.dark_grey()));
                bitmap.changePixel(35, 41, radius % 3 != 0 ? backgroundColor : colors.patternFromColor(colors.dark_grey()));
                bitmap.changePixel(36, 41, radius % 3 != 0 ? backgroundColor : colors.patternFromColor(colors.dark_grey()));
                bitmap.changePixel(165, 40, radius % 2 != 0 ? backgroundColor : colors.patternFromColor(colors.dark_grey()));
                bitmap.changePixel(164, 40, radius % 2 != 0 ? backgroundColor : colors.patternFromColor(colors.dark_grey()));
                bitmap.changePixel(162, 38, radius % 2 == 0 ? backgroundColor : colors.patternFromColor(colors.dark_grey()));
                bitmap.changePixel(161, 38, radius % 2 == 0 ? backgroundColor : colors.patternFromColor(colors.dark_grey()));
                bitmap.changePixel(158, 41, radius % 3 == 0 ? backgroundColor : colors.patternFromColor(colors.dark_grey()));
                bitmap.changePixel(159, 41, radius % 3 == 0 ? backgroundColor : colors.patternFromColor(colors.dark_grey()));
                // Window
                windowColor = colors.merge(colors.blue(), colors.white(), "gradientH=" + (100 + (radius > 4 ? radius - 4 : radius)) + "-140");
                bitmap.shapes.circle(bitmap.canvas_width / 2 + 25, 120, 20, windowColor, bodySideColor);
                // Render
                render_delayed(bitmap, 150);
            }
        }
    }

    public static void idle() {
        // Background
        bitmap.fillWin(backgroundColor);
        bitmap.lineH(100, colors.dark_grey(), 2);
        // Blinky
        bitmap.lineV(150, 180, bitmap.canvas_width / 2);
        bitmap.shapes.circle(bitmap.canvas_width / 2, 180, 7, colors.black());
        // Body
        bitmap.shapes.circle(bitmap.canvas_width / 2, 100, 59, colors.black(), bodySideColor);
        bitmap.shapes.circle(bitmap.canvas_width / 2 + 25, 120, 20, windowColor, bodySideColor);
        // Wheels
        bitmap.shapes.circle(70, 50, 22, colors.black(), wheelGripColor);
        bitmap.shapes.circle(190, 50, 22, colors.black(), wheelGripColor);
        // Render
        bitmap.render();
    }


    public static void charge(boolean discharge) {
        idle();

        Bitmap.Color indicatorColor = discharge ? colors.red() : colors.green();
        Bitmap.Color chargeColor = discharge ? colors.red() : colors.yellow();

        // Blinky
        bitmap.shapes.circle(bitmap.canvas_width / 2, 180, 1, indicatorColor);
        // Charging station

        // Stand
        for (int i = 0; i < 10; i += 2) {
            bitmap.shapes.circle(106, 27, i, colors.black());
            render_delayed(bitmap);
        }
        // Bar
        for (int i = 27; i < 116; i += 8) {
            if (i > 110) {
                bitmap.shapes.circle(106, 112, 3 + (i - 78) / 8, colors.light_grey());
            }
            bitmap.fillArea(100, 27, 112, i, colors.dark_grey());
            if (i > 31)
                bitmap.lineV(31, Math.min(i, 104), 106, colors.merge(colors.dark_grey(), colors.black(), "checkerboard"), 1);

            render_delayed(bitmap);
        }

        bitmap.shapes.circle(106, 105, 2, chargeColor);


        //Stick
        for (int i = 0; i < 13; i += 2) {
            bitmap.shapes.circle(125, 104, 1 + i / 2, colors.merge(colors.black(), colors.dark_grey(), "grid"));
            bitmap.lineH(113, 113 + i, 103, colors.merge(colors.merge(colors.dark_grey(), colors.black(), "gradientV=113-125"), colors.black(), "grid"), 3);

            render_delayed(bitmap);
        }


        for (int i = 0; i < 100; i++) {
            String shift = (i % 2 == 0 ? ">" : "");
            bitmap.shapes.circle(125, 104, 3, colors.merge(colors.black(), i % 3 == 0 ? chargeColor : colors.dark_grey(), "grid"));
            bitmap.lineV(31, 103, 106, colors.merge(chargeColor, colors.black(), "checkerboard" + shift), 1);
            bitmap.lineH(113, 125, 103, colors.merge(colors.merge(colors.dark_grey(), colors.black(), "gradientV=113-125"), chargeColor, "grid" + shift), 3);
            render_delayed(bitmap, 150);
        }


        bitmap.render();
    }

    private static void render_delayed(Bitmap bitmap, int ms) {
        try {
            Thread.sleep(ms);
            bitmap.render();
        } catch (InterruptedException ignored) {
        }
    }

    private static void render_delayed(Bitmap bitmap) {
        render_delayed(bitmap, 140);
    }
}
