
import org.jcodec.api.awt.AWTSequenceEncoder;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.awt.Graphics2D;

class FrameBuffer {
    public List<BufferedImage> frames = new ArrayList<BufferedImage>();
    public void addFrame(BufferedImage frame){
        frames.add(frame);
    }
}

public final class VideoEncoder {
    private VideoEncoder() {}
    
    public static void addVideoEnv(Environment env){
        env.addFrame(
            new Pair<> ("make-frame-buffer", (Supplier<FrameBuffer>) () -> {
                return new FrameBuffer();
            }),
            new Pair<> ("add-frame", (BiFunction<FrameBuffer, PixelGraphics, String>) (buffer, gfx) -> {
                buffer.addFrame(snapshot(gfx));
                return "#t";
            }),
            new Pair<> ("encode", (TriFunction<FrameBuffer, Number, File, String>) (frames, fps, file) -> {
                try {
                    encode(frames.frames, (int)fps.intVal, file);
                    return "#t";
                }
                catch (IOException e){
                    System.out.println(e);
                    return "#f";
                }
            })
        );
    }

    public static void encode(List<BufferedImage> frames, int fps, File target) throws IOException {
        AWTSequenceEncoder enc = AWTSequenceEncoder.createSequenceEncoder(target, fps);
        boolean completed = false;
        try {
            for (BufferedImage frame : frames) {
                enc.encodeImage(frame);
            }
            enc.finish();
            completed = true;
        } finally {
            if (!completed) {
                try {
                    enc.finish();
                } catch (IOException ignored) {
                    // ignore secondary exception to preserve original failure
                }
            }
        }
    }

    private static BufferedImage snapshot(PixelGraphics gfx) {
        BufferedImage src = gfx.canvas;
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }
}
