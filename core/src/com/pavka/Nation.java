package com.pavka;

public enum Nation {
    AUSTRIA,
    BRITAIN,
    FRANCE,
    PRUSSIA,
    RUSSIA;

    public static final int WHITE = 1;
    public static final int BLACK = -1;
    private float nationalMorale;
    public int color;
    static
    {
        FRANCE.nationalMorale = 1;
        AUSTRIA.nationalMorale = 1;
        FRANCE.color = WHITE;
        AUSTRIA.color = BLACK;
    }

    public float getNationalMorale() {
        return nationalMorale;
    }

    public void setNationalMorale(float nationalMorale) {
        this.nationalMorale = nationalMorale;
    }
}
