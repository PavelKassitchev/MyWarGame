package com.pavka;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;

import java.io.FileReader;
import java.util.Random;

import static com.pavka.Nation.*;


public class Base extends Image implements Supplier {

    public static final float IMAGE_SIZE = 15f;

    Nation nation;
    Hex hex;
    double foodLimit;
    double ammoLimit;
    double foodStock;
    double ammoStock;
    Play play;
    boolean isSelected;
    boolean isOccupied;
    int num;
    Random random;
    int turnsSieged;

    //public Texture textureFrance = new Texture("symbols/CavBlueDivision.png");
    public Texture textureFrance = new Texture("blueBase.png");

    //public Texture textureAustria = new Texture("symbols/CavRedDivision.png");
    public Texture textureAustria = new Texture("redBase.png");

    public Base(Play play, Nation nation, Hex hex) {
        this.play = play;
        this.nation = nation;
        this.hex = hex;
        hex.locate(this);

        if(nation.color == WHITE) play.whiteBases.add(this);
        if(nation.color == BLACK) play.blackBases.add(this);

        play.addActor(this);

        setBounds(hex.getRelX() - 8, hex.getRelY() - 8, 15, 15);
        foodStock = 1000;
        ammoStock = 1000;

        random = new Random();

    }

    public void upgrade() {
        System.out.println("UPGRADE");
    }
    public void destroy() {
        hex.eliminate(this);
        if(nation.color == WHITE) {
            System.out.println("White Base removed: " + play.whiteBases.removeValue(this, true));
        }
        if(nation.color == BLACK) {
            System.out.println("Initial Number: " + play.blackBases.size);
            for(Base b: play.blackBases) {
                System.out.println(b.hex.getGeneralInfo());
            }
            System.out.println("Black Base removed: " + play.blackBases.removeValue(this, true));
            System.out.println("End Number: " + play.blackBases.size);
        }
        remove();
    }


    @Override
    public String toString() {
        return "BASE. Food: " + foodStock + " Ammo: " + ammoStock + " Hex: column: " + hex.col + " row: " + hex.row;
    }
    public String getGeneralInfo() {
        float f = (float)(Math.round(foodStock * 10) / 10.0);
        float a = (float)(Math.round(ammoStock * 10) / 10.0);
        return "BASE " + nation + ", " + f + " / " + a;
    }
    @Override
    public void draw(Batch batch, float alpha) {
        Texture texture = nation == FRANCE ? textureFrance : textureAustria;

        if (!isSelected) batch.draw(texture, hex.getRelX() - 8, hex.getRelY() - 8, IMAGE_SIZE, IMAGE_SIZE);
        else batch.draw(texture, hex.getRelX() - 8, hex.getRelY() - 8, IMAGE_SIZE * 1.1f, IMAGE_SIZE * 1.1f);}

    @Override
    public Force sendSupplies(Force force, double food, double ammo) {
        Force train = createTrain(food, ammo);
        train.name = "Train " + nation + " " + ++num;
        train.order.target = new Target(force, Target.JOIN);
        return train;
    }
    public Force createTrain(double food, double ammo) {
        Force train = new Force(nation, hex);
        train.setPlay(play);
        play.addActor(train);
        double foodLoad = Math.min(foodStock, food);
        double ammoLoad = Math.min(ammoStock, ammo);
        int num = (int)Math.ceil(Math.max(foodLoad / UnitType.SUPPLY.FOOD_LIMIT, ammoLoad / UnitType.SUPPLY.AMMO_LIMIT));
        for (int i=0; i<num; i++) {
            Wagon w = new Wagon(nation, hex);
            if(foodLoad < UnitType.SUPPLY.FOOD_LIMIT) {
                foodStock -= foodLoad;
                w.foodStock = foodLoad;
                foodLoad = 0;
            }
            else {
                foodStock -= UnitType.SUPPLY.FOOD_LIMIT;
                foodLoad -= UnitType.SUPPLY.FOOD_LIMIT;
            }
            if(ammoLoad < UnitType.SUPPLY.AMMO_LIMIT) {
                ammoStock -= ammoLoad;
                w.ammoStock = ammoLoad;
                ammoLoad = 0;
            }
            else {
                ammoStock -= UnitType.SUPPLY.AMMO_LIMIT;
                ammoLoad -= UnitType.SUPPLY.AMMO_LIMIT;
            }
            train.attach(w);
        }
        return train;
    }

    public Force createTrain(int i, Force train) {

        for(int j = 0; j < i; j++) {
            Wagon w = new Wagon(nation, hex);
            w.setPlay(play);
            train.attach(w);
            System.out.println("Wagon attached");
            foodStock -= UnitType.SUPPLY.FOOD_LIMIT;
            ammoStock -= UnitType.SUPPLY.AMMO_LIMIT;
        }
        return train;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        switch(nation.color) {
            case WHITE:
                if(hex.blackForces != null && !hex.blackForces.isEmpty()) {
                    turnsSieged++;
                    System.out.println("Days in Siege = " + turnsSieged);
                    if(turnsSieged > 8) {
                        destroy();
                        System.out.println("Base Destroyed!");
                    }
                    }
                else {
                    turnsSieged = 0;
                    int k = random.nextInt(100);
                    if(k == 0) {
                        play.whiteTroops.add(new Battery(play, FRANCE, hex));
                    }
                    else if(k < 4) play.whiteTroops.add(new Squadron(play, FRANCE, hex));
                    else if(k < 7) play.whiteTroops.add(new Battalion(play, FRANCE, hex));
                }
                break;

            case BLACK:
                if(hex.whiteForces != null && !hex.whiteForces.isEmpty()) {
                    turnsSieged++;
                    System.out.println("Days in Siege = " + turnsSieged);
                    if(turnsSieged > 8) {
                        destroy();
                        System.out.println("Base Destroyed! Black Bases Number = " + play.blackBases.size + " In this hex: " + hex.base);
                    }
                }
                else {
                    turnsSieged = 0;
                    int k = random.nextInt(100);
                    if(k == 0) {
                        play.blackTroops.add(new Battery(play, AUSTRIA, hex));
                    }
                    else if(k < 4) play.blackTroops.add(new Squadron(play, AUSTRIA, hex));
                    else if(k < 7) play.blackTroops.add(new Battalion(play, AUSTRIA, hex));
                }
                break;
        }
    }
}
