package com.pavka;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.util.Iterator;

public class Play implements Screen, InputProcessor {

    public static final String MAP = "maps/WarMap.tmx";

    public static TiledMap map = new TmxMapLoader().load(MAP);

    public static HexGraph hexGraph;

    public static GraphPath<Hex> graphPath;

    MapLayer objectLayer;
    TiledMapTileLayer tileLayer;
    Hex startHex;
    Hex endHex;
    ShapeRenderer shapeRenderer;
    Array<Path> paths;

    Array<Force> blackTroops = new Array<Force>();
    Array<Force> whiteTroops = new Array<Force>();

    private HexagonalTiledMapRenderer renderer;
    private OrthographicCamera camera;
    private Texture texture;
    private Sprite sprite;
    private SpriteBatch sb;

    {
        Hex hex;
        hexGraph = new HexGraph();
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 64; j++) {
                hex = new Hex(i, j);
                hexGraph.addHex(hex);

            }
        }
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 64; j++) {
                hex = hexGraph.getHex(i, j);
                Array<Hex> hexes = hex.getNeighbours();
                for (Hex h : hexes) {
                    hexGraph.connectHexes(hex, hexGraph.getHex(h.col, h.row));
                }
            }
        }

    }

    @Override
    public void show() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        //map = new TmxMapLoader().load(MAP);
        shapeRenderer = new ShapeRenderer();
        renderer = new MyRenderer(map);
        camera = new OrthographicCamera();
        camera.setToOrtho(false, w, h);
        Gdx.input.setInputProcessor(this);
        texture = new Texture("badlogic.jpg");

        TextureRegion tr = new TextureRegion(texture);
        //sb = new SpriteBatch();
        objectLayer = map.getLayers().get("ObjectLayer");
        TextureMapObject tmo = new TextureMapObject(tr);
        tmo.setX(8);
        tmo.setY(0);

        objectLayer.getObjects().add(tmo);



        tileLayer = (TiledMapTileLayer) map.getLayers().get("TileLayer");
        TiledMapTileLayer.Cell cell = tileLayer.getCell(0, 0);
        TiledMapTileSet tileSet = map.getTileSets().getTileSet("WarTiles");
        float type = (Float) cell.getTile().getProperties().get("cost");

        System.out.println(type);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        renderer.setView(camera);
        renderer.render();

        shapeRenderer.setProjectionMatrix(camera.combined);

        if (paths != null) {
            for (Path path : paths) {
                path.render(shapeRenderer);


            }
        }

        camera.update();

    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.position.set(width / 2, height / 2, 0);
        camera.update();

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        map.dispose();
        renderer.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.LEFT)
            camera.translate(-32, 0);
        if (keycode == Input.Keys.RIGHT)
            camera.translate(32, 0);
        if (keycode == Input.Keys.UP)
            camera.translate(0, 32);
        if (keycode == Input.Keys.DOWN) {
            camera.translate(0, -32);
        }

        return false;
    }

    //TODO just for testing
    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.C) {
            Force force = new Battalion(Nation.FRANCE, new FieldHex());
            force.hex.hex = hexGraph.getHex(0, 32);

            texture = new Texture("badlogic.jpg");

            TextureRegion tr = new TextureRegion(texture);

            objectLayer = map.getLayers().get("ObjectLayer");
            TextureMapObject tmo = new TextureMapObject(tr);
            tmo.setX(force.hex.hex.getX() - 8);
            tmo.setY(force.hex.hex.getY() - 8);

            objectLayer.getObjects().add(tmo);
            whiteTroops.add(force);

        }
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }


    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            Hex hex = getHex(getMousePosOnMap().x, getMousePosOnMap().y);


            TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get("TileLayer");
            TiledMapTileLayer.Cell cell = null;
            if (hex != null) {
                cell = layer.getCell(hex.col, hex.row);
                System.out.println(cell.getTile().getProperties().get("cost"));
            }

            if (startHex == null) startHex = hex;
            else {
                if (endHex == null) {
                    endHex = hex;
                    graphPath = hexGraph.findPath(startHex, endHex);
                    System.out.println("Start = " + startHex.index + " end = " + endHex.index +
                            " Counts = " + graphPath.getCount());
                    paths = new Array<Path>();
                    Iterator<Hex> iterator;
                    if (Play.graphPath != null) {
                        iterator = Play.graphPath.iterator();
                        Hex sHex = iterator.next();
                        Hex eHex;
                        while (iterator.hasNext()) {
                            eHex = iterator.next();
                            paths.add(Play.hexGraph.getPath(sHex, eHex));
                            sHex = eHex;
                        }

                    }
                } else {
                    endHex = null;
                    startHex = hex;
                    paths = null;
                    graphPath = null;
                }
            }

        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        if (amount == 1) camera.zoom += 0.2;
        else camera.zoom -= 0.2;
        return true;
    }

    Vector3 getMousePosOnMap() {
        return camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
    }

    //TODO make it correct, this is the simplest version
    Hex getHex(float x, float y) {
        if (y < 2 || y > 774) return null;
        int row = (int) ((y - 2) / 12);

        int col;
        if (row % 2 == 0) {
            if (x < 8 || x > 1032) return null;
            col = (int) ((x - 8) / 16);
        } else {
            if (x < 0 || x > 1024) return null;
            col = (int) (x / 16);
        }


        return hexGraph.getHex(col, row);
    }
}