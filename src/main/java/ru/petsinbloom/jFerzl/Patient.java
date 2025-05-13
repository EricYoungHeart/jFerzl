package ru.petsinbloom.jFerzl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Patient {
    private final long id;
    private final String caseNumber;
    private final String fam;
    private final String im;
    private final String ot;
    private final LocalDate dateOfBirth;
    private final String socialSecurityNumber;

    // New fields from response
    private String rid;
    private String enp;
    private String oip;
    private Byte gender;

    private LocalDate dateB;
    private LocalDate dateE;
    private String okato;
    private String insurName;
    private String insurfName;
    private String insurCode;
    private String insurfCode;

    private final List<Krep> kreps = new ArrayList<>();

    public Patient(Long id, String caseNumber, String fam, String im, String ot, LocalDate dateOfBirth, String socialSecurityNumber) {
        this.id = id;
        this.caseNumber = caseNumber;
        this.fam = fam;
        this.im = im;
        this.ot = ot;
        this.dateOfBirth = dateOfBirth;
        this.socialSecurityNumber = socialSecurityNumber;
    }

    public List<Krep> getKreps() {
        return kreps;
    }

    // Getters and setters for new fields
    public void setRid(String rid) {
        this.rid = rid;
    }

    public void setEnp(String enp) {
        this.enp = enp;
    }

    public void setOip(String oip) {
        this.oip = oip;
    }

    public void setGender(Byte gender) {
        this.gender = gender;
    }

    public String getRid() { return rid; }
    public String getEnp() { return enp; }
    public String getOip() { return oip; }
    public Byte getGender() { return gender; }

    public long getId() {
        return id;
    }
    public String getCaseNumber() {
        return caseNumber;
    }

    public String getFam() {
        return fam;
    }

    public String getIm() {
        return im;
    }

    public String getOt() {
        return ot;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getSocialSecurityNumber() {
        return socialSecurityNumber;
    }

    public void setDateB(LocalDate dateB) { this.dateB = dateB; }
    public void setDateE(LocalDate dateE) { this.dateE = dateE; }
    public void setOkato(String okato) { this.okato = okato; }
    public void setInsurName(String insurName) { this.insurName = insurName; }
    public void setInsurfName(String insurfName) { this.insurfName = insurfName; }
    public void setInsurCode(String insurCode) { this.insurCode = insurCode; }
    public void setInsurfCode(String insurfCode) { this.insurfCode = insurfCode; }

    public LocalDate getDateB() { return dateB; }
    public LocalDate getDateE() { return dateE; }
    public String getOkato() { return okato; }
    public String getInsurName() { return insurName; }
    public String getInsurfName() { return insurfName; }
    public String getInsurCode() { return insurCode; }
    public String getInsurfCode() { return insurfCode; }

    @Override
    public String toString() {
        return "CaseRecord{" +
                "id=" + id +
                "caseNumber='" + caseNumber + '\'' +
                ", enp='" + enp + '\'' +
                ", fam='" + fam + '\'' +
                ", im='" + im + '\'' +
                ", ot='" + ot + '\'' +
                ", w='" + gender + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", socialSecurityNumber='" + socialSecurityNumber + '\'' +
                '}';
    }
}
