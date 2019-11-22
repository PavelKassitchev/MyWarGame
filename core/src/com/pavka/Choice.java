package com.pavka;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class Choice extends Table {
    Hex hex;
    Base base;
    Force force;
    Label builtLabel;
    Label createLabel;
    Label upgradeLabel;
    Label destroyLabel;
    Label moveLabel;
    Label detachLabel;
    Label attachLabel;
    Tableau tableau;
    float totalHeight;
    Label.LabelStyle style;

    public Choice(Tableau tableau, Hex hex, float x, float y) {
        setStyle(tableau);
        this.hex = hex;

        builtLabel = new Label("Build a Base", style);
        tableau.play.addActor(builtLabel);
        add(builtLabel).width(164);
        builtLabel.setDebug(true);
        builtLabel.setAlignment(0);
        totalHeight += builtLabel.getPrefHeight();
        row();

        createLabel = new Label("Create New Force", style);
        initLabel(createLabel);
        init(x, y);
    }

    public Choice(Tableau tableau, Base base, float x, float y) {
        setStyle(tableau);
        this.base = base;

        upgradeLabel = new Label("Upgrade the Base", style);
        tableau.play.addActor(upgradeLabel);
        add(upgradeLabel).width(164);
        upgradeLabel.setDebug(true);
        upgradeLabel.setAlignment(0);
        totalHeight += upgradeLabel.getPrefHeight();
        row();
        destroyLabel = new Label("Destroy the Base", style);
        tableau.play.addActor(destroyLabel);
        add(destroyLabel).width(164);
        destroyLabel.setDebug(true);
        destroyLabel.setAlignment(0);
        totalHeight += destroyLabel.getPrefHeight();

        init(x, y);
    }

    public Choice(Tableau tableau, Force force, float x, float y) {
        setStyle(tableau);
        this.force = force;

        moveLabel = new Label("Move to...", style);
        tableau.play.addActor(moveLabel);
        add(moveLabel).width(164);
        moveLabel.setDebug(true);
        moveLabel.setAlignment(0);
        totalHeight += moveLabel.getPrefHeight();
        row();

        detachLabel = new Label("Detach", style);
        initLabel(detachLabel);
        row();

        attachLabel = new Label("Attach to...", style);
        initLabel(attachLabel);

        init(x, y);
    }

    private void initLabel(Label label) {
        tableau.play.addActor(label);
        add(label).width(164);
        label.setDebug(true);
        label.setAlignment(0);
        totalHeight += label.getPrefHeight();
    }


    private void setStyle(Tableau tableau) {
        this.tableau = tableau;
        System.out.println("Tableau, label color " + tableau + " ");
        tableau.labelColor.setColor(Color.BROWN);
        tableau.labelColor.fill();
        style = new Label.LabelStyle(tableau.font, new Color(1, 1, 0, 1));
        style.background = new Image(new Texture(tableau.labelColor)).getDrawable();
    }

    private void init(float x, float y) {
        setPosition(x, y);
        setBounds(getX() + 8, getY() + 8, 164, totalHeight);
        setTouchable(Touchable.enabled);
        setVisible(true);
        Skin skin = new Skin();
        Color color = new Color(0, 0, 0, 1);
        skin.add("color", color);
        TextureRegion region = new TextureRegion();
        region.setRegion(new Texture("square-32.png"));
        skin.add("region", region);
        setSkin(skin);
        setBackground(skin.getDrawable("region"));
    }
}