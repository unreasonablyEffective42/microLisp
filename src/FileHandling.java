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
      new Pair<>("read-from-file", (Function<String, String>) (name) ->{
          File f = new File(name);
          try (BufferedReader reader = new BufferedReader(new FileReader(name))){
              StringBuilder sb = new StringBuilder("");
              String src = reader.readLine();
              while (src != null){
                 sb.append(src);
                 src = reader.readLine();
              }
              return sb.toString();
          }catch (IOException e){
              System.out.println(e);
              return "IO failed";
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
  
  public static void main(String[] args){
    try{
      File f = new File("text.txt");
      //BufferedReader reader = new BufferedReader(new FileReader("input.txt"));
      FileWriter writer = new FileWriter(f);
      writer.write("Hello, World!\n"); 
      writer.write("FileIO");
      writer.close();
      System.out.println("Wrote to file :" + f.getName());
    } catch(IOException e){
      e.printStackTrace();
    }
  }
}
