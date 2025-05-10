import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Project3 {
    static final int BLOCK_SIZE = 512;
    static final String MAGIC = "4348PRJ3";

    static class Header {
        long rootId;
        long nextBlockId;

        static Header read(RandomAccessFile raf) throws IOException {
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

        void write(RandomAccessFile raf) throws IOException {
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
        int numKeys = 0;
        long[] keys = new long[19];
        long[] values = new long[19];
        long[] children = new long[20];

        void write(RandomAccessFile raf, long blockId) throws IOException {
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
            default -> System.err.println("Unknown command: " + command);
        }
    }

    private static void createFile(String[] args) throws IOException {
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

    private static void insert(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("Needs 'project3 insert <filename> <key> <value>'");
            return;
        }

        String filename = args[1];
        long key = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        try (RandomAccessFile raf = new RandomAccessFile(filename, "rw")) {
            Header header = Header.read(raf);

            if (header.rootId == 0) {
                // Tree is empty, create root node
                long rootId = header.nextBlockId;
                Node node = new Node();
                node.blockId = rootId;
                node.numKeys = 1;
                node.keys[0] = key;
                node.values[0] = value;

                node.write(raf, rootId);

                header.rootId = rootId;
                header.nextBlockId++;
                header.write(raf);

                System.out.println("Inserted into new root node.");
            } else {
                System.out.println("Insert into existing tree not yet implemented.");
                // Later: read root node, insert recursively
            }
        }
    }
    private static void print(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: project3 print <filename>");
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
    private static Node readNode(RandomAccessFile raf, long blockId) throws IOException {
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
}