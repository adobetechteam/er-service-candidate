package com.skiclinic.er.service;

public class TreatmentCapacityPool {
    private final int totalCapacity;
    private int availableSlots;

    public TreatmentCapacityPool(int totalCapacity) {
        if (totalCapacity < 1) {
            throw new IllegalArgumentException("totalCapacity must be positive");
        }
        this.totalCapacity = totalCapacity;
        this.availableSlots = totalCapacity;
    }

    public int totalCapacity() {
        return totalCapacity;
    }

    public int availableSlots() {
        return availableSlots;
    }

    public void reserveSlot() {
        availableSlots--;
    }

    public void releaseSlot() {
        availableSlots++;
    }
}
