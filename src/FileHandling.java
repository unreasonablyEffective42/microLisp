/*Extremely basic file IO system for microlisp
 *Needs considerable work 
 */

import java.util.Scanner;
import java.io.*;
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
            })
        );
  }
}
