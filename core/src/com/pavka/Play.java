package com.pavka;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;

import java.util.Iterator;
import java.util.Random;

import static com.pavka.Nation.*;
import static com.pavka.UnitType.INFANTRY;

public class Play extends Stage implements Screen {

    public static final String MAP = "maps/WarMap.tmx";
    public static int turn;
    public static int time;
    public static TiledMap map = new TmxMapLoader().load(MAP);


    public static HexGraph hexGraph;

    public static GraphPath<Hex> graphPath;

    public static MapLayer objectLayer = map.getLayers().get("ObjectLayer");
    ;
    public static TiledMapTileLayer tileLayer = (TiledMapTileLayer) map.getLayers().get("TileLayer");
    public static Commander blackCommander;
    public static Commander whiteCommander;
    static Array<Force> blackTroops = new Array<Force>();
    static Array<Force> whiteTroops = new Array<Force>();
    static Array<Base> blackBases = new Array<Base>();
    static Array<Base> whiteBases = new Array<Base>();
    public boolean newMode;
    private boolean isDrugging;
    Hex drugHex;
    Hex startHex;
    Hex endHex;
    MileStone mileStone;
    MileStone start;
    ShapeRenderer shapeRenderer;
    Array<Path> paths;
    private HexagonalTiledMapRenderer renderer;
    private OrthographicCamera camera;
    private Force selectedForce;
    private Force forceToAttach;
    private Force forceToMove;
    private Force drugForce;
    private Hex selectedHex;
    private Base selectedBase;

    private Hex currentHex;
    private MileStone currentStone;


    private Control control;


    private Window selectedWindow;
    //private Tableau tableau;
    private Array<Tableau> tableaus = new Array<Tableau>();
    private int tableauNum;

    private boolean specialAction;

    //TODO exclude this variables

    private Force austria;
    private Force france;
    private Base a;
    private Base b;
    private Force frenchArtillery;
    private Force austrianArtillery;
    private Force frenchCavalry;
    private Force austrianCavalry;

    {
        Hex hex;
        hexGraph = new HexGraph();
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 64; j++) {
                hex = new Hex(i, j);
                hexGraph.addHex(hex);
                addActor(hex);
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
        for (Path p : hexGraph.paths) addActor(p);

    }

    public static Base selectRandomBase(int color) {
        Array<Base> bases = null;
        switch (color) {
            case WHITE:
                bases = whiteBases;
                break;
            case BLACK:
                bases = blackBases;
                break;
        }
        if(bases == null || bases.isEmpty()) return null;
        Random random = new Random();
        int index = random.nextInt(bases.size);
        return bases.get(index);
    }

    public static Array<Path> navigate(Hex start, Hex finish) {
        Array<Path> paths = new Array<Path>();
        if (start != finish) {
            graphPath = hexGraph.findPath(start, finish);
            Iterator<Hex> iterator = graphPath.iterator();
            Hex sHex = null;
            if (iterator.hasNext()) sHex = iterator.next();
            Hex eHex;
            while (iterator.hasNext()) {
                eHex = iterator.next();
                paths.add(Play.hexGraph.getPath(sHex, eHex));
                sHex = eHex;
            }


        }
        return paths;
    }

    @Override
    public void show() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        shapeRenderer = new ShapeRenderer();
        renderer = new MyInnerRenderer(map);
        camera = (OrthographicCamera) getCamera();
        camera.setToOrtho(false, w, h);
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.setView(camera);
        renderer.render();
        draw();

        shapeRenderer.setProjectionMatrix(camera.combined);

        if (paths != null) {
            for (Path path : paths) {
                path.render(shapeRenderer);
            }
        }


        camera.update();
        //act(Gdx.graphics.getDeltaTime());
        //draw();
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
        shapeRenderer.dispose();
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
        if(keycode == Input.Keys.G) {
            System.out.println("Selected force = " + selectedForce + " selected window = " + selectedWindow + " force to attach = " + forceToAttach + " selected hex = " +
                    selectedHex);
            if(selectedWindow != null) {
                System.out.println("Choice = " + selectedWindow.choice);
            }
        }
        if (keycode == Input.Keys.C) {
            /*Force force = new Force(new Squadron(this, Nation.FRANCE, hexGraph.getHex(8, 4)), new Squadron(this, Nation.FRANCE, hexGraph.getHex(8, 4)));
            force.order.isForaging = 0.8;
            force.order.seekBattle = true;
            force.name = "Cavalry Sq.";

            System.out.println(force.getX() + " " + force.getY());
            System.out.println(force.order.pathsOrder);
            whiteTroops.add(force);
            addActor(force);*/

            france = Test.force1;
            france.setPlay(this);
            //france.hex = hexGraph.getHex(8, 4);
            france.order.seekBattle = true;
            france.order.isForaging = 0.8;
            france.name = "France";
            //whiteTroops.add(france);
            addActor(france);
        }

        if (keycode == Input.Keys.O) {
            /*Force force = new Force(new Squadron(this, Nation.AUSTRIA, hexGraph.getHex(18, 18)), new Squadron(this, Nation.AUSTRIA, hexGraph.getHex(18, 18)));
            force.order.isForaging = 0.9;
            force.order.seekBattle = true;
            force.name = "2.Squadron";
            blackTroops.add(force);
            addActor(force);*/
            austria = Test.force2;
            austria.setPlay(this);
            //austria.hex = hexGraph.getHex(18, 18);
            austria.order.seekBattle = true;
            austria.order.isForaging = 0.8;
            austria.name = "Austria";
            //blackTroops.add(austria);
            addActor(austria);
        }

        if (keycode == Input.Keys.B) {

            a = new Base(this, Nation.AUSTRIA, hexGraph.getHex(28, 28));
            //addActor(a);

        }
        if (keycode == Input.Keys.R) {
            b = new Base(this, Nation.FRANCE, hexGraph.getHex(2, 2));
            //addActor(b);
        }
        if (keycode == Input.Keys.P) {

            a.sendSupplies(austria, 250, 50);
            //System.out.println(train.order.pathsOrder);
        }
        if (keycode == Input.Keys.L) {

            b.sendSupplies(france, 250, 50);
            //System.out.println(train.order.pathsOrder);
        }


        if (keycode == Input.Keys.T) {
            /*Commander commander = new Commander(Nation.FRANCE, hexGraph.getHex(32, 32));
            Force force = new Force(commander);
            force.name = "Headquarters";
            force.general = commander;
            whiteCommander = commander;
            whiteTroops.add(force);
            addActor(force);*/
            //addActor(commander);
            austrianArtillery = Test.austrianArt;
            austrianArtillery.setPlay(this);
            austrianArtillery.order.seekBattle = true;
            austrianArtillery.order.isForaging = 0.8;
            austrianArtillery.name = "Austrian Artillery";
            addActor(austrianArtillery);

            frenchArtillery = Test.frenchArt;
            frenchArtillery.setPlay(this);
            frenchArtillery.order.seekBattle = true;
            frenchArtillery.order.isForaging = 0.8;
            frenchArtillery.name = "French Artillery";
            addActor(frenchArtillery);

            austrianCavalry = Test.austrianCav;
            austrianCavalry.setPlay(this);
            austrianCavalry.order.seekBattle = true;
            austrianCavalry.order.isForaging = 0.8;
            austrianCavalry.name = "Austrian Cavalry";
            addActor(austrianCavalry);

            frenchCavalry = Test.frenchCav;
            frenchCavalry.setPlay(this);
            frenchCavalry.order.seekBattle = true;
            frenchCavalry.order.isForaging = 0.8;
            frenchCavalry.name = "French Cavalry";
            addActor(frenchCavalry);

        }
        if (keycode == Input.Keys.Q) {
            /*turn++;
            act();*/

            if (selectedForce != null) selectedForce.isSelected = false;
            selectedForce = null;
            forceToMove = null;
            forceToAttach = null;
            selectedHex = null;
            selectedBase = null;
            startHex = null;
            endHex = null;
            paths = null;

            if (mileStone != null) {
                mileStone.remove();
                mileStone = null;
            }
            if(start != null) {
                start.remove();
                start = null;
            }

            /*Array<Hex> battlefields = new Array<Hex>();
            for(Hex h: hexGraph.hexes){
                if(!h.whiteForces.isEmpty() && !h.blackForces.isEmpty()) battlefields.add(h);
            }
            for(Hex hx: battlefields) {
                Fighting fighting = hx.startFighting();
                fighting.resolve();
            }*/
            for (time = 0; time < 4; time++) act();

            System.out.println();
            System.out.println("Number of battles = " + Fighting.battles);
            System.out.println();

        }

        if (keycode == Input.Keys.S) {
            Test.main(null);
        }
        /*if (keycode == Input.Keys.L) {
            LogisticTest.main(null);
        }*/

        if (keycode == Input.Keys.M) {
            newMode = !newMode;
        }

        if (keycode == Input.Keys.F) {
            for (Force force : whiteTroops) {
                System.out.println(force.foodStock);
                force.eat();
                System.out.println(force.forage());
            }
        }
        if (keycode == Input.Keys.U) {
            System.out.println("White bases: " + whiteBases);
            for (Base base : blackBases) System.out.println(base);
        }
        if (keycode == Input.Keys.Z) {
            if (selectedForce != null) Test.list(selectedForce);
        }
        return true;

    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    private void navigate(double speed) {
        if (startHex != endHex) {
            graphPath = hexGraph.findPath(startHex, endHex);
            paths = new Array<Path>();
            Iterator<Hex> iterator = graphPath.iterator();
            Hex sHex = iterator.next();
            Hex eHex;
            while (iterator.hasNext()) {
                eHex = iterator.next();
                paths.add(Play.hexGraph.getPath(sHex, eHex));
                sHex = eHex;
            }
            if(mileStone != null) {
                mileStone.remove();
                mileStone = null;
            }
            mileStone = new MileStone(paths.peek().getToNode());
            mileStone.days = Path.getDaysToGo(paths, speed);
            addActor(mileStone);
        } else {
            paths = new Array<Path>();
        }

    }

    private void closeTableau(int i) {
        for (int num = tableauNum; num > i - 1; num--) {
            tableaus.get(num - 1).remove();
            tableaus.removeValue(tableaus.get(num - 1), true);
        }
        tableauNum = i - 1;
    }
    private void clearWindow(Window w) {
        w.parent = null;
        w.remove();
        if(!w.children.isEmpty()) {
            for(Window child: w.children) {
                clearWindow(child);
            }
        }
    }

    private void closeWindow(Window w) {

        if(w.parentLabel != null) {
            w.parentLabel.childWindow = null;
            w.parentLabel.changeStyle();
        }
        if(w.parent != null) {
            Window p = w.parent;
            if(p.choice != null) {
                closeWindow(p);
                forceToAttach = null;
            }
            else {
                selectedWindow = p;
                p.children.removeValue(w, true);
            }
        }
        else {
            clearSelections();

        }

        clearWindow(w);
        specialAction = false;

    }

    private void closeWindows() {
        closeWindows(false);
    }

    private void closeWindows(boolean forceSelected) {
        if (selectedWindow != null) {
            Window root = selectedWindow;
            while (root.parent != null) {
                root = root.parent;
            }
            closeWindow(root);
            clearSelections(forceSelected);
        }
        clearSelections(forceSelected);
        specialAction = false;
    }

    private void clearSelections() {
        clearSelections(false);
    }

    private void clearSelections(boolean forceSelected) {
        if(!forceSelected) {
            selectedForce = null;
        }
            forceToAttach = null;
            selectedWindow = null;
            selectedBase = null;
            selectedHex = null;

    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        //Just to try
        if(button == Input.Buttons.RIGHT) {
            float X = getMousePosOnMap().x;
            float Y = getMousePosOnMap().y;
            Actor a = hit(X, Y, true);
            System.out.println("ACTOR IS " + a);

            if(a instanceof Hex) {
                Hex h = (Hex)a;
                selectedWindow = new Window(this, h, true, X, Y);
                return true;
            }

            if(a instanceof Base) {
                BitmapFont font = new BitmapFont();
                font.getData().setScale(0.5f);
                Skin skin = new Skin();
                Color color = new Color(1, 0, 1, 1);
                skin.add("color", color);
                TextureRegion region = new TextureRegion();
                region.setRegion(new Texture("square-32.png"));
                skin.add("region", region);
                Drawable drawable = skin.getDrawable("region");
                com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle ws = new com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle(font, color, drawable);
                skin.add("default", ws);
                com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle ts = new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle(drawable, drawable, drawable, font);
                skin.add("default", ts);
                Skin s = new Skin(Gdx.files.internal("ui/uiskin.json"));
                Dialog dialog = new Dialog("My Long Long Dialog", s);
                dialog.button("Hey!");
                dialog.show(this);
            }


            if(a instanceof SwitchLabel) {
                SwitchLabel label = (SwitchLabel)a;
                Choice choice = selectedWindow.choice;

                if(label == choice.whiteBattalion) {
                    Force force = new Battalion(this, FRANCE, selectedWindow.hex);
                    whiteTroops.add(force);
                    closeWindows();
                    return true;
                }
                if(label == choice.whiteSquadron) {
                    Force force = new Squadron(this, FRANCE, selectedWindow.hex);
                    whiteTroops.add(force);
                    closeWindows();
                    return true;
                }
                if(label == choice.whiteBattery) {
                    Force force = new Battery(this, FRANCE, selectedWindow.hex);
                    whiteTroops.add(force);
                    closeWindows();
                    return true;
                }
                if(label == choice.whiteWagon) {
                    Force force = new Wagon(this, FRANCE, selectedWindow.hex);
                    whiteTroops.add(force);
                    closeWindows();
                    return true;
                }
                if(label == choice.blackBattalion) {
                    Force force = new Battalion(this, AUSTRIA, selectedWindow.hex);
                    blackTroops.add(force);
                    closeWindows();
                    return true;
                }
                if(label == choice.blackSquadron) {
                    Force force = new Squadron(this, AUSTRIA, selectedWindow.hex);
                    blackTroops.add(force);
                    closeWindows();
                    return true;
                }
                if(label == choice.blackBattery) {
                    Force force = new Battery(this, AUSTRIA, selectedWindow.hex);
                    blackTroops.add(force);
                    closeWindows();
                    return true;
                }
                if(label == choice.blackWagon) {
                    Force force = new Wagon(this, AUSTRIA, selectedWindow.hex);
                    blackTroops.add(force);
                    closeWindows();
                    return true;
                }

            }
        }
        if(button == Input.Buttons.LEFT) {
//            float X = getMousePosOnMap().x;
//            float Y = getMousePosOnMap().y;
//            Actor a = hit(X, Y, true);
//
//            Hex h = null;
//            if(a instanceof Hex) h = (Hex)a;
//            if(a instanceof Base) h = ((Base)a).hex;
//            if(a instanceof Force) h = ((Force)a).hex;

            Hex h = getHex(getMousePosOnMap().x, getMousePosOnMap().y);

            if(Path.isHexInside(paths, h)) {
                drugHex = h;
                isDrugging = true;

                if(paths.get(0).startForce != null) {
                    drugForce = paths.get(0).startForce;
                }
            }
        }


        return true;
    }


    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        //NEW VERSION

        if (button == Input.Buttons.LEFT) {

            Hex current = getHex(getMousePosOnMap().x, getMousePosOnMap().y);
            if(isDrugging && current != drugHex) {
                if(drugForce != null) {
                    drugForce.order.setPathsOrder(paths);
                    drugForce = null;
                }
                isDrugging = false;
                drugHex = null;
                return true;
            }

            drugHex = null;
            float X = getMousePosOnMap().x;
            float Y = getMousePosOnMap().y;
            Actor a = hit(X, Y, true);


            if (a instanceof Hex) {
                Hex hx = (Hex) a;
                if (selectedForce == null && selectedHex == null && selectedBase == null && forceToAttach == null
                        && startHex == null && forceToMove == null) {
                    closeWindows();
                    selectedWindow = new Window(this, hx, X, Y);

                }
                else if (startHex != null && forceToMove == null) {
                    endHex = hx;
                    navigate(INFANTRY.SPEED);
                    startHex = null;
                    endHex = null;
                }
                else if(forceToMove != null) {
                    if (!specialAction) {
                        endHex = hx;
                        navigate(forceToMove.getForceSpeed());
                        //paths new function
                        paths.get(0).startForce = forceToMove;
                        forceToMove.order.setPathsOrder(paths);

                        clearActor(start);
                        startHex = null;
                        endHex = null;
                        forceToMove = null;
                    }
                }
                else closeWindows();
                return true;
            }


            if (a instanceof Base) {
                Base b = (Base) a;
                Hex hx = b.hex;
                if (selectedForce == null && selectedHex == null && selectedBase == null && forceToAttach == null
                        && startHex == null && forceToMove == null) {
                    closeWindows();
                    selectedWindow = new Window(this, hx, X, Y);

                }
                else if (startHex != null && forceToMove == null) {
                    endHex = hx;
                    navigate(INFANTRY.SPEED);
                    startHex = null;
                    endHex = null;
                }
                else if(forceToMove != null) {
                    if (!specialAction) {
                        endHex = hx;
                        navigate(forceToMove.getForceSpeed());
                        //paths new function
                        paths.get(0).startForce = forceToMove;
                        forceToMove.order.setPathsOrder(paths);
                        clearActor(start);
                        startHex = null;
                        endHex = null;
                        forceToMove = null;
                    }
                }
                else closeWindows();
                return true;

            }
            if (a instanceof Force) {
                Force f = (Force) a;
                Hex hx = f.hex;
                if (selectedForce == null && selectedHex == null && selectedBase == null && forceToAttach == null
                        && startHex == null && forceToMove == null) {
                    closeWindows();
                    selectedWindow = new Window(this, hx, X, Y);

                }
                else if (startHex != null && forceToMove == null) {
                    endHex = hx;
                    navigate(INFANTRY.SPEED);
                    startHex = null;
                    endHex = null;
                }
                else if(forceToMove != null) {
                    if (!specialAction) {
                        endHex = hx;
                        navigate(forceToMove.getForceSpeed());
                        //paths new function
                        paths.get(0).startForce = forceToMove;
                        forceToMove.order.setPathsOrder(paths);
                        clearActor(start);
                        startHex = null;
                        endHex = null;
                        forceToMove = null;
                    }
                    else {
                        if(forceToMove.nation == f.nation) {
                            endHex = hx;
                            navigate(forceToMove.getForceSpeed());
                            forceToMove.order.target = new Target(f, Target.JOIN);
                            clearActor(start);
                            startHex = null;
                            endHex = null;
                            forceToMove = null;
                            specialAction = false;
                        }
                    }
                }
                else {
                    closeWindows();
                }
                return true;

            }
            if (a instanceof SwitchLabel) {
                SwitchLabel label = (SwitchLabel) a;
                Window w = label.window;

                if(label == w.closeLabel) {
                    closeWindow(w);
                    return true;
                }
                if(label == w.hexLabel) {
                    if(selectedWindow != w) {
                        Array<Window> ch = new Array<Window>(w.children);
                        for(Window wind: ch){
                            closeWindow(wind);
                        }
                        selectedHex = null;
                        selectedBase = null;
                        selectedForce = null;
                        forceToAttach = null;
                    }
                    else {
                        selectedHex = w.hex;
                        selectedWindow = new Window(this, selectedWindow, selectedWindow.hex, X, Y);
                    }
                    return true;
                }
                if(label == w.baseLabel) {
                    if(selectedWindow != w) {
                        Array<Window> ch = new Array<Window>(w.children);
                        for(Window wind: ch){
                            closeWindow(wind);
                        }
                        selectedHex = null;
                        selectedBase = null;
                        selectedForce = null;
                        forceToAttach = null;
                    }
                    else {
                        selectedBase = w.base;
                        selectedWindow = new Window(this, selectedWindow, selectedWindow.base, X, Y);
                    }
                    return true;
                }
                if(w.choice != null) {

                    Choice choice = w.choice;

                    if(label == choice.pathLabel) {
                        startHex = selectedHex;
                        if(start != null) {
                            start.remove();
                        }
                        start = new MileStone(startHex, 0);
                        addActor(start);
                        closeWindows();
                        return true;
                    }
                    if(label == choice.buildWLabel) {
                        selectedHex.builtWhiteBase(this);
                        closeWindows();
                        return true;
                    }

                    if(label == choice.buildBLabel) {
                        selectedHex.builtBlackBase(this);
                        closeWindows();
                        return true;
                    }

                    if(label == choice.createWLabel) {
                        Force force = new Force(this, FRANCE, selectedHex);
                        whiteTroops.add(force);
                        closeWindows();
                        return true;
                    }
                    if(label == choice.createBLabel) {
                        Force force = new Force(this, AUSTRIA, selectedHex);
                        blackTroops.add(force);
                        closeWindows();
                        return true;
                    }
                    if(label == choice.upgradeLabel) {
                        selectedBase.upgrade();
                        closeWindows();
                        return true;
                    }
                    if(label == choice.destroyLabel) {
                        selectedBase.destroy();
                        closeWindows();
                        return true;
                    }
                    if(label == choice.detachLabel) {
                        if(forceToAttach != null) {
                            forceToAttach = null;
                            closeWindow(w);
                        }
                        else if(selectedForce.superForce != null) {
                            selectedForce.superForce.detach(selectedForce);
                            closeWindows();
                            return true;
                        }
                    }
                    if(label == choice.attachLabel) {
                        if(forceToAttach != null) {
                            forceToAttach = null;
                            closeWindow(w);
                        }
                        else {
                            forceToAttach = selectedForce;
                            forceToAttach.order.target = null;
                            selectedForce = null;
                            selectedWindow = new Window(this, selectedWindow, forceToAttach, true, X, Y);
                            return true;
                        }
                    }
                    if(label == choice.meetLabel) {
                        if(forceToAttach != null) {
                            forceToAttach = null;
                            closeWindow(w);
                        }
                        else {
                            forceToMove = selectedForce;
                            selectedForce = null;
                            startHex = forceToMove.hex;
                            closeWindows();
                            specialAction = true;
                        }
                        return true;
                    }

                    if(label == choice.moveLabel) {
                        if(forceToAttach != null) {
                            forceToAttach = null;
                            closeWindow(w);
                        }
                        else {
                            forceToMove = selectedForce;
                            forceToMove.order.target = null;
                            selectedForce = null;
                            startHex = forceToMove.hex;
                            closeWindows();
                        }
                        return true;
                    }
                    if(label == choice.showLabel) {
                        if(!selectedForce.order.pathsOrder.isEmpty()) {

                            clearActor(start, mileStone);

                            paths = selectedForce.order.pathsOrder;
                            paths.get(0).startForce = selectedForce;
                            mileStone = selectedForce.order.mileStone;
                            addActor(mileStone);
                            System.out.println("MILESTONE = " + mileStone + " MileStone HEX = " + mileStone.hex);
                            closeWindows();
                        }
                        else closeWindow(w);
                    }
                    return true;
                }
                if(w.forces != null){

                    for(int i = 0; i < w.forces.size; i++) {
                        if(label == w.extendLabels[i]) {
                            if(w != selectedWindow && selectedWindow.choice != null) {
                                selectedHex = null;
                                selectedBase = null;
                                selectedForce = null;
                                Array<Window> ch = new Array<Window>(w.children);
                                for(Window wind: ch){
                                    closeWindow(wind);
                                }
                            }
                            label.changeStyle();
                            if (label.getStyle() == label.styleTwo) {
                                Force fc = w.forces.get(i);
                                selectedWindow = new Window(this, fc, label, X, Y);
                            }
                            else {
                                closeWindow(label.childWindow);
                                label.changeStyle();
                            }
                        }
                        else if(label == w.forceLabels[i]) {
                            if (forceToAttach != null) {
                                if (!(w.forces.get(i)).isUnit) {
                                    w.forces.get(i).attach(forceToAttach);
                                    closeWindows();
                                }
                                else if(forceToAttach.isUnit) {
                                    Unit u = (Unit)forceToAttach;
                                    Unit r = (Unit)(w.forces.get(i));
                                    if(u.type == r.type) {
                                        r.getReplenished(u);
                                        closeWindows();
                                    }
                                }
                            }
                            else {
                                if(selectedWindow != w) {
                                    Array<Window> ch = new Array<Window>(w.children);
                                    for(Window wind: ch){
                                        closeWindow(wind);
                                    }
                                    selectedHex = null;
                                    selectedBase = null;
                                    selectedForce = null;
                                }
                                else {
                                    selectedForce = w.forces.get(i);
                                    selectedWindow = new Window(this, w, selectedForce, X, Y);
                                }
                                return true;

                            }
                        }
                    }
                }

            }

        }


        return true;
    }
    //OLD VERSION
    /*{
        if (button == Input.Buttons.LEFT) {

            System.out.println("Touch UP Pos: " + getMousePosOnMap().x + " " + getMousePosOnMap().y);


            Actor actor = hit(getMousePosOnMap().x, getMousePosOnMap().y, true);

            if (selectedHex != null && actor instanceof Hex && selectedHex != (Hex) actor) {
                System.out.println("YES!");
                Hex start = paths.first().fromHex;
                Hex finish = paths.peek().toHex;

                if (selectedHex != endHex) {

                } else {
                    endHex = (Hex) actor;
                }


                mileStone.remove();
                double speed = INFANTRY.SPEED;
                //mileStone = new MileStone(end);
                mileStone = new MileStone(endHex);
                if (selectedForce != null) {
                    System.out.println("FORCE SELECTED");
                    speed = selectedForce.getForceSpeed();
                    selectedForce.order.setPathsOrder(paths);
                    selectedForce.order.mileStone = mileStone;
                }
                mileStone.days = Path.getDaysToGo(paths, speed);
                System.out.println(mileStone.days);
                addActor(mileStone);
                selectedHex = null;
                //TODO DRAG PATH METHOD!
            } else {
                selectedHex = null;
                //first touch
                if (startHex == null && endHex == null) {
                    //hex touched
                    if (actor instanceof Hex) {
                        startHex = (Hex) actor;
                    }
                    //force touched
                    if (actor instanceof Force) {
                        selectedForce = (Force) actor;
                        selectedForce.isSelected = true;
                        startHex = selectedForce.hex;

                        //orders has already been set
                        if (selectedForce.order.pathsOrder.size > 0) {
                            paths = selectedForce.order.pathsOrder;
                            mileStone = selectedForce.order.mileStone;
                            addActor(mileStone);
                        }
                    }
                }
                //second touch
                else if (startHex != null && endHex == null) {
                    if (mileStone != null) mileStone.remove();
                    //hex touched
                    if (actor instanceof Hex) {
                        endHex = (Hex) actor;

                        //first hex was touched
                        if (selectedForce == null) {
                            navigate(INFANTRY.SPEED);
                        }
                        //first force was touched
                        if (selectedForce != null) {
                            selectedForce.order.target = null;
                            navigate(selectedForce.getForceSpeed());
                            selectedForce.order.setPathsOrder(paths);
                            selectedForce.order.mileStone = mileStone;

                        }
                    }

                    //force touched
                    if (actor instanceof Force) {
                        Force force = (Force) actor;
                        endHex = force.hex;

                        //first hex was touched
                        if (selectedForce == null) {
                            navigate(INFANTRY.SPEED);
                        }

                        //first force was touched
                        if (selectedForce != null) {
                            navigate(selectedForce.getForceSpeed());
                            selectedForce.order.setPathsOrder(paths);
                            selectedForce.order.mileStone = mileStone;
                            //TODO attach?

                        }
                    }

                }
                //further touches
                else if (endHex != null) {
                    endHex = null;
                    graphPath = null;
                    paths = null;
                    if (selectedForce != null) selectedForce.isSelected = false;
                    selectedForce = null;
                    if (mileStone != null) mileStone.remove();
                    mileStone = null;

                    //hex touched
                    if (actor instanceof Hex) {
                        startHex = (Hex) actor;
                    }
                    //force touched
                    if (actor instanceof Force) {
                        selectedForce = (Force) actor;
                        selectedForce.isSelected = true;
                        startHex = selectedForce.hex;

                        //orders has already been set
                        System.out.println(selectedForce.order.pathsOrder);
                        if (selectedForce.order.pathsOrder.size > 0) {
                            paths = selectedForce.order.pathsOrder;
                            mileStone = selectedForce.order.mileStone;
                            addActor(mileStone);
                        }
                    }
                }
            }
        }


        return false;
    }*/

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {

        Hex current = getHex(getMousePosOnMap().x, getMousePosOnMap().y);
        if(isDrugging && current != drugHex) {

            Hex start = paths.first().fromHex;
            Hex finish = paths.peek().toHex;
            paths = navigate(start, current);
            paths.addAll(navigate(current, finish));
            double speed = drugForce == null? INFANTRY.SPEED : drugForce.getForceSpeed();
            mileStone.days = Path.getDaysToGo(paths, speed);
        }
//        if (selectedHex != null) {
//
//            Hex start = paths.first().fromHex;
//            Hex finish = paths.peek().toHex;
//            //Hex end = finish;
//
//            Hex current = getHex(getMousePosOnMap().x, getMousePosOnMap().y);
//            if (selectedHex != endHex) {
//
//                paths = navigate(start, current);
//                paths.addAll(navigate(current, finish));
//            } else {
//                System.out.println("FINISH SELECTED!");
//                //paths = navigate(start, endHex);
//                //paths.addAll(navigate(endHex, current));
//                //end = current;
//                //selectedPaths = paths;
//                paths = new Array<Path>(selectedPaths);
//                paths.addAll(navigate(endHex, current));
//                System.out.println("SELECTED PATH: " + selectedPaths.size + "TOTAL PATH: " + paths.size);
//            }
//        }

//        System.out.println("startHex = " + startHex + " endHex = " + endHex + " selectedHex = " + selectedHex);
//        System.out.println("selectedBase = " + selectedBase + " selectedForce = " + selectedForce + " forceToMove = " + forceToMove +
//                " forceToAttach = " + forceToAttach);
//        System.out.println("selectedWindow = " + selectedWindow + " paths = " + paths + " start = " + start +
//                " mileStone = " + mileStone + " currentStone = " + currentStone);

        return true;
    }
    private void clearActor(Actor... actors) {
        for (Actor actor : actors) {
            if (actor != null) {
                actor.remove();
                actor = null;
            }
        }
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (startHex != null && endHex == null) {
            Hex hex = getHex(getMousePosOnMap().x, getMousePosOnMap().y);
            if (hex != currentHex) {
                if (currentStone != null) {
                    currentStone.remove();
                }
                currentStone = new MileStone((hex));
                addActor(currentStone);
                currentHex = hex;
                Array<Path> trace = navigate(startHex, currentHex);
                double speed = INFANTRY.SPEED;
                if (forceToMove != null) speed = forceToMove.getForceSpeed();
                currentStone.days = Path.getDaysToGo(trace, speed);
            }
        } else {
            currentHex = null;
            if (currentStone != null) currentStone.remove();
            currentStone = null;
        }
        return true;
    }

    @Override
    public boolean scrolled(int amount) {
        if (amount == 1) camera.zoom += 0.2;
        else camera.zoom -= 0.2;
        return true;
    }

    public int getDaysToArrival(double speed) {
        if (paths != null) {
            double trip = 0;
            for (Path path : paths) trip += path.getDays(speed);
            return (int) Math.round(trip);
        }
        return 0;
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

    class MyInnerRenderer extends HexagonalTiledMapRenderer {
        public MyInnerRenderer(TiledMap map) {
            super(map);
        }

        @Override
        public void render() {
            super.render();

        }

        @Override
        public void renderObject(MapObject object) {
            float width = 14;
            float height = 14;
            if (object instanceof TextureMapObject) {
                TextureMapObject textureObj = (TextureMapObject) object;
                if (selectedForce != null && textureObj == selectedForce.symbol) {
                    width = 16;
                    height = 16;
                }
                this.getBatch().draw(textureObj.getTextureRegion(), textureObj.getX(), textureObj.getY(),
                        width, height);
            }
        }

    }
}