package com.snowgears.shop.util;

import org.bukkit.Location;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

public class ArmorStandData {

    private Location location;
    private double yaw;
    private ItemStack equipment;
    private EquipmentSlot equipmentSlot;
    private boolean isSmall;
    private EulerAngle rightArmPose;

    public ArmorStandData(){
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public ItemStack getEquipment() {
        return equipment;
    }

    public void setEquipment(ItemStack equipment) {
        this.equipment = equipment;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    public void setEquipmentSlot(EquipmentSlot equipmentSlot) {
        this.equipmentSlot = equipmentSlot;
    }

    public boolean isSmall() {
        return isSmall;
    }

    public void setSmall(boolean small) {
        isSmall = small;
    }

    public EulerAngle getRightArmPose() {
        return rightArmPose;
    }

    public void setRightArmPose(EulerAngle angle) {
        this.rightArmPose = angle;
    }

    public double getYaw(){
        return yaw;
    }
}
