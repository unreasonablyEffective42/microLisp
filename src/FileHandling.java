/*Extremely basic file IO system for microlisp
 *Needs considerable work 
 */

import java.util.Scanner;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FileHandling{
  public static void addFileHandlingEnv(Environment env){
        env.addFrame(
            new Pair<>("read", (Supplier<Object>) () ->{
                Scanner sc = new Scanner(System.in);
                Parser parser = new Parser(sc.nextLine());
                return Evaluator.eval(parser.parse(), env);
            }),
            new Pair<>("read-from-file", (Function<Object, Object>) (name) ->{
                if (name instanceof File){
                    try (BufferedReader reader = new BufferedReader(new FileReader((File) name))){
                        StringBuilder sb = new StringBuilder("");
                        String src = reader.readLine();
                        while (src != null){
                            sb.append(src).append("\n"); // preserve newlines for callers that split on lines
                            src = reader.readLine();
                        }
                        return LinkedList.fromString(sb.toString());
                    }catch (IOException e){
                        System.out.println(e);
                        return LinkedList.fromString("IO failed");
                    }

                }
                else if (name instanceof LinkedList){
                    File f = new File(LinkedList.listToRawString((LinkedList) name));
                    try (BufferedReader reader = new BufferedReader(new FileReader(f))){
                        StringBuilder sb = new StringBuilder("");
                        String src = reader.readLine();
                        while (src != null){
                            sb.append(src).append("\n"); // preserve newlines for callers that split on lines
                            src = reader.readLine();
                        }
                        return LinkedList.fromString(sb.toString());
                    }catch (IOException e){
                        System.out.println(e);
                        return LinkedList.fromString("IO failed");
                    }
                }else {
                    return LinkedList.fromString("Error unknown");
                }
            }),
            new Pair<>("make-file", (Function<Object, File>) (name) -> {
                String filename;
                if (name instanceof String s) {
                    filename = s;
                } else if (name instanceof LinkedList<?> list && !list.isEmpty()) {
                    filename = LinkedList.listToRawString(list);
                } else {
                    throw new RuntimeException("make-file: expected string, got " + name.getClass().getSimpleName());
                }

                File f = new File(filename);
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    System.out.println("Error creating file: " + e.getMessage());
                }
                return f;
            }),
            new Pair<>("make-directory", (Function<Object, String>) (name) -> {
                String dirname;
                if (name instanceof String s) {
                    dirname = s;
                } else if (name instanceof LinkedList<?> list && !list.isEmpty()) {
                    dirname = LinkedList.listToRawString(list);
                } else {
                    throw new RuntimeException("make-directory: expected string, got " + name);
                }
                File dir = new File(dirname);
                if (dir.exists()) {
                    return dir.isDirectory() ? "#t" : "#f";
                }
                if (dir.mkdirs()) {
                    return "#t";
                }
                System.out.println("Error creating directory: " + dirname);
                return "#f";
            }),
            new Pair<>("write-to-file", (BiFunction<File, LinkedList, String>) (file, text)->{
                try(FileWriter writer = new FileWriter(file)){
                    writer.write(LinkedList.listToRawString(text));
                    return "#t";
                } catch (IOException e) {
                    System.out.println("Error writing to file: " + file.getName() + " " +e);
                    return "#f";
                }
            }),
            new Pair<>("write-lines", (BiFunction<File, LinkedList, String>) (file, lines) -> {
                try (FileWriter writer = new FileWriter(file)) {
                    LinkedList<?> current = lines;
                    while (current != null && current.head() != null) {
                        Object row = current.head();
                        String output;
                        if (row instanceof LinkedList<?> list) {
                            output = LinkedList.listToRawString(list);
                        } else if (row == null) {
                            output = "";
                        } else {
                            output = row.toString();
                        }
                        writer.write(output);
                        writer.write(System.lineSeparator());
                        Object tail = current.tail();
                        if (tail instanceof LinkedList<?> next) {
                            current = next;
                        } else {
                            break;
                        }
                    }
                    return "#t";
                } catch (IOException e) {
                    System.out.println("Error writing lines to file: " + file.getName() + " " + e);
                    return "#f";
                }
            }),
            new Pair<>("read-lines", (Function<Object, LinkedList<LinkedList<String>>>) (name) -> {
                File f;
                if (name instanceof File file) {
                    f = file;
                } else if (name instanceof LinkedList<?> list) {
                    f = new File(LinkedList.listToRawString(list));
                } else if (name instanceof String s) {
                    f = new File(s);
                } else {
                    System.out.println("read-lines: unsupported path type " + name);
                    return new LinkedList<>();
                }

                ArrayList<LinkedList<String>> lines = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(LinkedList.fromString(line));
                    }
                } catch (IOException e) {
                    System.out.println("read-lines IO failed: " + e.getMessage());
                    return new LinkedList<>();
                }
                return new LinkedList<>(lines);
            }),
            new Pair<>("split-by-comma", (Function<Object, LinkedList<LinkedList<String>>>) (value) -> {
                String s;
                if (value instanceof LinkedList<?> list) {
                    s = LinkedList.listToRawString(list);
                } else {
                    s = value == null ? "" : value.toString();
                }
                ArrayList<LinkedList<String>> cells = new ArrayList<>();
                int start = 0;
                for (int i = 0; i <= s.length(); i++) {
                    if (i == s.length() || s.charAt(i) == ',') {
                        String part = s.substring(start, i);
                        cells.add(LinkedList.fromString(part));
                        start = i + 1;
                    }
                }
                return new LinkedList<>(cells);
            }),
            new Pair<>("import", (Function<Object, String>) (resource) -> {
                String filename;
                if (resource instanceof Symbol sym) {
                    filename = sym.name;
                } else if (resource instanceof String s) {
                    filename = s;
                } else {
                    throw new RuntimeException("import: expected symbol or string, got " + resource);
                }
                if (!filename.endsWith(".mu"))
                    filename += ".mu"; 
                try {
                    InputStream in = resolveImportStream(filename);
                    // Read source and evaluate sequentially
                    String src = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    Parser parser = new Parser(src);
                    Node current = parser.parse();
                    while (!((Token) current.value).type().equals("EOF")) {
                        Evaluator.eval(current, env);
                        current = parser.parse();
                    }

                    return "#t";

                } catch (FileNotFoundException e) {
                    throw new RuntimeException("import: cannot find " + filename);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to import resource: " + filename, e);
                }
            })
        );
  }

  private static InputStream resolveImportStream(String filename) throws IOException {
      Path localPath = Path.of(filename);
      if (Files.exists(localPath)) {
          return Files.newInputStream(localPath);
      }

      Path found = findInWorkspace(filename);
      if (found != null) {
          return Files.newInputStream(found);
      }

      InputStream in = FileHandling.class.getResourceAsStream("/lib/" + filename);
      if (in != null) {
          return in;
      }

      throw new FileNotFoundException("File not found in lib/ or workspace: " + filename);
  }

  private static Path findInWorkspace(String filename) throws IOException {
      Path start = Path.of(".").toAbsolutePath().normalize();
      try (Stream<Path> stream = Files.walk(start)) {
          return stream
                  .filter(Files::isRegularFile)
                  .filter(p -> p.getFileName().toString().equals(filename))
                  .findFirst()
                  .orElse(null);
      }
  }
}
