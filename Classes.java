package VectorFields;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class DrawField {

    //TODO : add color to the arrows, push github, try to make animations

    public static void main(String args[]) throws IOException, InterruptedException {
        Vector[] positions = {
                new Vector(0,0), // the position following the cursor
                new Vector(0, 0),
                new Vector(0, 0.4),
                new Vector(0.1, 0.4),
                new Vector(0.2, 0.4),
                new Vector(0.3, 0.4),
                new Vector(0.4, 0.4),
                new Vector(0.5, 0.4),
                new Vector(0.6, 0.4),
                new Vector(0.7, 0.4)
        };
        double[] charges = {1, -1, -1, 1,-1, 1,-1, 1,-1, 1,}; // that part could be changed through an interface
        double x1 = -1;
        double x2 = 2;
        double y1 = -0.7;
        double y2 = 0.7;
        int stepCountX = 80;
        int defX = 1700;

        VectorField E = new ElectricField(positions, charges);
        BufferedImage image = draw(E, x1, x2, y1, y2, stepCountX, defX);

        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        JLabel label = new JLabel(new ImageIcon(image));
        frame.getContentPane().add(label);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        while(true) {
            Point p = label.getMousePosition();
            if (p == null) continue;
            double x = ((x2 - x1) * (p.x)) / image.getWidth() + x1;
            double y = (y2 - y1) * (image.getHeight() - p.y) / image.getHeight() + y1;
            positions[0] = new Vector(x, y);
            E = new ElectricField(positions, charges);
            image = draw(E, x1, x2, y1, y2, stepCountX, defX);

            label.setIcon(new ImageIcon(image));
        }
    }

    public static BufferedImage draw(VectorField F,
                                     double x1, double x2, double y1, double y2,
                                     int stepCountX, int defX) {

        BufferedImage image = new BufferedImage(
                defX, (int) (defX * (y2 - y1) / (x2 - x1)), BufferedImage.TYPE_INT_RGB);

        for(int i = 0; i <  image.getWidth(); ++i){ //black background
            for(int j = 0; j <  image.getHeight(); ++j){
                safeSetRGB(image, i, j, 0x0);
            }
        }

        int step = defX / stepCountX;

        for(int i = 0; i < image.getWidth(); i += step) {
            for(int j = 0; j < image.getHeight(); j += step) {
                double x = x1 + (x2 - x1) * (double)i / image.getWidth(); //could be optimised
                double y = y1 + (y2 - y1) * (double)j / image.getHeight();
                drawArrow(image, F.value(x, y), step, i, j);
            }
        }


        //axes
        int xAxis = (int) (-x1 * image.getWidth() / (x2 - x1));
        if(xAxis >= 0 && xAxis < image.getWidth()){
            for(int y = 0; y < image.getHeight(); ++y) safeSetRGB(image, xAxis, y, 0x00FF00);
        }
        int yAxis = (int) (-y1 * image.getHeight() / (y2 - y1));
        if(yAxis >= 0 && yAxis < image.getHeight()){
            for(int x = 0; x < image.getWidth(); ++x) safeSetRGB(image, x, yAxis, 0x007FFF);
        }



        return image;
    }

    public static void drawArrow(BufferedImage image, Vector direction, int size, int x, int y) {

        double norm = direction.norm;
        double dx = direction.x / norm;
        double dy = direction.y / norm;
        double x_ = x;
        double y_ = y;

        //Shaft
        int length = (int) Math.max(Math.min(size, norm), size / 4);
        int color = colorFunction(norm);
        for(int i = 0; i < length; ++i) {
            safeSetRGB(image, (int)x_, (int)y_, color);
            x_ += dx;
            y_ += dy;
        }

        //Spike
        Vector leftBit = rotated(direction, -3 * Math.PI / 4);
        Vector rightBit = rotated(direction, 3 * Math.PI / 4);
        double x_save = x_;
        double y_save = y_;
        double lx = leftBit.x / norm;
        double ly = leftBit.y / norm;
        //draws the arrow's spike left
        for(int i = 0; i < length / 4; ++i) {
            safeSetRGB(image, (int)x_, (int)y_, color);
            x_ += lx;
            y_ += ly;
        }
        x_ = x_save; y_ = y_save;
        double rx = rightBit.x / norm;
        double ry = rightBit.y / norm;
        //draws the arrow's spike right
        for(int i = 0; i < length / 4; ++i) {
            safeSetRGB(image, (int)x_, (int)y_, color);
            x_ += rx;
            y_ += ry;
        }


    }

    public static Vector rotated(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vector(cos * v.x - sin * v.y, sin * v.x + cos * v.y);
    }

    public static void safeSetRGB(BufferedImage image, int x, int y, int color) {
        if(x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) return;
        image.setRGB(x, image.getHeight() - y - 1, color);
    }

    public static int colorFunction(double norm) {
        double normLog = (Math.log(norm) - 3) * 10;

        int b = normLog >= 0 ? 0 : (int) (-255 * normLog / (1 - normLog));
        int g = (int) Math.max(0, 255 - Math.pow(normLog, 2));
        int r = normLog <= 0 ? 0 : (int) (255 * normLog / (1 + normLog));

        return (r << 16) | (g << 8) | b;
    }
}

class ElectricField implements VectorField {
    Vector[] positions;
    double[] charges;

    public ElectricField(Vector[] positions, double[] charges){
        if(positions.length != charges.length)
            throw new IllegalArgumentException("different charges and positions arrays lengths!");
        this.positions = positions.clone();
        this.charges = charges.clone();
    }

    @Override
    public Vector value(double x, double y) {
        Vector[] relativePos = new Vector[positions.length];
        for(int i = 0; i < relativePos.length; ++i)
            relativePos[i] = new Vector(x - positions[i].x, y - positions[i].y);

        double rx = 0;
        for(int i = 0; i < relativePos.length; ++i)
            rx += relativePos[i].x * charges[i] / Math.pow(relativePos[i].norm, 3);

        double ry = 0;
        for(int i = 0; i < relativePos.length; ++i)
            ry += relativePos[i].y * charges[i] / Math.pow(relativePos[i].norm, 3);

        return new Vector(rx, ry);
    }
}

@FunctionalInterface
interface VectorField {
    Vector value(double x, double y);
}

class Vector {
    final double x;
    final double y;
    final double norm;

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
        this.norm = Math.sqrt(x*x + y*y);
    }


    @Override
    public String toString() {
        return "[" + x + ", " + y  +"]";
    }
}
