# **Development Log - Project 3**

# 4/18/2025 11:00 AM
- Created this development log to track changes and document progress.
- Initial setup for the project, deciding on the overall structure and goals.
- Trying to understand the project requirements and expected functionalities.
- Going with Java 
- First step: implement the create command. I’ll write a Header class to handle the first 512-byte block, storing the magic number, root ID, and next block ID. 
- Java's long type will be used for keys/values, since unsigned integers are a pain and the spec allows signed longs in Java.