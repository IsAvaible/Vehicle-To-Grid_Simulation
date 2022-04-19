public abstract class Car {

    public final int ps;
    public final String brand;
    public final String model_name;
    public final String license_plate;

    public String owner;

    Car(int ps, String brand, String model_name, String owner, String license_plate) {
        this.ps = ps;
        this.brand = brand;
        this.model_name = model_name;
        this.owner = owner;
        this.license_plate = license_plate;
    }

    abstract public boolean drive(double route_length);

}
