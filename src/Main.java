import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private static final String EMBED = "-embed";
    private static final String EXTRACT = "-extract";
    private static final String IN = "-in";
    private static final String P = "-p";
    private static final String OUT = "-out";
    private static final String STEG = "-steg";
    private static final String A = "-a";
    private static final String M = "-m";
    private static final String PASS = "-pass";

    private static String in;
    private static String p;
    private static String out;
    private static String steg;
    private static String a;
    private static String m;
    private static String pass;
    private static boolean embed =false;



    public static void main(String[] args) {
        //java -classpath ./target/classes ar.edu.itba.Main -embed -in <file> -p <in.bmp> -out <out.bmp> -steg <LSB1|LSB4|LSBI> [-a <aes128|aes192|aes256|des>] [-m <cbc|cfb|ofb|ecb>] [-pass <password>]
        //java -classpath ./target/classes ar.edu.itba.Main -extract -p <in.bmp> -out <file_name> -steg <LSB1|LSB4|LSBI> [-a <aes128|aes192|aes256|des>] [-m <cbc|cfb|ofb|ecb>] [-pass <password>]
        //i need to extract the arguments from the command line and save them in variables
        //then i need to call the corresponding function with the corresponding arguments

        //extract the arguments from the command line
        for(int i = 0; i < args.length; i++){
            switch(args[i]){
                case EMBED:
                    embed=true;
                    break;
                case EXTRACT:
                    break;
                case IN:
                    in = args[i+1];
                    break;
                case P:
                    p = args[i+1];
                    break;
                case OUT:
                    out = args[i+1];
                    break;
                case STEG:
                    steg = args[i+1];
                    break;
                case A:
                    a = args[i+1];
                    break;
                case M:
                    m = args[i+1];
                    break;
                case PASS:
                    pass = args[i+1];
                    break;
            }

        }
        verifyArgs();
        if(embed){
            try {
                embed(in, p, out, steg, a, m, pass);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            extract(p, out, steg, a, m, pass);
        }

    }

    private static void embed(String in, String p, String out, String steg, String a, String m, String pass) throws IOException {
        //call the embed function with the arguments
        byte[] inFile = Files.readAllBytes(Path.of(in));

//        stenograph(steg, out, p);

    }
    private static void extract(String p, String out, String steg, String a, String m, String pass){
        //call the extract function with the arguments
    }

    private static void verifyArgs(){//mejorar
        if(embed){
            if(in == null || p == null || out == null || steg == null){
                System.out.println("Missing arguments");
                System.exit(1);
            }
        }else{
            if(p == null || out == null || steg == null){
                System.out.println("Missing arguments");
                System.exit(1);
            }
        }

    }
}
