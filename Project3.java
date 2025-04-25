import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Project3 {
    static final int BLOCK_SIZE = 512;
    static final String MAGIC = "4348PRJ3";

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || !args[0].equals("create")) {
            System.err.println("Usage: project3 create <filename>");
            return;
        }

        String filename = args[1];
        File file = new File(filename);

        if (file.exists()) {
            System.err.println("Error: File already exists.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            byte[] block = new byte[BLOCK_SIZE];
            ByteBuffer buffer = ByteBuffer.wrap(block);

            // Write magic number (8 bytes, padded if needed)
            buffer.put(MAGIC.getBytes(StandardCharsets.US_ASCII));
            if (MAGIC.length() < 8) {
                buffer.put(new byte[8 - MAGIC.length()]);
            }

            // Root block ID (0)
            buffer.putLong(0);

            // Next block ID (1)
            buffer.putLong(1);

            // Remaining bytes are already zero-initialized

            raf.write(block); // Write the full 512-byte block
        }

        System.out.println("Index file created: " + filename);
    }
}
