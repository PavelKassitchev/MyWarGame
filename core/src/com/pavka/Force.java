package com.pavka;


import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

import static com.pavka.Direction.*;
import static com.pavka.Nation.FRANCE;
import static com.pavka.Unit.*;

public class Force extends Image {

    public static final float IMAGE_SIZE = 12f;

    public TextureMapObject symbol;

    public Play play;

    public Texture textureFrance = new Texture("symbols/CavBlueDivision.png");
    public Texture textureAustria = new Texture("symbols/CavRedDivision.png");

    public boolean isSelected;

    List<Force> forces;
    List<Battalion> battalions;
    List<Squadron> squadrons;
    List<Battery> batteries;
    List<Wagon> wagons;
    General general;
    Trace trace;
    Force superForce;
    Force formerSuper;

    Nation nation;
    String name;
    Hex hex;
    Hex backHex;

    Order order;
    Message message;

    boolean isUnit;
    boolean isSub;

    int strength;

    double xp;
    double morale;
    double fatigue;

    double foodStock;
    double ammoStock;
    double foodNeed;
    double ammoNeed;
    double foodLimit;
    double ammoLimit;

    double fire;
    double charge;

    double speed;

    public void draw(Batch batch, float alpha) {
        Texture texture = nation == FRANCE ? textureFrance : textureAustria;

        if (!isSelected) batch.draw(texture, hex.getRelX() - 8, hex.getRelY() - 8, IMAGE_SIZE, IMAGE_SIZE);
        else batch.draw(texture, hex.getRelX() - 8, hex.getRelY() - 8, IMAGE_SIZE * 1.1f, IMAGE_SIZE * 1.1f);

        //THIS IS FOR DELAYED VIEWS

        /*if (general != null && general instanceof Commander) {
            if (!isSelected) batch.draw(texture, hex.getRelX() - 8, hex.getRelY() - 8, IMAGE_SIZE, IMAGE_SIZE);
            else batch.draw(texture, hex.getRelX() - 8, hex.getRelY() - 8, IMAGE_SIZE * 1.1f, IMAGE_SIZE * 1.1f);
        } else {
            for (Report report : Play.whiteCommander.receivedReports) {
                if (report.force == this) {
                    Hex hex = report.hex;
                    if (!isSelected) batch.draw(texture, hex.getRelX() - 8, hex.getRelY() - 8, IMAGE_SIZE, IMAGE_SIZE);
                    else
                        batch.draw(texture, hex.getRelX() - 8, hex.getRelY() - 8, IMAGE_SIZE * 1.1f, IMAGE_SIZE * 1.1f);
                    break;
                }
            }

        }*/
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        //This is for delays
        if (message != null && message.turn == Play.turn) {
            if (message instanceof Order) order = (Order) message;
        }
        //checkHunger();
        eat();
        levelMorale();
        System.out.println("EAT");
        Test.list(this);
        forage();
        System.out.println("FORAGE");
        Test.list(this);
        //move();
        suffer();
        doOrders();
        trace.add(hex);
        //This is for delays
        if (general != null && general instanceof Commander) {
            Commander commander = (Commander) general;
            commander.getReports();
            commander.getViews();
            if (commander.sentReports.size > 0)
                System.out.println("Sent from " + commander.sentReports.peek().force.name +
                        "expected turn " + commander.sentReports.peek().turn);
            System.out.println("Reported: " + commander.sentReports.size +
                    " in Views: " + commander.receivedReports.size + " Element: " +
                    commander.receivedReports.get(0).force.name + " turn: " + commander.receivedReports.get(0).turn);

        }

    }

    public Hex getBackHex() {
        if (backHex == null) System.out.println("BACK HEX NULL!");
        else System.out.println("BACK HEX IS NOT NULL!");
        return backHex;
    }

    public Hex getForwardHex() {
        if (order.frontDirection != null) {
            return hex.getNeighbour(order.frontDirection);
        }

        if (order.pathsOrder != null && order.pathsOrder.size > 0) {
            return order.pathsOrder.get(0).toHex;
        }

        if (getBackHex() != null) {
            System.out.println("My hex: " + hex.col + " " + hex.row);
            System.out.println("Back Hex : " + getBackHex().col + " " + getBackHex().row);
            System.out.println("Direction = " + hex.getDirection(getBackHex()));
            System.out.println(hex.getDirection(getBackHex()).getOpposite());
            return hex.getNeighbour(hex.getDirection(getBackHex()).getOpposite());
        }
        return null;
    }

    //new retreat method

    @Override
    public String toString() {
        String type = "";
        if (isUnit) type += ", Type" + ((Unit) this).type;
        return nation + ": " + strength + " mrl: " + ((int) (morale * 100) / 100.0) + " ftg: " + ((int)(fatigue * 100) / 100.0) +
                " food: " + ((int) (foodStock * 100) / 100.0)
                + " ammo: " + ((int) (ammoStock * 100) / 100.0) + type;
    }

    public void retreat() {
        Hex hx = hex.getNeighbour(order.retreatDirection);
        if (hx == null) surrender();
        else moveTo(hx);

    }

    public void retreat(double dispersal) {
        //double dispersal = 0.2;
        Random random = new Random();
        double d = 1 + 2 * dispersal;
        double s = random.nextDouble() * d;
        Direction direction;
        Hex back;
        if (order.retreatDirection != null) {
            if (s < dispersal) direction = order.retreatDirection.getLeftForward();
            else if (s > dispersal + 1) direction = order.retreatDirection.getRightForward();
            else direction = order.retreatDirection;

            back = hex.getNeighbour(direction);
            System.out.println(name + " Retreat Order Direction = " + order.retreatDirection + " But retreat to " + direction);

            moveTo(back);
            trace.add(back);
        } else disappear();
    }

    public void setRetreatDirection(Force enemy) {
        if (enemy.getForwardHex() != null) {
            order.retreatDirection = hex.getDirection(enemy.getForwardHex());
            System.out.println("Forward is not null");
        } else if (getBackHex() != null) {
            order.retreatDirection = hex.getDirection(getBackHex());
            System.out.println("Back is not null");
        } else {
            order.retreatDirection = Direction.getRandom();
            System.out.println("ELSE");
        }
        System.out.println("Retreat Direction = " + order.retreatDirection + " " + name + " " + nation);
    }

    //STATIC SECTION

    //TODO exclude SUPPLY xp

    public Force(Play play, Nation nation, Hex hex) {
        this(nation, hex);
        this.play = play;

        play.addActor(this);
    }

    public Force(Nation nation, Hex hex) {
        this.play = null;
        this.nation = nation;
        this.hex = hex;
        forces = new ArrayList<Force>();
        battalions = new ArrayList<Battalion>();
        squadrons = new ArrayList<Squadron>();
        batteries = new ArrayList<Battery>();
        wagons = new ArrayList<Wagon>();
        trace = new Trace();
        trace.add(hex);
        order = new Order(false, 0, 0);
        speed = 100;
        //TODO this should be moved to the constructor with Play
        setTouchable(Touchable.enabled);
        setBounds(hex.getRelX() - 8, hex.getRelY() - 8, 12, 12);

        hex.locate(this);


    }

    public Force(Force... subForces) {

        this(subForces[0].nation, subForces[0].hex);
        trace = subForces[0].trace;
        order = subForces[0].order;
        play = subForces[0].play;
        message = subForces[0].message;
        for (Force force : subForces) {
            attach(force);
        }

    }

    public void setPlay(Play play) {
        this.play = play;
        if (!isUnit) {
            for (Force force : forces) force.setPlay(play);
        }
    }

    public Force attach(Force force) {

        hex.eliminate(force);
        force.isSub = true;
        force.superForce = this;
        forces.add(force);
        include(force);
        if (play != null) {
            force.remove();
            if (nation == FRANCE) play.whiteTroops.removeValue(force, true);
            else play.blackTroops.removeValue(force, true);
        }
        return this;
    }

    public Force detach(Force force) {
        force.isSub = false;
        System.out.println("Detaching... From " + force.superForce.name + " Play: " + force.superForce.play);
        force.formerSuper = force.superForce;
        force.superForce = null;
        force.hex = hex;
        hex.locate(force);
        //TODO This may be a mistake! What if the force isn't in forces list?
        forces.remove(force);
        force.order = new Order();
        force.trace = new Trace();
        force.trace.add(hex);
        force.message = null;
        exclude(force);

        //force.setBounds(hex.getRelX() - 8, hex.getRelY() - 8, 12, 12);
        if (play != null) {
            System.out.println("Detaching from... Strength: " + force.strength + " Hex: " + force.hex.getRelX() + " " + force.hex.getRelY());
            force.setBounds(hex.getRelX() - 8, hex.getRelY() - 8, 12, 12);
            play.addActor(force);
            force.play = play;
            if (nation == FRANCE) play.whiteTroops.add(force);
            else play.blackTroops.add(force);
        }

        return force;
    }

    public Unit getReplenished(Unit unit) {
        if (isUnit) return ((Unit) this).getReplenished(unit);
        else {
            for (Force force : forces) {
                if (unit == null) break;
                if (force.isUnit && ((Unit) force).type == unit.type) {
                    return ((Unit) force).getReplenished(unit);
                } else {
                    force.getReplenished(unit);
                }
            }
        }
        return unit;
    }

    // the methods takes into account Super Forces
    private void include(Force force) {
        double x = xp * strength;
        double m = morale * strength;
        double f = fatigue * strength;

        if (force.isUnit) {
            UnitType type = ((Unit) force).type;
            if (type == UnitType.SUPPLY) wagons.add((Wagon) force);
            if (type == UnitType.INFANTRY) battalions.add((Battalion) force);
            if (type == UnitType.CAVALRY) squadrons.add((Squadron) force);
            if (type == UnitType.ARTILLERY) batteries.add((Battery) force);
        } else {
            battalions.addAll(force.battalions);
            squadrons.addAll(force.squadrons);
            batteries.addAll(force.batteries);
            wagons.addAll(force.wagons);
        }
        strength += force.strength;
        foodLimit += force.foodLimit;
        ammoLimit += force.ammoLimit;
        foodNeed += force.foodNeed;
        ammoNeed += force.ammoNeed;
        foodStock += force.foodStock;
        ammoStock += force.ammoStock;
        fire += force.fire;
        charge += force.charge;

        if (strength != 0) {
            x += force.strength * force.xp;
            m += force.strength * force.morale;
            f += force.strength * force.fatigue;


            xp = x / strength;
            morale = m / strength;
            fatigue = f / strength;
        } else {
            xp = 0;
            fatigue = 0;
            morale = (morale * (wagons.size() - 1) + force.morale) / wagons.size();
        }
        if (speed > force.speed) {
            speed = force.speed;
        }


        if (isSub) {
            superForce.include(force);
        }
    }

    public void getReinforced(int s, double x, double m, double f, double fStock, double aStock,
                              double fNeed, double aNeed, double fLimit, double aLimit, double fi, double c) {
        xp = (xp * strength + x * s) / (strength + s);
        morale = (morale * strength + m * s) / (strength + s);
        fatigue = (fatigue * strength + f * s) / (strength + s);

        strength += s;
        foodStock += fStock;
        ammoStock += aStock;
        foodNeed += fNeed;
        ammoNeed += aNeed;
        foodLimit += fLimit;
        ammoLimit += aLimit;
        fire += fi;
        charge += c;

        if (isSub) superForce.getReinforced(s, x, m, f, fStock, aStock, fNeed, aNeed, fLimit, aLimit, fi, c);
    }

    // the methods takes into account Super Forces

    private void exclude(Force force) {
        double x = xp * strength;
        double m = morale * strength;
        double f = fatigue * strength;
        strength -= force.strength;
        foodLimit -= force.foodLimit;
        ammoLimit -= force.ammoLimit;
        foodNeed -= force.foodNeed;
        ammoNeed -= force.ammoNeed;
        foodStock -= force.foodStock;
        ammoStock -= force.ammoStock;
        fire -= force.fire;
        charge -= force.charge;


        if (strength > 0) {
            xp = (x - force.xp * force.strength) / strength;
            morale = (m - force.morale * force.strength) / strength;
            fatigue = (f - force.fatigue * force.strength) / strength;
        } else {
            xp = 0;
            morale = 0;
            fatigue = 0;

        }

        if (force.isUnit) {
            UnitType type = ((Unit) force).type;
            if (type == UnitType.SUPPLY) wagons.remove(force);
            if (type == UnitType.INFANTRY) battalions.remove(force);
            if (type == UnitType.CAVALRY) squadrons.remove(force);
            if (type == UnitType.ARTILLERY) batteries.remove(force);
        } else {
            wagons.removeAll(force.wagons);
            battalions.removeAll(force.battalions);
            squadrons.removeAll(force.squadrons);
            batteries.removeAll(force.batteries);
        }
        if (wagons.size() > 0 || batteries.size() > 0) speed = Battery.SPEED;
        else if (battalions.size() > 0) speed = Battalion.SPEED;
        else speed = Squadron.SPEED;

        if (isSub) {
            superForce.exclude(force);
        }

    }
     public void fatigue(double f)
     {
         if(isUnit) ((Unit)this).changeFatigue(f);
         else {
             for(int i: COMBAT_TYPES_BY_FOOD) {
                 for(Unit u: getUnits(i)) {
                     u.changeFatigue(f);
                 }
             }
         }
     }
     public void rest() {
        fatigue(-FATIGUE_RECOVER);
     }

    public double eat() {

        double eatenFood = 0;
        if (isUnit && !isSub) {
            if (foodStock >= foodNeed) {
                foodStock -= foodNeed;
                eatenFood += foodNeed;
            } else {
                eatenFood = foodStock;
                foodStock = 0;
                ((Unit)this).changeFatigue(-FATIGUE_DROP * (foodStock / foodNeed - 1));
                ((Unit)this).changeMorale(-OUT_OF_FOOD_PENALTY * (foodStock / foodNeed - 1), true);
            }
        } else {
            for (Force force : forces) {
                if (force.isUnit) {
                    if (force.foodStock >= force.foodNeed) {
                        force.foodStock -= force.foodNeed;
                        foodStock -= force.foodNeed;
                        eatenFood += force.foodNeed;
                    } else {
                        foodStock -= force.foodStock;
                        eatenFood += force.foodStock;
                        force.foodStock = 0;
                        ((Unit)force).changeFatigue(-FATIGUE_DROP * (force.foodStock / force.foodNeed - 1));
                        ((Unit)force).changeMorale(-OUT_OF_FOOD_PENALTY * (force.foodStock / force.foodNeed - 1), true);
                    }
                } else {
                    double f = force.eat();
                    force.superForce.foodStock -= f;
                    eatenFood += f;
                }
            }
        }
        return eatenFood;
    }

    //this methods shows morale change dependency on inside Unit morale change

    public void updateMorale(int strength, double change) {
        double sChange = change * strength / this.strength;
        morale += sChange;
        if (isSub) superForce.updateMorale(this.strength, sChange);
    }

    public double doFire(double stockDrop, double fireDrop) {
        double fireAttack = 0;
        ammoStock += stockDrop;
        fire += fireDrop;
        if (isSub) superForce.doFire(stockDrop, fireDrop);
        return fireAttack;
    }

    public Unit selectRandomUnit() {
        if (isUnit) return (Unit) this;

        Random random = new Random();
        ArrayList<Unit> units = new ArrayList<Unit>();
        units.addAll(battalions);
        units.addAll(squadrons);
        units.addAll(batteries);
        int index = random.nextInt(units.size());
        return units.get(index);
    }

    public void throwWagons(double caught) {
        List<Wagon> toThrow = new ArrayList<Wagon>(wagons);
        Random random = new Random();

        for (Wagon wagon : toThrow) {
            wagon.superForce.detach(wagon);
            if (random.nextDouble() > caught) wagon = null;
        }
    }

    public void throwWagons() {
        throwWagons(0);
    }

    //This method distributs all ammo of the force plus extra ammo (argument double ammo) between all the units and sub-forces
    //Also below are auxillary methods

    public double distributeAmmo(double ammo) {
        double free = ammo + unloadAmmo();
        if (free > ammoLimit) {

            free -= loadAmmo(ALL_TYPES);

            while (free > 0) {
                Wagon wagon = new Wagon(nation, hex);
                wagon.name = "Extra Wagon";
                wagon.foodStock = 0;
                if (free <= wagon.ammoLimit) {
                    wagon.ammoStock = free;
                    free = 0;
                } else {
                    free -= wagon.ammoLimit;

                }
                attach(wagon);
            }
        } else if (free >= ammoLimit - wagons.size() * Wagon.AMMO_LIMIT) {

            free -= loadAmmo(COMBAT_TYPES_BY_AMMO);
            free -= loadAmmoToWagons(free);
        } else {
            int min = 0;
            double need = ammoNeed;

            while (free / need > Unit.getAmmoRatio(COMBAT_TYPES_BY_AMMO[min])) {
                for (Unit u : getUnits(COMBAT_TYPES_BY_AMMO[min])) {
                    need -= u.ammoNeed;
                    //System.out.println();
                }
                free -= loadAmmo(COMBAT_TYPES_BY_AMMO[min++]);
            }
            double ratio = free / need;
            System.out.println("min = " + min + " ratio = " + ratio + " free = " + free + " ammoNeed = " + need);
            for (int i = min; i < COMBAT_TYPES_BY_AMMO.length; i++) {
                free -= loadAmmo(ratio, COMBAT_TYPES_BY_AMMO[i]);
            }
        }
        /*if (free < ammoNeed * Battery.AMMO_LIMIT / Battery.AMMO_NEED) {

                double ratio = free / ammoNeed;
                free -= loadAmmo(ratio, INFANTRY, CAVALRY, ARTILLERY);
            } else if (free >= ammoNeed * Battalion.AMMO_LIMIT / Battalion.AMMO_NEED) {

                free -= loadAmmo(INFANTRY, ARTILLERY);
                System.out.println("FREE= " + free);
                double ratio = free / ((ammoLimit - wagons.size() * Wagon.AMMO_LIMIT - ammoStock) * Squadron.AMMO_NEED / Squadron.AMMO_LIMIT);
                free -= loadAmmo(ratio, CAVALRY);
            } else {

                double distributed = loadAmmo(ARTILLERY);

                free -= distributed;
                double ratio = free / (ammoNeed - distributed * Battery.AMMO_NEED / Battery.AMMO_LIMIT);
                if (ratio < Battalion.AMMO_LIMIT / Battalion.AMMO_NEED) {

                    free -= loadAmmo(ratio, CAVALRY, INFANTRY);
                } else {

                    free -= loadAmmo(ratio, INFANTRY);
                    ratio = free / ((ammoLimit - wagons.size() * Wagon.AMMO_LIMIT - ammoStock) * Squadron.AMMO_NEED / Squadron.AMMO_LIMIT);
                    free -= loadAmmo(ratio, CAVALRY);
                }
            }
        }*/

        return free;
    }

    public double loadAmmo(double ratio, int... types) {
        double need = 0;
        if (isUnit && !isSub && ((Unit) this).belongsToTypes(types)) {
            ammoStock = ammoNeed * ratio;
            need = ammoStock;
            if (ratio < 1) {
                fire = ((Unit) this).maxFire * ratio * strength / ((Unit) this).maxStrength;
            } else {
                fire = ((Unit) this).maxFire * strength / ((Unit) this).maxStrength;
            }
        }
        for (Force force : forces) {
            if (force.isUnit) {
                if (((Unit) force).belongsToTypes(types)) {
                    double n = force.ammoNeed * ratio;
                    force.ammoStock = n;
                    ammoStock += n;
                    need += n;
                    if (ratio < 1) {
                        force.fire = ((Unit) force).maxFire * ratio * force.strength / ((Unit) force).maxStrength;
                    } else {
                        force.fire = ((Unit) force).maxFire * force.strength / ((Unit) force).maxStrength;
                    }
                    fire += force.fire;
                }
            } else {
                double initFire = force.fire;
                double n = force.loadAmmo(ratio, types);
                ammoStock += n;
                need += n;
                fire += (force.fire - initFire);
            }
        }

        return need;
    }

    public double loadAmmoToWagons(double ammo) {
        if (isUnit && ((Unit) this).type == UnitType.SUPPLY) {
            if (ammo <= ammoLimit) {
                ammoStock = ammo;
                ammo = 0;
            } else {
                ammoStock = ammoLimit;
                ammo -= ammoLimit;
            }

        } else {
            for (Wagon wagon : wagons) {
                if (ammo <= Wagon.AMMO_LIMIT) {
                    wagon.ammoStock = ammo;
                    ammoStock += ammo;
                    ammo = 0;
                    break;
                } else {
                    wagon.ammoStock = Wagon.AMMO_LIMIT;
                    ammoStock += Wagon.AMMO_LIMIT;
                    ammo -= Wagon.AMMO_LIMIT;
                }
            }
        }
        return ammo;
    }

    public double loadAmmo(int... types) {
        double need = 0;
        if (isUnit && !isSub && ((Unit) this).belongsToTypes(types)) {
            ammoStock = ammoLimit;
            need = ammoLimit;
            fire = ((Unit) this).maxFire * strength / ((Unit) this).maxStrength;
        } else {
            for (Force force : forces) {
                if (force.isUnit) {
                    if (((Unit) force).belongsToTypes(types)) {
                        force.ammoStock = force.ammoLimit;
                        ammoStock += force.ammoLimit;
                        need += force.ammoLimit;
                        force.fire = ((Unit) force).maxFire * force.strength / ((Unit) force).maxStrength;
                        fire += force.fire;
                    }
                } else {
                    double initFire = force.fire;
                    double n = force.loadAmmo(types);
                    need += n;
                    ammoStock += n;
                    fire += (force.fire - initFire);
                }
            }
        }
        return need;
    }

    public double unloadAmmo() {
        double ammo = 0;
        if (isUnit && !isSub) {
            ammo = ammoStock;
            ammoStock = 0;
            fire = 0;

        } else {
            for (Force force : forces) {
                if (force.isUnit) {
                    ammo += force.ammoStock;
                    ammoStock -= force.ammoStock;
                    force.ammoStock = 0;
                    fire -= force.fire;
                    force.fire = 0;
                } else {
                    double fi = force.fire;
                    double f = force.unloadAmmo();
                    ammoStock -= f;
                    fire -= fi;
                    ammo += f;

                }
            }
        }
        return ammo;
    }


    //Pair methods for food
    //
    public double distributeFood(double food) {
        double free = food + unloadFood();

        if (free > foodLimit) {

            free -= loadFood(ALL_TYPES);
            while (free > 0) {
                Wagon wagon = new Wagon(nation, hex);
                wagon.name = "Extra Wagon";
                wagon.ammoStock = 0;
                if (free <= wagon.foodLimit) {
                    wagon.foodStock = free;
                    free = 0;
                } else {
                    free -= wagon.foodLimit;

                }
                attach(wagon);
            }
        } else if (free >= foodLimit - wagons.size() * Wagon.FOOD_LIMIT) {
            free -= loadFood(COMBAT_TYPES_BY_FOOD);
            free -= loadFoodToWagons(free);

        } else {

            int min = 0;
            double need = foodNeed;

            while (free / need > Unit.getFoodRatio(COMBAT_TYPES_BY_FOOD[min])) {
                for (Unit u : getUnits(COMBAT_TYPES_BY_FOOD[min])) {
                    need -= u.foodNeed;
                    //System.out.println();
                }
                free -= loadFood(COMBAT_TYPES_BY_FOOD[min++]);
            }
            double ratio = free / need;
            System.out.println("min = " + min + " ratio = " + ratio + " free = " + free + " foodNeed = " + need);
            for (int i = min; i < COMBAT_TYPES_BY_FOOD.length; i++) {
                free -= loadFood(ratio, COMBAT_TYPES_BY_FOOD[i]);
            }
            /*for (int i = 0; i < COMBAT_TYPES_BY_FOOD.length; i++) {
                if(free / foodNeed < getFoodRatio(COMBAT_TYPES_BY_FOOD[i])){
                    min = i;
                    break;
                }
            }

            for (int i = 0; i < min; i++) {
                free -= loadFood(COMBAT_TYPES_BY_FOOD[i]);
                }
            }*/


            /*if (free < foodNeed * Battalion.FOOD_LIMIT / Battalion.FOOD_NEED) {

                double ratio = free / foodNeed;
                free -= loadFood(ratio, INFANTRY, CAVALRY, ARTILLERY);
            } else if (free > foodNeed * Squadron.FOOD_LIMIT / Squadron.FOOD_NEED) {

                free -= loadFood(INFANTRY, CAVALRY);
                double ratio = free / ((foodLimit - wagons.size() * Wagon.FOOD_LIMIT - foodStock) * Battery.FOOD_NEED / Battery.FOOD_LIMIT);
                free -= loadFood(ratio, ARTILLERY);
            } else {
                System.out.println("FREE TO DISTRIBUTE: " + free);
                double distributed = loadFood(INFANTRY);
                free -= distributed;
                double ratio = free / (foodNeed - distributed * Battalion.FOOD_NEED / Battalion.FOOD_LIMIT);
                System.out.println(ratio);
                if (ratio < Squadron.FOOD_LIMIT / Squadron.FOOD_NEED) {

                    free -= loadFood(ratio, CAVALRY, ARTILLERY);
                } else {

                    free -= loadFood(ratio, CAVALRY);
                    ratio = free / ((foodLimit - wagons.size() * Wagon.FOOD_LIMIT - foodStock) * Battery.FOOD_NEED / Battery.FOOD_LIMIT);
                    free -= loadFood(ratio, ARTILLERY);
                }
            }*/
        }

        return free;
    }

    public double loadFood(double ratio, int... types) {
        double need = 0;
        if (isUnit && !isSub && ((Unit) this).belongsToTypes(types)) {

            foodStock = foodNeed * ratio;
            need = foodStock;
        } else {
            for (Force force : forces) {
                if (force.isUnit) {
                    if (((Unit) force).belongsToTypes(types)) {
                        double n = force.foodNeed * ratio;
                        force.foodStock = n;
                        foodStock += n;
                        need += n;
                    }
                } else {
                    double n = force.loadFood(ratio, types);
                    foodStock += n;
                    need += n;
                }
            }
        }

        return need;
    }

    public double loadFoodToWagons(double food) {
        if (isUnit && ((Unit) this).type == UnitType.SUPPLY) {
            if (food <= foodLimit) {
                foodStock = food;
                food = 0;
            } else {
                foodStock = foodLimit;
                food -= foodLimit;
            }
        }
        for (Wagon wagon : wagons) {
            if (food <= Wagon.FOOD_LIMIT) {
                wagon.foodStock = food;
                foodStock += food;
                food = 0;
                break;
            } else {
                wagon.foodStock = Wagon.FOOD_LIMIT;
                foodStock += Wagon.FOOD_LIMIT;
                food -= Wagon.FOOD_LIMIT;
            }
        }
        return food;
    }

    public double loadFood(int... types) {
        double need = 0;
        if (isUnit && !isSub && ((Unit) this).belongsToTypes(types)) {

            foodStock = foodLimit;
            need = foodLimit;
        } else {
            for (Force force : forces) {
                if (force.isUnit) {
                    if (((Unit) force).belongsToTypes(types)) {
                        force.foodStock = force.foodLimit;
                        foodStock += force.foodLimit;
                        need += force.foodLimit;
                    }
                } else {
                    double n = force.loadFood(types);
                    need += n;
                    foodStock += n;
                }
            }
        }
        return need;
    }

    public double unloadFood() {
        double food = 0;
        if (isUnit && !isSub) {
            food = foodStock;
            foodStock = 0;
        }
        for (Force force : forces) {
            if (force.isUnit) {
                food += force.foodStock;
                foodStock -= force.foodStock;
                force.foodStock = 0;
            } else {
                double f = force.unloadFood();
                foodStock -= f;
                food += f;
            }
        }
        return food;
    }

    public void setHex(Hex hex) {
        this.hex = hex;
        if (!isUnit) {
            for (Force force : forces) {
                force.setHex(hex);
            }
        }
    }

    public boolean move() {
        double movePoints = getForceSpeed();
        float movementCost;
        Hex start = hex;
        while (order.pathsOrder.size > 0 && movePoints > 0) {
            movementCost = Hex.SIZE * (Float) hex.cell.getTile().getProperties().get("cost");
            if (movePoints / movementCost < 1) {
                Random random = new Random();
                if (random.nextDouble() < movePoints / movementCost) movePoints = movementCost;
                else movePoints = 0;
            }
            if (movePoints / movementCost >= 1) {
                backHex = hex;
                forage();
                hex.eliminate(this);
                Hex newHex = order.pathsOrder.get(0).toHex;
                //trace.add(newHex);
                order.pathsOrder.removeRange(0, 0);
                //symbol.setX(newHex.getRelX() - 8);
                //symbol.setY(newHex.getRelY() - 8);
                //hex = newHex;
                setHex(newHex);
                //if (general != null) general.hex = hex;
                setBounds(newHex.getRelX() - 8, newHex.getRelY() - 8, 12, 12);
                hex.locate(this);
                //trace.add(hex);
                movePoints -= movementCost;

            }
        }
        if(hex != start) {
            fatigue(FATIGUE_DROP);
        }
        else {
            rest();
        }
        order.mileStone.days = Path.getDaysToGo(order.pathsOrder, getForceSpeed());
        //if (order.pathsOrder.SIZE == 0) order.mileStone = new MileStone();

        return true;
    }

    public void moveTo(Hex hex) {
        this.hex.eliminate(this);
        backHex = this.hex;
        hex.locate(this);
        //this.hex = hex;
        setHex(hex);
        setBounds(hex.getRelX() - 8, hex.getRelY() - 8, 12, 12);
        //trace.add(hex);
        if(backHex != hex) fatigue(-FATIGUE_DROP);
    }

    public double forage() {
        double food = 0;
        double space = foodLimit - foodStock;
        hex.getNeighbours();
        if ((foodStock - foodNeed) / (foodLimit - foodNeed) >= order.isForaging) {
            return 0;
        }
        if (space <= hex.currentHarvest) {
            hex.currentHarvest -= space;
            food = space;
            distributeFood(food);
            return food;
        }
        food = hex.currentHarvest;
        hex.currentHarvest = 0;
        System.out.println("Zero here!");
        space -= food;
        for (Hex h : hex.getNeighbours()) {
            if (space <= h.currentHarvest) {
                h.currentHarvest -= space;
                System.out.println("neighbour: " + h.currentHarvest);
                System.out.println(h.col + " " + h.row);
                food += space;
                break;
            } else {
                food += h.currentHarvest;
                space -= h.currentHarvest;
                System.out.println("Current Harvest = " + h.currentHarvest + " MAX:" + h.maxHarvest);
                h.currentHarvest = 0;
                System.out.println("ZERO = " + h.currentHarvest + " Max: " + h.maxHarvest);
            }
        }
        System.out.println("Current food stock: " + foodStock);
        System.out.println("Food to distribute: " + food);
        distributeFood(food);
        System.out.println("Food after fouraging: " + foodStock);
        return food;
    }

    public void disappear() {
        if (isSub) superForce.detach(this);
        hex.eliminate(this);
        if (play != null) {
            if (nation == FRANCE) {
                play.whiteTroops.removeValue(this, true);
            } else {
                play.blackTroops.removeValue(this, true);
            }
            remove();

        }
        strength = 0;
    }

    public void setRetreatDirection(Set<Force> enemies, boolean dispersed) {
        Set<Direction> directions = new HashSet<Direction>();
        for (Force f : enemies) {
            directions.add(f.order.frontDirection);
        }
        Direction d = hex.getDirection(getBackHex());
        if (!directions.contains(d) && !dispersed) {
            order.retreatDirection = d;
            return;
        }

        Set<Direction> allDirections = Direction.asSet();
        allDirections.removeAll(directions);
        if (allDirections.isEmpty()) {
            order.retreatDirection = null;
            return;
        }
        int num = (int) (Math.random() * allDirections.size());
        for (Direction direction : allDirections) {
            if (--num < 0) d = direction;
        }
        order.retreatDirection = d;
        return;
    }

    public void doOrders() {

        if (order.target != null) {
            switch (order.target.action) {
                case Target.FIGHT:
                    break;
                case Target.FOLLOW:
                    follow(order.target.force);
                    break;
                case Target.JOIN:
                    join(order.target.force);
                    break;
                case Target.TAKE:
                    take(order.target.force);
                    break;
            }
        }
        move();
    }

    public int surrender() {
        int prisoners = strength;
        disappear();
        return prisoners;
    }

    public double getForceSpeed() {
        return speed;
    }

    public int suffer() {
        int casualties = 0;
        if (fatigue > 0) {
            Random random = new Random();
            double ratio = (0.5 + random.nextDouble()) * fatigue / 500;
            if (isUnit) {
                casualties += ((Unit) this).bearLoss(ratio);
            } else {
                int loss = (int)(ratio * strength);
                System.out.println("LOST: " + loss);
                while(loss > 0) {
                    Unit u = selectRandomUnit();
                    casualties += u.bearLoss(1.0 / u.strength);
                    loss--;

                }
            }
        }
        return casualties;
    }

    public void updateFatigue(int s, double f) {
        double sF = f * s / strength;
        fatigue += sF;
        if (isSub) superForce.updateFatigue(strength, sF);

    }

    public void checkHunger() {
        if (foodStock < foodNeed) {
            System.out.println("WE ARE HUNGRY!!!");
            if (isUnit) {
                ((Unit) this).changeMorale(-OUT_OF_FOOD_PENALTY * (foodStock / foodNeed - 1), true);
            } else {
                for (int i : COMBAT_TYPES_BY_FOOD) {
                    for (Unit u : getUnits(i)) {
                        if (u.foodStock < u.foodNeed) {
                            u.changeMorale(-OUT_OF_FOOD_PENALTY * (u.foodStock / u.foodNeed - 1), true);
                            System.out.println(-OUT_OF_FOOD_PENALTY * (u.foodStock / u.foodNeed - 1));
                        }
                    }
                }
            }
        }
    }

    public void levelMorale() {
        if (isUnit && morale != nation.getNationalMorale()) ((Unit) this).levelUnitMorale();
        else {
            for (int i : COMBAT_TYPES_BY_FOOD) {
                for (Unit u : getUnits(i)) {
                    if (morale != u.nation.getNationalMorale()) u.levelUnitMorale();
                }
            }
        }
    }

    public void follow(Force force) {
        /*if(hex.isNeighbour(force.hex) || hex == force.hex) {
            moveTo(force.hex);
        }
        else {*/
        order.setPathsOrder(play.navigate(hex, force.hex));
        order.mileStone = new MileStone(force.hex);
        order.mileStone.days = Path.getDaysToGo(order.pathsOrder, getForceSpeed());
        //}
    }

    public void join(Force force) {
        follow(force);
        if (hex == force.hex) {
            force.attach(this);
            System.out.println("Wagon Train Arrived!");
        }
    }

    public void take(Force force) {
        follow(force);
        if (hex == force.hex) attach(force);
    }

    public void askForSupplies(Base base, double food, double ammo) {
        base.sendSupplies(this, food, ammo);
    }

    public List<? extends Unit> getUnits(int type) {
        switch (type) {
            case 1:
                return battalions;

            case 2:
                return squadrons;

            case 3:
                return batteries;

        }
        return null;
    }

}
