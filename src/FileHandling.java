/*Extremely basic file IO system for microlisp
 *Needs considerable work 
 */

import java.util.Scanner;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
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
                            sb.append(src);
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
                            sb.append(src);
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
            new Pair<>("write-to-file", (BiFunction<File, LinkedList, String>) (file, text)->{
                try(FileWriter writer = new FileWriter(file)){
                    writer.write(LinkedList.listToRawString(text));
                    return "#t";
                } catch (IOException e) {
                    System.out.println("Error writing to file: " + file.getName() + " " +e);
                    return "#f";
                }
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
                try (InputStream inClasspath = FileHandling.class.getResourceAsStream("/lib/" + filename)) { 
                    InputStream in;
                    if (inClasspath != null) {
                        in = inClasspath;
                    } else { 
                        Path localPath = Path.of(filename);
                        if (!Files.exists(localPath)) {
                            throw new FileNotFoundException("File not found in lib/ or current directory: " + filename);
                        }
                        in = Files.newInputStream(localPath);
                    }
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
                    // neither in /lib/ nor in local filesystem
                    throw new RuntimeException("import: cannot find " + filename);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to import resource: " + filename, e);
                }
            })
        );
  }
}
