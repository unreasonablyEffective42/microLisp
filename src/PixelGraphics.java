import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.lang.Thread;


class ImageDisplay extends JFrame {
    private JLabel imageLabel;
    private BufferedImage image;

    public ImageDisplay(PixelGraphics image,String name){
        this.image = image.canvas;
        setTitle(name);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(image.width, image.height);
        setLocationRelativeTo(null);

        imageLabel = new JLabel(new ImageIcon(this.image));
        add(imageLabel);

        setVisible(true);
    }

    public void refresh() {
        imageLabel.setIcon(new ImageIcon(image));
        imageLabel.revalidate();
        imageLabel.repaint();
    }
}

public class PixelGraphics{
    public int width, height;
    public BufferedImage canvas;

    
    public static void addPixelGraphicsEnv(Environment env){
        env.addFrame(
            new Pair<>("create-window", (BiFunction<PixelGraphics,LinkedList,ImageDisplay>) (image, name) -> { 
                return new ImageDisplay(image,LinkedList.listToRawString(name));
            }),
            new Pair<>("refresh-window", (Function<ImageDisplay,String>) (window) -> { 
                window.refresh();
                return "#t"; 
            }),
            new Pair<>("close-window", (Function<ImageDisplay,String>) (window) -> {
                window.dispose();
                return "#t";
            }),
            new Pair<>("create-graphics-device", (BiFunction<Number,Number,PixelGraphics>) (width, height) -> {
                return new PixelGraphics((int)width.intVal, (int)height.intVal);
            }),
            new Pair<>("write-image", (BiFunction<PixelGraphics,File,String>) (graphicsDevice, file) ->{
                try {
                    ImageIO.write(graphicsDevice.canvas, "png", file);
                    System.out.println("Image " + file.getName() + " saved successfully");
                    return "#t";
                } catch (IOException e) {
                    System.err.println("Error saving PNG image: " + e.getMessage());
                    e.printStackTrace();
                    return "#f";
                }
            }),
            new Pair<>("make-color", (TriFunction<Number,Number,Number,Integer>) (red,green,blue) -> {
                return (int)color((int)red.intVal, (int)green.intVal, (int)blue.intVal);
            }),
            new Pair<>("make-rgba", (QuadFunction<Number,Number,Number,Number,Integer>) (alpha,red,green,blue) -> {
                return (int)color((int)alpha.intVal, (int)red.intVal, (int)green.intVal, (int)blue.intVal);
            }),
            new Pair<>("draw-pixel", (QuadFunction<PixelGraphics,Number,Number,Integer,String>) (image,x,y,color)-> {
                try {
                    image.putPixel((int)x.intVal,(int)y.intVal,color);
                    return "#t";
                }
                catch (ArrayIndexOutOfBoundsException e){
                    System.out.println(e);
                    return "#f";
                }
            }),
            new Pair<>("fill", (BiFunction<PixelGraphics, Integer, String>) (img,color) ->{
                img.fillCanvas(color);
                return "#t";
            }),
            new Pair<>("wait", (Function<Number,String>) (time) -> {
                wait((int)time.intVal);
                return "#t";
            }),
            new Pair<>("image-width", (Function<PixelGraphics,Integer>) (img) -> {
                return img.width;
            }),
            new Pair<>("image-height", (Function<PixelGraphics,Integer>) (img) -> {
                return img.height;
            }),
            new Pair<>("lines", (HexFunction<PixelGraphics, Number, Number, Number, Number, Integer, String>)
                (img, x0, y0, x1, y1, color) -> {
                    img.drawLineBresenham((int) x0.intVal,(int) y0.intVal,(int) x1.intVal,(int) y1.intVal, (int) color);
                    return "#t";
            }),
            new Pair<>("circle", (PentaFunction<PixelGraphics, Number, Number, Number, Integer, String>)
                (img, cx, cy, r, color) -> {
                    // Defensive: ignore negative radii
                    int R = (int) r.intVal;
                    if (R < 0) return "#t";
                    img.drawCircleBresenham((int) cx.intVal, (int) cy.intVal, R, color);
                    return "#t";
            })
        );
    }

    public PixelGraphics(int width, int height){
        this.width = width;
        this.height = height;
        this.canvas = new BufferedImage(width,height, BufferedImage.TYPE_INT_ARGB);
    }

    public static int color(int r, int g, int b){
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }

    public static int color(int a, int r, int g, int b){
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    public static int getA(int color){
        return (color >> 24) & 0xFF;
    }

    public static int getR(int color){
        return (color >> 16) & 0xFF;
    }

    public static int getG(int color){
        return (color >> 8) & 0xFF;
    }

    public static int getB(int color){
        return color & 0xFF;
    }

    public void fillCanvas(int color){
        for (int i = 0; i < this.width; i++){
            for (int j = 0; j < this.height; j++){
                canvas.setRGB(i,j,color);
            }
        }
    }

    public void putPixel(int x, int y, int color){
        canvas.setRGB(x,y,color);
    }


    // Inside PixelGraphics (add anywhere among instance methods)
    private void putPixelSafe(int x, int y, int color) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            canvas.setRGB(x, y, color);
        }
    }

    public void plotImplicit(BiFunction<Integer,Integer ,Boolean> isPoint,int color){
        for (int i = 0; i < this.width; i++){
            for (int j = 0; j < this.height; j++){
                if (isPoint.apply(i,j)){
                  this.canvas.setRGB(i,j,color);
                }
            }
        }   
    }

    public void drawLineBresenham(int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy; // = dx - |dy|

        while (true) {
            putPixelSafe(x0, y0, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = err << 1;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    private void plot8(int cx, int cy, int x, int y, int color) {
        putPixelSafe(cx + x, cy + y, color);
        putPixelSafe(cx + y, cy + x, color);
        putPixelSafe(cx + y, cy - x, color);
        putPixelSafe(cx + x, cy - y, color);
        putPixelSafe(cx - x, cy + y, color);
        putPixelSafe(cx - y, cy + x, color);
        putPixelSafe(cx - y, cy - x, color);
        putPixelSafe(cx - x, cy - y, color);
    }

    public void drawCircleBresenham(int cx, int cy, int r, int color) {
        if (r < 0) return;
        int x = 0;
        int y = r;
        int d = 1 - r;            // decision variable

        plot8(cx, cy, x, y, color);
        while (y > x) {
            if (d < 0) {
                // Choose E: d += 2x + 3
                d += (x << 1) + 3;
            } else {
                // Choose SE: d += 2(x - y) + 5 ; y--
                d += ((x - y) << 1) + 5;
                y--;
            }
            x++;
            plot8(cx, cy, x, y, color);
        }
    }

    public void drawCircle(int x, int y, double r, int color){
        this.plotImplicit(
            (x1, y1) -> Math.abs(Math.sqrt(Math.pow(x1-x,2) + Math.pow(y1-y,2)) - r) < .5,
            color
        );
    }
    
    public static void wait(int ms){ 
        try {
            // Pause for 2 seconds (2000 milliseconds)
            Thread.sleep(ms); 
        } catch (InterruptedException e) {
            System.err.println("Thread was interrupted while sleeping.");
            Thread.currentThread().interrupt(); 
        }
    }
}
