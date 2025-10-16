import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.Scanner;
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
    
    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
    @FunctionalInterface 
    public interface QuadFunction<A, B, C, D, R>{
        R apply(A a, B b, C c, D d);
    }

    
    public static void addPixelGraphicsEnv(Environment env){
        env.addFrame(
            new Pair<>("create-window", (BiFunction<PixelGraphics,LinkedList,ImageDisplay>) (image, name) -> { 
                return new ImageDisplay(image,LinkedList.listToRawString(name));
            }),
            new Pair<>("refresh-window", (Consumer<ImageDisplay>) (window) -> window.refresh()),
            new Pair<>("create-graphics-device", (BiFunction<Number,Number,PixelGraphics>) (width, height) -> new PixelGraphics((int)width.intVal, (int)height.intVal)),
            new Pair<>("make-color", (TriFunction<Number,Number,Number,Integer>) (red,green,blue) -> (int)color((int)red.intVal, (int)green.intVal, (int)blue.intVal)),
            new Pair<>("draw-pixel", (QuadFunction<PixelGraphics,Number,Number,Integer,String>) (image,x,y,color)-> {
                try {
                    image.putPixel((int)x.intVal,(int)y.intVal,color);
                    return "#t";
                }
                catch (ArrayIndexOutOfBoundsException e){
                    System.out.println(e);
                    return "#f";
                }
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

    public void drawLine(int x0, int x, int y0, int y, int color){
       if (true){
          double slope = ((double) y - y0)/(x - x0);
          int yc = 0;
          for (int i = x0; i <= x; i++){
              yc = (int) Math.floor(i*slope);
              this.canvas.setRGB(i,yc+y0,color);
          }
       }else{
         System.out.println("out of bounds");
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
    

    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        int c = color (100, 200, 150);
        int b = color (100, 0  , 200);
        int r = color (255, 0  , 0  );
        int g = color (150, 0  , 150);
        int o = color (245, 167, 66 );
        int p = color (164, 66 , 245);

        PixelGraphics image = new PixelGraphics(400,300);
        image.fillCanvas(c);
        image.drawLine(0,300,50,100,b);
        image.drawCircle(90,150,30.0,r);
        image.plotImplicit(
            (x, y) -> Math.abs(Math.sqrt(Math.pow(x-50,2) + Math.pow(y-50,2)) - 20.0) < .5,
            b
        );

        File out = new File("../images/output.png");
        try {
            ImageIO.write(image.canvas, "png", out);
            System.out.println("PNG image saved successfully to "); 
        } catch (IOException e) {
            System.err.println("Error saving PNG image: " + e.getMessage());
            e.printStackTrace();
        }
        ImageDisplay window = new ImageDisplay(image,"Animation");
        String toss = sc.nextLine();
        image.drawLine(0,399,299,0,g);
        image.drawCircle(200,100,40.0,b);
        window.refresh();
        toss = sc.nextLine();
        image.drawLine(50,250,0,200,o);
        image.drawCircle(300,200,40,p);
        window.refresh();
        while (true){
            image.drawCircle(200,100,40.0,b);
            image.drawLine(0,300,50,100,r);
            window.refresh();
            wait(1000);
            image.drawCircle(200,100,40.0,r);
            image.drawLine(0,300,50,100,o);
            window.refresh();
            wait(1000);
            image.drawCircle(200,100,40.0,o);
            image.drawLine(0,300,50,100,g);
            window.refresh();
            wait(1000);
            image.drawLine(0,500,50,100,r);
            image.drawCircle(200,100,40.0,g);
            window.refresh();
            wait(1000);
        }
        
    }
}
