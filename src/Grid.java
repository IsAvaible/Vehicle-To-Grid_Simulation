import Services.ASCII_sprites;
import Services.Out;
import Services.Time;

import java.util.*;

/**
 * The central class of the simulation. The grid stores assigned electric cars and v2g units.
 * It is responsible for power management and the time dimension of the simulation and thus also for the continuation
 * of trips and loading processes.
 * @see Electric_Car
 * @see V2G_Unit
 * @see Electricity_Grid
 */
public class Grid implements Out {
    // Similar to Python dic / stores all electric cars / v2g units with their corresponding ids
    Hashtable<String , Electric_Car> electric_cars = new Hashtable<>();
    Hashtable<String , V2G_Unit> v2g_units = new Hashtable<>();

    final Time time = new Time("01-00:00"); // The time object of the current grid
    private int tick_speed = 1; // How many minutes should be passed per tick

    // A Distribution object which is used to calculate the electricity snapshot every day
    public Electricity_Grid.Distribution energy_distribution;

    // A object of the electricity grid class, responsible for providing consumption and production
    private final Electricity_Grid electricity_grid = new Electricity_Grid();
    private Electricity_Grid.Electricity_Snapshot electricity_snapshot;

    /**
     * Generates a new Grid object.
     * @param energy_distribution Specifies the distribution of energy sources used by the underlying electricity grid.
     */
    Grid(Electricity_Grid.Distribution energy_distribution) {
        System.out.println(ASCII_sprites.computer);
        this.energy_distribution = energy_distribution;
    }

    /** Registers the electric_car in the grid and returns a unique ID
     * @param electric_car the electric_car
     * @return the ID
     */
    public String register(Electric_Car electric_car) {
        String id = generate_id_recursive(true); // Generate a unique ID

        electric_cars.put(id, electric_car); // Add the car to the Hashtable ("register it")
        return id;
    }

    /** Registers the v2g_unit in the grid and returns a unique ID
     * @param v2g_unit the v2g_unit
     * @return the ID
     */
    public String register(V2G_Unit v2g_unit) {
        String id = generate_id_recursive(false); // Generate a unique ID

        v2g_units.put(id, v2g_unit); // Add the V2g_Unit to the Hashtable ("register it")
        return id;
}

    /** Generates a unique id
     * @param for_car Whether the ID should be generated for a car (true) or for a V2G_Unit (false)
     * @return id : String | A unique ID
     */
    private String generate_id_iterative(boolean for_car) {
        boolean id_is_not_unique;
        String id;

        do { // First ever use case of a do while loop
            // Generates a random alpha-numeric String, deletes the "-" sign and extracts a substring with a length of 8
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            // The id is not unique if there is element in the dic assigned to the key
            id_is_not_unique = (for_car ? electric_cars : v2g_units ).get(id) != null;

        } while (id_is_not_unique);

        return id;
    }

    /** Generates a unique id, but it's recursive
     * @param for_car Whether the ID should be generated for a car (true) or for a V2G_Unit (false)
     * @return id : String | A unique ID
     */
    private String generate_id_recursive(boolean for_car) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return (for_car ? electric_cars : v2g_units ).get(id) == null ? id : generate_id_recursive(for_car);
    }


    /** Operates the grid (this) for a specified amount of time
     * @param minutes How many minutes the grid should operate
     */
    public void operate(int minutes) {
        // TODO: 2/11/2021 Add operate code
        int start_time = time.inMinutes();
        while (time.inMinutes() < start_time + minutes) {
            tick();
        }
    }

    /** Executes actions for the selected time frame equaling the tick speed
     *
     */
    private void tick() {
        // TODO: 2/11/2021 Add tick Code
        if (time.inMinutesWithHoursIsolated() == 0) { // Print the weather on a new day
            electricity_snapshot = electricity_grid.getSnapshot(energy_distribution);
            print(String.format("Day %s: %s", time.inDaysIsolated(), electricity_snapshot.weather.toString()) , Log_Level.INFO);
        }
        if (time.inMinutesIsolated() == 0) { // Print the time each hour
            printTime();
        }
        manage_cars();

        time.addTime(tick_speed);
    }
    
    public void printTime() {
        print(String.format("%s", time.asString().substring(3)), Log_Level.INFO);
    }

    public void printV2GUnits() {
        StringBuilder disconnected = new StringBuilder();
        StringBuilder connected = new StringBuilder();
        for (V2G_Unit v2g_unit : v2g_units.values()) {
            if (v2g_unit.isConnected()) {
                connected.append(v2g_unit.grid_id).append(", ");
            } else {
                disconnected.append(v2g_unit.grid_id).append(", ");
            }
        }
        if (disconnected.length() == 0 && connected.length() == 0) {
            print("No V2G units are available (Not yet initialized?)", Log_Level.ERROR);
        } else if (disconnected.length() == 0) {
            print(String.format("No V2G unit is currently available.", connected), Log_Level.WARN);
        } else {
            print(String.format("The V2G units with ID's [%s] are ready to be used.", disconnected.substring(0, disconnected.length() - 2)), Log_Level.INFO);
        }
    }

    /** Set the tickspeed of the grid (How many minutes are passed each tick)
     * @param tick_speed The desired tickspeed. Needs to be greater than 0.
     */
    public void setTick_speed(int tick_speed) {
        if (tick_speed >= 1) {
            this.tick_speed = tick_speed;
        }  else {
            throw new IllegalArgumentException("Tick speed needs to be greater than 0.");
        }
    }

    public int getTick_speed() {
        return tick_speed;
    }

    /**
     * Calculates charging priorities for each car that is connected to a V2G Unit and
     * determines which one should be charged / discharged to stabilize the Grid.
     */
    public void manage_cars() {
        // This Hashtable stores each charging priority and all cars that have it
        Hashtable<Electric_Car.ChargingPriority, ArrayList<Electric_Car>> priority_table = new Hashtable<>();
        // Initializing the Hashtable
        for (Electric_Car.ChargingPriority chargingPriority : Electric_Car.ChargingPriority.values()) {
            priority_table.put(chargingPriority, new ArrayList<>());
        }

        // Iterate through each car and do actions according to state
        for (Electric_Car electric_car : electric_cars.values()) {
            switch (electric_car.state.getDescriptor()) {
                case DRIVING -> electric_car.tick_drive();
                case BEING_TOWED -> electric_car.tick_be_towed();
                case CONNECTED_CHARGING, CONNECTED_DISCHARGING, CONNECTED_IDLE -> priority_table.get(electric_car.get_charging_priority()).add(electric_car);
                default -> { }
            }
        }

        // System.out.println(priority_table);

        // Discharge if energy is needed else charge
        ArrayList<Electric_Car> electric_cars = new ArrayList<>();
        boolean production_deficit = electricity_snapshot.consumption[time.inMinutesWithHoursIsolated()] > electricity_snapshot.production[time.inMinutesWithHoursIsolated()];

        if (production_deficit) { // Consumption is higher than the production
            // System.out.println("Production Deficit");
            for (var priority : Electric_Car.ChargingPriority.values()) {
                if (priority_table.get(priority) != null && priority != Electric_Car.ChargingPriority.IMMEDIATELY) {
                    electric_cars.addAll(priority_table.get(priority));
                }
            }
        } else {
            // System.out.println("Consumption Deficit");
            for (var priority : Electric_Car.ChargingPriority.values()) {
                if (priority_table.get(priority) != null) {
                    electric_cars.addAll(priority_table.get(priority));
                }
            }
            Collections.reverse(electric_cars); // Reverse to bring higher priority vehicles to the front.
        }

        // print(priority_table.toString(), Log_Level.INFO);


        // TODO: 4/3/2021 Create a list / hashmap that stores each car and it's charging priority to calculate who gets the energy | Done
        // TODO: 4/3/2021 Distributed energy must be pulled from the grid (hint: charge method returns used energy)
        // TODO: 4/3/2021 Pull energy from cars with a low priority based on the current network stability
        // Creating a dictionary with

    }

    /**
     * Overload of the inherited print method.
     * @see Out#print(String, String, String, Log_Level, Log_Level)
     */
    private void print(String out, Log_Level log_level) {
        Out.print("Grid", "Master", out, Log_Level.ALL, log_level);
    }

}
