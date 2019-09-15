package com.pavka;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

import static com.pavka.Nation.FRANCE;

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

    public Texture textureFrance = new Texture("symbols/CavBlueDivision.png");
    public Texture textureAustria = new Texture("symbols/CavRedDivision.png");

    public Base(Play play, Nation nation, Hex hex) {
        this.play = play;
        this.nation = nation;
        this.hex = hex;
        hex.locate(this);
        play.addActor(this);
        foodStock = 1000;
        ammoStock = 1000;
    }

    @Override
    public String toString() {
        return "BASE. Food: " + foodStock + " Ammo: " + ammoStock;
    }
    @Override
    public void draw(Batch batch, float alpha) {
        Texture texture = nation == FRANCE ? textureFrance : textureAustria;

        if (!isSelected) batch.draw(texture, hex.getRelX() - 8, hex.getRelY() - 8, IMAGE_SIZE, IMAGE_SIZE);
        else batch.draw(texture, hex.getRelX() - 8, hex.getRelY() - 8, IMAGE_SIZE * 1.1f, IMAGE_SIZE * 1.1f);}

    @Override
    public Force sendSupplies(Force force, double food, double ammo) {
        Force train = createTrain(food, ammo);
        train.order.target = new Target(force, Target.JOIN);
        return train;
    }
    private Force createTrain(double food, double ammo) {
        Force train = new Force(nation, hex);
        train.setPlay(play);
        play.addActor(train);
        double foodLoad = Math.min(foodStock, food);
        double ammoLoad = Math.min(ammoStock, ammo);
        int num = (int)Math.ceil(Math.max(foodLoad / Wagon.FOOD_LIMIT, ammoLoad / Wagon.AMMO_LIMIT));
        for (int i=0; i<num; i++) {
            Wagon w = new Wagon(nation, hex);
            if(foodLoad < Wagon.FOOD_LIMIT) {
                foodStock -= foodLoad;
                w.foodStock = foodLoad;
                foodLoad = 0;
            }
            else {
                foodStock -= Wagon.FOOD_LIMIT;
                foodLoad -= Wagon.FOOD_LIMIT;
            }
            if(ammoLoad < Wagon.AMMO_LIMIT) {
                ammoStock -= ammoLoad;
                w.ammoStock = ammoLoad;
                ammoLoad = 0;
            }
            else {
                ammoStock -= Wagon.AMMO_LIMIT;
                ammoLoad -= Wagon.AMMO_LIMIT;
            }
            train.attach(w);
        }
        return train;
    }
}