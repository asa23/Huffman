
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Scanner;

public class SimpleHuffProcessor implements IHuffProcessor {

 private HuffViewer myViewer;
 private PriorityQueue<TreeNode> Tree;
 private PriorityQueue<TreeNode> TreeU;
 int[] result = new int[IHuffConstants.ALPH_SIZE];
 String[] HuffTree = new String[IHuffConstants.ALPH_SIZE + 1];
 int inbits = 0;
 int huffbits = 0;
 TreeNode root;
 TreeNode rootU;

 public int compress(InputStream in, OutputStream out, boolean force)
   throws IOException {
  // throw new IOException("compress is not implemented");
  // write out the magic number
  if (huffbits > inbits && force == false) {
   int iii = huffbits - inbits;
   throw new IOException("compression uses " + iii + " more bits,"
     + "\n" + "use force compression to compress");
  }

  BitInputStream inStream = new BitInputStream(in);
  BitOutputStream outStream = new BitOutputStream(out);
  outStream.writeBits(BITS_PER_INT, MAGIC_NUMBER);

  for (int k = 0; k < ALPH_SIZE; k++) {
   outStream.writeBits(BITS_PER_INT, result[k]);
  }

  int currentWord = inStream.readBits(IHuffConstants.BITS_PER_WORD);
  while (currentWord != -1) {
   String write = HuffTree[currentWord];
   for (int i = 0; i < write.length(); ++i) {
    if (write.charAt(i) == '0') {
     outStream.writeBits(1, 0);
    }
    if (write.charAt(i) == '1') {
     outStream.writeBits(1, 1);
    }
   }
   currentWord = inStream.readBits(BITS_PER_WORD);
  }

  String writeE = HuffTree[PSEUDO_EOF];
  for (int i = 0; i < writeE.length(); ++i) {
   if (writeE.charAt(i) == '0') {
    outStream.writeBits(1, 0);
   }
   if (writeE.charAt(i) == '1') {
    outStream.writeBits(1, 1);
   }
  }

  outStream.flush();

  return huffbits;
 }

 public int preprocessCompress(InputStream in) throws IOException {
  // throw new IOException("preprocess not implemented");
  Tree = new PriorityQueue<TreeNode>();
  int[] result = countLetters(in);
  for (int i = 0; i < result.length; ++i) {
   Tree.add(new TreeNode(i, result[i]));
  }
  Tree.add(new TreeNode(IHuffConstants.PSEUDO_EOF, 1));

  while (Tree.size() > 1) {
   TreeNode Node1 = Tree.remove();
   TreeNode Node2 = Tree.remove();
   int Weight = Node1.myWeight + Node2.myWeight;
   TreeNode Combine = new TreeNode(-1, Weight, Node1, Node2);
   Tree.add(Combine);
  }

  root = Tree.remove();

  writeHelper(root, "");

  for (int i = 0; i < result.length; ++i) {
   huffbits += result[i] * HuffTree[i].length();
  }

  // Add in size of magic number and header
  huffbits += ALPH_SIZE * (BITS_PER_INT + 1);

  return inbits - huffbits;
 }

 private void writeHelper(TreeNode Root, String path) {
  if (Root.myLeft == null && Root.myRight == null) {
   HuffTree[Root.myValue] = path;
   return;
  }
  writeHelper(Root.myLeft, path + "0");
  writeHelper(Root.myRight, path + "1");
 }

 public void setViewer(HuffViewer viewer) {
  myViewer = viewer;
 }

 public int uncompress(InputStream in, OutputStream out) throws IOException {
  // throw new IOException("uncompress not implemented");
  BitInputStream inStream = new BitInputStream(in);
  BitOutputStream outStream = new BitOutputStream(out);

  int magic = inStream.readBits(BITS_PER_INT);
  if (magic != MAGIC_NUMBER) {
   throw new IOException("magic number not right");
  }

  int[] myCounts = new int[IHuffConstants.ALPH_SIZE];

  for (int k = 0; k < ALPH_SIZE; k++) {
   int bits = inStream.readBits(BITS_PER_INT);
   myCounts[k] = bits;
  }

  TreeU = new PriorityQueue<TreeNode>();
  int[] result = myCounts;
  for (int i = 0; i < result.length; ++i) {
   TreeU.add(new TreeNode(i, result[i]));
  }
  TreeU.add(new TreeNode(IHuffConstants.PSEUDO_EOF, 1));

  while (TreeU.size() > 1) {
   TreeNode Node1 = TreeU.remove();
   TreeNode Node2 = TreeU.remove();
   int Weight = Node1.myWeight + Node2.myWeight;
   TreeNode Combine = new TreeNode(-7, Weight, Node1, Node2);
   TreeU.add(Combine);
  }
  rootU = TreeU.remove();

  int bbit = 0;
  TreeNode tnode = rootU;
  while (true) {
   int bits = inStream.readBits(1);
   if (bits == -1) {
    throw new IOException("error reading bits, no PSEUDO-EOF");
   }

   if ((bits & 1) == 0)
    tnode = tnode.myLeft;
   else
    tnode = tnode.myRight;

   // if (tnode.myValue == -1) {
   if (tnode.myLeft == null && tnode.myRight == null) {
    if (tnode.myValue == PSEUDO_EOF)
     break; // out of while-loop
    else
     outStream.writeBits(BITS_PER_WORD, tnode.myValue);
    bbit += BITS_PER_WORD;
    tnode = rootU; // start back at top
   }
  }
  outStream.flush();

  return bbit;
 }

 private void showString(String s) {
  myViewer.update(s);
 }

 public int[] countLetters(InputStream in) throws IOException {
  BitInputStream inStream = new BitInputStream(in);
  int currentWord = inStream.readBits(IHuffConstants.BITS_PER_WORD);

  while (currentWord != -1) {
   result[currentWord]++;
   inbits += BITS_PER_WORD;
   currentWord = inStream.readBits(BITS_PER_WORD);
  }
  return result;
 }
}
