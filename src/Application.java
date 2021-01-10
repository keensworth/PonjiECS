
import ecs.ECS;
import ecs.Components.*;
import ecs.Events.Event;
import ecs.Events.EventManager;
import ecs.Systems.*;

public class Application {
    private ECS ecs;

    //------Initialize Components------//
    private Radius radius;                            //object radius
    private BallSplit ballSplit;                      //object split quantity
    private Position position;                        //object position
    private Rotation rotation;                        //object rotation
    private Velocity velocity;                        //object velocity
    private NoCollide noCollide;                      //noCollide grouping
    private Health health;                            //object health
    private Camera camera;                            //camera
    //private Render render;                          //current color base
    private Input input;                              //keeps track of user input
    private Shape polygon;
    private Points points;
    private World world;                              //stores terrain meshes

    //------Initialize Systems---------//
    private InputSys inputSys;                        //fetch input from GLFW
    private ControlSys controlSys;                    //check for input (click, vector) and launch launcher
    private CollisionSys collisionSys;                //build collision tree
    private ProceduralSys proceduralSys;              //procedurally generate terrain depending on player location
    private PointSys pointSys;
    private MovementSys movementSys;                  //update positions of moving entities
    private RenderSys renderSys;                      //color each object based on health, render to screen (paint screen)

    private final int WIDTH = 450;
    private final int HEIGHT = 900;
    protected final int RADIUS = 25;
    protected final float PI = (float) 3.14159;
    protected final float camDistance = (float) ((HEIGHT/2) * Math.sqrt(3));

    public Application() {
        ecs = new ECS(WIDTH, HEIGHT);


        ecs.addEventManager(new EventManager(
                new Event("example_event1"),
                new Event("example_event2")
        ));

        ecs.addRenderer(
                renderSys    = new RenderSys(ecs.width, ecs.height)
        );
        ecs.setWindow(renderSys.getWindow());

        ecs.addComponent(
                radius       = new Radius(),
                ballSplit    = new BallSplit(),
                position     = new Position(),
                rotation     = new Rotation(),
                velocity     = new Velocity(),
                noCollide    = new NoCollide(),
                health       = new Health(),
                camera       = new Camera(),
                //render,
                input        = new Input(),
                polygon      = new Shape(),
                points       = new Points(),
                world        = new World()
        );

        ecs.addSystem(
                inputSys      = new InputSys(),
                controlSys    = new ControlSys(),
                movementSys   = new MovementSys(),
                proceduralSys = new ProceduralSys(),
                collisionSys  = new CollisionSys(),
                pointSys      = new PointSys()

        );

        //initialize and run
        initScene(RADIUS);
        run();
    }

    private void initScene(int eRadius){
        ecs.createEntity(
                position.add(new float[]{0,0,camDistance}),
                rotation.add(new float[]{0,0,0}),
                camera.add()
        );

        for (int i = 0; i < 8; i ++){
            ecs.createEntity(
                    position.add(new float[]{0,-300 + i*100, 0}),
                    health.add((i+1)*12),
                    radius.add(eRadius),
                    ballSplit.add(2)
            );
        }

        for (int i = 0; i < 8; i ++){
            ecs.createEntity(
                    position.add(new float[]{75,-350 + i*100, 0}),
                    health.add((i+1)*12),
                    radius.add(eRadius),
                    ballSplit.add(2)
            );
        }

        for (int i = 0; i < 8; i ++){
            ecs.createEntity(
                    position.add(new float[]{-75,-350 + i*100, 0}),
                    health.add((i+1)*12),
                    radius.add(eRadius),
                    ballSplit.add(2)
            );
        }
    }

    //Main loop
    private void run(){
        double start,end;

        while(true){
            start = System.nanoTime();

            update((float)1/60);

            end = System.nanoTime();
            while (end-start < 16000000){
                end = System.nanoTime();
            }
        }
    }

    private void update(float delta){
        ecs.update(delta);
    }

    public void onExit(){
        ecs.exit();
    }

    public static void main(String[] args) {
        Application app = new Application();
    }
}
