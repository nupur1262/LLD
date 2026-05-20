package Models;

public class Location {
    private double latitude;
    private double longitude;

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    // Rough approximation of distance for LLD using a simple Euclidean distance 
    // multiplied by ~111 (km per degree). For a real app, use the Haversine formula.
    public double distanceTo(Location other) {
        double dLat = this.latitude - other.getLatitude();
        double dLon = this.longitude - other.getLongitude();
        return Math.sqrt(dLat * dLat + dLon * dLon) * 111.0;
    }
}
