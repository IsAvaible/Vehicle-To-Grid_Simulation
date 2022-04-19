import Services.Out;
import Services.Time;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;

/**
 * Class to instantiate a electric car. Primarily developed for V2G simulations. <br>
 * Created by Simon Conrad © 2021
 */
public class Electric_Car extends Car implements Out {

    private double charge_status = 100.0; // The charging status as percentage | 100%

    public final double energy_consumption; // The energy consumption in kWh/100km.
    private final double capacity; // The capacity of the car in kWh
    public final double max_charge_rate; // The maximal charge rate of the vehicle in kW

    public double speed = 90; // The speed of the vehicle in km/h

    public final String grid_id; // The unique identifier in the current grid

    V2G_Unit v2g_unit; // The current charger, null when none is connected

    public Log_Level log_level; // The log level of this.

    private final Grid operating_grid; // The grid in which the vehicle operates

    public ChargingProfile charging_profile = ChargingProfile.DEFAULT_PROFILE; // The charging profile of this. (Determines the charge and discharge behaviour)

    private final Time global_time; // The Time object of the grid

    final State state = new State(); // The state of the vehicle, is planned to be used in a future GUI

    /** Constructor for the Electric_Car class. Predefined car-models are available through the Models.java class.
     * ^ : has impact on the car's behavior, * : just for design purposes
     * @param owner * The owner of the vehicle.
     * @param license_plate * The license plate of the vehicle.
     * @param ps * The horsepower of the vehicle. | e.g. 160
     * @param brand * The brand of the vehicle. | e.g. Tesla
     * @param capacity ^ The maximum capacity in kWh. | e.g. 85.0
     * @param energy_consumption ^ The energy consumption in kWh/100km. | e.g. 20.3
     * @param max_charge_rate ^ The maximal charge rate of the vehicle in kW | e.g. 225
     * @param grid ^ The current V2G grid in which the vehicle should operate.
     * @param log_level ^ The log level of this.
     */
    Electric_Car(String owner, String license_plate, int ps, String brand, String model_name , double capacity, double energy_consumption, double max_charge_rate, Grid grid, Log_Level log_level) {
        super(ps, brand, model_name, owner, license_plate);

        this.capacity = capacity;
        this.energy_consumption = energy_consumption;
        this.max_charge_rate = max_charge_rate;
        this.grid_id = grid.register(this);
        this.global_time = grid.time;
        this.operating_grid = grid;

        this.log_level = log_level;
    }

    /** [Old drive method]
     * Tries to drive for specified km, returns false if the energy was insufficient.
     * @param route_length in km
     * @return boolean successful : Whether the car reached it's destination
     */
    private boolean out_of_sync_drive(double route_length) {
        double remaining_capacity = calculateRouteConsumption(route_length);
        boolean successful;
        if (v2g_unit != null) {
            print("Car cannot be driven while still being connected to a V2G unit.", Log_Level.ERROR);
            successful = false;
        } else if (remaining_capacity < 0) {
            print(String.format("Drove %s km.", this.capacity * (this.charge_status / 100) / (energy_consumption / 100)), Log_Level.WARN);
            print("Car was discharged before reaching the destination and needs to be recharged, please call a towing service.", Log_Level.ERROR);
            this.charge_status = 0.0;

            state.setDescriptor(State.Descriptor.IDLE);
            successful = false;
            // I'm considering using exceptions, but they seem not to be as user friendly in a complex network:
            // throw new Exceptions.EmptyBatteryException("Please charge your car now.");
        } else {
            print(String.format("Drove %s km.", route_length), Log_Level.INFO);
            this.charge_status = (remaining_capacity / capacity) * 100;
            printChargeLevel();

            state.setDescriptor(State.Descriptor.IDLE);
            successful = true;
        }
        return successful;
    }

    /** Initiates the driving state for a specified route length
     * @param route_length The length of the route
     * @return successful : boolean | Whether the initialisation was successful.
     */
    @Override
    public boolean drive(double route_length) {
        boolean successful;
        if (state.descriptor != State.Descriptor.IDLE) {
            print("Vehicle must be in idle before attempting to drive.", Log_Level.ERROR);
            successful = false;
        } else {
            print(String.format("Started route with %skm. This will take approximately %s minutes.", route_length, calculateRouteDuration(route_length).inMinutes()), Log_Level.INFO);
            state.setDriving_descriptor(route_length);
            successful = true;
        }

        return successful;
    }

    /** Validates whether the route_length is possible
     * @param route_length : double | The traveled distance
     * @return possible : boolean
     */
    private boolean apply_route_consumption(double route_length) {
        boolean possible;
        double remaining_capacity = calculateRouteConsumption(route_length);
        // If the remaining route length is still inside the capacity spectrum the method will still return true
        boolean big_tick_speed_prevention = calculateRouteConsumption(state.remaining_route_length) >= 0;
        if (remaining_capacity >= 0.0 || big_tick_speed_prevention) {
            // Capacity is sufficient
            this.charge_status = (remaining_capacity / capacity) * 100;
            possible = true;
        } else {
            // Capacity is insufficient
            print("Vehicle was discharged before reaching the destination and needs to be recharged, a towing service was automatically called.", Log_Level.WARN);
            this.charge_status = 0.0;
            state.setBeing_towed_descriptor();
            possible = false;
        }
        return possible;
    }

    /**
     * Drives for the length of one tick.
     */
    void tick_drive() {
        if (state.getDescriptor() != State.Descriptor.DRIVING) throw new IllegalArgumentException("tick_drive() called whilst not driving");

        int tick_speed = operating_grid.getTick_speed();
        double traveled_distance = (speed/60) * tick_speed;
        if (state.remaining_route_length - traveled_distance > 0.0) {
            if (!apply_route_consumption(traveled_distance)) return;
            state.remaining_route_length = state.remaining_route_length - traveled_distance;
        } else {
            if (!apply_route_consumption(traveled_distance)) return;
            traveled_distance += (state.remaining_route_length - traveled_distance);

            print(String.format("Drove %skm.", state.total_route_length), Log_Level.INFO);
            printChargeLevel();

            state.remaining_route_length = -1;
            state.total_route_length = -1;
            state.descriptor = State.Descriptor.IDLE;
        }
    }

    /**
     * Advances the towed state for the length of one tick.
     */
    void tick_be_towed() {
        if (state.getDescriptor() != State.Descriptor.BEING_TOWED) throw new IllegalArgumentException("tick_be_towed() called whilst not being towed");
        // Time penalty : 30min / Tower speed : 70 km/h
        int tick_speed = operating_grid.getTick_speed();
        if (state.remaining_time_penalty > 0) state.remaining_time_penalty  = state.remaining_time_penalty - tick_speed <= 0 ? -1 : state.remaining_time_penalty - tick_speed;
        else {
            double traveled_distance = 70.0 / 60 * operating_grid.getTick_speed();
            if (state.remaining_route_length - traveled_distance > 0) state.remaining_route_length = state.remaining_route_length - traveled_distance;
            else {
                print(String.format("Vehicle has reached it's destination with the towing service. Drove %skm.", state.total_route_length), Log_Level.INFO);
                printChargeLevel();
                state.remaining_route_length = -1;
                state.total_route_length = -1;
                state.descriptor = State.Descriptor.IDLE;

            }
        }


    }

    /** Calculates the energy consumption for a specified route_length as a negative %
     * @param route_length in km | e.g. 200
     * @return The energy consumption in % as double | e.g. -74.2
     */
    public double calculateRouteConsumption(double route_length) {
        double overall_consumption = route_length * (energy_consumption / 100);

        return capacity * (charge_status / 100.0) - overall_consumption;
    }

    /** Calculates the route duration
     * @param route_length The length of the route
     * @return duratinon : Time | A Time object containing the duration
     */
    public Time calculateRouteDuration(double route_length) {
        Time duration = new Time();
        int minutes = (int) (route_length / (90.0/60));
        duration.addTime(minutes);
        return duration;
    }

    /** Connects the electric car to the v2g_unit
     * @param v2g_unit a object of the V2G_Unit class
     * @return successful : boolean
     */
    public boolean connectToV2GUnit(V2G_Unit v2g_unit) {
        boolean successful;
        if (this.v2g_unit == null && state.descriptor == State.Descriptor.IDLE) {
            successful = v2g_unit.connectElectricCar(this);
            if (!successful) {
                operating_grid.printV2GUnits();
            } else {
                state.setDescriptor(State.Descriptor.CONNECTED_IDLE);
            }
        } else if (Set.of(State.Descriptor.CONNECTED_CHARGING, State.Descriptor.CONNECTED_DISCHARGING, State.Descriptor.CONNECTED_IDLE).contains(state.descriptor)) {
            print(String.format("Can't connect to new V2G_Unit while unit with ID %s is still connected.", v2g_unit.grid_id), Log_Level.ERROR);
            successful = false;
        } else {
            print(String.format("Cannot connect to V2G unit because the vehicle is currently in state %s and not IDLE.", state.descriptor.toString()), Log_Level.ERROR);
            successful = false;
        }
        return successful;
    }

    /** Disconnects the electric car from the v2g_unit
     * @return successful : boolean
     */
    public boolean disconnectFromV2GUnit() {
        boolean successful;
        if (this.v2g_unit != null) {
            successful = v2g_unit.disconnectElectricCar(this);
            state.setDescriptor(State.Descriptor.IDLE);
        } else {
            print("Disconnection was unsuccessful: No V2G unit is connected.", Log_Level.ERROR);
            successful = false;
        }
        return successful;
    }

    /**
     * Outputs the charge level in a user friendly format.
     */
    public void printChargeLevel() {
        double remaining_km = this.capacity * (this.charge_status / 100) / (energy_consumption / 100);
        String recommend;
        Log_Level message_log_level;

        if (charge_status > 50.0) {
            recommend = "Have a nice day!";
            message_log_level = Log_Level.INFO;
        } else if (charge_status > 20.0) {
            recommend = "Please consider the remaining capacity when planning your route.";
            message_log_level = Log_Level.WARN;
        } else {
            recommend = "Please charge your car now.";
            message_log_level = Log_Level.WARN;
        }

        print(String.format("The current charge level is %s%%, you will be able to drive for %skm. %s", Math.round(charge_status), Math.round(remaining_km), recommend), message_log_level);
    }

    public double getCharge_status() {
        return this.charge_status;
    }
    void setCharge_status(double charge_status) {
        this.charge_status = charge_status;
    }

    public double getCapacity() {
        return this.capacity;
    }

    /**
     * Overload of the inherited print method.
     * @see Out#print(String, String, String, Log_Level, Log_Level)
     */
    private void print(String out, Log_Level message_log_level) {
        Out.print(String.format("%s's car", owner.split(" ")[0]), grid_id, out, this.log_level, message_log_level);
    }

    /**
     * Method to check whether a key is present in a array.
     */
    private static boolean _contains(final int[] arr, final int key) {
        return Arrays.stream(arr).anyMatch(i -> i == key);
    }

    /** Method that calculates and returns the charging priority of the vehicle
     * @return charging_priority : ChargingPriority |  The charging priority of the vehicle
     */
    public ChargingPriority  get_charging_priority () {
        int[] contracted_times = charging_profile.contracted_times;
        int min_charge_status = charging_profile.min_charge_status;
        Integer contracted_charge_status = charging_profile.contracted_charge_status;

        ChargingPriority charging_priority;

        if (charge_status <= min_charge_status) { // The min_charge_status is one of out contracts and we want to keep it at all costs
            return ChargingPriority.IMMEDIATELY;
        }

        if (contracted_times != null) {
            contracted_times = Arrays.stream(contracted_times).map(time -> time * 60).toArray();
            Time simulated_time = new Time();
            simulated_time.addTime(global_time.inMinutes());
            int time_until_next_contract = 0; // Time in minutes until the next contracted time has to be reached

            while (!_contains(contracted_times, simulated_time.inMinutesIsolated() + simulated_time.inHoursIsolated() * 60)) {
                simulated_time.addTime(1);
                time_until_next_contract++;
            }

            int charging_time = v2g_unit.calculate_charging_duration(contracted_charge_status).inMinutes(); // Time that is needed to charge the car up to the specified point
            if (time_until_next_contract <= charging_time) { // If there is less time left to charge the car than there is time until the next contract needs to be fulfilled, charge immediately
                charging_priority = ChargingPriority.IMMEDIATELY;
            } else if (time_until_next_contract <= charging_time * 1.5) { // Else if there is a buffer of up to 50%
                charging_priority = ChargingPriority.URGENT;
            } else if (time_until_next_contract <= charging_time * 2) { // 100% buffer
                charging_priority = ChargingPriority.NORMAL;
            } else if (time_until_next_contract <= charging_time * 3) { // 200% buffer
                charging_priority = ChargingPriority.WEAK;
            } else { // Anything else does basically not need to be charged
                charging_priority = ChargingPriority.NONE;
            }
        } else { // Contracts that don't have contracted times only need to fulfill their min_charge_status
            charging_priority = ChargingPriority.NONE;
        }

        return charging_priority;
    }

    /**
     * A enum that stores different charging priorities that are calculated in the get_charging_priority()
     * method and used by the grid to balance energy distribution.
     */
    public enum ChargingPriority {
        IMMEDIATELY, URGENT, NORMAL, WEAK, NONE,
    }

    /**
     * A enum that stores different charging profiles which can be set by the user to change the
     * charging behavior of the car. Can also be changed whilst being connected to a V2G_Unit.
     */
    public enum ChargingProfile {
        DEFAULT_PROFILE("Default Profile (recommend): \n\tThe grid can decide when and how long the car is charged / discharged.\n\tMin Percentage: 20%", 20, null, null),
        WORK_PROFILE("Work Profile: \n\tYour vehicle will always be on at least 80% when you need it most (08:00am and 05:00pm).\n\tMin Percentage: 30%", 30, new int[]{8, 17}, 80),
        SAFE_PROFILE("Safe Profile: \n\tYou need that power and we got it for you. Stay safe with a least 70% at all time.\n\tMin Percentage: 70%", 70, null, null),
        FULL_PROFILE("Full Profile: \n\tYou got your reasons, charge at all times and always get to 100%.\n\tMin Percentage: 100%", 100, null, null);

        /**
         * @param description A description of the behaviour of the profile
         * @param contracted_times Time in hours when the contracted charge_status has to be reached, e.g. 8 and 17 (o' clock)
         * @param min_charge_status The minimal charge status, e.g. 30 (%)
         * @param contracted_charge_status What charge status has to be reached at the contracted time, e.g. 80 (%)
         */
        ChargingProfile(String description, int min_charge_status, int[] contracted_times, Integer contracted_charge_status) {
            this.description = description;
            this.min_charge_status = min_charge_status;
            this.contracted_times = contracted_times;
            this.contracted_charge_status = contracted_charge_status;
        }

        public final String description;
        public final int min_charge_status;
        public final int[] contracted_times;
        public final Integer contracted_charge_status;

    }


    /**
     * The State class stores the state of the vehicle (duh)
     */
    public static class State {
        private Descriptor descriptor = Descriptor.IDLE;

        // Additional information of the driving descriptor
        private double total_route_length = -1;
        private double remaining_route_length = -1;

        // Additional information of the being_towed descriptor
        private double remaining_time_penalty = -1;

        // Descriptors that are not directly setable, because they require more information.
        private final Set<Descriptor> non_directly_setable_descriptors = Set.of(Descriptor.DRIVING, Descriptor.BEING_TOWED);

        private void isIdle() {
            if (this.descriptor != Descriptor.IDLE) throw new IllegalArgumentException("State was improperly switched.");
        }

        public void setDriving_descriptor(double total_route_length) {
            isIdle();
            this.descriptor = Descriptor.DRIVING;
            this.total_route_length = total_route_length;
            this.remaining_route_length = total_route_length;
        }

        public void setBeing_towed_descriptor() {
            if (descriptor != Descriptor.DRIVING) throw new IllegalArgumentException("Being towed was set but vehicle is not driving");
            this.descriptor = Descriptor.BEING_TOWED;
            remaining_time_penalty = 25 + new Random().nextInt(10); // The towing vehicle needs approx 30minutes until it arrives at the destination
        }

        public void setRemaining_route_length(double remaining_route_length) {
            if (descriptor != Descriptor.DRIVING) throw new IllegalArgumentException("Remaining route length can only be set if the vehicle is currently driving");
            this.remaining_route_length = remaining_route_length;
        }

        public void setDescriptor(Descriptor descriptor) {
            if (non_directly_setable_descriptors.contains(descriptor)) {
                throw new IllegalArgumentException(String.format("Descriptor %s requires additional information to be set.", descriptor.toString()));
            }
            this.descriptor = descriptor;
        }

        public Descriptor getDescriptor() {
            return descriptor;
        }

        enum Descriptor {
            CONNECTED_CHARGING,
            CONNECTED_DISCHARGING,
            CONNECTED_IDLE,
            IDLE,
            DRIVING,
            BEING_TOWED
        }
    }

    /**
     * A class that stores custom exceptions.
     * [NOT IN USE]
     */
    public static class Exceptions {
        /** [NOT IN USE]
         * Throws when the capacity reaches zero before reaching the destination
         */
        public static class EmptyBatteryException extends RuntimeException {
            EmptyBatteryException (String msg) {
                super(msg);
            }
        }

    }


    // ## Overloads with default parameter values

    /**
     * Defaults parameter log_level to ALL.
     * @see #Electric_Car(String, String, int, String, String, double, double, double, Grid, Log_Level)
     */
    Electric_Car(String owner, String license_plate, int ps, String brand, String model_name, double capacity, double energy_consumption, double max_charge_rate, Grid grid) {
        this(owner, license_plate, ps, brand, model_name, capacity, energy_consumption, max_charge_rate, grid, Log_Level.ALL);
    }
}
