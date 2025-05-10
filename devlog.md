# **Development Log - Project 3**

# 4/18/2025 11:00 AM
- Created this development log to track changes and document progress.
- Initial setup for the project, deciding on the overall structure and goals.
- Trying to understand the project requirements and expected functionalities.
- Going with Java 
- First step: implement the create command. I’ll write a Header class to handle the first 512-byte block, storing the magic number, root ID, and next block ID. 
- Java's long type will be used for keys/values, since unsigned integers are a pain and the spec allows signed longs in Java.

# 4/24/2025 3:00 PM
- Start with create command, initialize new file with 512 byte header block
- magic number, root block, next block id
- randomaccessfile and bytebuffer libraries for big endian writes

# 4/24/2025 6:00 PM
- ByteBuffer handled alignment and padding. 
- Confirm that the magic number and block structure are exactly 512 bytes. 
- Next: define the Header class and add methods to load/save it from a file, since later commands will rely on keeping the header in sync.

# 5/9/2025 12:36 PM
- Create insert function
- Inserts a key/value into b-tree stored in index
- create node class
- locate root - if 0, new node

# 5/9/2025 5:00 PM
- added print function to easily print values in nodes
- system inserts and can print , validating structure 

# 5/10/2025 9:46 AM
- implement search at least
- read root node and traverses tree, comparing keys 