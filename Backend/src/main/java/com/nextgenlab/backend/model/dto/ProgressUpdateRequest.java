package com.nextgenlab.backend.model.dto;

public class ProgressUpdateRequest {
    private String role;
    private int amount;

    public ProgressUpdateRequest() {
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}