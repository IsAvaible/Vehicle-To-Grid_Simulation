package Services;

import java.lang.reflect.MalformedParametersException;
import java.util.Arrays;

/**
 * Simple class to generate a time object.
 * Services.Time is stored in the format [day, hour, minute] and is automatically recalculated if changed.
 *
 * One may criticise the extensive this.time usage in this class, but it helps keep track of
 * methods that interact directly with the time object (IDE highlighting) and I'm a Python
 * developer who forgot that this. is not used in the same way as self.
 */
public class Time {

    private int[] time = {1, 0, 0};

    /**
     * @param initialTime Initiate the object with specified time, defaults to 01-00:00
     */
    public Time (String initialTime) {
        try {
            setTime(initialTime);
        } catch (MalformedParametersException e) {
            System.out.println("Invalid String provided, defaulting to 00-00:00");
        }
    }

    /**
     * Default constructor to set the time to 01-00:00
     */
    public Time () {
        this("00-00:00"); // Overload of the default constructor
    }

    /**
     * @param time in the format "dd-hh:mm"
     */
    public void setTime(String time) {
        String day;
        String[] hour_and_time;
        try {
            day = time.split("-")[0];
            hour_and_time = time.split("-")[1].split(":");
        } catch (IndexOutOfBoundsException e) {
            throw new MalformedParametersException();
        }

        if (hour_and_time.length != 2 || day.length() != 2 || hour_and_time[0].length() != 2 || hour_and_time[1].length() != 2) {
            throw new MalformedParametersException();
        } else {
            this.time = new int[]{Integer.parseInt(day), Integer.parseInt(hour_and_time[0]), Integer.parseInt(hour_and_time[1])};
            updateTime();
        }
    }

    /**
     * @return String representation of the time in the format 'dd-hh:mm'
     */
    public String asString() {
        String[] result = {String.valueOf(this.time[0]), String.valueOf(this.time[1]), String.valueOf(this.time[2])};
        for (int i = 0; i < result.length; i++) {
            if (result[i].length() == 1) {
                result[i] = "0" + result[i];
            }
        }
        return String.join("-", new String[]{result[0], String.join(":",Arrays.copyOfRange(result, 1, 3))});
    }

    /**
     * @return Integer array representation of the time in the format [dd, hh, mm]
     */
    public int[] asIntArr() {
        return this.time.clone();
    }

    /**
     * @param timedelta add the timedelta in the format 'dd-hh:mm' to the time
     */
    public void addTime(String  timedelta) throws MalformedParametersException{
        String day;
        String[] hour_and_time;
        try {
            day = timedelta.split("-")[0];
            hour_and_time = timedelta.split("-")[1].split(":");
            addTime(new int[]{Integer.parseInt(day), Integer.parseInt(hour_and_time[0]), Integer.parseInt(hour_and_time[1])});
        } catch (IndexOutOfBoundsException e) {
            throw new MalformedParametersException(timedelta + " is not a valid timedelta.");
        }
    }

    public int inMinutes() {
        return time[0] * 24 * 60 + time[1] * 60 + time[2];
    }
    public int inMinutesIsolated() {return time[2];}

    public double inHours() {
        return time[0] * 24 + time[1] + time[2] / 60.0;
    }
    public int inHoursIsolated() {return time[1];}

    public int inMinutesWithHoursIsolated() {return time[1] * 60 + time[2];}

    public double inDays() {
        return time[0] + time[1] / 24.0 + time[2] / 60.0 / 24.0;
    }
    public int inDaysIsolated() {return time[2];}

    /**
     * @param timedelta add the timedelta in the format int[] [dd, hh, mm]
     */
    public void addTime(int[] timedelta) {
        if (timedelta.length != 3) {
            throw new MalformedParametersException();
        }  else {
            this.time[2] += timedelta[2];
            this.time[1] += timedelta[1];
            this.time[0] += timedelta[0];
            updateTime();
        }
    }

    public void addTime(int minutes) {
        this.time[2] += minutes;
        updateTime();
    }

    private void updateTime() {
        int[] counters = new int[2];

        for (int i = 60; i > 0; i -= 36) { // This for loops only purpose is to safe 5 lines of code
            int temp = this.time[i/24];
            while (temp >= i) {
                temp -= i;
                counters[i/24 - 1] += 1;
            }
            this.time[i/24 - 1] += counters[i/24 - 1];
            this.time[i/24] -= i * counters[i/24 - 1];
        }

    }

    private String[] parseInputString(String inputString) throws MalformedParametersException{
        String day;
        String[] hour_and_time;
        try {
            day = inputString.split("-")[0];
            hour_and_time = inputString.split("-")[1].split(":");
        } catch (IndexOutOfBoundsException e) {
            throw new MalformedParametersException("inputString wasn't parcelable");
        }
        return new String[]{day, hour_and_time[0], hour_and_time[1]};
    }

}
