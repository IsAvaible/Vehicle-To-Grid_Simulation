import Services.Out;
import Services.Time;

/**
 * Simulates a "smart charger" that can automatically switch between charging and stabilising the grid.
 */
public class V2G_Unit implements Out {

    // TODO : Add integration with the grid | Mostly done

    public final double charging_rate; // The charging rate of the V2G Unit in kW | e.g. 150kW
    public final String grid_id; // The unique id in the current grid
    private final Grid operating_grid; // The grid in which the unit operates

    public Log_Level log_level; // The log level of the unit. Can be raised or lowered to only provide critical information.

    private Electric_Car connected_electric_car; // The connected electric car, null if none is connected.

    /**
     * @param charging_rate The charging rate of the V2G Unit in kW | e.g. 150kW
     * @param operating_grid The grid in which the unit operates
     * @param log_level The log level of the unit.
     */
    V2G_Unit(double charging_rate, Grid operating_grid, Log_Level log_level) {
        this.charging_rate = charging_rate;
        grid_id = operating_grid.register(this);
        this.operating_grid = operating_grid;

        this.log_level = log_level;
    }

    V2G_Unit(double charging_rate, Grid operating_grid) {
        this(charging_rate, operating_grid, Log_Level.WARN);
    }

    /** Charges the car for specified minutes and with specified charging rate. Returns the consumed energy.
     * @param charging_duration in minutes | e.g. 45
     * @return charge_status in % | e.g. 83
     */
    private double charge(int charging_duration) { // Old charge method, before it was integrated with the grid.
        double old_charge_status = connected_electric_car.getCharge_status();

        double charge;
        double used_energy = 0.0;
        do {
            charge = tick_charge();
            used_energy += charge;
        } while (charge != 0.0);

        print(String.format("Charged from %s%% to %s%%.", Math.round(old_charge_status), Math.round(connected_electric_car.getCharge_status())), Log_Level.INFO);
        return used_energy;
    }

    /** Charges the car for the duration of one tick in the grid
     * @param custom_charge_status A custom charge status, is used to calculate the charging time
     * @return How much energy was used
     */
    private double tick_charge(double custom_charge_status) {
        int tick_speed = operating_grid.getTick_speed();
        boolean is_simulated = custom_charge_status >= 0;

        if (!this.isConnected()) { print("Charging not possible, no vehicle is connected.", Log_Level.ERROR); return 0.0; }
        
        double car_charge_status = is_simulated ? custom_charge_status : connected_electric_car.getCharge_status();

        // Charging curve similar to: (Blue bar stretched) https://forococheselectricos.com/wp-content/uploads/2019/08/Charge-curve-Model-SX-LR-CCS.png
        // Below array defines the charging curve
        // Tl;DR Model S with 150W: 0 -> 20 in ~10min, 0 -> 50 in ~20min, 0 -> 100 in ~70min
        double[] charge_speed_reduction = new double[] {0.50, 0.85, 0.90, 0.95, 0.775, 0.60, 0.425, 0.38, 0.36, 0.35, 0.0};

        double max_charge_rate = Math.min(charging_rate, connected_electric_car.max_charge_rate);
        double reduced_charging_rate = (max_charge_rate > 10.0 ? charge_speed_reduction[(int) (car_charge_status / 10)] : 1.0) * max_charge_rate; // Charging curve does not apply if the charging_rate is less than 10kW (btw what is wrong with VW?? Max 3.6kW is super slow...)

        double charge = car_charge_status != 100.0 ? reduced_charging_rate / 60 * (tick_speed) : 0.0;

        double new_charge_status = Math.min(((connected_electric_car.getCapacity() * (car_charge_status / 100)) + (charge)) / connected_electric_car.getCapacity() * 100, 100.0);

        if (!is_simulated) {
            connected_electric_car.setCharge_status(new_charge_status);
            connected_electric_car.state.setDescriptor(charge > 0.0 ? Electric_Car.State.Descriptor.CONNECTED_CHARGING : Electric_Car.State.Descriptor.CONNECTED_IDLE);

            return charge;
        } else {
            return new_charge_status;
        }
    }

    double tick_discharge() {
        // The cars discharge rate is 20% slower than it's max charging rate
        double discharge_rate = Math.min(connected_electric_car.max_charge_rate, charging_rate) * 0.8;
        double discharge = discharge_rate / 60 * 1;
        double total_discharge = 0.0;
        // Calculating on a per minute basis to prevent excessive discharges
        for (int i = 0; i < operating_grid.getTick_speed(); i++) {
            double new_charge_status = ((connected_electric_car.getCapacity() * (connected_electric_car.getCharge_status() / 100)) - (discharge)) / connected_electric_car.getCapacity() * 100;
            if (new_charge_status >= connected_electric_car.charging_profile.min_charge_status && new_charge_status >= 0.0) {
                total_discharge += discharge;
                connected_electric_car.setCharge_status(new_charge_status);
                connected_electric_car.state.setDescriptor(Electric_Car.State.Descriptor.CONNECTED_DISCHARGING);
            } else {
                break;
            }
        }
        // 10% of energy is lost when discharging
        double energy_loss = 0.1;

        return total_discharge * (1 - energy_loss);
    }

    double tick_charge() {
        return tick_charge(-1);
    }

    /** Calculates the charging duration up to 100% (if constantly charged)
     * @return A Time object storing the duration.
     */
    public Time calculate_charging_duration() {
        return calculate_charging_duration(100.0);
    }

    /** Calculates the charging duration up to a point specified by the user (if constantly charged)
     * @return A Time object storing the duration.
     */
    public Time calculate_charging_duration (double up_to) {
        Time duration = new Time();
        double charge_status = connected_electric_car.getCharge_status();

        while (charge_status < up_to) {
            charge_status = tick_charge(charge_status);
            duration.addTime(operating_grid.getTick_speed());
        }

        return duration;
    }
    

    public Electric_Car getConnected_electric_car() {
        return connected_electric_car;
    }

    /** Connects a electric_car to the V2G_Unit if possible.
     * @param electric_car The electric car that should be connected.
     * @return successful : boolean | If the connection was successful
     */
    boolean connectElectricCar(Electric_Car electric_car) {
        boolean successful;
        if (isConnected()) {
            print(String.format("Connected electric car with ID %s needs to be disconnected before the unit can be used by another vehicle.", connected_electric_car.grid_id), Log_Level.ERROR);
            successful = false;
        } else {
            this.connected_electric_car = electric_car;
            connected_electric_car.v2g_unit = this;
            successful = true;
        }
        return successful;
    }

    /** Disconnects the specified electric car from the V2G Unit if possible.
     * @param electric_car The electric car that should be disconnected.
     * @return successful : boolean | If the disconnection was successful
     */
    boolean disconnectElectricCar(Electric_Car electric_car) {
        boolean successful;
        if (connected_electric_car == electric_car) {
            connected_electric_car.v2g_unit = null;
            connected_electric_car = null;
            successful = true;
            print(String.format("Successfully disconnected the electric car with ID %s.", electric_car.grid_id), Log_Level.INFO);
        } else {
            print(String.format("Tried disconnecting electric car with ID %s, but selected vehicle is not connected.", electric_car.grid_id), Log_Level.ERROR);
            successful = false;
        }
        return successful;
    }

    public boolean isConnected() {
        return connected_electric_car != null;
    }

    /**
     * Overload of the inherited print method.
     * @see Out#print(String, String, String, Log_Level, Log_Level)
     */
    private void print(String out, Log_Level message_log_level) {
        Out.print("V2G_Unit", this.grid_id, out, this.log_level, message_log_level);
    }

}
