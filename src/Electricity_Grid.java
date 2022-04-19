import java.util.Random;

public class Electricity_Grid {


    /**
     * @return A Electricity_Snapshot for one day.
     */
    public Electricity_Snapshot getSnapshot(Distribution distribution) {
        Weather weather = new Weather();

        int[] production = new int[24*60];
        int[] wind = wind_farm(weather);
        int[] pv = pv_farm(weather);
        int[] water = water_power_plant(weather);
        int[] nuclear = nuclear_power_plant();
        int[] coal = coal_fired_power_station();
        for (int i = 0; i < production.length; i++) {
            production[i] = distribution.weak_distribution ?
                    ((wind[i] + pv[i] + water[i]) * distribution.renewable_energy + (nuclear[i] + coal[i]) * distribution.nonrenewable_energy) / (distribution.renewable_energy + distribution.nonrenewable_energy) :
                    (wind[i] * distribution.wind + pv[i] * distribution.pv + water[i] * distribution.water + nuclear[i] * distribution.nuclear + coal[i] * distribution.coal) / (distribution.wind + distribution.pv + distribution.water + distribution.nuclear + distribution.coal);
        }
        for (int i = 0; i < production.length; i++) {
            production[i] /= 4;
        }
        int[] consumption = generateConsumption();
        // System.out.println("Production: " + Arrays.toString(production) + "\nConsumption: " + Arrays.toString(consumption));
        return new Electricity_Snapshot(production, consumption, weather);
    }

    private int[] generateConsumption() {
        int[] consumption = {35, 32, 30, 30, 30, 32, 36, 50, 57, 54, 48, 48, 51, 57, 50, 45, 40, 50, 65, 80, 82, 65, 53, 45, 40};
        Random random = new Random();
        for (int i = 0; i < consumption.length; i++) {
            consumption[i] += random.nextInt(6);
        }
        return smooth(consumption);
    }

    public static class Distribution {
        final int wind; final int pv; final int water; final int nuclear; final int coal;
        final int renewable_energy; final int nonrenewable_energy;
        final boolean weak_distribution;

        Distribution(int wind, int pv, int water, int nuclear, int coal) {
            if (wind < 0 || pv < 0 || water < 0 || nuclear < 0 || coal < 0) {throw new IllegalArgumentException("Proportion needs to be greater or equal to 0.");}
            this.wind = wind; this.pv = pv; this.water = water; this.nuclear = nuclear; this.coal = coal;
            this.renewable_energy = -1; this.nonrenewable_energy = -1;
            weak_distribution = false;
        }

        Distribution(int renewable_energy, int nonrenewable_energy) {
            if (renewable_energy < 0 || nonrenewable_energy < 0) {throw new IllegalArgumentException("Proportion needs to be greater or equal to 0.");}
            this.renewable_energy = renewable_energy; this.nonrenewable_energy = nonrenewable_energy;
            this.wind = -1; this.pv = -1; this.water = -1; this.nuclear = -1; this.coal = -1;
            weak_distribution = true;
        }

    }

    /** Inserts values in between energy production values at given hours
     * @param values A array of 24 integer values
     * @return A array with 1440 integer values
     */
    private static int[] smooth(int[] values) {
        assert values.length == 24;
        int[] smoothed_values = new int[24*60];
        for (int i = 0; i < values.length-1; i++) {
            int difference = values[i+1] - values[i];
            double step_width = difference / 60.0;
            for (int j = 0; j < 60; j++) {
                smoothed_values[60*i + j] = values[i] + (int) (step_width * j);
            }
        }
        return smoothed_values;
    }

    // Values in %
    private static int[] wind_farm(Weather weather) {
        // Generating values on a per hour basis
        Random random = new Random();
        int[] values = new int[24*60];
        values[0] = 45;
        for (int i = 1; i < values.length; i++) {
            // Wind blow can be a bit random A value between -5 and 5 is added to the last value each time.
            // Math.min prevents greater 100 values, Math.max sub 0 values.
            values[i] = Math.min(Math.max(values[i-1] + (5 - random.nextInt(10)), 0), 100);
        }
        double multiplier;
        switch (weather.wind_speed) {
            case MUCH -> multiplier = 1.5;
            case A_NORMAL_AMOUNT_OF -> multiplier = 1;
            case NO -> multiplier = 0.2;
            default -> throw new IllegalStateException("Unexpected value: " + weather.cloudiness);
        }
        for (int i = 0; i < 24; i++) {
            values[i] *= multiplier;
            values[i] = Math.min(100, values[i]);
        }
        return values;
    }

    private static int[] pv_farm (Weather weather) {
        Random random = new Random();
        int[] values = new int[24];
        for (int i = 0; i < 24; i++) {
            // The further away i is from 13 (sunniest time) the smaller the value gets. Math.max prevents sub 0 values.
            values[i] = Math.max((100 - (17 - random.nextInt(4)) * (i > 13 ? i - 13 : 13 - i)), 0);
        }
        double multiplier;
        switch (weather.cloudiness) {
            case SUNNY -> multiplier = 1.5;
            case CLOUDY -> multiplier = 0.5;
            case RAINY -> multiplier = 0.7;
            case CLEAR -> multiplier = 1;
            default -> throw new IllegalStateException("Unexpected value: " + weather.cloudiness);
        }
        for (int i = 0; i < 24; i++) {
            values[i] *= multiplier;
        }
        return smooth(values);
    }

    private static int[] water_power_plant(Weather weather) {
        Random random = new Random();
        int[] values = new int[24*60];
        for (int i = 0; i < values.length; i++) {
            values[i] = 70 + random.nextInt(15);
        }
        double multiplier;
        switch (weather.cloudiness) {
            case SUNNY -> multiplier = 0.7;
            case CLOUDY, CLEAR -> multiplier = 1;
            case RAINY -> multiplier = 1.3;
            default -> throw new IllegalStateException("Unexpected value: " + weather.cloudiness);
        }
        for (int i = 0; i < values.length; i++) {
            values[i] *= multiplier;
        }
        return values;
    }

    private static int[] nuclear_power_plant (int fixed_production) {
        Random random = new Random();
        int[] values = new int[24];
        for (int i = 0; i < 24; i++) {
            values[i] = fixed_production + random.nextInt(6);
        }
        return smooth(values);
    }

    private static int[] nuclear_power_plant () {return nuclear_power_plant(94);}

    private static int[] coal_fired_power_station () {return nuclear_power_plant(80);}

    /**
     * Stores the energy ratio over one day per minute and the weather of the day.
     */
    public class Electricity_Snapshot {
        public final int[] total_energy;
        public final int[] production;
        public final int[] consumption;
        public final Weather weather;

        Electricity_Snapshot (int[] production, int[] consumption, Weather weather) {
            this.production = production;
            this.consumption = consumption;
            total_energy = new int[production.length];
            for (int i = 0; i < production.length; i++) {
                total_energy[i] = production[i] - consumption[i];
            }
            this.weather = weather;
        }
    }

    public class Weather {
        Cloudiness cloudiness;
        WindSpeed wind_speed;

        Weather() {
            var random = new Random();
            cloudiness = Cloudiness.values()[random.nextInt(Cloudiness.values().length)];
            wind_speed = WindSpeed.values()[random.nextInt(WindSpeed.values().length)];
        }

        @Override
        public String toString() {
            return String.format("It's a %s day with %s wind", cloudiness, wind_speed);
        }
    }

    private enum Cloudiness {
        SUNNY, CLEAR, CLOUDY, RAINY;

        @Override
        public String toString() {
            return super.toString().toLowerCase().replace("_", " ");
        }
    }

    private enum WindSpeed {
        MUCH, A_NORMAL_AMOUNT_OF, NO;

        @Override
        public String toString() {
            return super.toString().toLowerCase().replace("_", " ");
        }
    }
}
