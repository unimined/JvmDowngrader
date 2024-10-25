package xyz.wagyourtail.downgradetest;

import org.objectweb.asm.Opcodes;
import xyz.wagyourtail.asm.annotations.MethodASM;
import xyz.wagyourtail.asm.annotations.md.InsnNode;
import xyz.wagyourtail.asm.annotations.md.insn.FieldInsn;
import xyz.wagyourtail.asm.annotations.md.insn.InvokeDynamicInsn;
import xyz.wagyourtail.asm.annotations.md.insn.MethodInsn;
import xyz.wagyourtail.asm.annotations.md.insn.TypeInsn;
import xyz.wagyourtail.asm.annotations.md.shared.ConstantDynamic;
import xyz.wagyourtail.asm.annotations.md.shared.Handle;
import xyz.wagyourtail.asm.annotations.ref.ClassRef;
import xyz.wagyourtail.asm.annotations.ref.FieldRef;
import xyz.wagyourtail.asm.annotations.ref.MethodRef;

import java.io.PrintStream;
import java.lang.constant.ClassDesc;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class TestCondy {

    public static void main(String[] args) {
        testExplicitCast();
        testNestedCondy();
    }

    @MethodASM(
        code = {
            @InsnNode(
                field = @FieldInsn(
                    opcode = Opcodes.GETSTATIC,
                    field = @FieldRef.Qualified(
                        owner = @ClassRef(System.class),
                        field = @FieldRef(
                            name = "out",
                            desc = @ClassRef(PrintStream.class)
                        )
                    )
                )
            ),
            @InsnNode(ldc = @InsnNode.LDCValue(
                constantDynamicValue = @ConstantDynamic(
                    name = "_",
                    desc = @ClassRef(CharSequence.class),
                    bsm = @Handle(
                        tag = Opcodes.H_INVOKESTATIC,
                        method = @MethodRef.Qualified(
                            owner = @ClassRef(ConstantBootstraps.class),
                            method = @MethodRef(
                                name = "explicitCast",
                                desc = @MethodRef.Desc(
                                    args = {
                                        @ClassRef(MethodHandles.Lookup.class),
                                        @ClassRef(String.class),
                                        @ClassRef(Class.class),
                                        @ClassRef(Object.class)
                                    },
                                    returnType = @ClassRef(Object.class)
                                )
                            )
                        )
                    ),
                    bsmArgs = {
                        @InvokeDynamicInsn.BSMArg(
                            stringValue = "test"
                        )
                    }
                )
            )),
            @InsnNode(type = @TypeInsn(
                opcode = Opcodes.CHECKCAST,
                type = @ClassRef(String.class)
            )),
            @InsnNode(method = @MethodInsn(
                opcode = Opcodes.INVOKEVIRTUAL,
                method = @MethodRef.Qualified(
                    owner = @ClassRef(PrintStream.class),
                    method = @MethodRef(
                        name = "println",
                        desc = @MethodRef.Desc(
                            args = {
                                @ClassRef(String.class)
                            },
                            returnType = @ClassRef(void.class)
                        )
                    )
                ),
                isInterface = false
            )),
            @InsnNode(opcode = Opcodes.RETURN)
        }
    )
    public static void testExplicitCast() {
        throw new AssertionError();
    }

    @MethodASM(
        code = {
            @InsnNode(
                field = @FieldInsn(
                    opcode = Opcodes.GETSTATIC,
                    field = @FieldRef.Qualified(
                        owner = @ClassRef(System.class),
                        field = @FieldRef(
                            name = "out",
                            desc = @ClassRef(PrintStream.class)
                        )
                    )
                )
            ),
            @InsnNode(
                ldc = @InsnNode.LDCValue(
                    constantDynamicValue = @ConstantDynamic(
                        name = "_",
                        desc = @ClassRef(Enum.EnumDesc.class),
                        bsm = @Handle(
                            tag = Opcodes.H_INVOKESTATIC,
                            method = @MethodRef.Qualified(
                                owner = @ClassRef(ConstantBootstraps.class),
                                method = @MethodRef(
                                    name = "invoke",
                                    desc = @MethodRef.Desc(
                                        args = {
                                            @ClassRef(MethodHandles.Lookup.class),
                                            @ClassRef(String.class),
                                            @ClassRef(Class.class),
                                            @ClassRef(MethodHandle.class),
                                            @ClassRef(Object[].class)
                                        },
                                        returnType = @ClassRef(Object.class)
                                    )
                                )
                            )
                        ),
                        bsmArgs = {
                            @InvokeDynamicInsn.BSMArg(
                                handle = @Handle(
                                    tag = Opcodes.H_INVOKESTATIC,
                                    method = @MethodRef.Qualified(
                                        owner = @ClassRef(Enum.EnumDesc.class),
                                        method = @MethodRef(
                                            name = "of",
                                            desc = @MethodRef.Desc(
                                                returnType = @ClassRef(Enum.EnumDesc.class),
                                                args = {
                                                    @ClassRef(ClassDesc.class),
                                                    @ClassRef(String.class)
                                                }
                                            )
                                        )
                                    )
                                )
                            ),
                            @InvokeDynamicInsn.BSMArg(
                                constantDynamic = @ConstantDynamic(
                                    name = "_",
                                    desc = @ClassRef(ClassDesc.class),
                                    bsm = @Handle(
                                        tag = Opcodes.H_INVOKESTATIC,
                                        method = @MethodRef.Qualified(
                                            owner = @ClassRef(ConstantBootstraps.class),
                                            method = @MethodRef(
                                                name = "invoke",
                                                desc = @MethodRef.Desc(
                                                    args = {
                                                        @ClassRef(MethodHandles.Lookup.class),
                                                        @ClassRef(String.class),
                                                        @ClassRef(Class.class),
                                                        @ClassRef(MethodHandle.class),
                                                        @ClassRef(Object[].class)
                                                    },
                                                    returnType = @ClassRef(Object.class)
                                                )
                                            )
                                        )
                                    ),
                                    bsmArgs = {
                                        @InvokeDynamicInsn.BSMArg(
                                            handle = @Handle(
                                                tag = Opcodes.H_INVOKESTATIC,
                                                method = @MethodRef.Qualified(
                                                    owner = @ClassRef(ClassDesc.class),
                                                    method = @MethodRef(
                                                        name = "of",
                                                        desc = @MethodRef.Desc(
                                                            returnType = @ClassRef(ClassDesc.class),
                                                            args = {
                                                                @ClassRef(String.class),
                                                            }
                                                        )
                                                    )
                                                ),
                                                isInterface = true
                                            )
                                        ),
                                        @InvokeDynamicInsn.BSMArg(
                                            stringValue = "xyz.wagyourtail.downgradetest.TestCondy$Type"
                                        )
                                    }
                                )
                            ),
                            @InvokeDynamicInsn.BSMArg(
                                stringValue = "VALUE_A"
                            )
                        }
                    )
                )
            ),
            @InsnNode(method = @MethodInsn(
                opcode = Opcodes.INVOKEVIRTUAL,
                method = @MethodRef.Qualified(
                    owner = @ClassRef(PrintStream.class),
                    method = @MethodRef(
                        name = "println",
                        desc = @MethodRef.Desc(
                            args = {
                                @ClassRef(Object.class)
                            },
                            returnType = @ClassRef(void.class)
                        )
                    )
                ),
                isInterface = false
            )),
            @InsnNode(opcode = Opcodes.RETURN)
        }
    )
    public static void testNestedCondy() {
        throw new AssertionError();
    }

    private enum Type {
        VALUE_A,
        VALUE_B
    }

}
