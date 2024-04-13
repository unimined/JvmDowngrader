# JvmDowngrader

downgrades modern java bytecode to older versions. at either compile and/or runtime (requires runtime for certain things
to work such as `MethodHandles$Lookup#defineClass`).

## initial relesase TODO:

- [ ] finish downgrade task for gradle plugin
- [ ] stub for MethodHandles$Lookup#defineClass

### inspired by

https://github.com/Chocohead/Not-So-New and https://github.com/luontola/retrolambda
