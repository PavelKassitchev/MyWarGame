package com.pavka;



public abstract class Unit extends Force {

    public static final int SUPPLY = 0;
    public static final int INFANTRY = 1;
    public static final int CAVALRY = 2;
    public static final int ARTILLERY = 3;
    public static final int HEADQUATERS = 4;

    public static final int[] ALL_TYPES = {SUPPLY, INFANTRY, CAVALRY, ARTILLERY};
    public static final int[] COMBAT_TYPES_BY_FOOD = {INFANTRY, CAVALRY, ARTILLERY};
    public static final int[] COMBAT_TYPES_BY_AMMO = {ARTILLERY, INFANTRY, CAVALRY};

    public static final double CHARGE_ON_ARTILLERY = 1.25;
    public static final double CHARGE_ON_CAVALRY = 0.75;
    public static final double LACK_OF_AMMO_PENALTY = - 0.1;
    public static final double OUT_OF_AMMO_PENALTY = - 0.3;

    int type;
    int maxStrength;
    double maxFire;
    double maxCharge;
    double imprisoned;
    double isolatedUnitPenalty;
    boolean isDisordered;

    public Unit(Nation nation, Hex hex) {

        super(nation, hex);
        isUnit = true;
    }
    public Unit(Play play, Nation nation, Hex hex) {
        this(nation, hex);
        this.play = play;
    }

    public static double getFoodRatio(int type) {
        switch(type) {
            case 1:
                return (Battalion.FOOD_LIMIT / Battalion.FOOD_NEED);
            case 2:
                return (Squadron.FOOD_LIMIT / Squadron.FOOD_NEED);
            case 3:
                return (Battery.FOOD_LIMIT / Battery.FOOD_NEED);
        }
        return 0;
    }

    public static double getUnitFoodNeed(int type) {
        switch(type) {
            case 1:
                return Battalion.FOOD_NEED;
            case 2:
                return Squadron.FOOD_NEED;
            case 3:
                return Battery.FOOD_NEED;
        }
        return 0;
    }

    public static double getAmmoRatio(int type) {
        switch(type) {
            case 1:
                return (Battalion.AMMO_LIMIT / Battalion.AMMO_NEED);
            case 2:
                return (Squadron.AMMO_LIMIT / Squadron.AMMO_NEED);
            case 3:
                return (Battery.AMMO_LIMIT / Battery.AMMO_NEED);
        }
        return 0;
    }

    public static double getUnitAmmoNeed(int type) {
        switch(type) {
            case 1:
                return Battalion.AMMO_NEED;
            case 2:
                return Squadron.AMMO_NEED;
            case 3:
                return Battery.AMMO_NEED;
        }
        return 0;
    }



    @Override
    public Unit getReplenished(Unit unit) {
        if (unit.type == type) {
            int replenish = maxStrength - strength;
            if (unit.strength < replenish) {
                replenish = unit.strength;
            }
            double ratio = (double) (replenish / unit.strength);
            getReinforced(replenish, unit.xp, unit.morale, unit.fatigue, unit.foodStock * ratio, unit.ammoStock * ratio,
                    unit.foodNeed * ratio, unit.ammoNeed * ratio, unit.foodLimit * ratio, unit.ammoLimit * ratio,
                    unit.fire * ratio, unit.charge * ratio);


            unit.foodStock *= (1 - ratio);
            unit.ammoStock *= (1 - ratio);
            unit.foodNeed *= (1 - ratio);
            unit.ammoNeed *= (1 - ratio);
            unit.foodLimit *= (1 - ratio);
            unit.ammoLimit *= (1 - ratio);
            unit.fire *= (1 - ratio);
            unit.charge *= (1 - ratio);

            unit.strength -= replenish;

        }
        if (unit.strength == 0) return null;
        return unit;
    }

    public boolean belongsToTypes(int... types) {
        for (int i = 0; i < types.length; i++) {
            if (type == types[i]) return true;
        }
        return false;
    }

    public int bearLoss(double ratio) {
        if (ratio > 1) ratio = 1;
        int s = (int) (strength * ratio);
        double fS = foodStock * ratio;
        double aS = ammoStock * ratio;
        double fN = foodNeed * ratio;
        double aN = ammoNeed * ratio;
        double fL = foodLimit * ratio;
        double aL = ammoLimit * ratio;
        double f = fire * ratio;
        double c = charge * ratio;
        strength -= s;
        foodStock -= fS;
        ammoStock -= aS;
        foodNeed -= fN;
        ammoNeed -= aN;
        foodLimit -= fL;
        ammoLimit -= aL;
        fire -= f;
        charge -= c;

        if (isSub)
            superForce.getReinforced(-s, this.xp, this.morale, this.fatigue, -fS, -aS, -fN, -aN, -fL, -aL, -f, -c);

        return s;
    }

    public Unit changeMorale(double change) {
        if (type == CAVALRY) change *= CHARGE_ON_CAVALRY;
        if (type == ARTILLERY) change *= CHARGE_ON_ARTILLERY;
        morale += change;
        if (isSub) superForce.updateMorale(strength, change);
        return this;
    }

    public double fire(double ratio) {
        double fireAttack = fire;
        double initStock = ammoStock;
        if (ammoStock > ammoNeed * ratio) {
            ammoStock -= ammoNeed * ratio;
            if (ammoStock < ammoNeed * ratio) changeMorale(LACK_OF_AMMO_PENALTY);
        }
        else {
            ammoStock = 0;
            fire = 0;
            changeMorale(OUT_OF_AMMO_PENALTY);
        }
        if (isSub) superForce.doFire(ammoStock - initStock, fire - fireAttack);
        return fireAttack;
    }
    public void route() {
        if (isSub) superForce.detach(this);
        Hex hx = hex.getNeighbour(order.retreatDirection);
        if(hx == null) surrender();
        else moveTo(hx);

    }


}
