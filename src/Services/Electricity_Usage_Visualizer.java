package Services;

import java.util.function.Predicate;

public class Electricity_Usage_Visualizer {

    final Bitmap bm;
    final Bitmap.Colors cols;
    final Bitmap.PatternBuilders pbs;
    final Time global_time;

    public Electricity_Usage_Visualizer(Time global_time) {
        bm = new Bitmap(60*24, 1000,"Electricity_Usage", false);
        cols = bm.cols;
        pbs = bm.pbs;
        this.global_time = global_time;
    }

    public void draw() {
        drawCanvas();
        drawGraph();
        // drawCars();
        bm.render();
    }

    private void drawGraph() {
        // ...
    }

    private void drawCars() {
        // Driving
        carSprite(40, 1, cols.dark_grey());
    }

    private void carSprite(int pos_x, int pos_y, Bitmap.ColorProvider bodyColor) {
        Bitmap.Outline wheelGripColor = bm.new Outline(2, pbs.checkerboard(cols.dark_grey(), cols.black()).withShiftPattern(pos_x % 2 == 0).b());
        Bitmap.Outline bodySideColor = bm.new Outline(2, cols.merge(cols.light_grey(), cols.black(), "checkerboard>"));
        Bitmap.Pattern windowColor = cols.merge(cols.blue(), cols.white(), "gradientH=auto");
        // Body
        bm.shapes.circle(pos_x, pos_y + 41, 30, bodyColor, false, bodySideColor);
        // Wheels
        bm.shapes.circle(pos_x - 27, pos_y + 13, 12, cols.black(), false, wheelGripColor);
        bm.shapes.circle(pos_x + 27, pos_y + 13, 12, cols.black(), false, wheelGripColor);
        // Window
        bm.shapes.circle(pos_x+12, pos_y + 52, 10, windowColor, bm.new Outline(cols.white()));
    }



    private void drawCanvas() {
        var current_time = global_time.inHoursIsolated() * 60;
        var background_side_color = cols.dark_grey().setAlpha(0.5);
        var background_center_color= cols.light_blue().setAlpha(0.7);
        // Background Gradient
        bm.fillArea(
                1, 1, bm.canvas_width/2, bm.canvas_height,
                pbs.gradient(background_side_color, background_center_color).withVertical().build()
                );
        bm.fillArea(
                bm.canvas_width/2 + 1, 1, bm.canvas_width, bm.canvas_height,
                pbs.gradient(background_center_color, background_side_color).withVertical().build()
        );
        // Hour Markers
        Predicate<int[]> line_function = arr -> arr[0] % 20 == 0;
        Predicate<int[]> current_hour_line = arr -> arr[0] % 7 == 0;
        for (int x = bm.canvas_width/24+1; x < bm.canvas_width; x+=bm.canvas_width/24) {
            bm.lineV(x, pbs.custom(cols.light_grey(), cols.transparent(), line_function).build());
        }
        bm.lineV(current_time + global_time.inMinutesIsolated() + 1, pbs.custom(cols.light_blue(), cols.transparent(), current_hour_line).build());
        // Baseline
        line_function = arr -> (arr[1]-1) % 10 == 0;
        bm.lineH(500, pbs.custom(cols.white(), cols.transparent(), line_function).build());
    }

}
