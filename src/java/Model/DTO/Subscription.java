/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model.DTO;

/**
 *
 * @author Admin
 */

public class Subscription {
    private int tierId;
    private String tierName;
    private int maxStorageMb;
    private int aiPromptLimitPerDay;
    private double price;
    private int totalStorageMb;

    public Subscription() {
    }

    public Subscription(int tierId, String tierName, int maxStorageMb, int aiPromptLimitPerDay, double price, int totalStorageMb) {
        this.tierId = tierId;
        this.tierName = tierName;
        this.maxStorageMb = maxStorageMb;
        this.aiPromptLimitPerDay = aiPromptLimitPerDay;
        this.price = price;
        this.totalStorageMb = totalStorageMb;
    }

    // Getters and Setters
    public int getTierId() { return tierId; }
    public void setTierId(int tierId) { this.tierId = tierId; }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }

    public int getMaxStorageMb() { return maxStorageMb; }
    public void setMaxStorageMb(int maxStorageMb) { this.maxStorageMb = maxStorageMb; }

    public int getAiPromptLimitPerDay() { return aiPromptLimitPerDay; }
    public void setAiPromptLimitPerDay(int aiPromptLimitPerDay) { this.aiPromptLimitPerDay = aiPromptLimitPerDay; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public int getTotalStorageMb() { return totalStorageMb; }
    public void setTotalStorageMb(int totalStorageMb) { this.totalStorageMb = totalStorageMb; }
}
