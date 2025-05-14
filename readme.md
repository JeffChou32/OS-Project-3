## B-Tree Index File – CS4348 Project 3
- B-Tree index system using Java and 512 byte file blocks
- Each node is stored as a fixed-size block, and the tree structure is maintained entirely on disk using RandomAccessFile.
- Key/value pairs are stored as 8-byte signed integers with B-Tree insertion and node splitting logic.
- A 512-byte header block stores metadata such as the root node’s block ID and the next free block ID.

### Commands:
- create <filename> – Initializes an empty index file with a header.
- insert <filename> <key> <value> – Inserts a key/value pair into the B-Tree.
- search <filename> <key> – Searches for a key and prints the value if found.
- print <filename> – Prints the keys and values 
- load <indexfile> <csvfile> – Loads key/value pairs from a CSV file into the B-Tree.
- extract <indexfile> <outputfile> – Writes all key/value pairs to a CSV file.

### Compilation
javac Project3.java

### To run:
java Project3 <command> <args>
