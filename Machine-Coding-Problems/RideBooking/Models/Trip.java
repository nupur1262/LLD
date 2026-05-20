package Models;

import java.sql.Time;

import Enums.PaymentStatus;
import Enums.ProductType;
import Enums.RideStatus;

public class Trip {
    private String id;
    private Rider rider;
    private Driver driver;
    private Location src;
    private Location dest;
    private RideStatus status;
    private Double fare;
    private Integer otp;
    private PaymentStatus paymentStatus;
    private Time createdAt;
    private Time lastUpdatedAt;
    private Product product;
    private ProductType productType;

    public Trip(Rider rider, Location src, Location dest, ProductType productType) {
        this.rider = rider;
        this.src = src;
        this.dest = dest;
        this.productType = productType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Rider getRider() {
        return rider;
    }

    public void setRider(Rider rider) {
        this.rider = rider;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Location getSrc() {
        return src;
    }

    public void setSrc(Location src) {
        this.src = src;
    }

    public Location getDest() {
        return dest;
    }

    public void setDest(Location dest) {
        this.dest = dest;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }

    public Double getFare() {
        return fare;
    }

    public void setFare(Double fare) {
        this.fare = fare;
    }

    public Integer getOtp() {
        return otp;
    }

    public void setOtp(Integer otp) {
        this.otp = otp;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Time getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Time createdAt) {
        this.createdAt = createdAt;
    }

    public Time getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Time lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

}
