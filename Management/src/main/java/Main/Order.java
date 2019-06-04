/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package csi311;

/**
 *
 * @author patrey
 */
public class Order 
{
    private int tenantid;
    private String timeMs;
    private String orderId;
    private String customerId;
    private String state;
    private String description;
    private int quantity;
    private float cost;
    private boolean flagged = false;
    
    public int getTenant() {
        return tenantid;
    }

    public void setTenant(int id) {
        this.tenantid = id;
    }
    
    public String getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(String timeMs) {
        this.timeMs = timeMs;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public float getCost() {
        return cost;
    }

    public void setCost(float cost) {
        this.cost = cost;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public boolean validateOrderFields() {
        try {
            testValidTenantId(getTenant());
            testValidOrderId(getOrderId());
            testValidQuantity(getQuantity());
            testValidCustomerId(getCustomerId());
            testValidCost(getCost());
            testValidTime(getTimeMs());   // time must also be a positive int
            
            return true;
        } catch (Exception e) {
            // System.out.println(e.getMessage());
            return false;
        }
    }

    private void testValidOrderId(String pkgId) throws Exception {
        String pattern = "^\\d\\d\\d-[A-Z][A-Z][A-Z]-\\d\\d\\d\\d";
        if (!pkgId.matches(pattern)) {
            throw new Exception("Invalid order id " + pkgId);
        }
    }

    private void testValidQuantity(long quantity) throws Exception {
        if (quantity <= 0) {
            throw new Exception("Invalid quantity " + quantity);
        }
    }
    
    private void testValidTime(String time) throws Exception {
        if (Long.valueOf(time) <= 0) {
            throw new Exception("Invalid time " + time);
        }
    }
    
    private void testValidTenantId(int id) throws Exception {
        String pattern = "^\\d\\d\\d\\d\\d";
        if (!String.valueOf(id).matches(pattern)) {
            throw new Exception("Invalid tenant id " + id);
        }
    }

    private void testValidCustomerId(String customerId) throws Exception {
        String pattern = "^\\d\\d\\d\\d\\d\\d\\d\\d\\d";
        if (!customerId.matches(pattern)) {
            throw new Exception("Invalid customer id " + customerId);
        }
    }

    private void testValidCost(float cost) throws Exception {
        if (cost <= 0.0) {
            throw new Exception("Invalid cost");
        }
    }

}
