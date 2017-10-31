//package entailment.learning;
//
//import java.io.FileWriter;
//import java.io.BufferedWriter;
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.File;
//import java.util.Vector;
//import java.util.Iterator;
//import java.util.Arrays;
//import java.util.Set;
//import java.util.HashSet;
//import java.util.HashMap;
//import java.util.Map;
//
//import nate.LibsvmReader;
//import nate.Pair;
//import nate.order.tb.InfoFile;
//import nate.order.tb.TLink;
//
//import lpsolve.*;
//
//
//public class Experiment {
//  private Closure closure;
//  private HashMap<String,Integer> indices = new HashMap();
//  private static int NUMCLASSES = 5;
//
//  private int CORRECT = 0, INCORRECT = 0, CORRECT_CONSTRAINED = 0, INCORRECT_CONSTRAINED = 0;
//  private int CORRECTS[] = new int[NUMCLASSES];
//  private int INCORRECTS[] = new int[NUMCLASSES];
//  private int CONFUSION[][];
//  private HashMap<String,Integer> CORRECT_FILES = new HashMap<String,Integer>();
//  private HashMap<String,Integer> INCORRECT_FILES = new HashMap<String,Integer>();
//  private Vector<String> FILENAMES = new Vector<String>();
//
//  public static int ADD = 1;
//  public static int REMOVE = 2;
//  public static int ADD_BI = 3;
//  public static int REMOVE_BI = 4;
//  public static int CROSS = 5;
//  public static int GRAPH = 6;
//  public static int FULL= 7;
//  public static int SORTEDGRAPH = 8;
//  public static int SORTEDGRAPH_BI = 12;
//  public static int ADD_2 = 9;
//  public static int REMOVE_2 = 10;
//  public static int ADD_BI_2 = 11;
//  public static int LINEAR_PROGRAM = 13;
//  public static int LINEAR_PROGRAM_BI = 14;
//  private boolean overlay = false;
//  private String overlayDir = null;
//  private InfoFile infoFile;
//  public int type;
//  public String directory = null;
//  public int folds = 10;
//
//
//  // order of sizes should correspond to FeatureVector.order
//  public int limitsizes[] = { 2, 42, 42, 42, 42, 4, 4, 7, 2, 7, 
//      42, 42, 42, 42, 4, 4, 7, 2, 7, 
//      3, 2, 34, 34, 2, 
//      2573, 2573, 1683, 1683, 1433, 1433,
//      //	      2397, 2397, 1563, 1563, 1320, 1320, 
//      16, 16, 50, 1796, 1796, 1796, 2, 2, 2 };
//  public static boolean fullkeep[] = 
//  { true, true, true, true, true, true, true, true, true, true,
//    true, true, true, true, true, true, true, true, true,
//    true, true, true, true, true,
//    true, true, true, true, true, true,
//    true, true, true, true, true, true, true, true, true };
//  public static boolean manikeep[] = 
//  { false, false, false, false, false, true, true, true, true, true,
//    false, false, false, false, true, true, true, true, true,
//    false, false, false, false, false,
//    true, true, false, false, false, false,
//    false, false, false, false, false, false, true, true, false };
//  public static boolean maniLapataKeep[] = 
//  { true, false, false, false, false, true, true, true, true, true,
//    false, false, false, false, true, true, true, true, true,
//    true, false, false, false, false,
//    true, true, true, true, true, true,
//    false, false, false, false, false, false, true, true, false };
//  public static boolean bestTBkeep[] = 
//    /*
//  { true, false, false, false, false, false, false, false, false, false,
//    false, false, false, false, false, false, false, false, false,
//    false, false, false, false, false,
//    false, false, false, false, false, false,
//    false, false, false, false, false, false, false, false, false };
//     */
//
//  { false, true, false, false, false, false, false, false, true, false,
//    false, false, false, true, false, true, true, true, false,
//    false, false, true, true, true,
//    false, false, false, false, false, true,
//    true, true, true, true, false, true, false, false, true };
//
//  public static boolean bestTBkeep1[] = // bi distributions
//  { true, true, true, false, false, false, false, false, true, false,
//    false, true, false, false, true, false, false, true, false,
//    true, false, true, true, false,
//    false, false, false, false, false, false,
//    false, false, true, false, false, true, false, false, false };
//  public static boolean bestTBkeep2[] = // bi distributions
//  { false, true, false, false, false, true, true, true, true, false,
//    false, false, false, false, false, false, false, false, false,
//    false, false, false, false, false,
//    false, true, false, false, false, true,
//    true, false, false, false, false, true, false, false, true };
//  public static boolean bestOTCkeep[] = 
//  { false, false, true, false, false, false, false, true, true, false,
//    true, false, false, false, false, false, false, false, false,
//    false, false, true, false, true,
//    true, false, false, false, false, true,
//    false, true, true, true, false, true, false, false, true };
//  public static boolean bestOTCkeep1[] = 
//  { true, false, true, false, false, false, false, false, true, false,
//    false, true, false, true, false, true, true, false, true,
//    true, false, true, false, false,
//    true, false, false, false, false, true,
//    true, true, true, true, false, true, true, false, true };
//  public static boolean bestOTCkeep2[] = 
//  { false, true, false, false, false, false, true, false, true, false,
//    false, false, true, false, false, false, true, false, false,
//    false, false, false, false, false,
//    false, false, false, false, false, false,
//    true, false, false, false, false, false, false, false, true };
//
//  public static boolean falsekeep[] = 
//  { false, false, false, false, false, false, false, false, false, false,
//    false, false, false, false, false, false, false, false, false,
//    false, false, false, false, false,
//    false, false, false, false, false, false,
//    false, false, false, false, false, false, false, false, false };
//
//  boolean keeps[] = fullkeep;
//  NaiveBayes nb;
//
//  String CONSTRAINED_FILES[] = {
//      "allfeatures-wsj_0568_orig.txt"
//      , "allfeatures-wsj_0806_orig.txt"
//      , "allfeatures-wsj_0292_orig.txt"
//      , "allfeatures-wsj_0660_orig.txt"
//      , "allfeatures-VOA19980303.1600.2745.txt"
//      , "allfeatures-wsj_0570_orig.txt"
//      , "allfeatures-wsj_0585_orig.txt"
//      , "allfeatures-wsj_0762_orig.txt"
//      , "allfeatures-wsj_0781_orig.txt"
//      , "allfeatures-WSJ910225-0066.txt"
//      , "allfeatures-wsj_0586_orig.txt"
//      , "allfeatures-wsj_0904_orig.txt"
//      , "allfeatures-AP900816-0139.txt"
//      , "allfeatures-wsj_0575_orig.txt"
//      , "allfeatures-wsj_0670_orig.txt"
//      , "allfeatures-wsj_0778_orig.txt"
//      , "allfeatures-wsj_0907_orig.txt"
//      , "allfeatures-ABC19980120.1830.0957.txt"
//      , "allfeatures-APW19980526.1320.txt"
//      , "allfeatures-wsj_0679_orig.txt"
//      , "allfeatures-wsj_0798_orig.txt"
//      , "allfeatures-wsj_0810_orig.txt"
//      , "allfeatures-wsj_0938_orig.txt"
//      , "allfeatures-wsj_0430_orig.txt"
//      , "allfeatures-wsj_1011_orig.txt"
//      , "allfeatures-ABC19980114.1830.0611.txt"
//      , "allfeatures-ABC19980304.1830.1636.txt"
//      , "allfeatures-CNN19980227.2130.0067.txt"
//      , "allfeatures-wsj_0325_orig.txt"
//      , "allfeatures-wsj_0505_orig.txt"
//      , "allfeatures-wsj_0583_orig.txt"
//      , "allfeatures-wsj_0918_orig.txt"
//      , "allfeatures-wsj_0376_orig.txt"
//      , "allfeatures-wsj_0542_orig.txt"
//      , "allfeatures-wsj_0551_orig.txt"
//      , "allfeatures-wsj_0557_orig.txt"
//      , "allfeatures-wsj_0584_orig.txt"
//      , "allfeatures-wsj_0805_orig.txt"
//      , "allfeatures-wsj_0927_orig.txt"
//      , "allfeatures-PRI19980205.2000.1890.txt"
//      , "allfeatures-PRI19980205.2000.1998.txt"
//      , "allfeatures-WSJ900813-0157.txt"
//      , "allfeatures-wsj_0471_orig.txt"
//      , "allfeatures-wsj_0906_orig.txt" };
//
//
//
//  Experiment() {
//    type = ADD; // default experiment
//    Arrays.sort(CONSTRAINED_FILES);
//
//    CONFUSION = new int[NUMCLASSES][NUMCLASSES];
//    for( int i = 0; i < NUMCLASSES; i++ ) {
//      //      CONFUSION[i] = new int[NUMCLASSES];
//    }  
//
//    if( TLink.currentMode == TLink.MODE.SYMMETRY )
//      closure = new Closure("closure-overlap.dat");
//    if( TLink.currentMode == TLink.MODE.FULLSYMMETRY )
//      closure = new Closure("closure-full-symmetry.dat");
//    else 
//      closure = new Closure(); // default closure.dat
//  }
//
//
//  /**
//   * Process the java arguments
//   */
//  public void handleParameters(String[] args) {
//    int i = 0;
//    while( i < args.length ) {
//      if( args[i].equalsIgnoreCase("-keep") ) {
//        if( args[i+1].equalsIgnoreCase("base") || args[i+1].equalsIgnoreCase("mani") ) {
//          System.out.println("Using base Mani feature set");
//          keeps = manikeep;
//        }
//        else if( args[i+1].startsWith("lap") ) {
//          System.out.println("Using Lapata and Mani feature set");
//          keeps = maniLapataKeep;
//        }
//        else if( args[i+1].equalsIgnoreCase("off") ) {
//          System.out.println("Using all false feature set");
//          keeps = falsekeep;
//        }
//        else if( args[i+1].equalsIgnoreCase("tb") ) {
//          System.out.println("Using TimeBank keeps");
//          keeps = bestTBkeep;
//        }
//        else if( args[i+1].equalsIgnoreCase("otc") ) {
//          System.out.println("Using OTC keeps");
//          keeps = bestTBkeep;
//        }
//        i++;
//      }
//      else if( args[i].equalsIgnoreCase("-info") ) {
//        infoFile = new InfoFile();
//        infoFile.readFromFile(new File(args[i+1]));
//        i++;
//      }
//      else if( args[i].equalsIgnoreCase("-type") ) {
//        System.out.println("-type arg " + args[i+1]);
//        if( args[i+1].charAt(0) == 'c' ) type = CROSS;
//        else if( args[i+1].charAt(0) == 'g' ) type = GRAPH;
//        else if( args[i+1].equals("sort") ) type = SORTEDGRAPH;
//        else if( args[i+1].equals("sortbi") ) type = SORTEDGRAPH_BI;
//        else if( args[i+1].charAt(0) == 'a' ) type = ADD;
//        else if( args[i+1].charAt(0) == 'r' ) type = REMOVE;
//        else if( args[i+1].charAt(0) == 'f' ) type = FULL;
//        else if( args[i+1].equals("lp") ) {
//          System.out.println("setting lp...");
//          type = LINEAR_PROGRAM;
//        }
//        else if( args[i+1].equals("lp2") ) {
//          System.out.println("setting lp with bi distribution...");
//          type = LINEAR_PROGRAM_BI;
//        }
//        else if( args[i+1].charAt(0) == 'b' ) {
//          if( args[i+1].charAt(1) == 'a' ) type = ADD_BI;
//          if( args[i+1].charAt(1) == 'r' ) type = REMOVE_BI;
//          if( args[i+1].charAt(1) == '2' ) type = ADD_BI_2;
//        }
//        else if( args[i+1].charAt(0) == '2' ) {
//          if( args[i+1].charAt(1) == 'a' ) type = ADD_2;
//          if( args[i+1].charAt(1) == 'r' ) type = REMOVE_2;
//        }
//        i++;
//      }
//      else if( args[i].equalsIgnoreCase("-overlay") ) {
//        overlay = true;
//        overlayDir = args[i+1];
//        i++;
//      }
//      else directory = args[i];
//      i++;
//    }
//    if( directory == null ) {
//      System.err.println("no directory given");
//      System.exit(1);
//    }
//  }
//
//  private Vector<Features> readFile(String path) {
//    return readFile(path, null);
//  }
//
//  /**
//   * Reads a file of features into a Vector of FeatureVectors
//   * @param path The path of the file to read
//   * @param eventRelation The type of events we are reading.
//   *        Set to null if file contains tlink features.
//   * @return A Vector of FeatureVectors
//   */
//  public Vector<Features> readFile(String path, String eventRelation) {
//    //    System.out.println("Reading file " + path);
//    Features vec;
//    Vector vecs = new Vector();
//
//    try {
//      BufferedReader in = new BufferedReader(new FileReader(path));
//      while( in.ready() ) {
//        String line = in.readLine();
//        if( !(line.charAt(0) == '%') ) {
//          if( eventRelation != null ) 
//            vec = SingleFeatureVec.fromString(line, eventRelation);
//          else vec = FeatureVector.fromString(line);
//
//          if( vec != null ) vecs.add(vec);
//          else {
//            System.err.println("Training line not compatible");
//            System.exit(1);
//          }
//        }
//      }
//      in.close();
//    } catch( Exception ex ) { ex.printStackTrace(); }
//
//    return vecs;
//  }
//
//
//  /**
//   * Reads a file of features into a Vector of Vectors of FeatureVectors.
//   * Each subVector is a self-contained file of Vectors.
//   * Used for learning that is globally file based and not just local decisions.
//   * @param path The path of the file to read
//   * @param eventRelation The type of events we are reading.
//   *        Set to null if file contains tlink features.
//   * @return A Vector of Vectors of FeatureVectors
//   */
//  public Vector<FileOfVecs> readFileSeparateFiles(String path, String eventRelation) {
//    return readFileSeparateFiles(path,eventRelation,-1);
//  }
//  public Vector<FileOfVecs> readFileSeparateFiles(String path, String eventRelation, int sizeCheck) {
//    //    System.out.println("Reading file " + path);
//    Features vec = null;
//    Vector<FileOfVecs> vecs = new Vector();
//    FileOfVecs fileVec = new FileOfVecs();
//    int numVecs = 0;
//
//    try {
//      BufferedReader in = new BufferedReader(new FileReader(path));
//      while( in.ready() ) {
//        String line = in.readLine();
//        if( line.charAt(0) == '%' ) {
//          // save the previous list of features now
//          if( fileVec.size() > 0 ) vecs.add(fileVec);
//          fileVec = new FileOfVecs(line.substring(line.indexOf(' ')).trim());
//        } 
//        // else reading in a new feature vector
//        else {
//          if( eventRelation != null ) 
//            vec = SingleFeatureVec.fromString(line, eventRelation);
//          else if( line.matches("e.+ e.+ .*") )
//            vec = FeatureVector.fromString(line);
//          else if( line.matches("t.+ e.+ .*") || line.matches("e.+ t.+ .*") )
//            vec = ETFeatureVec.fromString(line);
//          else { System.out.println("ERROR: OOPS, unknown feature line! " + line);
//          }
//
//          if( vec != null ) {
//            fileVec.add(vec);
//            numVecs++;
//          } else {
//            System.err.println("Training line not compatible");
//            System.exit(1);
//          }
//        }
//      }
//
//      // save the last list of features
//      if( fileVec.size() > 0 ) vecs.add(fileVec);
//
//      in.close();
//    } catch( Exception ex ) { ex.printStackTrace(); }
//
//    if( sizeCheck > -1 && sizeCheck != numVecs ) {
//      System.out.println("ERROR: read in " + sizeCheck + " probabilities, but " +
//          numVecs + " feature vectors");
//      System.exit(1);
//    }
//    return vecs;
//  }
//
//
//  public double crossValidate() {
//    return crossValidate(fullkeep);
//  }
//
//  public double crossValidate(boolean keep[]) {
//    return crossValidate(keep, null);
//  }
//
//  /**
//   * Runs cross-validation over a directory of train and test files
//   * @param bi True if you want two distributions trained
//   */
//  public double crossValidate(boolean keep[], boolean keep2[]) {
//    if( directory == null ) {
//      System.err.println("no directory given");
//      System.exit(1);
//    }
//
//    double acc, totalacc = 0;
//    int guesses[];
//
//    // Run each fold
//    for( int f = 0; f < folds; f++ ) {
//      System.out.print(".");
//
//      // Train on the data (6 possible classifications
//      Vector<Features> trainVecs = readFile(directory + "/train" + f);
//      if( keep2 != null ) nb.trainBi(trainVecs, 6, keep, keep2);
//      else nb.train(trainVecs, keep);
//
//      // Now test each file in the fold
//      Vector<FileOfVecs> files = readFileSeparateFiles(directory + "/test" + f, null);
//      for( FileOfVecs vecs : files ) {
//        //	System.out.println("----------------------------------------\nNew file " + vecs.filename());
//
//        if( keep2 != null ) guesses = nb.testBi(vecs.getVecs(), keep, keep2);
//        else guesses = nb.test(vecs.getVecs(), keep);
//
//        acc = computeAccuracy(vecs.getVecs(), guesses, vecs.filename());
//      }
//    }
//
//    return (double)CORRECT/(double)(CORRECT+INCORRECT);
//  }
//
//
//  private double computeAccuracy(Vector<Features> vecs, int guesses[], String filename) {
//    int i = 0, total = 0, correct = 0;
//    for( Features vec : vecs ) {
//      int gold = Integer.valueOf(vec.relation());
//      //      System.out.println("guess=" + guesses[i] + ", gold=" + gold);
//      trackAnswer(guesses[i], gold, filename, false);
//      if( gold == guesses[i] ) correct++;
//      total++;
//      i++;
//    }
//    return (double)correct/(double)total;
//  }
//
//
//  public void featureAdd() {
//    featureAdd(keeps);
//  }
//
//  /**
//   * @return The best accuracy we found
//   */
//  public double featureAdd(boolean keep[]) {
//    Vector history = new Vector();
//    Vector history_scores = new Vector();
//    boolean newkeep[] = new boolean[keep.length];
//    double best = 0, oldbest;
//    int besti = 0;
//    boolean changed = true;
//
//    // initial run
//    best = crossValidate(newkeep);
//    System.out.println("full: " + best);
//
//    while( changed ) {
//      changed = false;
//      oldbest = best;
//
//      for( int i = 0; i < newkeep.length; i++ ) {
//        if( !newkeep[i] && keep[i] ) {
//          newkeep[i] = true;
//
//          // debug print
//          printKeeps(newkeep);
//
//          double acc = crossValidate(newkeep);
//          newkeep[i] = false;
//
//          if( acc > best ) {
//            best = acc;
//            besti = i;
//            changed = true;
//          }
//        }
//      }
//
//      if( changed ) {
//        //	  history.add(FeatureVector.order[besti]);
//        history.add(FeatureType.values()[besti]);
//        history_scores.add(best-oldbest);
//        //	System.out.println("Best feature: " + FeatureVector.order[besti] + " up " 
//        System.out.println("Best feature: " + FeatureType.values()[besti] + " up " 
//            + (best-oldbest) + " to " + best);
//      }
//      newkeep[besti] = true;
//    }
//
//    // Print the features we added
//    for( int i = 0; i < history.size(); i++ )
//      System.out.println(history.elementAt(i) + " " + 
//          history_scores.elementAt(i));
//
//    System.out.println("Final acc: " + best);
//    return best;
//  }
//
//
//  /**
//   * Feature addition by trying PAIRS of features at a time, all possible combos
//   * of 2 features on each round.  The highest pair (or single feature) is added
//   * on each round.
//   * @return The best accuracy we found
//   */
//  public double featureAdd2(boolean keep[]) {
//    Vector history = new Vector();
//    Vector history_scores = new Vector();
//    boolean newkeep[] = new boolean[keep.length];
//    double best = 0, oldbest;
//    int besti = 0, bestj = 0;
//    boolean changed = true;
//
//    // initial run
//    best = crossValidate(newkeep);
//    System.out.println("full: " + best);
//
//    while( changed ) {
//      changed = false;
//      oldbest = best;
//
//      for( int i = 0; i < newkeep.length; i++ ) {
//        if( !newkeep[i] && keep[i] ) {
//
//          for( int j = i; j < newkeep.length; j++ ) { // j == i for single add case
//            if( !newkeep[j] && keep[j] ) {
//
//              newkeep[i] = true;
//              newkeep[j] = true;
//
//              // debug print
//              printKeeps(newkeep);
//
//              double acc = crossValidate(newkeep);
//              newkeep[i] = false;
//              newkeep[j] = false;
//
//              if( acc > best ) {
//                best = acc;
//                besti = i;
//                bestj = j;
//                changed = true;
//              }
//            }
//          }
//        }
//      }
//
//      if( changed ) {
//        history.add(FeatureType.values()[besti]);
//        history_scores.add(best-oldbest);
//        System.out.println("Best feature: " + FeatureType.values()[besti] + " up " 
//            + (best-oldbest) + " to " + best);
//        newkeep[besti] = true;
//        newkeep[bestj] = true;
//      }
//    }
//
//    // Print the features we added
//    for( int i = 0; i < history.size(); i++ )
//      System.out.println(history.elementAt(i) + " " + 
//          history_scores.elementAt(i));
//
//    System.out.println("Final acc: " + best);
//    return best;
//  }
//
//
//  /**
//   * Runs feature selection on two distributions.  One is over events that
//   * are in the same sentence, and the other is over events that cross 
//   * sentence boundaries.
//   * @return The best accuracy we find
//   */
//  public double featureAddBi(boolean keep[]) {
//    nb.initBiDistribution();
//
//    Vector history = new Vector();
//    Vector history_scores = new Vector();
//    boolean newkeep1[] = new boolean[keep.length];
//    boolean newkeep2[] = new boolean[keep.length];
//    boolean newkeep[];
//    double best = 0, oldbest;
//    int besti = 0;
//
//    // Run the loop twice, for each distribution
//    for( int x = 0; x < 2; x++ ) {
//      if( x == 0 ) newkeep = newkeep1;
//      else newkeep = newkeep2;
//
//      // initial run
//      best = crossValidate(newkeep1, newkeep2);
//      System.out.println("full: " + best);
//
//      boolean changed = true;
//
//      // Loop until we don't find any more features
//      while( changed ) {
//        changed = false;
//        oldbest = best;
//
//        // Add each feature one at a time and compare their accuracies
//        for( int i = 0; i < newkeep.length; i++ ) {
//          if( !newkeep[i] && keep[i] ) {
//            newkeep[i] = true;
//
//            // debug print
//            printKeeps(newkeep);
//
//            double acc = crossValidate(newkeep1, newkeep2);
//            newkeep[i] = false;
//
//            if( acc > best ) {
//              best = acc;
//              besti = i;
//              changed = true;
//            }
//          }
//        }
//
//        // if we added a helpful feature
//        if( changed ) {
//          System.out.println("Best feature: " + FeatureType.values()[besti] + " up " 
//              + (best-oldbest) + " to " + best);
//          newkeep[besti] = true;
//          history.add(x + "-" + FeatureType.values()[besti]);
//          history_scores.add(best-oldbest);
//        }
//      }
//
//      System.out.println("--Best of (" + (x+1) + ")--");
//      printKeeps(newkeep);
//      System.out.println();
//    }
//
//    // Print the features we added
//    for( int i = 0; i < history.size(); i++ )
//      System.out.println(history.elementAt(i) + " " + 
//          history_scores.elementAt(i));
//    return best;
//  }
//
//
//  /**
//   * Runs feature selection on two distributions.  One is over events that
//   * are in the same sentence, and the other is over events that cross 
//   * sentence boundaries.
//   * @return The best accuracy we find
//   */
//  public double featureAddBi2(boolean keep[]) {
//    nb.initBiDistribution();
//
//    Vector history = new Vector();
//    Vector history_scores = new Vector();
//    boolean newkeep1[] = new boolean[keep.length];
//    boolean newkeep2[] = new boolean[keep.length];
//    boolean newkeep[];
//    double best = 0, oldbest;
//    int besti = 0, bestj = 0;
//
//    // Run the loop twice, for each distribution
//    for( int x = 0; x < 2; x++ ) {
//      if( x == 0 ) newkeep = newkeep1;
//      else newkeep = newkeep2;
//
//      // initial run
//      best = crossValidate(newkeep1, newkeep2);
//      System.out.println("full: " + best);
//
//      boolean changed = true;
//
//      // Loop until we don't find any more features
//      while( changed ) {
//        changed = false;
//        oldbest = best;
//
//        // Add two features at a time and compare their accuracies
//        for( int i = 0; i < newkeep.length; i++ ) {
//          if( !newkeep[i] && keep[i] ) {
//
//            for( int j = i; j < newkeep.length; j++ ) {
//              if( !newkeep[j] && keep[j] ) {
//
//                newkeep[i] = true;
//                newkeep[j] = true;
//
//                // debug print
//                printKeeps(newkeep);
//
//                double acc = crossValidate(newkeep1, newkeep2);
//                newkeep[i] = false;
//                newkeep[j] = false;
//
//                if( acc > best ) {
//                  best = acc;
//                  besti = i;
//                  bestj = j;
//                  changed = true;
//                }
//              }
//            }
//          }
//        }
//
//        // if we added a helpful feature
//        if( changed ) {
//          System.out.println("Best feature: " + FeatureType.values()[besti] + " up " 
//              + (best-oldbest) + " to " + best);
//          newkeep[besti] = true;
//          newkeep[bestj] = true;
//          history.add(FeatureType.values()[besti]);
//          history_scores.add(best-oldbest);
//        }
//      }
//
//      System.out.println("--Best of (" + (x+1) + ")--");
//      printKeeps(newkeep);
//      System.out.println();
//    }
//
//    // Print the features we added
//    for( int i = 0; i < history.size(); i++ )
//      System.out.println(history.elementAt(i) + " " + 
//          history_scores.elementAt(i));
//    return best;
//  }
//
//
//  public void featureRemove() {
//    featureRemove(keeps);
//  }
//
//  /**
//   * @return The best accuracy we found
//   */
//  public double featureRemove(boolean keep[]) {
//    boolean newkeep[] = keep.clone();
//    double best = 0, oldbest;
//    int besti = 0;
//    boolean changed = true;
//
//    // initial run
//    best = crossValidate(newkeep);
//    System.out.println("full: " + best);
//
//    while( changed ) {
//      changed = false;
//      oldbest = best;
//
//      for( int i = 0; i < newkeep.length; i++ ) {
//        if( newkeep[i] ) {
//          newkeep[i] = false;
//
//          // debug print
//          printKeeps(newkeep);
//
//          double acc = crossValidate(newkeep);
//          newkeep[i] = true;
//
//          if( acc > best ) {
//            best = acc;
//            besti = i;
//            changed = true;
//          }
//        }
//      }
//      System.out.println("Best removed: " + FeatureType.values()[besti] + " up " 
//          + (best-oldbest) + " to " + best);
//      newkeep[besti] = false;
//    }
//
//    return best;
//  }
//
//
//  /**
//   * Runs feature selection on two distributions.  One is over events that
//   * are in the same sentence, and the other is over events that cross 
//   * sentence boundaries.
//   * @return The best accuracy we found
//   */
//  public double featureRemoveBi(boolean keep[]) {
//    nb.initBiDistribution();
//    boolean newkeep1[] = keep.clone();
//    boolean newkeep2[] = keep.clone();
//    boolean newkeep[];
//    double best = 0, oldbest;
//    int besti = 0;
//
//    for( int x = 1; x < 3; x++ ) {
//      if( x == 1 ) newkeep = newkeep1;
//      else newkeep = newkeep2;
//
//      // initial run
//      best = crossValidate(newkeep1, newkeep2);
//      System.out.println("full: " + best);
//
//      boolean changed = true;
//
//      while( changed ) {
//        changed = false;
//        oldbest = best;
//
//        for( int i = 0; i < newkeep.length; i++ ) {
//          if( newkeep[i] ) {
//            newkeep[i] = false;
//
//            // debug print
//            printKeeps(newkeep);
//
//            double acc = crossValidate(newkeep1, newkeep2);
//            newkeep[i] = true;
//
//            if( acc > best ) {
//              best = acc;
//              besti = i;
//              changed = true;
//            }
//          }
//        }
//        if( changed ) {
//          System.out.println("Best removed: " + FeatureType.values()[besti] + " up " 
//              + (best-oldbest) + " to " + best);
//          newkeep[besti] = false;
//        }
//      }
//
//      System.out.println("--Best of (" + x + ")--");
//      printKeeps(newkeep);
//      System.out.println();
//    }
//
//    return best;
//  }
//
//
//  public double greedyGraphCrossValidate(boolean keep[], boolean sorted, boolean bi) {
//    if( directory == null ) {
//      System.err.println("no directory given");
//      System.exit(1);
//    }
//
//    double totalacc = 0;
//    double acc;
//
//    // Run each fold
//    for( int f = 0; f < folds; f++ ) {
//      if( sorted && bi ) 
//        acc = sortFirstGreedyGraph(directory + "/train" + f, directory + "/test" + f, 
//            bestOTCkeep1, bestOTCkeep2);
//      else if( sorted )
//        acc = sortFirstGreedyGraph(directory + "/train" + f, directory + "/test" + f, keep);
//      else acc = greedyGraph(directory + "/train" + f, directory + "/test" + f, keep);
//      System.out.println("Graph fold " + f + " got " + acc);
//      totalacc += acc;
//    }
//
//    double avg = (totalacc / folds);
//    System.out.println("Avg Acc: " + avg);
//    return avg;
//  }
//
//
//  public double greedyGraph(String train, String test, boolean keep[]) {
//    Vector relations = new Vector();
//    int correct = 0;
//    int clashes = 0;
//    int total = 0;
//
//    // Train on the data (6 possible classifications)
//    Vector vecs = readFile(train);
//    nb.train(vecs, keep);
//
//    // Test the data
//    vecs.clear();
//    Vector<FileOfVecs> fileVecs = readFileSeparateFiles(test, null);
//
//    // Now process each file one by one
//    for( Iterator iter = fileVecs.iterator(); iter.hasNext(); ) {
//      //      System.out.println("----------------------------------------\nNew file");
//      // vector of features
//      vecs = (Vector)iter.next();
//      relations.clear();
//      total += vecs.size();
//
//      // Classify each test vector
//      for( Iterator it = vecs.iterator(); it.hasNext(); ) {
//        Features vec = (Features)it.next();
//        double acc[] = nb.fullClassify(vec, keep, false);
//
//        // Create a sortable array of results
//        FullResult result = new FullResult(vec, acc);
//        result.sort();
//
//        int i = acc.length-1;
//        boolean added = false;
//        boolean clashed = false;
//
//        System.out.println("----\n" + vec);
//        System.out.println(result);
//
//        // Keep trying closure till it works
//        while( !added && i >= 0 ) {
//
//          TLink link = new TLink(vec.event1(), vec.event2(), intToStringRelation(result.nth(i).index()));
//          System.out.println("Trying! " + link);
//          Vector newrels = closure.safeClosure(relations, link);
//
//          // See if closure worked
//          if( newrels != null ) {
//            System.out.print("Fits! " + result.nth(i).index());
//            System.out.println("...closed with " + newrels.size());
//            relations = newrels;
//            added = true;
//            // Check for correctness
//            if( relationToInt(link.relation().toString()) == Integer.valueOf(vec.relation()) ) correct++;
//          } 
//          else {
//            System.out.println("Closure clash " + i);
//            clashed = true;
//          }
//          i--;
//        }
//
//        if( !added ) // if all clashed, just guess the top one, don't add to graph
//          if( result.nth(result.length()-1).index() == Integer.valueOf(result.vec.relation()) )
//            correct++;
//
//        if( clashed ) clashes++;
//      }
//    }
//
//    System.out.println(clashes + " clashes...");
//    return (double)correct / (double)total;
//  }
//
//
//  /**
//   * Same as GreedyGraph, but all the guesses are sorted before placed into
//   * the graph. Higher confidence guesses are thus given first priority, 
//   * preventing lower confidence guesses from skewing the entire graph
//   * when they are wrong.
//   * @param train The file path of the training data
//   * @param test  The file path of the test data
//   * @param keep1 The active features you want used
//   * @param keep2 The active features for the second distribution. If you are
//   *              not using a bi distribution, set it to NULL.
//   * @return The accuracy achieved on this test set
//   */
//  public double sortFirstGreedyGraph(String train, String test, boolean keep1[], boolean keep2[]) {
//    Vector relations = new Vector();
//    int correct = 0, total = 0, clashCorrect = 0;
//    int clashes = 0;
//
//    // Train on the data (6 possible classifications)
//    Vector vecs = readFile(train);
//    if( keep2 == null ) nb.train(vecs, keep1);
//    else nb.trainBi(vecs, 6, keep1, keep2);
//
//    // Test the data
//    vecs.clear();
//    Vector<FileOfVecs> fileVecs = readFileSeparateFiles(test, null);
//
//    // Now process each file one by one
//    for( FileOfVecs fileOf : fileVecs ) {
//      System.out.println("----------------------------------------\nNew file " + fileOf.filename());
//      // vector of features
//      vecs = fileOf.getVecs();
//      relations.clear();
//      total += vecs.size();
//
//      // Get the TIME-EVENT links if they exist
//      if( infoFile != null ) {
//        Vector temp = getTimeLinksFromInfoFile(fileOf.filename());
//        if( temp != null ) relations.addAll(temp);
//      }
//
//      // Classify each test vector
//      FullResult allresults[] = new FullResult[vecs.size()];
//      int xx = 0;
//      for( Iterator it = vecs.iterator(); it.hasNext(); ) {
//        FeatureVector vec = (FeatureVector)it.next();
//        double acc[];
//
//        // Classify using the correct set of activated features
//        if( keep2 != null && vec.get(FeatureType.SAME_SENTENCE).equals("2") )
//          acc = nb.fullClassify(vec, keep2, true);
//        else acc = nb.fullClassify(vec, keep1, false);
//
//        // Create a sortable array of results
//        allresults[xx] = new FullResult(vec, acc);
//        allresults[xx].sort();
//        xx++;
//      }
//
//      // Sort our guesses by highest confidence
//      Arrays.sort(allresults);
//
//      // Loop over each problem, adding guesses to the graph
//      for( int r = allresults.length-1; r >= 0; r-- ) {
//        FullResult result = allresults[r];
//        Features vec = result.vec();
//        TLink.TYPE gold = TLink.TYPE.valueOf(vec.relation());
//        TLink.TYPE guess = TLink.TYPE.BEFORE;
//        boolean clashed = false;
//        boolean added = false;
//        int i = result.length()-1;
//
//        System.out.println("----\n" + result);
//
//        // Keep trying closure till it works
//        while( !added && i >= 0 ) {
//          TLink link = new TLink(vec.event1(), vec.event2(), intToStringRelation(result.nth(i).index()));
//          System.out.println("Trying! " + link);
//          Vector newrels = closure.safeClosure(relations, link);
//
//          // If closure worked
//          if( newrels != null ) {
//            System.out.print("Fits! " + link.relation());
//            System.out.println("...closed with " + newrels.size());
//            relations = newrels;
//            added = true;
//            guess = link.relation();
//          } else {
//            System.out.println("Closure clash " + link.relation());
//            clashed = true;
//          }
//          i--;
//        }
//
//        // if all clashed, just guess the top one, don't add to graph
//        if( !added ) guess = TLink.TYPE.valueOf(intToStringRelation(result.nth(result.length()-1).index()));
//
//        // Check for correctness
//        if( guess == gold ) {
//          correct++;
//          if( clashed ) clashCorrect++;
//        }
//        System.out.println("guess=" + guess + ", gold=" + gold);
//
//        if( clashed ) clashes++;
//      }
//    }
//
//    System.out.println(clashes + " clashes..." + clashCorrect + " corrected");
//    return (double)correct / (double)total;
//  }
//
//  public double sortFirstGreedyGraph(String train, String test, boolean keep[]) {
//    return sortFirstGreedyGraph(train, test, keep, null);
//  }
//
//
//  /**
//   * @desc Sorts the scores by highest confidence, and adds them incrementally.
//   *       Same as sortFirstGreedyGraph above, but instead operates on external scores
//   *       that are passed in, rather than training/testing in the function.
//   */
//  private int[] sortGreedySolveDocument(FileOfVecs doc, FullResult[] allresults) {
//    System.out.println("----------------------------------------\nNew file " + doc.filename());
//    // vector of features
//    Vector<FeatureVector> vecs = (Vector<FeatureVector>)doc.getVecs();
//    Vector relations = new Vector();
//    TLink.TYPE[] guesses = new TLink.TYPE[vecs.size()];
//
//    /*
//    // Get the TIME-EVENT links if they exist
//    System.out.println("ADDING event-time links from info file");
//    if( infoFile != null ) {
//      Vector temp = getTimeLinksFromInfoFile(doc.filename());
//      if( temp != null ) relations.addAll(temp);
//    }
//     */
//
//    /*
//    // Get the TIME-TIME links if they exist
//    System.out.println("ADDING time-time links from info file");
//    if( infoFile != null ) {
//      Vector temp = getTimeTimeLinksFromInfoFile(doc.filename());
//      if( temp != null ) relations.addAll(temp);
//    }
//     */
//
//    // Make sure each is sorted internally
//    for( int i = 0; i < allresults.length; i++ ) {
//      allresults[i].sort();
//      allresults[i].setIndex(i);
//    }
//
//    // Sort the guesses by highest confidence
//    Arrays.sort(allresults);
//
//    // Loop over each problem, adding guesses to the graph
//    for( int r = allresults.length-1; r >= 0; r-- ) {
//      FullResult result = allresults[r];
//      Features vec = result.vec();
//      //      int gold = Integer.valueOf(vec.relation());
//      TLink.TYPE guess = TLink.TYPE.BEFORE;
//      boolean clashed = false;
//      boolean added = false;
//      int i = result.length()-1;
//
//      System.out.println("----\n" + result);
//
//      // Keep trying closure till it works
//      while( !added && i >= 0 ) {
//        TLink link = new TLink(vec.event1(), vec.event2(), intToStringRelation(result.nth(i).index()));
//        System.out.println("Trying! " + link + " " + link.relation());
//        Vector newrels = closure.safeClosure(relations, link);
//
//        // If closure worked
//        if( newrels != null ) {
//          System.out.print("Fits! " + link.relation());
//          System.out.println("...closed with " + newrels.size());
//          relations = newrels;
//          added = true;
//          guess = link.relation();
//        } else {
//          System.out.println("Closure clash " + link.relation());
//          clashed = true;
//        }
//        i--;
//      }
//
//      // if all clashed, just guess the top one, don't add to graph
//      if( !added ) guess = TLink.TYPE.valueOf(intToStringRelation(result.nth(result.length()-1).index()));
//      guesses[r] = guess;
//    }
//
//    // Need to reorder the results to the original order as they were passed in
//    int[] reordered = new int[vecs.size()];
//    for( int i = 0; i < vecs.size(); i++ ) {
//      int j = 0;
//      for( FullResult res : allresults )  {
//        if( res.index() == i ) reordered[i] = relationToInt(guesses[j].toString());
//        j++;
//      }
//    }
//
//    return reordered;
//  }
//
//
//  public double lpCrossValidate(boolean keep[], boolean bi) {
//    if( directory == null ) {
//      System.err.println("no directory given");
//      System.exit(1);
//    }
//
//    double totalacc = 0;
//    double acc;
//    clearStats();
//
//    // Run each fold
//    for( int f = 0; f < folds; f++ ) {
//      if( bi ) 
//        acc = lpGraph(directory + "/train" + f, directory + "/test" + f, 
//            bestOTCkeep1, bestOTCkeep2);
//      else acc = lpGraph(directory + "/train" + f, directory + "/test" + f, keep, null);
//      System.out.println("LP fold " + f + " got " + acc);
//      totalacc += acc;
//    }
//
//    double avg = (totalacc / folds);
//    System.out.println("Avg Acc: " + avg);
//    printStats();
//    return avg;
//  }
//
//
//  public double lpGraph(String train, String test, boolean keep1[], boolean keep2[]) {
//    LinearProgramming lp;
//    Vector relations = new Vector();
//    double[] values;
//    int correct = 0, total = 0;
//    int clashes = 0;
//    int constraintDocs = 0;
//
//    // Train on the data (6 possible classifications)
//    Vector vecs = readFile(train);
//    if( keep2 == null ) nb.train(vecs, keep1);
//    else nb.trainBi(vecs, 6, keep1, keep2);
//
//    // Test the data
//    vecs.clear();
//    Vector<FileOfVecs> fileVecs = readFileSeparateFiles(test, null);
//
//    // Now process each file one by one
//    for( FileOfVecs fileOf : fileVecs ) {
//      System.out.println("----------------------------------------\nNew file " + fileOf.filename());
//      values = null;
//
//      // vector of features
//      vecs = fileOf.getVecs();
//      relations.clear();
//      total += vecs.size();
//
//      // Classify each test vector
//      FullResult allresults[] = new FullResult[vecs.size()];
//      int xx = 0;
//      for( Iterator it = vecs.iterator(); it.hasNext(); ) {
//        FeatureVector vec = (FeatureVector)it.next();
//        double acc[];
//
//        // Classify using the correct set of activated features
//        if( keep2 != null && vec.get(FeatureType.SAME_SENTENCE).equals("2") )
//          acc = nb.fullClassify(vec, keep2, true);
//        else acc = nb.fullClassify(vec, keep1, false);
//
//        //	normalizeProbs(acc);
//        // Create a sortable array of results
//        allresults[xx] = new FullResult(vec, acc);
//        xx++;
//      }
//
//
//      // Create the linear programming problem
//      lp = new LinearProgramming(NUMCLASSES);
//
//      // Loop over each problem, adding probabilities to the problem
//      indices.clear();
//      for( int r = 0; r < allresults.length; r++ ) {
//        FullResult result = allresults[r];
//        Features vec = result.vec();
//
//        // Add the probabilities
//        int index = lp.addProbs(result.scoreArray());
//
//        //	System.out.println(index + ": " + vec.event1() + " " + vec.event2() + " " + 
//        //			   TLink.intToStringRelation(Integer.valueOf(vec.relation())));
//
//        indices.put(vec.event1() + " " + vec.event2(), index);
//      }
//
//      /*
//      // Get the TIME-EVENT links if they exist
//      if( infoFile != null ) {
//	Vector<TLink> temp = getTimeLinksFromInfoFile(fileOf.filename());
//	for( TLink link : temp ) {
//	  values = new double[NUMCLASSES];
//	  System.out.println("Adding tlink " + link);
//	  values[link.relation-1] = 1.0;
//	  System.out.println("..." + Arrays.toString(values));
//	  int index = lp.addProbs(values);
//	  indices.put(link.event1() + " " + link.event2(), index);
//	  //	  values[link.relation-1] = 0.0; // reset to zero
//
//	  // add the event-time links to the list of event-event links
//	  vecs.add(new FeatureVector(link.event1(), link.event2(), 
//				     String.valueOf(link.relation())));
//	}
//      }
//       */
//
//      // Add transitivity constraints
//      Map<String, Set<Pair>> map = getVars(vecs);
//      boolean addedSomething = addLPConstraints(lp, map);
//
//      if( addedSomething ) constraintDocs++;
//
//      // Build and solve the constraints
//      try {
//        lp.buildSystem();
//        values = lp.solveSystem();
//      } catch( Exception ex ) { ex.printStackTrace(); }
//
//      // Check the answers
//      if( values != null ) {
//        for( int r = 0; r < allresults.length; r++ ) {
//          FullResult result = allresults[r];
//          Features vec = result.vec();
//          int gold = Integer.valueOf(vec.relation());
//
//          int guess = 1;
//          boolean found = false;
//          for( int p = 0; p < NUMCLASSES; p++ ) {
//            //	    System.out.print(" " + (r*LinearProgramming.numClasses + p));
//            if( values[r*NUMCLASSES + p] == 1.0 ) {
//              found = true;
//              guess = p+1; // +1 to reach the range [1-6]
//              break;
//            }
//          }
//          if( !found ) System.err.println("*************\n**************\nERROR: NO ANSWER GIVEN");
//          //	  System.out.println();
//          //	  System.out.println("gold=" + gold + " guess=" + guess);
//          if( guess == gold ) correct++;
//          System.out.println("guess=" + guess + ", gold=" + gold);
//          trackAnswer(guess, gold, fileOf.filename(), addedSomething);
//        }
//      }
//    } // each file
//
//    System.out.println("Documents with constraints: " + constraintDocs + "/" + fileVecs.size());
//    return (double)correct / (double)total;
//  }
//
//
//  /**
//   * @param doc FileOfVecs object, feature vectors from a single document
//   * @param allresults Array of probability estimates for each class
//   * @return The array of guesses
//   */
//  private int[] lpSolveDocument(FileOfVecs doc, FullResult[] allresults) {
//    System.out.println("----------------------------------------\nNew file " + doc.filename());
//    double[] values = null;
//
//    // vector of features
//    Vector<Features> vecs = (Vector<Features>)doc.getVecs();
//    Vector relations;
//
//    // Create the linear programming problem
//    LinearProgramming lp = new LinearProgramming(NUMCLASSES);
//
//    // Loop over each tlink, adding its probabilities to the graph
//    indices.clear();
//    for( int r = 0; r < allresults.length; r++ ) {
//      FullResult result = allresults[r];
//      Features vec = result.vec();
//
//      // Add the probabilities
//      int index = lp.addProbs(result.scoreArray());
//
//      //	System.out.println(index + ": " + vec.event1() + " " + vec.event2() + " " + 
//      //			   TLink.intToStringRelation(Integer.valueOf(vec.relation())));
//      indices.put(vec.event1() + " " + vec.event2(), index);
//    }
//
//    // Get the TIME-EVENT links if they exist
//    int numAdded = 0;
//    /*
//    if( infoFile != null ) {
////      Vector<TLink> temp = getTimeLinksFromInfoFile(doc.filename());
//      Vector<TLink> temp = getTimeTimeLinksFromInfoFile(doc.filename());
//      for( TLink link : temp ) {
//	values = new double[NUMCLASSES];
//	System.out.println("Adding tlink " + link);
//	values[link.relation-1] = 1.0;
//	System.out.println("..." + Arrays.toString(values));
//	int index = lp.addProbs(values);
//	indices.put(link.event1() + " " + link.event2(), index);
//	//	  values[link.relation-1] = 0.0; // reset to zero
//
//	// add the event-time links to the list of event-event links
//	vecs.add(new FeatureVector(link.event1(), link.event2(), 
//				   String.valueOf(link.relation())));
//	numAdded++;
//      }
//    }
//     */
//
//    // Add transitivity constraints
//    Map<String, Set<Pair>> map = getVars(vecs);
//    // Remove links that already chose NONE as the highest probability
//    if( TLink.currentMode != TLink.MODE.FULLSYMMETRY ) 
//      removeNoneRelations(map,allresults);
//    // Now add the constraints
//    boolean addedSomething = addLPConstraints(lp, map);
//    //    if( addedSomething ) constraintDocs++;
//
//    // Build and solve the constraints
//    try {
//      lp.buildSystem();
//      values = lp.solveSystem();
//    } catch( Exception ex ) { ex.printStackTrace(); }
//
//    // Remove the added time-time links
//    for( int i = 0; i < numAdded; i++ ) vecs.remove(vecs.size()-1);
//
//    // Return the answers
//    int guesses[] = new int[allresults.length];
//    if( values != null ) {
//      for( int r = 0; r < allresults.length; r++ ) {
//        //	FeatureVector vec = allresults[r].vec();
//        //	int gold = Integer.valueOf(vec.relation());
//
//        int guess = 1;
//        boolean found = false;
//        for( int p = 0; p < NUMCLASSES; p++ ) {
//          //	    System.out.print(" " + (r*LinearProgramming.numClasses + p));
//          if( values[r*NUMCLASSES + p] == 1.0 ) {
//            found = true;
//            guess = p+1; // +1 to reach the range [1-6]
//            break;
//          }
//        }
//        if( !found ) System.err.println("*************\n**************\nERROR: NO ANSWER GIVEN");
//        //	System.out.println("guess=" + guess + ", gold=" + gold);
//
//        guesses[r] = guess;
//        //	trackAnswer(guess, gold, doc.filename(), addedSomething);
//      }
//    }
//
//    return guesses;
//  }
//
//
//  /**
//   * This is an efficiency function that prunes any tlink pairs that
//   * have NONE as their highest probability class.  Since NONE is highest,
//   * and since NONE has no transitive rules, it will always be selected
//   * during graph construction.  Hence, we don't need to add constraints on
//   * it to the graph...
//   */
//  private void removeNoneRelations(Map<String, Set<Pair>> map, FullResult[] allresults) {
//    System.out.println("Removing nones...");
//    for( int i = 0; i < allresults.length; i++ ) {
//      int top = allresults[i].topIndex();
//      //      System.out.println("Top index " + top);
//      // Remove this tlink from the map if it's a NONE relation
//      if( top == relationToInt(TLink.TYPE.NONE.toString()) ) {
//        //	System.out.println("YES none...");
//        Features vec = allresults[i].vec();
//        Set<Pair> set = map.get(vec.event1());
//        // find the Pair with the second event
//        for( Pair pair : set ) {
//          if( pair.first().equals(vec.event2()) ) {
//            //	    System.out.println("Found the pair and removed it...");
//            set.remove(pair);
//            break;
//          }
//        }
//      }
//    }
//  }
//
//
//  private void normalizeProbs(double probs[]) {
//    double sum = 0;
//    for( int i = 0; i < probs.length; i++ ) {
//      sum += Math.exp(probs[i]);
//    }
//    sum = Math.log(sum / (double)probs.length);
//    for( int i = 0; i < probs.length; i++ ) {
//      probs[i] -= sum;
//    }
//  }
//
//  /**
//   * @return The variable name (index) of this specific relation as set
//   *         in the LinearProgramming problem.  Returns -1 if this 
//   *         relation was not set in the problem.
//   */
//  private int toIndex(String var1, String var2, int relation) {
//    //    System.out.print("toIndex " + var1 + "," + var2 + ":" + relation + " is ");
//    Integer index = indices.get(var1 + " " + var2);
//    if( index != null ) {
//      //      System.out.println(index + " and then " + 
//      //			 ((index-1)*LinearProgramming.numClasses+relation));
//      return ((index-1)*LinearProgramming.numClasses + relation);
//    }
//    //    System.out.println("null -1");
//    return -1;
//  }
////  private int toIndex(String var1, String var2, String relation) {
////    return toIndex(var1, var2, TLink.relationToInt(relation));
////  }
//
//
//  /**
//   * Adds transitive constraints based on the current pairs of relations.
//   * @param map A map from string IDs to <ID, Relation> pairs, indicating all
//   *            relations from an ID to other IDs.
//   * @return True if one or more constraints were added, false otherwise.
//   */
//  private boolean addLPConstraints(LinearProgramming lp, Map<String, Set<Pair>> map) {
//    int index1, index2, newIndex;
//    boolean added = false;
//
//    System.out.println("Adding lpconstraints " + map.keySet().size());
//
//    for( String key : map.keySet() ) {
//      System.out.println("key=" + key);
//      Set<Pair> range = map.get(key);
//      Set<Pair> domains = domainsOf(map, key);
//
//
//      // add inverse rules  A-B then B-A
//      for( Pair pair1 : range ) {
//        String id2 = (String)pair1.first();
//
//        for( int rel = 1; rel <= NUMCLASSES; rel++ ) {
//          Integer inv = closure.inverseConstraint(rel);
//          System.out.println("rel=" + rel + " inv=" + inv);
//          if( inv != null ) {
//            // check if the inverse pair is in our dataset...ignore if not
//            newIndex = toIndex(id2, key, inv);
//            if( newIndex != -1 ) {
//              index1 = toIndex(key, id2, rel);
//              System.out.println("Exp: adding " + id2 + " " + key + " rel=" + rel);
//
//              // This is called ... but it disappeared somehow from LinearProgramming.
//              // Need to write this code!!!
//              lp.addInverseConstraint( index1, newIndex );
//            }
//          }
//        }
//      }
//
//
//      // matchCase 0  // A-B A-C
//      if( range.size() > 1 ) {
//        //	System.out.println("---Match case 0---");
//        // loop over all pairs in the key's range
//        for( Pair pair1 : range ) {
//          String id1 = (String)pair1.first();
//          for( Pair pair2 : range ) {
//            String id2 = (String)pair2.first();
//            // 1 B 2, 1 B 3 = 2 B 3
//            if( pair1 != pair2 && (hasRule(map, id1, id2) || hasRule(map, id2, id1)) ) {
//              //	      System.out.println("Trying rule: " + pair1 + " and " + pair2);
//
//              for( int rel1 = 1; rel1 <= NUMCLASSES; rel1++ ) {
//                for( int rel2 = 1; rel2 <= NUMCLASSES; rel2++ ) {
//                  Integer transitive = closure.getClosed(0, rel1, rel2);
//
//                  if( transitive != null ) {
//                    int newrel = transitive.intValue();
//                    boolean flipped = closure.getFlipped(0, rel1, rel2);
//
//                    // Get the variable index from the Linear Program
//                    if( !flipped ) newIndex = toIndex(id1, id2, newrel);
//                    else {
//                      newIndex = toIndex(id2, id1, newrel);
//                      //		      System.out.println("flipped...");
//                    }
//
//                    // The new event pair may not exist in original data
//                    if( newIndex != -1 ) {
//                      index1 = toIndex(key, id1, rel1);
//                      index2 = toIndex(key, id2, rel2);
//                      lp.addTransitiveConstraint( index1, index2, newIndex );
//                      added = true;
//                      //		      System.out.println("Added " + key + "," + id1 + ":" + rel1 + 
//                      //					 " and "  + key + "," + id2 + ":" + rel2 +
//                      //					 " with relation " + transitive + " at " + newIndex);
//                    }
//                  }
//                }
//              }
//            }
//          }
//        }
//      }
//
//      // matchCase 1   // A-B C-A
//      if( inSomeRange(map, key) ) {
//        System.out.println("---Match case 1---");
//        // loop over all pairs in the key's range
//        for( Pair pair1 : range ) {
//          String id1 = (String)pair1.first();
//          System.out.println("pair1=" + pair1);
//          for( Pair pair2 : domains ) {
//            String id2 = (String)pair2.first();
//            System.out.println("pair2=" + pair2);
//            // 1 B 2, 3 B 1 = 2 B 3
//            if( pair1 != pair2 && (hasRule(map, id1, id2) || hasRule(map, id2, id1)) ) {
//              System.out.println("Trying1 rule: " + pair1 + " and " + pair2);
//
//              for( int rel1 = 1; rel1 <= NUMCLASSES; rel1++ ) {
//                for( int rel2 = 1; rel2 <= NUMCLASSES; rel2++ ) {
//                  Integer transitive = closure.getClosed(1, rel1, rel2);
//
//                  if( transitive != null ) {
//                    int newrel = transitive.intValue();
//                    boolean flipped = closure.getFlipped(1, rel1, rel2);
//
//                    // Get the variable index from the Linear Program
//                    if( !flipped ) newIndex = toIndex(id1, id2, newrel);
//                    else {
//                      newIndex = toIndex(id2, id1, newrel);
//                      //		      System.out.println("...flipped");
//                    }
//
//                    // The new transitive relation may not have existed in original data
//                    if( newIndex != -1 ) {
//                      index1 = toIndex(key, id1, rel1);
//                      index2 = toIndex(id2, key, rel2);
//                      lp.addTransitiveConstraint( index1, index2, newIndex );
//                      added = true;
//                      //		      System.out.println("Added " + key + "," + id1 + ":" + rel1 +
//                      //					 " and " + id2 + "," + key + ":" + rel2 +
//                      //					 " with relation " + transitive + " at " + newIndex);
//                    }
//                  }
//                }
//              }
//            }
//          }
//        }
//      }
//
//      // matchCase 2   // B-A A-C
//      if( inSomeRange(map, key) ) {
//        //	System.out.println("---Match case 2---");
//        // Loop over all pairs in the key's domain
//        for( Pair pair1 : domains ) {
//          String id1 = (String)pair1.first();
//          for( Pair pair2 : range ) {
//            String id2 = (String)pair2.first();
//            // 1 B 2, 3 B 1 = 2 B 3
//            if( pair1 != pair2 && (hasRule(map, id1, id2) || hasRule(map, id2, id1)) ) {
//              //	      System.out.println("Trying2 rule: " + pair1 + " and " + pair2);
//
//              for( int rel1 = 1; rel1 <= NUMCLASSES; rel1++ ) {
//                for( int rel2 = 1; rel2 <= NUMCLASSES; rel2++ ) {
//                  Integer transitive = closure.getClosed(2, rel1, rel2);
//
//                  if( transitive != null ) {
//                    int newrel = transitive.intValue();
//                    boolean flipped = closure.getFlipped(2, rel1, rel2);
//
//                    // Get the variable index from the Linear Program
//                    if( !flipped ) newIndex = toIndex(id1, id2, newrel);
//                    else {
//                      newIndex = toIndex(id2, id1, newrel);
//                      //		      System.out.println("...flipped");
//                    }
//
//                    // The new transitive relation may not have existed in original data
//                    if( newIndex != -1 ) {
//                      index1 = toIndex(id1, key, rel1);
//                      index2 = toIndex(key, id2, rel2);
//                      lp.addTransitiveConstraint( index1, index2, newIndex );
//                      added = true;
//                      //		      System.out.println("Added " + id1 + "," + key + ":" + rel1 +
//                      //					 " and " + key + "," + id2 + ":" + rel2 +
//                      //					 " with relation " + transitive + " at " + newIndex);
//                    }
//                  }
//                }
//              }
//            }
//          }
//        }
//      }
//
//      // matchCase 3   // B-A C-A
//      if( inSomeRange(map, key) ) {
//        //	System.out.println("---Match case 3---");
//        // loop over all pairs in the key's domain
//        for( Pair pair1 : domains ) {
//          String id1 = (String)pair1.first();
//          for( Pair pair2 : domains ) {
//            String id2 = (String)pair2.first();
//            // 2 B 1, 3 S 1 = 2 B 3
//            if( pair1 != pair2 && (hasRule(map, id1, id2) || hasRule(map, id2, id1)) ) {
//              //	      System.out.println("Trying3 rule: " + pair1 + " and " + pair2);
//
//              for( int rel1 = 1; rel1 <= NUMCLASSES; rel1++ ) {
//                for( int rel2 = 1; rel2 <= NUMCLASSES; rel2++ ) {
//                  Integer transitive = closure.getClosed(3, rel1, rel2);
//
//                  if( transitive != null ) {
//                    int newrel = transitive.intValue();
//                    boolean flipped = closure.getFlipped(3, rel1, rel2);
//
//                    // Get the variable index from the Linear Program
//                    if( !flipped ) newIndex = toIndex(id1, id2, newrel);
//                    else {
//                      newIndex = toIndex(id2, id1, newrel);
//                      //		      System.out.println("...flipped");
//                    }
//
//                    // The new transitive relation may not have existed in original data
//                    if( newIndex != -1 ) {
//                      index1 = toIndex(id1, key, rel1);
//                      index2 = toIndex(id2, key, rel2);
//                      lp.addTransitiveConstraint( index1, index2, newIndex );
//                      added = true;
//                      //		      System.out.println("Added " + id1 + "," + key + ":" + rel1 +
//                      //					 " and " + id2 + "," + key + ":" + rel2 +
//                      //					 " with relation " + transitive + " at " + newIndex);
//                    }
//                  }
//                }
//              }
//            }
//          }
//        }
//      }
//    }
//    return added;
//  }
//
//  /**
//   * @return True if one->two exists in the Map
//   */
//  private boolean hasRule(Map<String, Set<Pair>> map, String one, String two) {
//    Set<Pair> range = map.get(one);
//    if( range != null ) {
//      for( Pair pair : range ) {
//        if( pair.first().equals(two) ) return true;
//      }
//    }
//    return false;
//  }
//
//
//  /**
//   * @return True if the key is in the set of some other key
//   */
//  private boolean inSomeRange(Map<String, Set<Pair>> map, String key) {
//    for( String k : map.keySet() ) {
//      Set<Pair> range = map.get(k);
//      for( Pair pair : range ) {
//        if( pair.first().equals(key) ) return true;
//      }
//    }
//    return false;
//  }
//
//  /**
//   * @return True if the key is in the set of some other key
//   */
//  private Set<Pair> domainsOf(Map<String, Set<Pair>> map, String key) {
//    Set<Pair> set = new HashSet<Pair>();
//    for( String k : map.keySet() ) {
//      Set<Pair> range = map.get(k);
//      for( Pair pair : range ) {
//        if( pair.first().equals(key) ) set.add(new Pair(k,pair.second()));
//      }
//    }
//    return set;
//  }
//
//  /**
//   * @return A map from event ID strings to a set of event IDs
//   */
//  private Map<String, Set<Pair>> getVars(Vector<Features> vecs) {
//    Map<String, Set<Pair>> map = new HashMap<String, Set<Pair>>();
//
//    for( Features vec : vecs ) {
//      Set set = map.get(vec.event1());
//      if( set == null ) set = new HashSet();
//
//      set.add(new Pair(vec.event2(), vec.relation()));
//      map.put(vec.event1(), set);
//    }
//
//    return map;
//  }
//
//
//
//  /**
//   * @param filename The document name within the infofile to return
//   */
//  private Vector<TLink> getTimeTimeLinksFromInfoFile(String filename) {
//    if( infoFile == null ) {
//      System.out.println("WARNING: no infofile loaded, so no time-time links available");
//      return null;
//    }
//
//    // Trim off the "allfeatures" starter...
//    if( filename.startsWith("allfeatures-") ) filename = filename.substring(12);
//    if( filename.endsWith(".txt") ) 
//      filename = filename.substring(0,filename.length()-4) + ".tml.xml";
//
//    System.out.println("Getting tlinks from: " + filename);
//
//    // Adding TIME-TIME didn't seem to help
//    Vector<TLink> current = infoFile.getTlinksOfType(filename, TLink.TIME_TIME);
//    // remove the trailing ".xml"
//    if( current == null )
//      current = infoFile.getTlinksOfType(filename.substring(0,filename.length()-4), 
//          TLink.TIME_TIME);
//
//    // Create rule-based computed TIME-TIME links
//    Vector<TLink> newones = infoFile.computeTimeTimeLinks(filename);
//    for( TLink link : newones ) System.out.println("**" + link);
//
//    // Merge the corpus and generated ones...
//    Vector<TLink> merged = mergeTimeTimeLinks(current, newones);
//
//    return merged;
//  }
//
//
//  /**
//   * @param filename The document name within the infofile to return
//   */
//  private Vector<TLink> getTimeLinksFromInfoFile(String filename) {
//    if( infoFile == null ) return null;
//
//    // Trim off the "allfeatures" starter...
//    if( filename.startsWith("allfeatures-") ) filename = filename.substring(12);
//    if( filename.endsWith(".txt") ) 
//      filename = filename.substring(0,filename.length()-4) + ".tml.xml";
//
//    System.out.println("Getting tlinks from: " + filename);
//    Vector<TLink> links = infoFile.getTlinksOfType(filename, TLink.EVENT_TIME);
//    // remove the last .xml
//    if( links == null )
//      links = infoFile.getTlinksOfType(filename.substring(0,filename.length()-4), 
//          TLink.EVENT_TIME);
//
//    Vector<TLink> timetime = getTimeTimeLinksFromInfoFile(filename);
//
//    /*
//    // Adding TIME-TIME didn't seem to help
//    Vector<TLink> current = infoFile.getTlinksOfType(filename, TLink.TIME_TIME);
//    if( current == null )
//      current = infoFile.getTlinksOfType(filename.substring(0,filename.length()-4), 
//					 TLink.TIME_TIME);
//
//    // Create rule-based computed TIME-TIME links
//    Vector<TLink> newones = infoFile.computeTimeTimeLinks(filename);
//    for( TLink link : newones ) System.out.println("**" + link);
//
//    // Merge the corpus and generated ones...
//    Vector<TLink> merged = mergeTimeTimeLinks(current, newones);
//    if( links != null ) links.addAll(merged);
//    else links = merged;
//     */
//
//    if( links != null ) links.addAll(timetime);
//    else links = timetime;
//
//    return links;
//  }
//
//
//  private Vector<TLink> mergeTimeTimeLinks(Vector<TLink> core, Vector<TLink> adding) {
//    System.out.println("Trying to add " + adding.size() + " to " + core.size());
//    int count = 0;
//    Vector<TLink> merged = new Vector(core);
//
//    for( TLink add : adding ) {
//      boolean duplicate = false;
//      for( TLink old : core ) {
//        // don't add new links that are already in core
//        if( (add.event1().equals(old.event1()) && add.event2().equals(old.event2())) ||
//            (add.event1().equals(old.event2()) && add.event2().equals(old.event1())) ) {
//          duplicate = true;
//          break;
//        }
//      }
//      if( !duplicate ) {
//        merged.add(add);
//        count++;
//      }
//    }
//
//    System.out.println("...added " + count);
//    return merged;
//  }
//
//
//  /**
//   * Takes a directory of folds with precomputed probabilities for each
//   * class label.  It then builds an ILP problem to solve it.
//   * @param externalDir Path to the directory of external probabilities
//   */
//  private void graphOverlayDirectory(String externalDir) {
//    int flipped = 0;
//
//    for( int f = 0; f < folds; f++ ) {
//      System.out.println("Starting fold " + f);
//      //      graphOverlay(f, externalDir, "guess" + f + ".arff");
//      flipped += graphOverlay(f, externalDir, "guess" + f); // e.g. "dir/guess4"
//    }
//    System.out.println("Total Flipped All Folds: " + flipped);
//  }
//
//
//  /**
//   * Loads external probabilities and applies graph constraints to recompute
//   * the optimal classifications.
//   * @param fold The fold number
//   * @param externalDir The path to the external directory with guess probabilities
//   * @param file The filename of the probs within that directory
//   */
//  private int graphOverlay(int fold, String externalDir, String file) {
//    //    ARFFReader reader = new ARFFReader();
//    LibsvmReader reader = new LibsvmReader();
//    Vector<double[]> externalProbs = reader.fromFile(externalDir + File.separator + file);
//    int i = 0;
//    int flipped = 0;
//    int constraintDocs = 0;
//    double[] values = null;
//
//    BufferedWriter writer = null;
//    try { 
//      // save guesses to file
//      writer = new BufferedWriter(new FileWriter(externalDir + File.separator +
//          "overlay" + fold));
//
//      // Load the ORIGINAL test fold with the event IDs
//      Vector<FileOfVecs> files = readFileSeparateFiles(directory + "/test" + fold, null, externalProbs.size());
//      for( FileOfVecs fileOf : files ) {
//
//        // original features from a single document
//        Vector<Features> vecs = fileOf.getVecs();
//        FullResult allresults[] = new FullResult[vecs.size()];
//        int[] temp = new int[vecs.size()]; // for debugging only
//        int xx = 0;
//        for( Features vec : (Vector<Features>)vecs ) {
//          double[] probs = externalProbs.elementAt(i);
//          i++;
//
//          // Create a sortable array of results
//          allresults[xx] = new FullResult(vec, probs);
//          temp[xx] = allresults[xx].topIndex();
//          xx++;
//          System.out.println("vec: " + vec);
//        }
//
//        // Solve the graph
//        int[] guesses = null;
//        if( type == LINEAR_PROGRAM ) guesses = lpSolveDocument(fileOf, allresults);
//        else if( type == SORTEDGRAPH ) guesses = sortGreedySolveDocument(fileOf, allresults);
//        else { 
//          System.err.println("No graph type selected...");
//          System.exit(1);
//        }
//
//        // Print the guesses
//        if( guesses.length == 0 ) {
//          System.out.println("NO GUESSES??");
//          System.exit(1);
//        }
//        if( guesses.length != vecs.size() ) {
//          System.out.println("Guesses length different from vectors? " + guesses.length + " " + vecs.size());
//          System.exit(1);
//        }
//        for( int j = 0; j < guesses.length; j++ ) {
//          System.out.print(j + ": " + guesses[j] + " from guess " + temp[j] + " gold " + vecs.elementAt(j).relation());
//          writer.write(guesses[j] + "\n");
//          if( guesses[j] != temp[j] ) {
//            flipped++;
//            System.out.print(" FLIP!");
//          }
//          System.out.println();
//        }
//      }
//
//      writer.close();
//      System.out.println("Total Flipped Due To Graph: " + flipped);
//      return flipped;
//    } catch( Exception ex ) { ex.printStackTrace(); }
//    return 0;
//  }
//
//  public static String intToStringRelation(int rel) {
//    if( rel == 1 ) return "BEFORE";
//    if( rel == 2 ) return "IBEFORE";
//    if( rel == 3 ) return "INCLUDES";
//    if( rel == 4 ) return "BEGINS";
//    if( rel == 5 ) return "ENDS";
//    if( rel == 6 ) return "SIMULTANEOUS";
//    if( rel == 8 ) return "AFTER";
//    if( rel == 9 ) return "OVERLAP";
//    if( rel == 10 ) return "BEFORE_OR_OVERLAP";
//    if( rel == 11 ) return "OVERLAP_OR_AFTER";
//
//    if( rel == 7 ) return "NONE";
//    return null;
//  }
//
//  public static int relationToInt(String rel) {
//    if( rel.equalsIgnoreCase("before") ) return 1;
//    if( rel.equalsIgnoreCase("ibefore") ) return 2;
//    if( rel.equalsIgnoreCase("includes") ) return 3;
//    if( rel.equalsIgnoreCase("begins") ) return 4;
//    if( rel.equalsIgnoreCase("ends") ) return 5;
//    if( rel.equalsIgnoreCase("simultaneous") ) return 6;
//    if( rel.equalsIgnoreCase("none") ) return 7;
//    if( rel.equalsIgnoreCase("after") ) return 8;
//    System.err.println("(TLink.java) Converting an unknown relation (" + rel + ")!");
//    return 1;
//  }
//
//  /**
//   * Print the statistics to standard out
//   */
//  private void printStats() {
//    System.out.println();
//    System.out.println("--System Results--");
//    System.out.println("Acc: " + ((double)CORRECT / (double)(CORRECT+INCORRECT)));
//    System.out.println("Correct: " + CORRECT + "\nIncorrect: " + INCORRECT +
//        " Total: " + (CORRECT + INCORRECT));
//
//    System.out.println("Precision: ");
//    for( int i = 0; i < NUMCLASSES; i++ ) {
//      int sum = 0;
//      for( int j = 0; j < NUMCLASSES; j++ ) sum += CONFUSION[i][j];
//      System.out.print((i+1) + ": " + (double)CORRECTS[i]/(double)sum + "  ");
//    }
//    System.out.println();
//
//    System.out.println("Recall: ");
//    for( int i = 0; i < NUMCLASSES; i++ )
//      System.out.print((i+1) + ": " + 
//          (double)CORRECTS[i]/(double)(CORRECTS[i]+INCORRECTS[i]) + "  ");
//    System.out.println();
//
//    System.out.println("--constrained--");
//    System.out.println("Acc: " + ((double)CORRECT_CONSTRAINED / (double)(CORRECT_CONSTRAINED+INCORRECT_CONSTRAINED)));
//    System.out.println("Correct: " + CORRECT_CONSTRAINED + "\nIncorrect: " + INCORRECT_CONSTRAINED
//        + " Total: " + (CORRECT_CONSTRAINED + INCORRECT_CONSTRAINED));
//
//    System.out.println("--confusion matrix--");
//    for( int i = 0; i < NUMCLASSES; i++ ) {
//      for( int j = 0; j < NUMCLASSES; j++ ) System.out.print(CONFUSION[j][i] + " ");
//      System.out.println(" b = " + (i+1));
//    }
//
//    /*
//    if( FILENAMES.size() > 0 ) {
//      int tcorrect = 0, tincorrect = 0;
//
//      System.out.println("--by file--");
//      for( String filename : FILENAMES ) {
//	int correct = 0;
//	Integer c = CORRECT_FILES.get(filename);
//	if( c != null ) correct = c;
//	int incorrect = 0;
//	Integer i = INCORRECT_FILES.get(filename);
//	if( i != null ) incorrect = i;
//
//	tcorrect += correct;
//	tincorrect += incorrect;
//	System.out.println("File " + filename);
//	System.out.println("Acc: " + ((double)correct / (double)(correct+incorrect)) +
//			   " (" + correct + "/" + (correct+incorrect) + ")");
//      }
//      System.out.println("File Acc: " + ((double)tcorrect / (double)(tcorrect+tincorrect)) +
//			 " (" + tcorrect + "/" + (tcorrect+tincorrect) + ")");
//    }
//     */
//  }
//
//  /**
//   * Reset all the statistics to zero
//   */
//  private void clearStats() {
//    CORRECT = 0;
//    INCORRECT = 0;
//    CORRECT_CONSTRAINED = 0;
//    INCORRECT_CONSTRAINED = 0;
//    Arrays.fill(CORRECTS, 0);
//    Arrays.fill(INCORRECTS, 0);
//    CORRECT_FILES.clear();
//    INCORRECT_FILES.clear();
//    FILENAMES.clear();
//    for( int i = 0; i < NUMCLASSES; i++ ) Arrays.fill(CONFUSION[i], 0);
//  }
//
//  /**
//   * Globally keep track of the stats
//   * @constrained True if the answer came from a document that had transitive constraints
//   */  
//  private void trackAnswer(int guess, int gold, String filename, boolean constrained) {
//    if( guess == gold ) {
//      CORRECT++;
//      CORRECTS[gold-1]++;
//    }
//    else {
//      INCORRECT++;
//      INCORRECTS[gold-1]++;
//    }
//
//    CONFUSION[guess-1][gold-1]++;
//
//    // stats for individual document performance
//    if( filename != null && Arrays.binarySearch(CONSTRAINED_FILES, filename) >= 0 ) {
//      //    if( constrained && filename != null ) {
//      if( !FILENAMES.contains(filename) ) FILENAMES.add(filename);
//      if( guess == gold ) {
//        Integer c = CORRECT_FILES.get(filename);
//        if( c == null ) CORRECT_FILES.put(filename, new Integer(1));
//        else CORRECT_FILES.put(filename, ++c);
//      } else {
//        Integer i = INCORRECT_FILES.get(filename);
//        if( i == null )	INCORRECT_FILES.put(filename, new Integer(1));
//        else INCORRECT_FILES.put(filename, ++i);
//      }
//    }
//
//    // extra stats for graph constraints
//    if( constrained ) {
//      if( guess == gold ) CORRECT_CONSTRAINED++;
//      else INCORRECT_CONSTRAINED++;
//    }
//  }
//
//
//  private void runall() {
//    String types[] = new String[4];
//    types[0] = "Addition";
//    types[1] = "Removal";
//    types[2] = "Bi-Addition";
//    types[3] = "Bi-Removal";
//    double acc[] = new double[4];
//
//    acc[0] = featureAdd(keeps);
//    acc[1] = featureRemove(keeps);
//    acc[2] = featureAddBi(keeps);
//    acc[3] = featureRemoveBi(keeps);
//
//    for( int i = 0; i < types.length; i++ )
//      System.out.println(types[i] + ": " + acc[i]);
//  }
//
//
//  /**
//   * A debugging function that prints an array of booleans in 1's and 0's
//   */
//  private void printKeeps(boolean k[]) {
//    for( int j = 0; j < k.length; j++ ) {
//      if( k[j] ) System.out.print("1 ");
//      else System.out.print("0 ");
//    }
//    System.out.println();
//  }
//
//
//
//  private void run() {
//    double acc = 0;
//    //    nb = new NaiveBayes(FeatureVector.order, limitsizes, NUMCLASSES);
//    nb = new NaiveBayes(FeatureType.values(), limitsizes, NUMCLASSES);
//
//    // "keeps" is set by the command line args
//
//    // load external probs, put a graph over it
//    if( overlay ) { 
//      System.out.println("Running graph overlay");
//      graphOverlayDirectory(overlayDir);
//    }
//    // no external probs, run naive bayes in some way
//    else {
//      if( type == ADD ) {
//        System.out.println("Running feature addition");
//        acc = featureAdd(keeps);
//      }
//      else if( type == REMOVE ) {
//        System.out.println("Running feature removal");
//        acc = featureRemove(keeps);
//      }
//      else if( type == ADD_BI ) {
//        System.out.println("Running feature addition with split distribution");
//        acc = featureAddBi(keeps);
//      }
//      else if( type == REMOVE_BI ) {
//        System.out.println("Running feature removal with split distribution");
//        acc = featureRemoveBi(keeps);
//      }
//      else if( type == ADD_2 ) {
//        System.out.println("Running double addition");
//        acc = featureAdd2(keeps);
//      }
//      else if( type == ADD_BI_2 ) {
//        System.out.println("Running double addition with split distribution");
//        acc = featureAddBi2(keeps);
//      }
//      else if( type == CROSS ) {
//        System.out.println("Running cross validation");
//        acc = crossValidate(keeps);
//      }
//      else if( type == FULL ) {
//        System.out.println("Running full range");
//        runall();
//      }
//      else if( type == GRAPH ) {
//        System.out.println("Running greedy graph");
//        acc = greedyGraphCrossValidate(keeps, false, false);
//      }
//      else if( type == SORTEDGRAPH ) {
//        System.out.println("Running sorted greedy graph");
//        acc = greedyGraphCrossValidate(keeps, true, false);
//      }
//      else if( type == SORTEDGRAPH_BI ) {
//        System.out.println("Running sorted greedy graph with bi dist");
//        nb.initBiDistribution();
//        acc = greedyGraphCrossValidate(keeps, true, true);
//      }
//      else if( type == LINEAR_PROGRAM ) {
//        System.out.println("Running linear programming (" + NUMCLASSES + " classes)");
//        acc = lpCrossValidate(keeps, false);
//      }
//      else if( type == LINEAR_PROGRAM_BI ) {
//        System.out.println("Running linear programming with bi dist");
//        nb.initBiDistribution();
//        acc = lpCrossValidate(keeps, true);
//      }
//    }
//    printStats();
//    System.out.println("\nFinal Accuracy: " + (100*acc));
//  }
//
//
//  public class FileOfVecs {
//    String filename;
//    Vector vecs;
//
//    FileOfVecs() { vecs = new Vector(); }
//    FileOfVecs(String file) {
//      filename = file;
//      vecs = new Vector();
//    }
//
//    public Vector getVecs() { return vecs; }
//    public String filename() { return filename; }
//    public int size() { return vecs.size(); }
//    public void add(Object obj) { vecs.add(obj); }
//  }
//
//
//  public class FullResult implements Comparable {
//    Features vec;
//    Result scores[];
//    int index = 0; // index in an array - we sometimes sort arrays of these objects, but want to preserve the original ordering.
//
//    FullResult(Features vec, double arr[]) { 
//      this.vec = vec;
//      scores = new Result[arr.length];
//      for( int i = 0; i < arr.length; i++ )
//        scores[i] = new Result(i+1,arr[i]); // add one for tlink relations
//    }
//
//    public void sort() { Arrays.sort(scores); }
//    public double topScore() {
//      double best = Double.NEGATIVE_INFINITY;
//      for( int i = 0; i < scores.length; i++ ) {
//        if( scores[i].score() > best ) best = scores[i].score();
//      }
//      //      return scores[scores.length-1].score(); 
//      return best;
//    }
//    public int topIndex() {
//      double best = Double.NEGATIVE_INFINITY;
//      int besti = 0;
//      for( int i = 0; i < scores.length; i++ ) {
//        if( scores[i].score() > best ) {
//          best = scores[i].score();
//          besti = i;
//        }
//      }
//      return scores[besti].index();
//    }
//    public Result nth(int i) { return scores[i]; }
//    public int length() { return scores.length; }
//    public Features vec() { return vec; }
//    public void setIndex(int i) { index = i; }
//    public int index() { return index; }
//
//    // Comparable interface function
//    public int compareTo(Object r) {
//      if( topScore() < ((FullResult)r).topScore() ) return -1;
//      else if( topScore() == ((FullResult)r).topScore() ) return 0;
//      else return 1;
//    }
//
//    public double[] scoreArray() {
//      double arr[] = new double[scores.length];
//      for( int i = 0; i < scores.length; i++ ) arr[i] = scores[i].score();
//      return arr;
//    }
//    public String toString() { return Arrays.toString(scores); }
//  }
//
//
//  /**
//   * Separate class to store accuracy scores with their classes.
//   * This is only used so we can sort accuracy scores and still
//   * know which score goes with which class.
//   */
//  public class Result implements Comparable {
//    int index;
//    double score;
//
//    Result(int i, double score) {
//      this.index = i;
//      this.score = score;
//    }
//
//    public int index() { return index; }
//    public double score() { return score; }
//    public void setIndex(int i) { index = i; }
//    public void setScore(double d) { score = d; }
//
//    // Comparable interface function
//    public int compareTo(Object r) {
//      if( score < ((Result)r).score() ) return -1;
//      else if( score == ((Result)r).score() ) return 0;
//      else return 1;
//    }
//
//    public String toString() { return (index + ": " + score); }
//  }
//
//
//  /**
//   * Main
//   */
//  public static void main(String[] args) {
//    // CHOOSE THE TAGSET
//    //    TLink.changeMode(TLink.REDUCED_MODE);
//    //        TLink.changeMode(TLink.FULL_MODE);
//    //    TLink.changeMode(TLink.SYMMETRY_MODE);
//    TLink.changeMode(TLink.MODE.FULLSYMMETRY);
//
//    Experiment exp = new Experiment();
//    exp.handleParameters(args);
//
//    exp.run();
//  }
//}
