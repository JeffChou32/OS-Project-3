import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Project3 {
    static int BLOCK_SIZE = 512;
    static String MAGIC = "4348PRJ3";

    static class Header {
        long rootId; //root node block ID
        long nextBlockId;

        static Header read(RandomAccessFile raf) throws IOException { //loads first block, checks magic #, reads rootID/nextBlockID
            raf.seek(0);
            byte[] block = new byte[BLOCK_SIZE];
            raf.readFully(block);
            ByteBuffer buf = ByteBuffer.wrap(block);

            byte[] magic = new byte[8];
            buf.get(magic);
            String magicStr = new String(magic, StandardCharsets.US_ASCII).trim();
            if (!magicStr.equals(MAGIC)) throw new IOException("Invalid index file");

            long rootId = buf.getLong();
            long nextBlockId = buf.getLong();
            return new Header(rootId, nextBlockId);
        }

        void write(RandomAccessFile raf) throws IOException { //write 512 byte header block 
            byte[] block = new byte[BLOCK_SIZE];
            ByteBuffer buf = ByteBuffer.wrap(block);

            buf.put(MAGIC.getBytes(StandardCharsets.US_ASCII));
            if (MAGIC.length() < 8) buf.put(new byte[8 - MAGIC.length()]);

            buf.putLong(rootId);
            buf.putLong(nextBlockId);

            raf.seek(0);
            raf.write(block);
        }

        Header(long rootId, long nextBlockId) {
            this.rootId = rootId;
            this.nextBlockId = nextBlockId;
        }
    }

    static class Node {
        long blockId;
        long parentId = 0;
        int numKeys = 0; //keys currently used
        long[] keys = new long[19];
        long[] values = new long[19];
        long[] children = new long[20];

        void write(RandomAccessFile raf, long blockId) throws IOException { //serialize node into 512 byte array and write
            byte[] block = new byte[BLOCK_SIZE];
            ByteBuffer buf = ByteBuffer.wrap(block);
            buf.putLong(blockId);
            buf.putLong(parentId);
            buf.putLong(numKeys);
            for (int i = 0; i < 19; i++) buf.putLong(keys[i]);
            for (int i = 0; i < 19; i++) buf.putLong(values[i]);
            for (int i = 0; i < 20; i++) buf.putLong(children[i]);
            raf.seek(blockId * BLOCK_SIZE);
            raf.write(block);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Enter some arguments");
            return;
        }

        String command = args[0];
        switch (command) {
            case "create" -> createFile(args);
            case "insert" -> insert(args);
            case "print" -> print(args);
            case "search" -> search(args);
            case "load" -> load(args);
            case "extract" -> extract(args);
            default -> System.err.println("Unknown command: " + command);
        }
    }

    private static void createFile(String[] args) throws IOException { //create index file
        String filename = args[1];
        File file = new File(filename);
        if (file.exists()) {
            System.err.println("File already exists.");
            return;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            Header header = new Header(0, 1);
            header.write(raf);
        }
        System.out.println("Index file created: " + filename);
    }

    private static void insert(String[] args) throws IOException { //parse key/value
        if (args.length < 4) {
            System.err.println("Needs 'project3 insert <filename> <key> <value>'");
            return;
        }

        String filename = args[1];
        long key = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        try (RandomAccessFile raf = new RandomAccessFile(filename, "rw")) {
            Header header = Header.read(raf);

            if (header.rootId == 0) { //if tree empty, create root
                long rootId = header.nextBlockId++;
                Node root = new Node();
                root.blockId = rootId;
                root.numKeys = 1;
                root.keys[0] = key;
                root.values[0] = value;
                root.write(raf, rootId);
                header.rootId = rootId;
                header.write(raf);
                System.out.println("Inserted into new root node.");
            } else { //if root is full, splits and promotes middle key
                Node root = readNode(raf, header.rootId);
                if (root.numKeys == 19) {
                    long newRootId = header.nextBlockId++;
                    Node newRoot = new Node();
                    newRoot.blockId = newRootId;
                    newRoot.children[0] = root.blockId;
                    splitChild(raf, header, newRoot, 0, root);
                    insertNonFull(raf, header, newRoot, key, value);
                    header.rootId = newRootId;
                    header.write(raf);
                } else { //if not full, insert
                    insertNonFull(raf, header, root, key, value);
                }
            }
        }
    }

    private static void insertNonFull(RandomAccessFile raf, Header header, Node node, long key, long value) throws IOException {
        int i = node.numKeys - 1;

        if (node.children[0] == 0) { //if leaf node, inserts in sorted order
            while (i >= 0 && key < node.keys[i]) {
                node.keys[i + 1] = node.keys[i];
                node.values[i + 1] = node.values[i];
                i--;
            }
            node.keys[i + 1] = key;
            node.values[i + 1] = value;
            node.numKeys++;
            node.write(raf, node.blockId);
        } else { //if not leaf, goes to correct child, splits it if full, recalculate child index and inserts
            while (i >= 0 && key < node.keys[i]) i--;
            i++;
            Node child = readNode(raf, node.children[i]);
            if (child.numKeys == 19) {
                splitChild(raf, header, node, i, child);
                if (key > node.keys[i]) i++;
            }
            child = readNode(raf, node.children[i]);
            insertNonFull(raf, header, child, key, value);
        }
    }

    private static void splitChild(RandomAccessFile raf, Header header, Node parent, int index, Node fullChild) throws IOException { //create new node with half of keys
        Node newChild = new Node();
        newChild.blockId = header.nextBlockId++;
        newChild.parentId = parent.blockId;
        newChild.numKeys = 9;

        for (int j = 0; j < 9; j++) {
            newChild.keys[j] = fullChild.keys[j + 10];
            newChild.values[j] = fullChild.values[j + 10];
        }

        if (fullChild.children[0] != 0) {
            for (int j = 0; j < 10; j++) {
                newChild.children[j] = fullChild.children[j + 10];
            }
        }

        fullChild.numKeys = 9;

        for (int j = parent.numKeys; j > index; j--) {
            parent.children[j + 1] = parent.children[j];
            parent.keys[j] = parent.keys[j - 1];
            parent.values[j] = parent.values[j - 1];
        }

        parent.children[index + 1] = newChild.blockId; //promote middle key to parent
        parent.keys[index] = fullChild.keys[9];
        parent.values[index] = fullChild.values[9];
        parent.numKeys++;

        fullChild.write(raf, fullChild.blockId); //update file
        newChild.write(raf, newChild.blockId);
        parent.write(raf, parent.blockId);
    }

    private static void print(String[] args) throws IOException { //open file and print all key/value pairs
        if (args.length < 2) {
            System.err.println("Needs 'project3 print <filename>'");
            return;
        }
        String filename = args[1];
        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            Header header = Header.read(raf);
            if (header.rootId == 0) {
                System.out.println("Tree is empty.");
                return;
            }
            Node root = readNode(raf, header.rootId);
            System.out.println("Root Node:");
            for (int i = 0; i < root.numKeys; i++) {
                System.out.println("Key: " + root.keys[i] + ", Value: " + root.values[i]);
            }
        }
    }

    private static Node readNode(RandomAccessFile raf, long blockId) throws IOException { //reads a 512 byte block and extracts fields into Node
        byte[] block = new byte[BLOCK_SIZE];
        raf.seek(blockId * BLOCK_SIZE);
        raf.readFully(block);
        ByteBuffer buf = ByteBuffer.wrap(block);
        Node node = new Node();
        node.blockId = buf.getLong();
        node.parentId = buf.getLong();
        node.numKeys = (int) buf.getLong();
        for (int i = 0; i < 19; i++) node.keys[i] = buf.getLong();
        for (int i = 0; i < 19; i++) node.values[i] = buf.getLong();
        for (int i = 0; i < 20; i++) node.children[i] = buf.getLong();
        return node;
    }

    private static void search(String[] args) throws IOException { //looks up key
        if (args.length < 3) {
            System.err.println("Needs 'project3 search <filename> <key>'");
            return;
        }
        String filename = args[1];
        long targetKey = Long.parseLong(args[2]);
        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            Header header = Header.read(raf);
            if (header.rootId == 0) {
                System.out.println("Tree is empty.");
                return;
            }
            long currentBlockId = header.rootId;
            while (currentBlockId != 0) {
                Node node = readNode(raf, currentBlockId);               
                for (int i = 0; i < node.numKeys; i++) {
                    if (node.keys[i] == targetKey) {
                        System.out.println("Key: " + node.keys[i] + ", Value: " + node.values[i]);
                        return;
                    } else if (targetKey < node.keys[i]) {
                        currentBlockId = node.children[i];
                        continue;
                    }
                }
                currentBlockId = node.children[node.numKeys];
            }
            System.out.println("Key not found.");
        }
    }

    private static void load(String[] args) throws IOException { //load from csv
        if (args.length < 3) {
            System.err.println("Need 'project3 load <indexfile> <csvfile>'");
            return;
        }
        String indexFile = args[1];
        String csvFile = args[2];

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) { //reads line by line, call insert() for each entry
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    insert(new String[] {"insert", indexFile, parts[0].trim(), parts[1].trim()});
                }
            }
        }
    }

    private static void extract(String[] args) throws IOException { //write tree to csv
        if (args.length < 3) {
            System.err.println("Needs 'project3 extract <indexfile> <outputfile>'");
            return;
        }
        String indexFile = args[1];
        String outputFile = args[2];

        File file = new File(outputFile); 
        if (file.exists()) {
            System.err.println("File already exists.");
            return;
        }
        try (RandomAccessFile raf = new RandomAccessFile(indexFile, "r"); //open index file and output CSV
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            Header header = Header.read(raf);
            if (header.rootId == 0) {
                System.out.println("Tree is empty.");
                return;
            }
            extractRecursive(raf, header.rootId, writer);
        }
    }

    private static void extractRecursive(RandomAccessFile raf, long blockId, BufferedWriter writer) throws IOException { //in order traversal        
        if (blockId == 0) return;
        Node node = readNode(raf, blockId); 
        for (int i = 0; i < node.numKeys; i++) { //walks tree in sorted order (left child, key, right child)
            extractRecursive(raf, node.children[i], writer);
            writer.write(node.keys[i] + "," + node.values[i]);
            writer.newLine();
        }
        extractRecursive(raf, node.children[node.numKeys], writer);
    }
}