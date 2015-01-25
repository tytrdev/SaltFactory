package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.mygdx.game.collision.ICollisionMask;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.entity.Player;
import com.mygdx.game.entity.playerutils.Keys;
import com.mygdx.game.manager.CameraManager;
import org.lwjgl.Sys;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dubforce on 1/21/2015.
 */
public class Level {
    //scaling factor
    public static float PIXELS_PER_METER = 50;
    public static float METERS_PER_PIXEL = 1/PIXELS_PER_METER;
    Player player;

    //world
    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;
    private OrthographicCamera box2DCamera;
    private SpriteBatch spriteBatch;
    private CameraManager cameraManager;
    public String currentLevel;

    //physics
    private World world;
    TiledMap map;
    TiledMap backgroundLayers;
    Box2DDebugRenderer debugRenderer;

    //entities
    private List<Entity> entities;

    public Vector2 getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(Vector2 startLocation) {
        this.startLocation = startLocation;
    }

    //start location
    private Vector2 startLocation;

    public boolean doorOpen = false, dead = false;
    public MapObjects doors;
    public Audio audio;

    public Level(String fileName) {
        currentLevel = fileName;
        audio = new Audio();
        map = new TmxMapLoader().load(fileName);
        renderer = new OrthogonalTiledMapRenderer(map);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        box2DCamera = new OrthographicCamera();
        box2DCamera.setToOrtho(false, Gdx.graphics.getWidth() / PIXELS_PER_METER, Gdx.graphics.getHeight() / PIXELS_PER_METER);

        renderer.setView(camera);
        camera.update();
        spriteBatch = new SpriteBatch();

        //Set up backgroundMap
        TiledMapTileLayer background = (TiledMapTileLayer) map.getLayers().get("background");
        TiledMapTileLayer middleground1 = (TiledMapTileLayer) map.getLayers().get("middleground1");
        TiledMapTileLayer middleground = (TiledMapTileLayer) map.getLayers().get("middleground");
        TiledMapTileLayer middleground2 = (TiledMapTileLayer) map.getLayers().get("middleground2");

        backgroundLayers = new TiledMap();
        backgroundLayers.getLayers().add(background);
        backgroundLayers.getLayers().add(middleground1);
        backgroundLayers.getLayers().add(middleground);
        backgroundLayers.getLayers().add(middleground2);



        cameraManager = new CameraManager(box2DCamera, camera, renderer);

        entities = new ArrayList<Entity>();

        world = new World(new Vector2(0,-50f), true);

        

        //Create physics bodies for the ground.
        try {
            MapObjects ground = map.getLayers().get("solid").getObjects();


            for(MapObject object : ground)
            {
                Shape shape;
                if (object instanceof RectangleMapObject) {

                    shape = getRectangle((RectangleMapObject)object);
                }
                else if (object instanceof PolygonMapObject) {
                    shape = getPolygon((PolygonMapObject)object);
                }
                else if (object instanceof PolylineMapObject) {
                    shape = getPolyline((PolylineMapObject)object);
                }
                else
                    continue;

                    BodyDef bd = new BodyDef();
                    bd.type = BodyDef.BodyType.StaticBody;

                    Body body = world.createBody(bd);

                    FixtureDef fixtureDef = new FixtureDef();
                    fixtureDef.shape = shape;
                    fixtureDef.filter.categoryBits = ICollisionMask.GROUND;
                    fixtureDef.filter.maskBits = ICollisionMask.PLAYER | ICollisionMask.ENEMY;

                    body.createFixture(fixtureDef);

                    body.getFixtureList().first().setFriction(0);


                //bd.position.set(new Vector2(((RectangleMapObject) object).getRectangle().x / PIXELS_PER_METER, ((RectangleMapObject) object).getRectangle().y / PIXELS_PER_METER));
                //bd.position.set(((RectangleMapObject) object).getRectangle().getX(), ((RectangleMapObject) object).getRectangle().getY());

                //entities.add(body);
            }

            try {
                //Create physics bodies for death objects.
                MapObjects death = map.getLayers().get("death").getObjects();


                for (MapObject object : death) {
                    if (object instanceof RectangleMapObject) {
                        Shape shape;
                        shape = getRectangle((RectangleMapObject) object);

                        BodyDef bd = new BodyDef();
                        bd.type = BodyDef.BodyType.StaticBody;

                        Body body = world.createBody(bd);

                        FixtureDef fixtureDef = new FixtureDef();
                        fixtureDef.shape = shape;
                        fixtureDef.filter.categoryBits = ICollisionMask.ENEMY;
                        fixtureDef.filter.maskBits = ICollisionMask.PLAYER | ICollisionMask.ENEMY;

                        body.createFixture(fixtureDef);

                        body.getFixtureList().first().setFriction(0);
                        shape.dispose();
                    } else
                        continue;
                }
            }catch(Exception e){}


            doors = map.getLayers().get("teleport").getObjects();

            for(MapObject door : doors)
            {
                if(door.getProperties().get("start") != null) {
                    startLocation = new Vector2(((RectangleMapObject)door).getRectangle().x,
                            ((RectangleMapObject)door).getRectangle().y);
                }
                else if (door instanceof RectangleMapObject) {
                    Shape shape;
                    shape = getRectangle((RectangleMapObject)door);

                    BodyDef bd = new BodyDef();
                    bd.type = BodyDef.BodyType.StaticBody;

                    Body body = world.createBody(bd);

                    FixtureDef fixtureDef = new FixtureDef();
                    fixtureDef.shape = shape;
                    fixtureDef.filter.categoryBits = ICollisionMask.DOOR;
                    fixtureDef.filter.maskBits = ICollisionMask.PLAYER;
                    fixtureDef.isSensor = true;

                    Fixture fixture = body.createFixture(fixtureDef);
                    fixture.setUserData(door.getProperties().get("level"));

                    body.getFixtureList().first().setFriction(0);
                    shape.dispose();
                }
                else
                    continue;


                //bd.position.set(new Vector2(((RectangleMapObject) object).getRectangle().x / PIXELS_PER_METER, ((RectangleMapObject) object).getRectangle().y / PIXELS_PER_METER));
                //bd.position.set(((RectangleMapObject) object).getRectangle().getX(), ((RectangleMapObject) object).getRectangle().getY());

                //entities.add(body);
            }

            instantiateItems(map.getLayers().get("objects").getObjects(), world);

        } catch(Exception e){
            System.out.println(e.toString());
        }

        debugRenderer = new Box2DDebugRenderer();
    }

    public void update(float deltaTime)
    {
        camera.update();

        world.step(deltaTime, 1, 1);

        //Update other entities
        for(Entity entity : entities) {
            entity.update(deltaTime);
            if(entity instanceof Player){
                Player tempPlayer = (Player)entity;
                if(tempPlayer.onDoor && Keys.keyDown(Keys.UP)){
                    doorOpen = true;
                }
                if(tempPlayer.dead)
                {
                    dead = true;
                }
            }
        }
        audio.update(deltaTime);

    }

    public void draw()
    {
        renderer.setMap(backgroundLayers);
        renderer.render();
        renderer.setMap(map);

        renderer.render();

        renderer.getBatch().begin();
        if (map != null) {

            TiledMapTileLayer foreground = (TiledMapTileLayer) map.getLayers().get("foreground");

            for (Entity entity : entities) {
                entity.draw();
            }

            renderer.renderTileLayer(foreground);

        }
        renderer.getBatch().end();

        debugRenderer.render(world, box2DCamera.combined);
    }

    public OrthogonalTiledMapRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(OrthogonalTiledMapRenderer renderer) {
        this.renderer = renderer;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public void setCamera(OrthographicCamera camera) {
        this.camera = camera;
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    public void setSpriteBatch(SpriteBatch spriteBatch) {
        this.spriteBatch = spriteBatch;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void initializePlayer()
    {
        player = new Player(this,(SpriteBatch)renderer.getBatch(), cameraManager);

        BodyDef playerBodyDef = new BodyDef();
        playerBodyDef.type = BodyDef.BodyType.DynamicBody;
        playerBodyDef.position.set(startLocation.x / PIXELS_PER_METER, startLocation.y / PIXELS_PER_METER);

        Body playerBody = world.createBody(playerBodyDef);

        PolygonShape playerBox = new PolygonShape();
        playerBox.setAsBox((player.getSprite().getWidth() / 4)/PIXELS_PER_METER, (player.getSprite().getHeight() /2)/PIXELS_PER_METER);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = playerBox;
        fixtureDef.filter.categoryBits = ICollisionMask.PLAYER;
        fixtureDef.filter.maskBits = ICollisionMask.GROUND | ICollisionMask.ENEMY | ICollisionMask.WALL |
                ICollisionMask.ITEM;

        playerBody.createFixture(fixtureDef);
        playerBox.dispose();

        player.setBody(playerBody);

        entities.add(player);
        audio.playMusic();
        audio.getPlayer(player);
        world.setContactListener(player);
    }

    public static PolygonShape getRectangle(RectangleMapObject rectangleObject) {
        Rectangle rectangle = rectangleObject.getRectangle();
        PolygonShape polygon = new PolygonShape();
        Vector2 size = new Vector2((rectangle.x + rectangle.width * 0.5f) / PIXELS_PER_METER,
                (rectangle.y + rectangle.height * 0.5f ) / PIXELS_PER_METER);
        polygon.setAsBox(rectangle.width * 0.5f / PIXELS_PER_METER,
                rectangle.height * 0.5f / PIXELS_PER_METER,
                size,
                0.0f);

        //polygon.setAsBox(rectangle.width / PIXELS_PER_METER, rectangle.height / PIXELS_PER_METER);
        //rectangle.setPosition(rectangleObject.getRectangle().x, rectangleObject.getRectangle().y);
        return polygon;
    }

<<<<<<< Temporary merge branch 1
    private void instantiateItems(MapObjects mapObjects, World world) {
        for(MapObject mapObject : mapObjects) {
            String classToInstantiate = mapObject.getProperties().get("class").toString();

            if(classToInstantiate != null) {
                if(classToInstantiate.equalsIgnoreCase(IItemClass.JUMP_REFRESHER)) {
                    entities.add(new JumpRefresher(mapObject, world));
                }
            }
        }
    }
=======
    private static ChainShape getPolyline(PolylineMapObject polylineObject) {
        float[] vertices = polylineObject.getPolyline().getTransformedVertices();
        Vector2[] worldVertices = new Vector2[vertices.length / 2];

        for (int i = 0; i < vertices.length / 2; ++i) {
            worldVertices[i] = new Vector2();
            worldVertices[i].x = vertices[i * 2] / PIXELS_PER_METER;
            worldVertices[i].y = vertices[i * 2 + 1] / PIXELS_PER_METER;
        }

        ChainShape chain = new ChainShape();
        chain.createChain(worldVertices);
        return chain;
    }

    private static PolygonShape getPolygon(PolygonMapObject polygonObject) {
        PolygonShape polygon = new PolygonShape();
        float[] vertices = polygonObject.getPolygon().getTransformedVertices();

        float[] worldVertices = new float[vertices.length];

        for (int i = 0; i < vertices.length; ++i) {
            System.out.println(vertices[i]);
            worldVertices[i] = vertices[i] / PIXELS_PER_METER;
        }

        polygon.set(worldVertices);
        return polygon;
    }


>>>>>>> Temporary merge branch 2
}
