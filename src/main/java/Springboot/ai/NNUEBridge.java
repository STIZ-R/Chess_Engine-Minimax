package Springboot.ai;

import java.io.File;

public class NNUEBridge {

    private static final String BIG_NET_PATH = "src/main/resources/NNUE/nn-b1a57edbea57.nnue";
    private static final String SMALL_NET_PATH ="src/main/resources/NNUE/nn-baff1ede1f90.nnue";


    //    static {
//        File dll = new File("probe.dll");
//        System.load(dll.getAbsolutePath());
//    }

    /**
     * Lance le bon fichier selon l'os de la personne.
     */
//    static {
//        String os = System.getProperty("os.name").toLowerCase();
//
//        if (os.contains("win")) {
//            //System.out.println("windows");
//            File dll = new File("libs/probe.dll");
//            System.load(dll.getAbsolutePath());
//        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
//            //System.out.println("other");
//            System.load("libs/probe.so");
//        } else {
//            throw new UnsupportedOperationException("Unsupported operating system: " + os);
//        }
//    }

    static {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            File libraryFile;
            if (os.contains("win")) {
                libraryFile = new File("libs/probe.dll");
            } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                libraryFile = new File("libs/probe.so");
            } else {
                throw new UnsupportedOperationException("Unsupported OS: " + os);
            }

            String absolutePath = libraryFile.getAbsolutePath();
            System.out.println("Attempting to load library from: " + absolutePath);

            if (!libraryFile.exists()) {
                System.err.println("Library file not found at: " + absolutePath);
                System.out.println("Falling back to system library path...");
                System.loadLibrary("probe");
            } else {
                System.load(absolutePath);
                System.out.println("Library successfully loaded from: " + absolutePath);
            }
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Error loading native library: " + e.getMessage());
            throw new RuntimeException("Failed to load native library: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            throw new RuntimeException("Initialization failed: " + e.getMessage(), e);
        }


    }

    public static native void init(String bigNet, String smallNet);

    public static native int evalFen(String fen);

    public static native int evalArray(int[] pieceBoard, int side, int rule50);

    public static native int fasterEvalArray(int[] pieces, int[] squares, int pieceAmount, int side, int rule50);

    public static void main(String args[]) {
        NNUEBridge.init(BIG_NET_PATH, SMALL_NET_PATH);
        //System.out.println(NNUEBridge.evalFen("r4rk1/pp3ppp/4p3/q2n1Q2/PbB5/8/1P2RPPP/R1B3K1 w - - 2 19"));
        System.out.println(NNUEBridge.evalFen("rnbq1rk1/pp2bppp/4pn2/2pp4/2PP4/2NBP3/PPQ1NPPP/R1B1K2R w KQ c6 0 8"));
        System.out.println(NNUEBridge.evalFen("rnbq1rk1/pp2bppB/4pn2/2pp4/2PP4/2N1P3/PPQ1NPPP/R1B1K2R b KQ - 0 8"));
        System.out.println(NNUEBridge.evalFen("rnbq1rk1/pp2bppp/4pn2/2pP4/3P4/2NBP3/PPQ1NPPP/R1B1K2R b KQ - 0 8"));
        System.out.println(NNUEBridge.evalFen("rnbq1rk1/pp2bppp/4pn2/2pp4/2PP4/2NBP3/PPQ1NPPP/R1B2RK1 b - - 1 8"));

    }
}