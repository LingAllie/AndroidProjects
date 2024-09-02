package com.tnl.entity;

public class Data {
    private String date;
    private int room;
    private int event;
    private double totalMoney;
    private double totalKWh;
    private double fnbMoney;
    private double roomMoney;
    private double spaMoney;
    private double adminPublicMoney;
    private double fnbKWh;
    private double roomKWh;
    private double spaKWh;
    private double adminPublicKWh;

    public Data() {}

    public Data(String date, int room, int event, double totalMoney, double totalKWh, double fnbMoney, double roomMoney, double spaMoney, double adminPublicMoney, double fnbKWh, double roomKWh, double spaKWh, double adminPublicKWh) {
        this.date = date;
        this.room = room;
        this.event = event;
        this.totalMoney = totalMoney;
        this.totalKWh = totalKWh;
        this.fnbMoney = fnbMoney;
        this.roomMoney = roomMoney;
        this.spaMoney = spaMoney;
        this.adminPublicMoney = adminPublicMoney;
        this.fnbKWh = fnbKWh;
        this.roomKWh = roomKWh;
        this.spaKWh = spaKWh;
        this.adminPublicKWh = adminPublicKWh;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getRoom() {
        return room;
    }

    public void setRoom(int room) {
        this.room = room;
    }

    public int getEvent() {
        return event;
    }

    public void setEvent(int event) {
        this.event = event;
    }

    public double getTotalMoney() {
        return totalMoney;
    }

    public void setTotalMoney(double totalMoney) {
        this.totalMoney = totalMoney;
    }

    public double getTotalKWh() {
        return totalKWh;
    }

    public void setTotalKWh(double totalKWh) {
        this.totalKWh = totalKWh;
    }

    public double getFnbMoney() {
        return fnbMoney;
    }

    public void setFnbMoney(double fnbMoney) {
        this.fnbMoney = fnbMoney;
    }

    public double getRoomMoney() {
        return roomMoney;
    }

    public void setRoomMoney(double roomMoney) {
        this.roomMoney = roomMoney;
    }

    public double getSpaMoney() {
        return spaMoney;
    }

    public void setSpaMoney(double spaMoney) {
        this.spaMoney = spaMoney;
    }

    public double getAdminPublicMoney() {
        return adminPublicMoney;
    }

    public void setAdminPublicMoney(double adminPublicMoney) {
        this.adminPublicMoney = adminPublicMoney;
    }

    public double getFnbKWh() {
        return fnbKWh;
    }

    public void setFnbKWh(double fnbKWh) {
        this.fnbKWh = fnbKWh;
    }

    public double getRoomKWh() {
        return roomKWh;
    }

    public void setRoomKWh(double roomKWh) {
        this.roomKWh = roomKWh;
    }

    public double getSpaKWh() {
        return spaKWh;
    }

    public void setSpaKWh(double spaKWh) {
        this.spaKWh = spaKWh;
    }

    public double getAdminPublicKWh() {
        return adminPublicKWh;
    }

    public void setAdminPublicKWh(double adminPublicKWh) {
        this.adminPublicKWh = adminPublicKWh;
    }

    @Override
    public String toString() {
        return "Data{" +
                "date='" + date + '\'' +
                ", room=" + room +
                ", event=" + event +
                ", totalMoney=" + totalMoney +
                ", totalKWh=" + totalKWh +
                ", fnbMoney=" + fnbMoney +
                ", roomMoney=" + roomMoney +
                ", spaMoney=" + spaMoney +
                ", adminPublicMoney=" + adminPublicMoney +
                ", fnbKWh=" + fnbKWh +
                ", roomKWh=" + roomKWh +
                ", spaKWh=" + spaKWh +
                ", adminPublicKWh=" + adminPublicKWh +
                '}';
    }
}
