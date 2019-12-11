package com.pavka;

import com.badlogic.gdx.Input;

public class WagonListener implements Input.TextInputListener {

    Base base;
    Force train;

    public WagonListener(Base base, Force train) {
        this.base = base;
        this.train = train;
    }

    @Override
    public void input(String text) {
        System.out.println("INPUT IS " + text + " Base is " + base);
        System.out.println(base.getGeneralInfo());

        int n = 0;
        try {
            n = Integer.parseInt(text);
        }
        catch(Exception e) {
            System.out.println("WRONG INPUT " + e);
        }
        //base.createTrain(n, train);
        //new Force(base.play, base.nation, base.hex);
    }

    @Override
    public void canceled() {

    }
}
