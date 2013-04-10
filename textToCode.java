import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class textToCode
{
   public static void main(String args[]) {
      System.out.println("public class Code {");
      System.out.println("public static String text = \"\"");
      Scanner scanner = new Scanner(System.in);
      while (scanner.hasNext()) {
	 String input = scanner.nextLine();

	 String line = "";
	 for (int i = 0 ; i < input.length() ; i++)
	    line += input.charAt(i) == '"' ? "\\\"" : (char)input.charAt(i);

	 System.out.println("+\"" + line + "\\n\"");
      }
      System.out.println(";}");
   }
}

