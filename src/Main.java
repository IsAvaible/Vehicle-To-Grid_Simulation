import Services.Animations;
import Services.Bitmap;
import Services.Electricity_Usage_Visualizer;
import Services.Out;

import java.util.Random;
import java.util.function.Predicate;

public class Main {

    public static void main(String[] args) {
        Grid grid = new Grid(new Electricity_Grid.Distribution(70, 50)); // A Grid object, each Electric_Car needs to be registered into one

        Electric_Car my_e_car = Models.Tesla_Model_S("Simon Felix Conrad", "Rüd | SC | 888", grid); // Creates a new Tesla Model S
        Electric_Car other_e_car = Models.VW_eUp("Hans Peter", "Wi | HP | 193", grid, Out.Log_Level.ERROR); // By setting the log level, the vehicle will only report higher priority messages.

        V2G_Unit personal_charger = new V2G_Unit(150, grid);
        V2G_Unit public_charger = new V2G_Unit(150, grid);

        my_e_car.charging_profile = Electric_Car.ChargingProfile.DEFAULT_PROFILE;
        my_e_car.drive(200);

        other_e_car.charging_profile = Electric_Car.ChargingProfile.WORK_PROFILE;
        other_e_car.drive(100);

        grid.operate(200);

        other_e_car.connectToV2GUnit(public_charger);

        grid.operate(60);
        grid.manage_cars();

        my_e_car.drive(300);
        grid.operate(210);
        grid.printTime();


//        Electricity_Usage_Visualizer electricity_usage_visualizer = new Electricity_Usage_Visualizer(grid.time);
//        electricity_usage_visualizer.draw();


        // Animation with bitmap
        //Animations.appear();
        //Animations.drive();
        //Animations.charge(false);
    }

}
