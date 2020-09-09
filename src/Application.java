
import ecs.ECS;
import ecs.Components.*;
import ecs.Systems.*;

public class Application {
    private ECS ecs;

    //------Initialize Components------//
    private Radius radius;                            //object radius
    private BallSplit ballSplit;                      //object split quantity
    private Position position;                        //object position
    private Velocity velocity;                        //object velocity
    private Collision collision;                      //collision groups
    private NoCollide noCollide;
    private Health health;                            //object health
    //private Render render;                          //current color base
    private Input input;                              //keeps track of user input
    private Polygon polygon;
    //private World world = new World(800,800);

    //------Initialize Systems---------//
    private ControlSys controlSys;                    //check for input (click, vector) and launch launcher
    private CollisionSys collisionSys;                //build collision tree
    private MovementSys movementSys;                  //update positions of moving entities
    private RenderSys renderSys;                      //color each object based on health, render to screen (paint screen)

    public Application() {
        ecs = new ECS(400,600);

        ecs.addComponent(
                radius       = new Radius(),
                ballSplit    = new BallSplit(),
                position     = new Position(),
                velocity     = new Velocity(),
                collision    = new Collision(),
                noCollide    = new NoCollide(),
                health       = new Health(),
                //render,
                input        = new Input(),
                polygon      = new Polygon()
                //world
        );

        ecs.addSystem(
                controlSys   = new ControlSys(),
                movementSys  = new MovementSys(),
                collisionSys = new CollisionSys()
        );

        ecs.addRenderer(
                renderSys    = new RenderSys(ecs.width, ecs.height)
        );

        initScene(20);
        run();
    }

    private void update(float delta){
        ecs.update(delta);
    }

    public void onExit(){
        ecs.exit();
    }

    private void initScene(int eRadius){
        //POLYGONS MUST BE CREATED COUNTER-CLOCKWISE

        for (int i = 0; i < 4; i++){
            for (int j = 0; j < 4; j++){
                int ballHealth = 100;

                //if (i%2==0 || j%2==0)
                //    ballHealth = 100;

                ecs.createEntity(
                        position.add(new float[]{j*80 + 80,i*80 + 80}),
                        health.add(ballHealth),
                        radius.add(eRadius),
                        ballSplit.add(2)
                );
            }
        }

        /*
        ecs.createEntity(
                position.add(new float[]{200,400}),
                polygon.add(new java.awt.Polygon(
                        new int[]{0,100,100,0},
                        new int[]{250,250,225,225},
                        4
                ))
        );
        ecs.createEntity(
                position.add(new float[]{200,400}),
                polygon.add(new java.awt.Polygon(
                        new int[]{300,400,400,300},
                        new int[]{250,250,225,225},
                        4
                ))
        );
        ecs.createEntity(
                position.add(new float[]{200,400}),
                polygon.add(new java.awt.Polygon(
                        new int[]{175,225,225,175},
                        new int[]{250,250,225,225},
                        4
                ))
        );

         */
        /*
        ecs.createEntity(
                position.add(new float[]{200,400}),
                polygon.add(new java.awt.Polygon(
                        new int[]{200,105,141,259,295},
                        new int[]{200-50,269-50,381-50,381-50, 269-50},
                        5
                ))
        );

         */
    }

    private void run(){
        double start,end,delta= 8;

        while(true){
            start = System.nanoTime();


            update((float)1/60);

            end = System.nanoTime();
            while (end-start < 16000000){
                end = System.nanoTime();
            }

        }
    }

    public static void main(String[] args) {
        Application app = new Application();
    }
}
