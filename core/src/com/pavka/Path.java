package com.pavka;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;

import java.util.Iterator;

public class Path extends Image implements Connection<Hex> {

    Hex fromHex;
    Hex toHex;
    float cost;
    Force startForce;

    //static Texture texture = new Texture("Symbols/Blue.png");

    public Path(Hex fromHex, Hex toHex){
        this.fromHex = fromHex;
        this.toHex = toHex;
        //cost = Math.max(Math.abs(fromHex.col - toHex.col), Math.abs(fromHex.row - toHex.row));
        //cost = Vector2.dst(fromHex.col, fromHex.row, toHex.col, toHex.row);
        cost = ((Float)(fromHex.cell.getTile().getProperties().get("cost")) + (Float)(fromHex.cell.getTile().getProperties().get("cost"))) / 2;

        /*setBounds(fromHex.getRelX(), fromHex.getRelY(), 16, 0.5f);
        if (fromHex.row != toHex.row) {
            if (fromHex.row % 2 == 0) {
                rotateBy(60);
            }
        }*/
    }


    public static int getDaysToGo(Array<Path> paths, double speed) {
        double distance = 0;
        for (Path path: paths) {
            distance += Hex.SIZE * (Float)path.getFromNode().cell.getTile().getProperties().get("cost");
        }
        if (distance == 0) return 0;

        return (int)Math.ceil(distance / (speed * 2));
    }

    public static boolean isHexInside(Array<Path> paths, Hex hex) {
        if (paths == null || paths.size == 0) return false;
        for (Path path: paths) {
            if(path.fromHex == hex || path.toHex == hex) return true;
        }
        return false;
    }
    public double getDays(double speed) {
        return Hex.SIZE * (Float)getFromNode().cell.getTile().getProperties().get("cost") / speed;
    }
    public static Path getFrontDirection(Hex fromHex, Direction direction) {

        Hex toHex = fromHex.getNeighbour(direction);
        return Play.hexGraph.getPath(fromHex, toHex);
    }

    @Override
    public float getCost() {
        return cost;
    }

    @Override
    public Hex getFromNode() {
        return fromHex;
    }

    @Override
    public Hex getToNode() {
        return toHex;
    }

    public void render(ShapeRenderer shapeRenderer){
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1, 0, 0, 0.5f);
        shapeRenderer.rectLine(fromHex.getRelX(), fromHex.getRelY(), toHex.getRelX(), toHex.getRelY(), 1);
        shapeRenderer.end();
    }

    /*@Override
    public void draw(Batch batch, float alpha) {
        batch.draw(texture, fromHex.getRelX(), toHex.getRelY());

    }*/
}
