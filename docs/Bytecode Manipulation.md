# Bytecode Manipulation

This document describes the way in which JvmDowngrader manipulates the bytecode of Java class files.

## Overview

JvmDowngrader starts by initializing
a [VersionProvider](/src/main/java/xyz/wagyourtail/jvmdg/version/VersionProvider.java) which is used to provide a
downgrade between 2 sequential java versions.
This process is then chained until the desired version is reached.

The VersionProvider loads "Stubs", "Adapter" and "Modify" transforms which are used in the process of downgrading the
bytecode.
The "Stubs" are simple method replacements, that replace either a method call or a field access with a call to a static
method.
The "Modify" transforms allow for more complex changes to the bytecode, such as replacing InvokeDynamic calls with a
newly constructed private
method in the target class.
The "Adapter" transform replaces all class references with the target class.

## Version Provider

Classes can be added for processing in the VersionProvider's init method by calling `stub(Class<?>)` this will parse
them for
`@Stub`, `@Modify` and `@Adapter` annotations and add them to the appropriate lists.

Additionally, the version provider allows for other wider scale transforms that need to be done in order to downgrade
the bytecode by extending the
`otherTransforms` method.

### Stubs

To create a stub, a method must be `public static` and annotated with [`@Stub`](/src/shared/java/xyz/wagyourtail/jvmdg/version/Stub.java).
the `@Stub` annotation by default will use the first argument of the method as the target class and treat it as a
instance method replacement.
If the method is static, the `@Stub` annotation must be provided with an `@Ref` argument that specifies the target
class, this will treat the method as a static method replacement.

In some rare circumstances the target class should be different from the first argument, in that case (or when any
argument is needed to have it's type changed), the argument
may be annotated with `@Coerce` which will change the type of the argument to the specified type.

Stubs have several other fields that change their behavior:

* `abstractDefault` indicates if a method was abstract in the target version but now has a default implementation,
  this is used to determine if classes extending the target class need to also insert a reference to the stub.
  references to this method will not be replaced with the stub, and are handled specially as described below.
* `requiresRuntime` indicates if the stub requires the JVMDowngrader runtime in order to work as intended,
  mostly for things like reflection and runtime class definition.
* `noSpecial` indicates that the stub should not be used for `INVOKESPECIAL` calls.
* `downgradeVersion` adds an extra argument to the stub for the original version of the class, this is useful for
  multi-version stubs, such as reflection.
* `excludeChild` prevent a stub from being applied to a child-class of the target. for example, `String#isEmpty` is
  since java 6, but `CharSequence#isEmpty` is since java 15.

### Modify

The `@Modify` annotation is used to replace a specific instruction with a new instruction or list of instructions.
The `@Modify` annotation is provided with a `@Ref` argument that specifies the target class, method and descriptor.
the method must be `public static` and have 1-4 arguments, matching these:
`MethodNode mnode, int i, ClassNode cnode, Set<ClassNode> extra`.

This is used to replace `INVOKEDYNAMIC` calls and to stub constructor calls, as `@Stub` would be unable to handle the
super call within constructors.

### Adapter

The `@Adapter` annotation is used to replace all references to a class with a different class.
By default, it replaces the `value` class with the one it's annotated on, but it can be provided with a `target` class.
there is also a `keepInterface` field, if the stub is an interface, it can optionally be completely removed from classes
referencing it.

### RequiresResource

This annotation is a hint to the Shading Api task that the class or method requires a resource from the api jar in order
to operate.
such as emoji data in the java 21 `Character` class.

### InvokeDynamic Method Insertion

If a stub does not exactly match the method descriptor of an invokedynamic method it is replacing, it may be necessary
to wrap it with another method to adapt the types.
This is done automatically while stubbing handles in InvokeDynamic arguments.

### Abstract Method Insertion

The providers also scan the classpath to determine the inheritance hierarchy of the target class.
This is then used to insert abstractDefault stubs into classes that extend the target class of those stubs that do not
already implement the stubbed method themselves, or in a super class.

### Other Transforms

The `otherTransforms` method is used to apply wider scale transforms to the bytecode, such as replacing all references
to a class with a different class.
the current list of transforms is:
* Java 25->24
  * Create static main methods to call instance main methods.
* Java 17->16
  * Remove `ACC_SEALED` and add `@PermittedSubClasses` annotation
* Java 16->15
  * Remove `ACC_RECORD` and add `@RecordComponents` annotation
* Java 15->14
  * Insert synthetic "bridge" methods for private access from handles in nestmates.
* Java 11->10
  * Add a `@NestHost` annotation to nest members
  * Add a `@NestMembers` annotation to nest hosts
  * Insert synthetic "bridge" methods so that private fields/methods can be called from nest members.
  * Replace Constant Dynamic calls with `INVOKEDYNAMIC`
* Java 9->8
  * Make private interface methods use `INVOKESPECIAL` instead of `INVOKEINTERFACE`.
* Java 8->7
  * Move private/default methods in interfaces to a `$jvmdg$StaticDefaults` inner class.
* Java 7->6
  * replace INDY method calls with some *fun* `MethodHandle` stuff.
* Java 6->5 
  * remove frames
