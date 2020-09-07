package ecs.Systems;

import ecs.Components.*;
import ecs.Components.Polygon;
import ecs.Entity;
import ecs.System;
import util.BitMask;
import util.ETree.EntNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;

public class RenderSys extends System {
    private Display display;

    private Entity[] balls;
    private Entity[] levelObjects;

    private Health health;
    private Position position;
    private Radius radius;
    private Input input;
    private Polygon polygon;

    private int[] ballHealthIndices;
    private int[] ballPositionIndices;
    private int[] ballRadiusIndices;
    private int[] levelObjectPositionIndices;
    private int[] levelObjectPolygonIndices;

    public RenderSys(int width, int height) {
        super(Health.class, Position.class, Radius.class);

        JFrame frame = new JFrame();
        display = new Display(width, height, 180, 50, 100);

        frame.add(display, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Ponji V0.1");
        //frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);

    }

    @Override
    public Class update(float dt, EntNode entityTree, BitMask componentMask, boolean entityChange) {
        java.lang.System.out.println("Updating RenderSys");

        balls = getEntities(entityTree);
        levelObjects = getEntities(entityTree, new Class[]{Position.class, Polygon.class});

        health = (Health) componentMask.getComponent(Health.class);
        position = (Position) componentMask.getComponent(Position.class);
        radius = (Radius) componentMask.getComponent(Radius.class);
        input = (Input) componentMask.getComponent(Input.class);
        polygon = (Polygon)  componentMask.getComponent(Polygon.class);

        ballHealthIndices = getComponentIndices(Health.class, balls, componentMask);
        ballPositionIndices = getComponentIndices(Position.class, balls, componentMask);
        ballRadiusIndices = getComponentIndices(Radius.class, balls, componentMask);
        //levelObjectPositionIndices = getComponentIndices(Position.class, levelObjects, componentMask);
        levelObjectPolygonIndices = getComponentIndices(Polygon.class, levelObjects, componentMask);

        display.repaint();

        return null;
    }


    @Override
    public void exit() {

    }

    class Display extends JPanel {
        int width,height;
        int red, blue, green;
        int negRed, negBlue, negGreen;
        int pointerLength;
        int pointerBaseX, pointerBaseY;

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(width, height);
        }

        Display(int width, int height, int red, int green, int blue) {
            this.width = width;
            this.height = height;

            this.red = red;
            this.green = green;
            this.blue = blue;

            this.negRed = 255-red;
            this.negGreen = 255-green;
            this.negBlue = 255-blue;

            pointerLength = (int)((float)height/(1.5));
            pointerBaseX = width/2;
            pointerBaseY = height - 30;

            setBackground(Color.black);
            setDoubleBuffered(true);
            addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseMoved(MouseEvent mouseEvent) {
                    int x = mouseEvent.getX();
                    int y = mouseEvent.getY();

                    input.setMove(new int[]{x,y});
                }
                @Override
                public void mouseDragged(MouseEvent mouseEvent) { }
            });
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    int x = mouseEvent.getX();
                    int y = mouseEvent.getY();

                    input.setClicked();
                    input.setClick(new int[]{x,y});
                }

                @Override
                public void mousePressed(MouseEvent mouseEvent) { }
                @Override
                public void mouseReleased(MouseEvent mouseEvent) { }
                @Override
                public void mouseEntered(MouseEvent mouseEvent) { }
                @Override
                public void mouseExited(MouseEvent mouseEvent) { }
            });
        }

        @Override
        public void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            try {
                //Draw balls
                for (int i = 0; i < balls.length; i++) {
                    int eHealth = health.getHealth(ballHealthIndices[i]);
                    int eRadius = radius.getRadius(ballRadiusIndices[i]);
                    float[] ePosition = position.getPosition(ballPositionIndices[i]);
                    //float[] ePosition = position.getPosition(position.getEntityIndex(entities[i].getEntityId()));

                    float adjColor = 1 - ((float) eHealth / 100);
                    Color color = new Color(
                            (int) (red + negRed * adjColor),
                            (int) (green + negGreen * adjColor),
                            (int) (blue + negBlue * adjColor)
                    );

                    g2d.setColor(color);
                    Shape circle = new Ellipse2D.Float(ePosition[0] - eRadius, ePosition[1] - eRadius, 2 * eRadius, 2 * eRadius);
                    //java.lang.System.out.println(count++ + " " + ePosition[0]);
                    g2d.fill(circle);
                    //java.lang.System.out.println("Entity:" + i + " " + ePosition[0] + " " + ePosition[1]);
                }

                //Draw level objects
                for (int i = 0; i < levelObjects.length; i++) {
                    //float[] objPosition = position.getPosition(levelObjectPositionIndices[i]);
                    java.awt.Polygon objPolygon = polygon.getPolygon(levelObjectPolygonIndices[i]);

                    g2d.setColor(Color.darkGray);
                    Shape tempPoly = new java.awt.Polygon(objPolygon.xpoints, objPolygon.ypoints, objPolygon.npoints);
                    g2d.fill(tempPoly);
                }

                g2d.setStroke(new BasicStroke(4.0F));

                int[] mousePos = input.getMove();
                int i = mousePos[0] - pointerBaseX;
                int j = mousePos[1] - pointerBaseY;
                float dist = (float) Math.sqrt(Math.pow(i,2) + Math.pow(j,2));
                float iNorm = i/dist;
                float jNorm = j/dist;

                float iComp = iNorm*pointerLength;
                float jComp = jNorm*pointerLength;

                float ratio = Math.abs(pointerBaseX / iComp);
                if (ratio < 1){
                    GradientPaint gp = new GradientPaint(pointerBaseX + 2*iComp*ratio, pointerBaseY , Color.red,pointerBaseX + iComp*(2*ratio-1), pointerBaseY + jComp , new Color(128,128,0,0), false);
                    g2d.setPaint(gp);
                    g2d.drawLine((int)(pointerBaseX + iComp*ratio), (int) (pointerBaseY + jComp*ratio ), (int) (pointerBaseX + iComp*(2*ratio-1)), (int) (pointerBaseY+ jComp ));
                }
                GradientPaint gp = new GradientPaint(pointerBaseX, pointerBaseY , Color.red,pointerBaseX +  iComp, pointerBaseY + jComp ,  new Color(128,128,0,0), false);
                g2d.setPaint(gp);
                g2d.drawLine(pointerBaseX, pointerBaseY , pointerBaseX + (int)iComp, pointerBaseY + (int)jComp );

                g2d.dispose();
            } catch (NullPointerException e){
                java.lang.System.out.println("Incorrect Draw");
            }
        }
    }

}

