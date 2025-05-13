package ru.petsinbloom.jFerzl;

public class MedCompany {
    private final String mcod;
    private final String namMop;
    private final String namMok;

    public MedCompany(String mcod, String namMop, String namMok) {
        this.mcod = mcod;
        this.namMop = namMop;
        this.namMok = namMok;
    }

    public String getMcod() {
        return mcod;
    }

    public String getNamMop() {
        return namMop;
    }

    public String getNamMok() {
        return namMok;
    }

    @Override
    public String toString() {
        return mcod + " — " + namMok + " / " + namMop;
    }
}
