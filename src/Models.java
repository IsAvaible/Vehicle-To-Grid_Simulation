import Services.Out;

import java.util.Random;

/**
 * Derivative class of Electric_Car.java to provide predefined car models.
 */
public class Models {

    public static Electric_Car Random (String owner, String license_plate, Grid grid, Out.Log_Level log_level) {
        // Generate a random index between 0 and 2
        int index = new Random().nextInt(3);
        Electric_Car random_model = switch (index) {
            case 0 -> Tesla_Model_S(owner, license_plate, grid, log_level);
            case 1 -> VW_eUp(owner, license_plate, grid, log_level);
            case 2 -> Renault_Zoe(owner, license_plate, grid, log_level);
            default -> throw new IllegalStateException("Unexpected value: " + index);
        };
        // Get the Electric_Car by running the matching function
        // Return the random Electric_Car
        return random_model;
    }

    public static Electric_Car Tesla_Model_S (String owner, String license_plate, Grid grid, Out.Log_Level log_level) {
        return new Electric_Car(
                owner,
                license_plate,
                670,
                "Tesla",
                "Model S",
                85.0,
                18.9,
                225.0,
                grid,
                log_level
        );
    }

    public static Electric_Car VW_eUp (String owner, String license_plate, Grid grid, Out.Log_Level log_level) {
        return new Electric_Car(
                owner,
                license_plate,
                83,
                "Volkswagen",
                "e-up!",
                18.7,
                12.9,
                3.6,
                grid,
                log_level
        );
    }

    public static Electric_Car Renault_Zoe (String owner, String license_plate, Grid grid, Out.Log_Level log_level) {
        return new Electric_Car(
                owner,
                license_plate,
                135,
                "Renault",
                "Zoe",
                52.0,
                13.7,
                22.0,
                grid,
                log_level
        );
    }

    // ## Overloads with default parameter values
    public static Electric_Car Random(String owner, String license_plate, Grid grid) {
        return Random(owner, license_plate, grid, Out.Log_Level.ALL);
    }

    public static Electric_Car Tesla_Model_S(String owner, String license_plate, Grid grid) {
        return Tesla_Model_S(owner, license_plate, grid, Out.Log_Level.ALL);
    }
    public static Electric_Car VW_eUp(String owner, String license_plate, Grid grid) {
        return VW_eUp(owner, license_plate, grid, Out.Log_Level.ALL);
    }
    public static Electric_Car Renault_Zoe(String owner, String license_plate, Grid grid) {
        return Renault_Zoe(owner, license_plate, grid, Out.Log_Level.ALL);
    }
}
