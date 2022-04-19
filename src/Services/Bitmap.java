package Services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/** Java class to easily create and manipulate .ppm files. <br>
 * Created by Simon Conrad © 2021 <br>
 * <a href=https://github.com/IsAvaible/Bitmap>Github</a> <br>
 * <a href=https://imageglass.org/>Recommended Image Viewer</a>
 */
public class Bitmap {

    public int canvas_width; // The width of the canvas in pixel
    public int canvas_height; // The height of the canvas in pixel

    public Colors colors = new Colors(); // A object of the colors subclass
    public Shapes shapes = new Shapes(); // A object of the shapes subclass
    public PatternBuilders patternBuilders = new PatternBuilders(); // A object of the patternBuilders subclass

    public Colors cols = colors; public PatternBuilders pbs = patternBuilders; public Shapes shps = shapes; // Abbreviations

    public ArrayList<String> comments = new ArrayList<>();

    private String filename; // The filename that is used in the render method
    private int[][][] canvas; // A three dimensional representation of the array columns[rows[pixel[]]]

    /** Creates a new Bitmap object
     * d = default (can be left away)
     * @param canvas_width The width of the canvas in pixel
     * @param canvas_height The height of the canvas in pixel
     * @param filename The name of the file that is created upon execution (must be .ppm or none) | d = Bitmap.ppm
     * @param render_on_init Whether the canvas should be rendered upon execution (file is created / overwritten) | d = true
     */
    public Bitmap(int canvas_width, int canvas_height, String filename, boolean render_on_init) {
        this.canvas_width = canvas_width;
        this.canvas_height = canvas_height;
        canvas = new int[canvas_height][canvas_width][3];
        this.filename = filename;
        this.comments.add("#" + filename);
        if (render_on_init) render(true);
    }

    /**@see #Bitmap(int, int, String, boolean) **/
    public Bitmap(int canvas_width, int canvas_height, String filename) { this(canvas_width, canvas_height, filename, true); }
    /**@see #Bitmap(int, int, String, boolean) **/
    public Bitmap(int canvas_width, int canvas_height) { this(canvas_width, canvas_height, true); }
    /**@see #Bitmap(int, int, String, boolean) **/
    public Bitmap(int canvas_width, int canvas_height, boolean render_on_init) { this(canvas_width, canvas_height, "Bitmap.ppm", render_on_init); }

    public int[][][] getCanvas() {
        return canvas;
    }

    /** Validates the syntax of a color_provider
     * @param color The color_provider that should be validated
     * @throws IllegalArgumentException when the color_provider doesn't match criteria
     */
    private void validateColor(Color color) {
        if (color.color.length != 3) {
            throw new IllegalArgumentException("color has the wrong length");
        }

        for (int color_information : color.color) {
            if (color_information < 0 || color_information > 255) {
                throw new IllegalArgumentException("at least one rgb value is too big");
            }
        }

    }

    /** Changes exactly one pixel at a specified point
     * @param x coordinate of the addressed pixel
     * @param y coordinate of the addressed pixel
     * @param color_provider a object of type Color or Pattern that provides the color_provider
     * @throws IllegalArgumentException when the color_provider isn't a Pattern or a Color
     * @throws Exceptions.PixelOutOfBoundsException when the accessed pixel is outside the canvas
     */
    public void changePixel(int x, int y, ColorProvider color_provider)  {
        if (x < 1 || x > canvas_width) {
            throw new Exceptions.PixelOutOfBoundsException("x is out of bounds");
        } else if (y < 1 || y > canvas_height) {
            throw new Exceptions.PixelOutOfBoundsException("y is out of bounds");
        }

        if (color_provider.getClass() == Color.class) {
            canvas[canvas_height-y][x-1] = ((Color) color_provider).color;
        } else if (color_provider.getClass() == Pattern.class) {
            canvas[canvas_height-y][x-1] = ((Pattern) color_provider).run(x, y).color;
        } else {
            throw new IllegalArgumentException("How did you even get here? color_provider can be only be a Pattern or a Color");
        }
    }


    /** Validate if the type of the object is correct # Deprecate (was replaced by introducing abstract class ColorProvider)
     * @param obj ColorProvider to validate. Must be either Color or Pattern.
     * @throws IllegalArgumentException if the object isn't a color_provider or a pattern
     */
    private void checkType(ColorProvider obj) {
        if (obj.getClass() != Color.class && obj.getClass() != Pattern.class) {
            throw new IllegalArgumentException("color_provider can only be a Pattern or a Color");
        }
    }

    /**
     * @param x_p1 The x-coordinate of the first point
     * @param y_p1 The y-coordinate of the first point
     * @param x_p2 The x-coordinate of the second point
     * @param y_p2 The y-coordinate of the second point
     * @param color_provider The color_provider provider, has to be of type Color or Pattern
     * @param outline The outline object
     * @param borderclip Whether the PixelOutOfBoundsException should be thrown
     * @see Outline#Outline(boolean, int, ColorProvider)
     * @throws IllegalArgumentException if the color_provider isn't of type Color or pattern
     */
    public void fillArea(int x_p1, int y_p1, int x_p2, int y_p2, ColorProvider color_provider, Outline outline, boolean borderclip) {
        int min_x, max_x, min_y, max_y;
        min_x = Math.min(x_p1, x_p2); max_x = Math.max(x_p1, x_p2);
        min_y = Math.min(y_p1, y_p2); max_y = Math.max(y_p1, y_p2);

        if (color_provider.getClass() == Pattern.class) {
            setAutoPattern(min_x, max_x, min_y, max_y, color_provider, false);
        }

        if (outline.active) {
            border(min_x, min_y, max_x, max_y, outline.thickness, outline.color_provider);
        }

        for (int y = min_y; y <= max_y; y++) {
            for (int x = min_x; x <= max_x; x++) {
                try {
                    changePixel(x, y, color_provider);
                } catch (Exceptions.PixelOutOfBoundsException exception) {
                    if (borderclip) {throw exception;}
                }
            }
        }
    }

    /** Creates a horizontal line between two x-coordinate
     * @param x_from The first x-coordinate
     * @param x_to The second x-coordinate
     * @param y_pos The y-coordinate of the line
     * @param color_provider The color_provider provider, has to be either Color or Pattern
     * @param thickness The thickness of the line (upwards)
     */
    public void lineH(int x_from, int x_to, int y_pos, ColorProvider color_provider, int thickness) {
        fillArea(x_from, y_pos, x_to, y_pos+thickness-1, color_provider);
    }

    /** Creates a vertical line between two x-coordinate
     * @param y_from The first y-coordinate
     * @param y_to The second y-coordinate
     * @param x_pos The x-coordinate of the line
     * @param color_provider The color_provider provider, has to be either Color or Pattern
     * @param thickness The thickness of the line (to the right)
     */
    public void lineV(int y_from, int y_to, int x_pos, ColorProvider color_provider, int thickness) {
        fillArea(x_pos, y_from, x_pos+thickness-1,  y_to, color_provider);
    }

    /**
     * Clears the bitmap (this) by filling it with black
     */
    public void clear() {
        fillWin();
    }

    /** Creates a border in a specified area (outwards facing)
     * @param x_p1 The x-coordinate of the first point
     * @param y_p1 The y-coordinate of the first point
     * @param x_p2 The x-coordinate of the second point
     * @param y_p2 The y-coordinate of the second point
     * @param thickness The thickness of the border (outwards)
     * @param color_provider The color_provider provider, has to be either Color or Pattern
     */
    public void border (int x_p1, int y_p1, int x_p2, int y_p2, int thickness, ColorProvider color_provider) {

        int min_x, max_x, min_y, max_y;
        min_x = Math.min(x_p1, x_p2); max_x = Math.max(x_p1, x_p2);
        min_y = Math.min(y_p1, y_p2); max_y = Math.max(y_p1, y_p2);
        Pattern[] lockedPatterns =  setAutoPattern(min_x, max_x, min_y, max_y, color_provider, true);
        // X-Axis
        fillArea(min_x-thickness, min_y, max_x+thickness,min_y-thickness, color_provider);
        fillArea(min_x-thickness, max_y, max_x+thickness, max_y+thickness, color_provider);
        // Y-Axis
        fillArea(min_x, min_y, min_x-thickness, max_y, color_provider);
        fillArea(max_x, min_y, max_x + thickness, max_y, color_provider);
        // Unlocking the patterns again
        unlockAutoPattern(lockedPatterns);
    }

    /**
     * @param color_provider If cp is a Color, the function is more efficient than fillArea(). Can also be set to a Pattern.
     * @throws IllegalArgumentException if the color_provider provider is not of type Color or Pattern
     */
    public void fillWin(ColorProvider color_provider) {
        if (color_provider.getClass() == Color.class) {
            validateColor((Color) color_provider);
            for (int[][] row : canvas) {
                Arrays.fill(row, ((Color) color_provider).color);
            }
        } else if (color_provider.getClass() == Pattern.class) {
            fillArea(1, 1, canvas_width, canvas_height, color_provider);
        } else {
            throw new IllegalArgumentException("color_provider must be of class Color or Pattern");
        }

    }

    /** Sets the from and to on pattern that are labeled as auto recursively
     * @param from_x From when the pattern is horizontal
     * @param to_x To when the pattern is horizontal
     * @param from_y From when the pattern is vertical
     * @param to_y To when the pattern is vertical
     * @param color_provider The color_provider provider, has to be either Color or Pattern
     * @param lockIn Whether the patterns should be set to auto = false upon assignment
     * @return Pattern[] lockedPatterns : A list of all patterns that were locked during execution
     */
    private Pattern[] setAutoPattern(int from_x, int to_x, int from_y, int to_y, ColorProvider color_provider, boolean lockIn) {
        // Growable list storing the locked patterns
        ArrayList<Pattern> lockedPatterns = new ArrayList<>();
        if (color_provider.getClass() == Color.class) {
            // No branches here anymore
        } else if (color_provider.getClass() == Pattern.class) {
            // Recursive call of both slots to make sure every branch is visited
            lockedPatterns.addAll(Arrays.asList( setAutoPattern(from_x, to_x, from_y, to_y, ((Pattern) color_provider).slot_1, lockIn) ));
            lockedPatterns.addAll(Arrays.asList( setAutoPattern(from_x, to_x, from_y, to_y, ((Pattern) color_provider).slot_2, lockIn) ));
            // Setting the from and to only when auto mode is selected
            if (((Pattern) color_provider).auto) {
                boolean horizontal = ((Pattern) color_provider).horizontal;
                ((Pattern) color_provider).from = horizontal ? from_y : from_x;
                ((Pattern) color_provider).to = horizontal ? to_y : to_x;
                if (lockIn) {
                    ((Pattern) color_provider).auto = false; // If the pattern should be locked in, auto is set to false
                    lockedPatterns.add((Pattern) color_provider);
                }
            }
        }
        // Converting the list to an array
        Object[] temp = lockedPatterns.toArray();
        // Casting all Objects in the list to Pattern
        return Arrays.copyOf(temp, temp.length, Pattern[].class);
    }

    /** Can be used to unlock patterns previously locked by the setAutoPattern
     * @param patterns an array of patterns that should be unlocked
     */
    private void unlockAutoPattern(Pattern[] patterns) {
        for (Pattern pattern : patterns) {
            pattern.auto = true;
        }
    }

    /** Writes the bitmap to a file
     *  d = default
     * @param filename The name of the file to which the bitmap should be written | d = Bitmap.ppm
     * @param custom_win A three dimensional array containing custom pixel information | d = canvas
     * @param report_path | d false
     * @throws IllegalArgumentException if the specified file is not in the ppm format, or if the write operation wasn't successful
     */
    public void render(String filename, int[][][] custom_win, boolean report_path) {
        String format;
        String full_filepath =  filename.substring(0, 2).matches(".:") ? filename : System.getProperty("user.dir") + "/" + filename;
        try {
            format = filename.split("\\.")[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            format = "ppm";
            full_filepath = System.getProperty("user.dir") + "/" + filename + ".ppm";
        }

        try {
            // Creating a new File object
            File bitmap = new File(full_filepath);

            // Checking if the file exists
            if (!bitmap.exists()) {
                // Creating a new file
                if (bitmap.createNewFile()) {
                    System.out.println("created new file");
                }
            }
            // Creating a new file writer
            FileWriter render_obj = new FileWriter(full_filepath);
            // Better way to do build a string in a for loop
            StringBuilder converted_win = new StringBuilder();



            // Checking whether the file in the right format
            switch (format) {
                case "ppm" -> {
                    // Adding the header
                    converted_win.append(String.format("P3\n%s\n%s %s\n255\n", String.join("\n#", this.comments), canvas_width, canvas_height));

                    // Building the file content
                    for (int[][] row : custom_win) {
                        for (int[] pixel : row) {
                            for (int color_information : pixel) {
                                converted_win.append(color_information).append(" ");
                            }
                        }
                        converted_win.append("\n");
                    }
                }
                case "pbm" -> {
                    // Adding the header
                    converted_win.append(String.format("P1\n#%s\n%s %s\n", filename, canvas_width, canvas_height));

                    // Building the file content
                    for (int[][] row : custom_win) {
                        for (int[] pixel : row) {
                            int sum = 0;
                            for (int color_information : pixel) {
                                sum += color_information;
                            }
                            converted_win.append(sum > 255 * 3 / 2 ? 0 : 1).append(" ");
                        }
                        converted_win.append("\n");
                    }
                }
                case "pgm" -> {
                    // Adding the header
                    converted_win.append(String.format("P2\n#%s\n%s %s\n256\n", filename, canvas_width, canvas_height));

                    // Building the file content
                    for (int[][] row : custom_win) {
                        for (int[] pixel : row) {
                            int sum = 0;
                            for (int color_information : pixel) {
                                sum += color_information;
                            }
                            converted_win.append(sum / 3).append(" ");
                        }
                        converted_win.append("\n");
                    }
                }
                default -> {
                    render_obj.close();
                    throw new IllegalArgumentException(String.format("file format \"%s\" is not supported", format));
                }
            }



            if (report_path) { // Output the full filepath
                System.out.println("Writing object to: " + full_filepath);
            }
            // Write the build string to the file
            render_obj.write(converted_win.toString());
            // Close the file
            render_obj.close();

        } catch (FileNotFoundException e) {
            // When the current image viewer reloads the file, it is not accessible to Java.
            // Therefore waiting a small amount of time can ensure cleared file locks.
            // Might also result in a endless loop ¯\_(ツ)_/¯
            try {
                Thread.sleep(50);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            render(filename, custom_win, report_path);

        } catch (IOException e) {
            System.out.println(e.toString());
            throw new IllegalArgumentException("Error: couldn't write to file.");
        }
    }

    /**@see #render(String, int[][][], boolean) **/
    public void render(int[][][] custom_win) {render(filename, custom_win, false);}
    /**@see #render(String, int[][][], boolean) **/
    public void render() {render(filename, canvas, false);}
    /**@see #render(String, int[][][], boolean) **/
    public void render(String filename) {render(filename, canvas, false);}
    /**@see #render(String, int[][][], boolean) **/
    public void render(int[][][] custom_win, String filename) {render(filename, custom_win,false);}
    /**@see #render(String, int[][][], boolean) **/
    public void render(boolean report_path) {render(filename, canvas, report_path);}

    public Object[][] readFromFile(String filePath, boolean overwriteSettings) {
        if (!filePath.contains(".") || !filePath.split("\\.")[1].equals("ppm")) {throw new IllegalArgumentException("provided file isn't in the ppm format");}

        ArrayList<String> comments = new ArrayList<>();
        Integer width = null; Integer height = null;
        Integer maxVal = null;

        boolean contentReached = false;
        ArrayList<String> contentString = new ArrayList<>();
        ArrayList<String> growable_lines = new ArrayList<>();

        File file = new File(filePath);
        try {
            Scanner reader = new Scanner(file);
            while (reader.hasNextLine()) {
                growable_lines.add(reader.nextLine());
            }
        } catch (Exception ignored) {
            throw new IllegalArgumentException("file does not exist");
        }


        String[] lines = new String[growable_lines.size()];
        lines = growable_lines.toArray(lines);

        for (int index = 1; index < lines.length; index++) {
            String line = lines[index];
            if (line.startsWith("#")) { // Comment line
                comments.add(line.substring(1, line.length()));
            } else if (!contentReached) {
                var lineSplit = line.split(" ");
                if (lineSplit.length > 1)
                    if (width == null && height == null) { // Width and height line

                        if (lineSplit.length != 2) { throw new IllegalArgumentException("the provided file does not contain width and height in the file header"); }

                        width = Integer.parseInt(lineSplit[0]);
                        height = Integer.parseInt(lineSplit[1]);

                    } else {
                        throw new IllegalArgumentException(("the provided file does not contain a max values in the file header"));
                    }
                else {
                    try { // Max Val line
                        maxVal = Integer.parseInt(line);
                        contentReached = true;
                    } catch (Exception e) {
                        throw new IllegalArgumentException(String.format("the line which was expected to be the max value (\"%s\") could not be cast to int", line));
                    }

                }
            } else { // Content lines
                contentString.add(line);
            }
        }
        Function<String, Integer> toInt = str -> Integer.valueOf(str);
        //ArrayList<String[]> content = _partition((_partition(Arrays.stream(String.join(" ", contentString).trim().split(" +")).map(toInt).toArray(), 3)), width);


        Integer[] content_as_color_information = Arrays.stream(String.join(" ", contentString).trim().split(" +")).map(toInt).toArray(Integer[]::new);
        Integer[][] content_as_pixels = new Integer[content_as_color_information.length / 3][3];
        for (int i = 0; i < content_as_pixels.length; i += 1) {
            content_as_pixels[i] = Arrays.copyOfRange(content_as_color_information, i*3, i*3+3);
        }
        Integer[][][] content_as_rows = new Integer[height][content_as_pixels.length / width][3];
        for (int i = 0; i < height; i += 1) {
            content_as_rows[i] = Arrays.copyOfRange(content_as_pixels, i*width, i*width+width);
        }

        // System.out.println(Arrays.toString(new Object[]{width, height, maxVal, comments}));
        // System.out.println(Arrays.deepToString(content_as_rows));

        // Convert from Integer[][][] to int[][][]
        int[][][] content = new int[height][content_as_pixels.length / width][3];
        for (int row = 0; row < height; row++) {
            for (int pixel = 0; pixel < content[0].length; pixel++) {
                for (int color_information = 0; color_information < 3; color_information++) {
                    content[row][pixel][color_information] = content_as_rows[row][pixel][color_information];
                }
            }
        }

        if (overwriteSettings) {
            this.canvas_width = width;
            this.canvas_height = height;
            this.comments.addAll(comments);
            this.canvas = content;
        }

        // [int, int, int, String[]], int[][][]]
        return new Object[][]{{width, height, maxVal, comments}, content_as_rows};

    }

    // Stores a color_provider in the RGB color_provider format
    public class Color extends ColorProvider{
        int[] color;

        /**
         * @param color a int[] representation of the RGB-color_provider
         * @param alpha (brightness) between 0.0 (darkest) and 1.0 (normal)
         */
        Color (int[] color, double alpha) {
            this.color = color;
            if (alpha != 1.0) {
                setAlpha(alpha);
            }
            validateColor(this);
        }

        /**
         * @param alpha (brightness) between 0.0 (darkest) and 1.0 (brightest)
         * @return this
         */
        public Color setAlpha(double alpha) {
            int[] col = this.color.clone();

            if (alpha < 0.0 || alpha > 1.0) {
                throw new IllegalArgumentException("alpha should be a double between 0 and 1");
            }
            for (int i = 0; i < 3; i++) {
                col[i] = (int) (col[i] * alpha);
            }
            this.color = col;
            return this;
        }


        /**
         * @param r red
         * @param g green
         * @param b blue
         * @param alpha (brightness) between 0.0 (darkest) and 1.0 (brightest)
         */
        public Color(int r, int g, int b, double alpha) { this(new int[]{r, g, b}, alpha); }

        /**
         * @param color a string representation of the color_provider in the format "r g b"
         * @param alpha (brightness) between 0.0 (darkest) and 1.0 (brightest)
         */
        Color (String color, double alpha) { this(Stream.of(color.split(" ")).mapToInt(Integer::parseInt).toArray(), alpha); }

        /**@see #Color(int[], double)**/
        public Color(int[] color) {this(color, 1.0);}
        /**@see #Color(int, int, int, double)**/
        Color (int r, int g, int b) {this(r,g,b,1.0);}
        /**@see #Color(String, double)**/
        Color (String color) {this(color,1.0);}
    }

    /** Class to create patterns.
     * Patterns are a mix out of two color_provider providers.
     */
    public class Pattern extends ColorProvider{

        private ColorProvider slot_1;
        private ColorProvider slot_2;
        private final String pattern;

        private boolean horizontal;
        private boolean vertical;

        boolean shiftPattern;

        Predicate<int[]> custom_function;
        Function<int[], Integer> smoothed_function;

        int from;
        int to;
        boolean auto = false;

        double opacity;

        private Pattern (ColorProvider slot_1, ColorProvider slot_2, String pattern, boolean horizontal, boolean shiftPattern, int from, int to, boolean fastPattern, Predicate<int[]> custom_function, double opacity, boolean auto, Function<int[], Integer> smoothed_function) {
            this.slot_1 = slot_1; this.slot_2 = slot_2;

            this.horizontal = horizontal; this.vertical = !horizontal;
            this.shiftPattern = shiftPattern;
            this.from = from;
            this.to = to;
            this.auto = auto;

            this.custom_function = custom_function;
            this.smoothed_function = smoothed_function;
            this.opacity = opacity;

            if (fastPattern) pattern = evaluateFastPattern(pattern);
            validatePattern(pattern);

            this.pattern = pattern;
        }

        /** Calls the first slot recursively or returns the color_provider directly
         * @param x the x-coordinate
         * @param y the y-coordinate
         * @return the color_provider that is returned by the recursive call
         */
        public Color run_slot_1(int x, int y) {
            return slot_1.getClass() == Color.class ? (Color) slot_1 : ((Pattern) slot_1).run(horizontal ? y : x, horizontal ? x : y);
        }

        /** Calls the second slot recursively or returns the color_provider directly
         * @param x the x-coordinate
         * @param y the y-coordinate
         * @return the color_provider that is returned by the recursive call
         */
        public Color run_slot_2(int x, int y) {
            return slot_2.getClass() == Color.class ? (Color) slot_2 : ((Pattern) slot_2).run(horizontal ? y : x, horizontal ? x : y);
        }

        public void setSlot_1(ColorProvider slot_1) { this.slot_1 = slot_1; }
        public void setSlot_2(ColorProvider slot_2) { this.slot_2 = slot_2; }
        public ColorProvider getSlot_1() { return slot_1; }
        public ColorProvider getSlot_2() { return slot_2; }

        public Color run(int x, int y) {
            return run(x, y, "normal");
        }

        /** Calculates the slot that is returned
         * @param x the x-coordinate
         * @param y the y-coordinate
         * @return a Color
         */
        public Color run(int x, int y, String caller) {
            if (shiftPattern) {x++; y++;}
            if (horizontal) {int temp = x; x = y; y = temp;}
            switch (pattern) {
                // Main patterns
                case "normal":
                    return run_slot_1(x, y);
                case "opacity":
                    return colors.mix(
                            run_slot_1(x, y),
                            new Color(vertical ? (canvas[canvas_height-y][x-1]) : (canvas[canvas_height-x][y-1])),
                            opacity);
                case "blur":
                    Color col = null;
                    if (!caller.equals("blur")) {
                        for (int y_i = Math.max(1, y - 1); y_i < Math.min(y + 1, canvas_height); y_i++) {
                            for (int x_i = Math.max(1, x - 1); x_i < Math.min(x + 1, canvas_height); x_i++) {
                                if (x_i != x && y_i != y) {
                                    if (col == null) {
                                        col = run(x_i, y_i, "blur");
                                    } else {
                                        col = colors.mix(col, run(x_i, y_i, "blur"), opacity / 2);
                                    }
                                }
                            }
                        }
                    } else {
                        col = new Color(vertical ? (canvas[canvas_height-y][x-1]) : (canvas[canvas_height-x][y-1]));
                    }
                    return col;
                case "grid":
                    return x * y % 2 == 0 ? run_slot_1(x, y) : run_slot_2(x, y);
                case "stripes":
                    return x % 2 == 0 ? run_slot_1(x, y) : run_slot_2(x, y);
                case "checkerboard":
                    return (x+y) % 2 == 0 ? run_slot_1(x, y) : run_slot_2(x, y);
                case "gradient":
                    double balance = Math.max(Math.min((x - from) / (double) (to - from), 1.0), 0.0);
                    if (slot_1.getClass() == Pattern.class && slot_2.getClass() == Pattern.class && ((Pattern) slot_1).pattern.equals("gradient") && ((Pattern) slot_2).pattern.equals("gradient")) {
                        return colors.mix(new Color(run_slot_1(x, y).color), new Color(run_slot_2(x, y).color), 0.5); // If both slots are gradients, the colors should be mixed 1:1
                    }
                    return colors.mix(new Color(run_slot_1(x, y).color), new Color(run_slot_2(x, y).color), balance);
                case "cells":
                    return (shiftPattern == ((Math.sin(x * y)) > 0.5)) ? run_slot_1(x, y) : run_slot_2(x, y);
                case "bigcells":
                    return Math.sin(Math.toDegrees(x*y)) > 0.1 ? run_slot_1(x, y) : run_slot_2(x, y);
                case "space":
                    return  (int) Math.toDegrees(Math.sin(x*y))*1.5 % 2 == 0 ? run_slot_1(x, y) : run_slot_2(x, y);
                case "dotgrid":
                    return  x*y % 4 == 0 ? run_slot_1(x, y) : run_slot_2(x, y);
                case "biggrid":
                    return  x*y % 5 == 0 ? run_slot_1(x, y) : run_slot_2(x, y);
                case "hugegrid":
                    return  x*y % 19 == 0 ? run_slot_1(x, y) : run_slot_2(x, y);
                case "superhugegrid":
                    return  x*y % 73 == 0 ? run_slot_1(x, y) : run_slot_2(x, y);
                case "flowergrid":
                    return  x*y % 6 == 0 ? run_slot_1(x, y) : run_slot_2(x, y);
                case "dotlines":
                    return  (int) Math.sin(Math.toRadians(x*y)) % 2 == 0 ? run_slot_1(x, y) : run_slot_2(x, y);
                case "wave":
                    return  Math.sin((double) x/y) > 0.05 ? run_slot_1(x, y) : run_slot_2(x, y);
                case "custom":
                    return custom_function.test(new int[]{x, y, from, to}) ? run_slot_1(x, y) : run_slot_2(x, y);
                case "smoothed_function":
                    int finalX = x;
                    int finalY = y;
                    Predicate<int[]> smoothed = arg -> {
                        int this_y = smoothed_function.apply(new int[]{finalX, finalY, from, to});
                        int next_y = smoothed_function.apply(new int[]{finalX+1, finalY, from, to});
                        int last_y = smoothed_function.apply(new int[]{finalX-1, finalY, from, to});

                        return this_y == arg[1] || arg[1] > last_y && arg[1] < next_y || arg[1] < last_y && arg[1] > next_y;
                    };
                    return smoothed.test(new int[]{x, y, from, to}) ? run_slot_1(x, y) : run_slot_2(x, y);
            }

            throw new IllegalArgumentException(pattern + " is a unknown pattern");
        }

        /** Checks if the pattern is known
         * @param pattern a lowercase String containing the pattern name
         */
        public void validatePattern(String pattern) {
            if (!Set.of("grid", "checkerboard", "stripes", "gradient", "wave", "cells", "bigcells", "dotgrid", "biggrid", "hugegrid", "superhugegrid", "flowergrid", "space", "dotlines", "custom", "opacity", "normal", "smoothed_function").contains(pattern)) {
                throw new IllegalArgumentException(pattern + " is a unknown pattern");
            }
            if (from > to) throw new IllegalArgumentException("from must be smaller than to");
            if (from < 0) throw new IllegalArgumentException("from and to must be higher than 0");
            if (opacity < 0) throw new IllegalArgumentException("opacity can't be below 0.0");
        }

        /** Parses the provided fast-pattern. <br>
         * A fast pattern must consist of the pattern name e.g. "gradient". <br>
         * It can also contain either "H" (for horizontal) or "V" (for vertical). <br>
         * To shift the pattern, the character ">" can be used. <br>
         * From and to or the opacity can be set by adding a "=" + "from-to" / "opacity". <br>
         * To activate auto mode, add "=auto" at the end of the pattern. <br>
         * For example: {@code evaluateFastPattern("gradientH=auto")}  <br>
         * or: {@code evaluateFastPattern("stripesV>")}  <br>
         * or: {@code evaluateFastPattern("opacity=0.34")}  <br>
         * @param fast_pattern the fast-pattern
         * @return the pattern extracted from the fast-pattern
         */
        public String evaluateFastPattern(String fast_pattern) {
            int pattern_length = -1;
            for (int c = 0; c < fast_pattern.length(); c++) {
                if (fast_pattern.charAt(c) == 'H' || fast_pattern.charAt(c) == 'V') { // Horizontal / Vertical
                    if (pattern_length == -1) pattern_length = c;
                    setHorizontal(fast_pattern.charAt(c) == 'H');
                } else if (fast_pattern.charAt(c) == '>') { // Shift pattern
                    if (pattern_length == -1) pattern_length = c;
                    this.shiftPattern = true;
                } else if (fast_pattern.charAt(c) == '=') { // Set from and to / opacity
                    if (pattern_length == -1) pattern_length = c;
                    if (fast_pattern.substring(c+1).equals("auto")) this.auto = true;
                    else {
                        String[] from_to = fast_pattern.substring(c+1).split("-");
                        try {
                            this.from = Integer.parseInt(from_to[0]);
                            this.to = Integer.parseInt(from_to[1]);
                        } catch (Exception ignored) {
                            try {
                                this.opacity = Double.parseDouble(fast_pattern.substring(c+1));
                            } catch (Exception ignored_2) {
                                throw new IllegalArgumentException("from-to / opacity was not declared properly");
                            }
                        }
                    }
                }
            }

            if (pattern_length == -1) return fast_pattern;
            else return fast_pattern.substring(0, pattern_length);
        }

        public void setHorizontal(boolean horizontal) {
            this.horizontal = horizontal; this.vertical = !horizontal;
        }

        public String getPattern() {
            return pattern;
        }

        private Pattern(ColorProvider slot_1, ColorProvider slot_2, String pattern, boolean horizontal, boolean shiftPattern, int from, int to, boolean fastPattern, Predicate<int[]> custom_function, double opacity, boolean auto) {
            this(slot_1, slot_2, pattern, horizontal, shiftPattern, from, to, fastPattern, custom_function, opacity, auto, null);
        }

        private Pattern (ColorProvider slot_1, ColorProvider slot_2, String pattern, boolean horizontal, boolean shiftPattern, int from, int to, boolean fastPattern, Predicate<int[]> custom_function, double opacity) {
            this(slot_1, slot_2, pattern, horizontal, shiftPattern, from, to, fastPattern, custom_function, opacity, false);
        }

        private Pattern (ColorProvider slot_1, ColorProvider slot_2, String pattern, boolean horizontal, boolean shiftPattern, int from, int to, boolean fastPattern, Predicate<int[]> custom_function) {
            this(slot_1, slot_2, pattern, horizontal, shiftPattern, from, to, fastPattern, custom_function, 1.0);
        }

        private Pattern (ColorProvider slot_1, ColorProvider slot_2, String pattern, boolean horizontal, boolean shiftPattern, int from, int to, boolean fastPattern) {
            this(slot_1, slot_2, pattern, horizontal, shiftPattern, from, to, fastPattern, null);
        }

        /** Fast pattern overload
         * @param slot_1 The first object
         * @param slot_2 The second object
         * @param fast_pattern To define for example a fast gradient: "gradient{H/V}={from : int}-{to : int}", for example {@code "gradientH=100-200"}
         * @see #evaluateFastPattern(String)
         */
        public Pattern (ColorProvider slot_1, ColorProvider slot_2, String fast_pattern) {
            this(slot_1, slot_2, fast_pattern, true, false, 0, 0, true);
        }

        /** Opacity pattern
         * @param slot_1 The color_provider provider
         * @param opacity The opacity of the color_provider provider (1.0 = cover, 0.0 = invisible)
         */
        public Pattern(ColorProvider slot_1, double opacity) {
            this(slot_1, colors.white(), "opacity", true, false, 0, 0, false, null, opacity, false);
        }

        /** Custom pattern
         * @param slot_1 The first object
         * @param slot_2 The second object
         * @param custom_function Predicate that accepts a Integer Array. The first element is
         *                        the x-coordinate, the second element is the y-coordinate and
         *                        the third and fourth element are the from and to variables. <br>
         *                        When the Predicate returns true, the first slot is called, vice
         *                        versa the second one, when it returns false. <br>
         *                        Example: <br>
         *                        {@code Predicate<int[]> custom_fun = arr -> arr[0] * arr[1] % 16 == 0;}
         *
         */
        public Pattern (ColorProvider slot_1, ColorProvider slot_2, Predicate<int[]> custom_function) {
            this(slot_1, slot_2, "custom", false, false, 0, 0, true, custom_function);
        }

        /** Pattern overload for patterns that use from and to (gradient)
         * @param slot_1 The first object
         * @param slot_2 The second object
         * @param pattern The name of the pattern
         * @param horizontal Whether the pattern should be horizontal or vertical
         * @param from The first coordinate (int)
         * @param to The second coordinate (int)
         */
        public Pattern (ColorProvider slot_1, ColorProvider slot_2, String pattern, boolean horizontal, int from, int to, boolean auto) {
            this(slot_1, slot_2, pattern, horizontal, false, from, to, true, null, 1.0, auto);
        }

        public Pattern (ColorProvider slot_1, ColorProvider slot_2, String pattern, boolean horizontal, int from, int to) {
            this(slot_1, slot_2, pattern, horizontal, false, from, to, true);
        }

        /** Standard pattern
         * @param slot_1 The first object
         * @param slot_2 The second object
         * @param pattern The name of the pattern
         * @param horizontal Whether the pattern should be horizontal or vertical
         * @param shiftPattern Whether the pattern should be shifted by one pixel to the right
         */
        public Pattern (ColorProvider slot_1, ColorProvider slot_2, String pattern, boolean horizontal, boolean shiftPattern) {
            this(slot_1, slot_2, pattern, horizontal, shiftPattern, 0, 0, false);
        }
        /**@see #Pattern(ColorProvider, ColorProvider, String, boolean, boolean) **/
        public Pattern(ColorProvider slot_1, ColorProvider slot_2, String pattern, boolean horizontal) {
            this(slot_1, slot_2, pattern, horizontal, false);
        }

        public Pattern(PatternBuilder patternBuilder) {
            this(patternBuilder.slot_1, patternBuilder.slot_2, patternBuilder.pattern, patternBuilder.horizontal, patternBuilder.shiftPattern, patternBuilder.from, patternBuilder.to, patternBuilder.isFastPattern, patternBuilder.custom_function, patternBuilder.opacity, patternBuilder.auto, patternBuilder.smoothed_function);
        }

    }

    public class PatternBuilder {
        ColorProvider slot_1;
        ColorProvider slot_2;
        private final String pattern;

        boolean horizontal = true;
        boolean shiftPattern = false;

        Predicate<int[]> custom_function;
        Function<int[], Integer> smoothed_function;

        int from = 0;
        int to = 0;
        boolean auto = true;

        boolean isFastPattern = false;

        double opacity = 1.0;

        // PatternBuilder should only be created through the PatternBuilders class
        private PatternBuilder(ColorProvider slot_1, ColorProvider slot_2, String pattern) {
            this.slot_1 = slot_1; this.slot_2 = slot_2;
            this.pattern = pattern;
        }
        private PatternBuilder(ColorProvider slot_1, String pattern) {
            this(slot_1, colors.white(), pattern);
        }

        public PatternBuilder withHorizontal() {
            this.horizontal = true;
            return this;
        }

        public PatternBuilder withVertical() {
            this.horizontal = false;
            return this;
        }

        public PatternBuilder withShiftPattern(boolean shiftPattern) {
            this.shiftPattern = shiftPattern;
            return this;
        }
        public PatternBuilder withFromAndTo(int from, int to) {
            this.from = from; this.to = to;
            this.auto = false;
            return this;
        }
        public PatternBuilder withCustomFunction(Predicate<int[]> custom_function) {
            this.custom_function = custom_function;
            return this;
        }
        public PatternBuilder withSmoothedFunction(Function<int[], Integer> smoothed_function) {
            this.smoothed_function = smoothed_function;
            return this;
        }
        public PatternBuilder withOpacity(double opacity) {
            this.opacity = opacity;
            return this;
        }
        public PatternBuilder withAuto(boolean auto) {
            this.auto = auto;
            return this;
        }
        public PatternBuilder withIsFastPattern(boolean isFastPattern) {
            this.isFastPattern = isFastPattern;
            return this;
        }
        public Pattern build() {
            return new Pattern(this);
        }
        public Pattern b() {return  new Pattern(this);}
    }

    /**
     * Stores most used colors and helpful functions.
     * To get more control over pattern creation use.
     * {@code Bitmap.Pattern pattern = this.new Pattern()}
     * @see Color
     * @see Pattern
     */
    public class Colors {
        public final Color red() { return new Color(new int[]{255, 0, 0});}
        public final Color green() { return new Color(new int[]{0, 255, 0});}
        public final Color blue() { return new Color(new int[]{0, 0, 255});}

        public final Color yellow() { return new Color(new int[]{255, 255, 0});}
        public final Color purple() { return new Color(new int[]{255, 0, 255});}
        public final Color turquoise() { return new Color(new int[]{0, 255, 255});}
        public final Color orange() { return new Color(new int[]{255, 100, 0});}
        public final Color brown() { return new Color(new int[]{170, 80, 0});}
        public final Color pink() { return new Color(new int[]{255, 105, 180});}

        public final Color light_blue() { return mix(blue(), white()); }

        public final Color black() { return new Color(new int[]{0, 0, 0});}
        public final Color white() { return new Color(new int[]{255, 255, 255});}

        public final Color light_grey() { return new Color(new int[]{210, 210, 210});}
        public final Color grey() { return new Color(new int[]{140, 140, 140});}
        public final Color dark_grey() { return new Color(new int[]{70, 70, 70});}

        /**@return a transparent color_provider*/
        public final Pattern transparent() {return new Pattern(white(), 1.0);}

        /**
         * @return a list of (almost) all colors available
         */
        public final Color[] list() {
            return new Color[]{
                    red(), green(), blue(),
                    yellow(), purple(), turquoise(), orange(), brown(), pink(),
                    black(), white(), grey()
            };
        }

        /** Returns a random color_provider
         * @param true_random Whether the color_provider should be selected completely random or from a predefined list.
         * @return the random color_provider
         */
        public Color random (boolean true_random) {
            Random ran = new Random();
            Color color;
            if (true_random) {
                color = new Color(ran.nextInt(255), ran.nextInt(255), ran.nextInt(255));
            } else {
                Color[] list = list();
                color = list[ran.nextInt(list.length)];
            }
            return color; // Single exit point cause idk
        }


        /**
         * @return a random predefined color_provider
         */
        public Color random() {
            return random(false);
        }


        /** Merges two color_provider providers in a custom pattern.
         * @param slot_1 The first color_provider provider
         * @param slot_2 The second color_provider provider
         * @param custom_function A custom function <br>
         * @return the created pattern
         * @throws IllegalArgumentException if the color_provider providers are not of type Pattern or Color
         * @see Pattern#Pattern(ColorProvider, ColorProvider, Predicate)
         */
        public Pattern merge(ColorProvider slot_1, ColorProvider slot_2, Predicate<int[]> custom_function) {
            if (custom_function == null) {
                throw new IllegalArgumentException("custom_function needs to be set when using the \"custom\" fast pattern");
            } else {
                return new Pattern(slot_1, slot_2, custom_function);
            }


        }

        /** Merges the two color_provider providers in a custom fast pattern
         * @param slot_1 The first color_provider provider
         * @param slot_2 The second color_provider provider
         * @param fast_pattern The fast pattern
         * @return the created pattern
         * @throws IllegalArgumentException
         * @see Pattern#evaluateFastPattern(String)
         */
        public Pattern merge(ColorProvider slot_1, ColorProvider slot_2, String fast_pattern) {
            return new Pattern(slot_1, slot_2, fast_pattern);
        }

        /** Mixes two colors with a specified balance
         * @param color_1 The first integer array with color_provider information in rgb
         * @param color_2 The second integer array with color_provider information in rgb
         * @param balance a double between 0.0 (color_1) and 1.0 (color_2) representing the balance
         * @return a new Color object
         * @throws IllegalArgumentException if the balance is out of bounds
         */
        public Color mix(int[] color_1, int[] color_2, double balance) {
            if (balance > 1.0 || balance < 0.0) {
                throw new IllegalArgumentException("balance should be a value between 0.0 (color_1) and 1.0 (color_2)");
            }

            int[] merged_color = new int[3];
            for (int i = 0; i<3; i++) {
                merged_color[i] = (int) (color_1[i] * (1.0 - balance) + color_2[i] * balance);
            }
            return new Color(merged_color);
        }

        /**
         * @param color_1 The first color_provider
         * @param color_2 The second color_provider
         * @param balance a double between 0.0 (color_1) and 1.0 (color_2) representing the balance
         * @return a new Color object
         * @throws IllegalArgumentException if the balance is out of bounds
         */
        public Color mix(Color color_1, Color color_2, double balance) {
            return mix(color_1.color, color_2.color, balance);
        }

        /** Mixes both colors with a 50:50 balance
         * @see #mix(Color, Color, double)
         */
        public Color mix(Color color_1, Color color_2) { return mix(color_1, color_2, 0.5); }

        /** Adds a transparency to the object
         * @see Pattern#Pattern(ColorProvider, double)
         */
        public Pattern opacity(ColorProvider slot_1, double opacity) { return new Pattern(slot_1, opacity); }
        /**Makes the current object semi-transparent*/
        public Pattern opacity(ColorProvider slot_1) { return opacity(slot_1, 0.5); }

        /** Brightens up the selected color_provider by a selected balance
         * @param color The color_provider
         * @param balance A double between 0.0 (normal) and 1.0 (white)
         * @return The brighter color_provider
         */
        public Color brighten(Color color, double balance) {return  mix(color, this.white(), balance);}

        /** Brightens up the selected color_provider
         * @param color The color_provider
         * @return A brighter color_provider (50/50 mix with this.white())
         */
        public Color brighten(Color color) {return brighten(color, 0.5);}

        /** Converts the Color object into a Pattern
         * @param color the color_provider
         * @return the created "normal" pattern
         */
        public Pattern patternFromColor(Color color) { return new Pattern(color, colors.black(), "normal");}
    }


    /**
     * Stores methods for easier creation of PatternBuilders. <br>
     * To get a pattern from one of these methods, call .build()
     */
    public class PatternBuilders {

        public PatternBuilder opacity(ColorProvider slot_1, double opacity) { return new PatternBuilder(slot_1, "opacity").withOpacity(opacity); }
        public PatternBuilder opacity(ColorProvider slot_1) { return opacity(slot_1, 0.5); }
        public PatternBuilder gradient(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "gradient"); }
        // public PatternBuilder blur(double factor) {return new PatternBuilder(opacity(colors.white(), 1.0).build(), "blur").withOpacity(factor);}
        // public PatternBuilder blur() { return blur(0.5); }

        public PatternBuilder fastPattern(ColorProvider slot_1, ColorProvider slot_2, String fast_pattern) {return new PatternBuilder(slot_1, slot_2, fast_pattern).withIsFastPattern(true); }

        public PatternBuilder custom(ColorProvider slot_1, ColorProvider slot_2, Predicate<int[]> custom_function) { return new PatternBuilder(slot_1, slot_2, "custom").withCustomFunction(custom_function); }

        /**
         * @param function_cp The color_provider of the function (foreground)
         * @param background_cp The color_provider of the background
         * @param smoothed_function A Function that accepts a array with up to 4 Values. [x, y, from, to]
         * @return A PatterBuilder, build it with .build()
         */
        public PatternBuilder smoothedFunction(ColorProvider function_cp, ColorProvider background_cp, Function<int[], Integer> smoothed_function) { return new PatternBuilder(function_cp, background_cp, "smoothed_function").withSmoothedFunction(smoothed_function).withVertical(); }

        public PatternBuilder gridVariants(ColorProvider slot_1, ColorProvider slot_2, int factor) { return new PatternBuilder(slot_1, slot_2, "custom").withCustomFunction(arr ->  arr[0] * arr[1] % (factor) == 0); }
        public PatternBuilder cellsVariants(ColorProvider slot_1, ColorProvider slot_2, int factor) { return new PatternBuilder(slot_1, slot_2, "custom").withCustomFunction(arr ->  (int) (Math.PI * arr[0] * arr[1]) % (factor) == 0); }
        public PatternBuilder circleBorderVariants(ColorProvider border_cp, ColorProvider background_cp, int spread) { return new PatternBuilder(border_cp, background_cp, "custom").withCustomFunction(arr ->  (int) (Math.sqrt(arr[0] * arr[1])) % spread == 0); }

        public PatternBuilder aslantStripes(ColorProvider slot_1, ColorProvider slot_2, int spread) { return new PatternBuilder(slot_1, slot_2, "custom").withCustomFunction(arr ->  (arr[0] + arr[1]) % (spread) == 0); }

        public PatternBuilder normal(ColorProvider slot_1) { return new PatternBuilder(slot_1, "normal"); }
        public PatternBuilder grid(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "grid"); }
        public PatternBuilder stripes(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "stripes"); }
        public PatternBuilder checkerboard(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "checkerboard"); }
        public PatternBuilder cells(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "cells"); }
        public PatternBuilder bigcells(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "bigcells"); }
        public PatternBuilder space(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "space"); }
        public PatternBuilder dotgrid(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "dotgrid"); }
        public PatternBuilder biggrid(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "biggrid"); }
        public PatternBuilder hugegrid(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "hugegrid"); }
        public PatternBuilder superhugegrid(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "superhugegrid"); }
        public PatternBuilder flowergrid(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "flowergrid"); }
        public PatternBuilder dotlines(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "dotlines"); }
        public PatternBuilder wave(ColorProvider slot_1, ColorProvider slot_2) { return new PatternBuilder(slot_1, slot_2, "wave"); }


    }


    /**
     * Class to store and to create outline elements
     */
    public class Outline {
        boolean active; int thickness; ColorProvider color_provider;

        /**
         * @param active Whether the outline is active or not
         * @param thickness The thickness of the outline in pixels
         * @param color_provider The color_provider of the outline (Pattern / Color)
         */
        public Outline (boolean active, int thickness, ColorProvider color_provider) {
            this.active = active;
            this.thickness = thickness;
            this.color_provider = color_provider;
        }
        /**@see #Outline(boolean, int, ColorProvider) **/
        public Outline (boolean active) {
            this(active, 1, colors.white());
        }

        /**@see #Outline(boolean, int, ColorProvider) **/
        public Outline (int thickness, ColorProvider color_provider) {
            this(true, thickness, color_provider);
        }

        /**@see #Outline(boolean, int, ColorProvider) **/
        public Outline (int thickness) { this(true, thickness, colors.white());}

        /**@see #Outline(boolean, int, ColorProvider) **/
        public Outline (ColorProvider color_provider) { this(true, 1, color_provider);}

        /**@see #Outline(boolean, int, ColorProvider) **/
        public Outline () {this(true, 1, colors.white());}
    }

    /** Class that stores methods to create shapes
     * currently cross, circle and tree are available
     */
    public class Shapes {


        /** Method to create a cross at a specified position
         * @param pos_x The x-coordinate
         * @param pos_y The y-coordinate
         * @param size The size in pixel (diameter is size * 2)
         * @param thickness The thickness of the cross
         * @param color_provider The color_provider p
         * @param outline
         */
        public void cross(int pos_x, int pos_y, int size, int thickness, ColorProvider color_provider, Outline outline){
            Pattern[] lockedPatterns = setAutoPattern(pos_x - size, pos_x + size, pos_y - size, pos_y + size, color_provider, true);
            if (outline != null) setAutoPattern(pos_x - size, pos_x + size, pos_y - size, pos_y + size, outline.color_provider, true);

            thickness = thickness / 2;
            size = size / 2;
            fillArea(pos_x - size, pos_y - thickness, pos_x + size, pos_y + thickness, color_provider, outline);
            fillArea(pos_x - thickness, pos_y - size, pos_x + thickness, pos_y + size, color_provider, outline);
            fillArea(pos_x - size, pos_y - thickness, pos_x + size, pos_y + thickness, color_provider);
            unlockAutoPattern(lockedPatterns);
        }

        // *** Overloads ***
        public void cross(int pos_x, int pos_y, int size, int thickness, ColorProvider color) {
            cross(pos_x, pos_y, size, thickness, color, new Outline(false));
        }
        public void cross(int pos_x, int pos_y, int size, int thickness, Outline outline) {
            cross(pos_x,pos_y, size, thickness, colors.black(), outline);
        }
        public void cross(int pos_x, int pos_y, int size, int thickness) {
            cross(pos_x, pos_y, size, thickness, colors.black(), new Outline(false));
        }


        /**
         * @param pos_x of the circle
         * @param pos_y of the circle
         * @param radius of the circle
         * @param color_provider a color_provider provider for the body, can be either a Pattern or a Color
         * @param borderclip whether the circle should raise an exception if the accessed pixel is outside the canvas
         * @param outline a border object, set to null if unwanted
         * @throws IllegalArgumentException when borderclip is set to false and the accessed pixel is outside the canvas
         */
        public void circle(int pos_x, int pos_y, int radius, ColorProvider color_provider, boolean borderclip, Outline outline) {

            //Gradient auto
            setAutoPattern(pos_x-radius, pos_x+radius, pos_y-radius, pos_y+radius, color_provider, false);
            if (outline != null && outline.active) circle(pos_x, pos_y, radius+outline.thickness, outline.color_provider, borderclip);
            // if (outline != null) setAutoPattern(pos_x-radius, pos_x+radius, pos_y-radius, pos_y+radius, outline, false);

            for (int y = pos_y-radius; y < pos_y+radius; y++) {
                for (int x = pos_x-radius; x < pos_x+radius; x++) {
                    int equation = (int) (Math.pow(radius, 2) - (Math.pow(pos_x - x, 2) + Math.pow(pos_y-y, 2)));

                    if (equation > 0) {
                        try {
                            changePixel(x, y, color_provider);
                        } catch (Exceptions.PixelOutOfBoundsException e) {
                            if (borderclip) throw e;
                        }
                    }
                }
            }
        }

        // *** Overloads ***
        public void circle(int pos_x, int pos_y, int radius, ColorProvider color_provider, Outline outline) {circle(pos_x, pos_y, radius, color_provider, false, outline);}
        public void circle(int pos_x, int pos_y, int radius, ColorProvider color_provider, ColorProvider outline_color) {circle(pos_x, pos_y, radius, color_provider, false, new Outline(outline_color));}
        public void circle(int pos_x, int pos_y, int radius, ColorProvider color_provider) {circle(pos_x, pos_y, radius, color_provider, (Outline) null);}
        public void circle(int pos_x, int pos_y, int radius, ColorProvider color_provider, boolean borderclip) {circle(pos_x, pos_y, radius, color_provider, borderclip, null);}


        public void triangle(int pos_x, int pos_y, int size, ColorProvider color_provider, ColorProvider outline_color_provider) {
            System.out.println("NOT IMPLEMENTED");
            setAutoPattern(pos_x-size, pos_x+size, pos_y-size, pos_y+size, color_provider, false);

            for (int y = pos_y-size; y < pos_y+size; y++) {
                for (int x = pos_x-size; x < pos_x+size; x++) {
                    break;
                }
            }
        }

        public void tree(int x_pos, int y_pos, double size) {
            lineV(y_pos, y_pos + (int) (22 * size), x_pos, colors.brown(), (int) (8 * size));
            shapes.circle(x_pos + ((int) (8 * size))/2, y_pos + (int) (22 * size), (int) (18 * size), colors.green().setAlpha(0.5), false);
            shapes.circle(x_pos + ((int) (8 * size))/2, y_pos + (int) (37 * size), (int) (14 * size), colors.green().setAlpha(0.6), false);
            shapes.circle(x_pos + ((int) (8 * size))/2, y_pos + (int) (50 * size), (int) (10 * size), colors.green().setAlpha(0.7), false);
        }
    }


    public static class Exceptions {
        // A RuntimeException (unchecked) does not need to be handled (must not be specified in method signature)
        // More about the differences: https://jaxenter.de/java-trickkiste-von-checked-und-unchecked-exceptions-1350

        /**
         * Is thrown when the accessed pixel is outside the canvas
         */
        public static class PixelOutOfBoundsException extends RuntimeException {
            public PixelOutOfBoundsException (String msg) {
                super(msg);
            }
        }
    }

    // *** Overloads ***

    /**@see #fillArea(int, int, int, int, ColorProvider, Outline, boolean)  */
    public void fillArea(int x_p1, int y_p1, int x_p2, int y_p2, ColorProvider color_provider, Outline outline) {
        fillArea(x_p1, y_p1, x_p2, y_p2, color_provider, outline, true);
    }

    /**@see #fillArea(int, int, int, int, ColorProvider, Outline, boolean)  */
    public void fillArea(int x_p1, int y_p1, int x_p2, int y_p2, ColorProvider color_provider) {
        fillArea(x_p1, y_p1, x_p2, y_p2, color_provider, new Outline(false), true);
    }

    /**
     * @see #border(int, int, int, int, ColorProvider)
     */
    public void border(int x_p1, int y_p1, int x_p2, int y_p2, ColorProvider color_provider) {
        border(x_p1, y_p1, x_p2, y_p2, 1, color_provider);
    }

    public void fillWin() {
        fillWin(colors.black());
    }

    /**@see #lineH(int, int, int, ColorProvider, int) */
    public void lineH(int x_from, int x_to, int y_pos) {lineH(x_from, x_to, y_pos, colors.black(), 1);} 
    /**@see #lineH(int, int, int, ColorProvider, int) */
    public void lineH(int x_from, int x_to, int y_pos, int thickness) {lineH(x_from, x_to, y_pos, colors.black(), thickness);}
    /**@see #lineH(int, int, int, ColorProvider, int) */
    public void lineH(int y_pos, ColorProvider color_provider) {lineH(1, canvas_width, y_pos, color_provider, 1);}
    /**@see #lineH(int, int, int, ColorProvider, int) */
    public void lineH(int y_pos, ColorProvider color_provider, int thickness) {lineH(1, canvas_width, y_pos, color_provider, thickness);}
    /**@see #lineH(int, int, int, ColorProvider, int) */
    public void lineH(int y_pos) {lineH(y_pos, colors.black());}

    /**@see #lineV(int, int, int, ColorProvider, int) */
    public void lineV(int y_from, int y_to, int x_pos) {lineV(y_from, y_to, x_pos, colors.black(), 1);}
    /**@see #lineH(int, int, int, ColorProvider, int) */
    public void lineV(int y_from, int y_to, int x_pos, int thickness) {lineV(y_from, y_to, x_pos, colors.black(), thickness);}
    /**@see #lineH(int, int, int, ColorProvider, int) */
    public void lineV(int x_pos, ColorProvider color_provider) {lineV(1, canvas_height, x_pos, color_provider, 1);}
    /**@see #lineH(int, int, int, ColorProvider, int) */
    public void lineV(int x_pos, ColorProvider color_provider, int thickness) {lineV(1, canvas_height, x_pos, color_provider, thickness);}
    /**@see #lineH(int, int, int, ColorProvider, int) */
    public void lineV(int x_pos) {lineV(x_pos, colors.black());}

    // This class is only extended to unify method parameters as "ColorProvider"
    public abstract static class ColorProvider { }

}
