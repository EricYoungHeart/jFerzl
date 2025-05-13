package ru.petsinbloom.jFerzl;

import java.time.LocalDate;

public class Krep {
    private LocalDate dateB;
    private LocalDate dateE;
    private String moId;
    private String moCode;
    private String moFId;
    private String moOkato;
    private String moName;
    private String status;
    private String area;

    public Krep(LocalDate dateB, LocalDate dateE, String moId, String moCode, String moFId, String moOkato) {
        this.dateB = dateB;
        this.dateE = dateE;
        this.moId = moId;
        this.moCode = moCode;
        this.moFId = moFId;
        this.moOkato = moOkato;
    }

    public LocalDate getDateB() { return dateB; }
    public LocalDate getDateE() { return dateE; }
    public String getMoId() { return moId; }
    public String getMoCode() { return moCode; }
    public String getMoFId() { return moFId; }
    public String getMoOkato() { return moOkato; }
    public String getMoName() { return moName; }
    public String getStatus() { return status; }
    public String getArea() { return area; }

    public void setMoName(String moName) { this.moName = moName; }
    public void setStatus(String status) { this.status = status; }
    public void setArea(String area) { this.area = area; }

    @Override
    public String toString() {
        return "Krep{" +
                "dateB=" + dateB +
                ", dateE=" + dateE +
                ", moId='" + moId + '\'' +
                ", moCode='" + moCode + '\'' +
                ", moFId='" + moFId + '\'' +
                ", moOkato='" + moOkato + '\'' +
                '}';
    }
}
