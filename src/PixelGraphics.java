import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.util.function.BiFunction;

public class PixelGraphics{
    public int width, height;
    public BufferedImage canvas;

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
       if ((x0 < x && x <= this.width) && (y0 < y && y <= this.height)){
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
    

    public static void main(String[] args){
        int c = color (100, 200, 150);
        int b = color (100, 0  , 200);
        int r = color (255, 0  , 0  );
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
    }
}
