# Coverage

The current api stubs cover most of the commonly used and easy `java.base` functions. 
See [Bytecode Manipulation](./Bytecode%20Manipulation.md) for more details on how.

Some stubs should be fairly easy to add while others are more difficult.

Some functions that access internals may not work on non OpenJDK java implementations.

There are multiple API areas that aren't currently present for various reasons, 
mostly due to their difficulty to reproduce; due to either size or complexity.
reproducing an api requires implementing it myself due to the difference in license; 
You can't just copy everything from the existing implementation.

for example:

* [FFM/FFI](https://docs.oracle.com/en/java/javase/21/core/foreign-function-and-memory-api.html)
  * it's big
  * it uses natives
  * would probably need to be on top of [JNA](https://github.com/java-native-access/jna) to implement easiest
* [ClassFile](https://docs.oracle.com/en/java/javase/22/vm/class-file-api.html)
  * it's big
* [Unix Sockets](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/UnixDomainSocketAddress.html)
  * hard to add to the socket creation function
  * hard to actually implement without natives

