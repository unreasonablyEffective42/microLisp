import org.jcodec.api.awt.AWTSequenceEncoder;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class VideoEncoder {
    private VideoEncoder() {}

    private static final class RecordingSession {
        final AWTSequenceEncoder encoder;
        RecordingSession(AWTSequenceEncoder encoder) {
            this.encoder = encoder;
        }
    }

    private static final Map<PixelGraphics, RecordingSession> sessions = new HashMap<>();

    public static void addVideoEnv(Environment env) {
        env.addFrame(
            new Pair<>("start-recording",
                (TriFunction<PixelGraphics, Number, File, String>) (gfx, fpsNum, file) -> {
                    if (gfx == null || fpsNum == null || file == null) {
                        System.out.println("start-recording: expected canvas, fps, and file");
                        return "#f";
                    }
                    if (sessions.containsKey(gfx)) {
                        System.out.println("start-recording: recording already active for canvas");
                        return "#f";
                    }

                    int fps = Math.max(1, (int) fpsNum.intVal);
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        System.out.println("start-recording: unable to create directory " + parent);
                        return "#f";
                    }

                    try {
                        AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(file, fps);
                        sessions.put(gfx, new RecordingSession(encoder));
                        return "#t";
                    } catch (IOException e) {
                        System.out.println("start-recording failed: " + e.getMessage());
                        return "#f";
                    }
                }),

            new Pair<>("encode-frame",
                (Function<PixelGraphics, String>) gfx -> {
                    RecordingSession session = sessions.get(gfx);
                    if (session == null) {
                        System.out.println("encode-frame: no active recording for canvas");
                        return "#f";
                    }
                    try {
                        session.encoder.encodeImage(snapshot(gfx));
                        return "#t";
                    } catch (IOException e) {
                        System.out.println("encode-frame failed: " + e.getMessage());
                        return "#f";
                    }
                }),

            new Pair<>("stop-recording",
                (Function<PixelGraphics, String>) gfx -> {
                    RecordingSession session = sessions.remove(gfx);
                    if (session == null) {
                        System.out.println("stop-recording: no active recording for canvas");
                        return "#f";
                    }
                    try {
                        session.encoder.finish();
                        return "#t";
                    } catch (IOException e) {
                        System.out.println("stop-recording failed: " + e.getMessage());
                        return "#f";
                    }
                })
        );
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
