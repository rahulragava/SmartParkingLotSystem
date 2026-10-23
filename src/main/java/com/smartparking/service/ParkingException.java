package com.smartparking.service;

import org.springframework.http.HttpStatus;

/** Base for business-rule failures; carries the HTTP status the API should answer with. */
public class ParkingException extends RuntimeException {

    private final HttpStatus status;

    public ParkingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ParkingException lotFull(String vehicleType) {
        return new ParkingException(HttpStatus.CONFLICT, "No free spot available for vehicle type " + vehicleType);
    }

    public static ParkingException alreadyParked(String plate) {
        return new ParkingException(HttpStatus.CONFLICT, "Vehicle " + plate + " is already parked");
    }

    public static ParkingException ticketNotFound(String ticketNumber) {
        return new ParkingException(HttpStatus.NOT_FOUND, "Ticket " + ticketNumber + " not found");
    }

    public static ParkingException ticketNotActive(String ticketNumber) {
        return new ParkingException(HttpStatus.CONFLICT, "Ticket " + ticketNumber + " is already checked out");
    }

    public static ParkingException floorExists(int floorNumber) {
        return new ParkingException(HttpStatus.CONFLICT, "Floor " + floorNumber + " already exists");
    }

    public static ParkingException invalid(String message) {
        return new ParkingException(HttpStatus.BAD_REQUEST, message);
    }
}
