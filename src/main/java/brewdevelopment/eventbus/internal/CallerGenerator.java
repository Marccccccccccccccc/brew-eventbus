package brewdevelopment.eventbus.internal;

import brewdevelopment.eventbus.asm.ASMClassUtils;
import brewdevelopment.eventbus.asm.ClassDefiner;
import brewdevelopment.eventbus.event.Event;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicLong;

public class CallerGenerator implements Opcodes {

    private static final AtomicLong ID_GEN = new AtomicLong();

    public static WrappedEventCaller generate(Object container, Method method, Class<? extends Event> eventType) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        Class<?> ownerClass = isStatic ? (Class<?>) container : container.getClass();

        if (!Modifier.isPublic(ownerClass.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
            return event -> {
                try {
                    method.setAccessible(true); //BOOO
                    method.invoke(isStatic ? null : container, event);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke listener: " + method, e);
                }
            };
        }

        String className = "brewdevelopment/eventbus/generated/GeneratedCaller_" + method.getName() + "_" + ID_GEN.incrementAndGet();

        //probably some unnecessary steps in here...
        ClassNode node = new ClassNode();
        node.visit(V11, ACC_PUBLIC | ACC_FINAL, className, null, "java/lang/Object", new String[]{Type.getInternalName(WrappedEventCaller.class)});

        if (!isStatic) {
            node.visitField(ACC_PRIVATE | ACC_FINAL, "container", Type.getDescriptor(ownerClass), null, null);
        }

        MethodVisitor mv = node.visitMethod(ACC_PUBLIC, "<init>", isStatic ? "()V" : "(" + Type.getDescriptor(ownerClass) + ")V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, className, "container", Type.getDescriptor(ownerClass));
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = node.visitMethod(ACC_PUBLIC, "call", "(" + Type.getDescriptor(Event.class) + ")V", null, null);
        mv.visitCode();

        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "container", Type.getDescriptor(ownerClass));
        }

        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(eventType));

        if (isStatic) {
            mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ownerClass), method.getName(), Type.getMethodDescriptor(method), false);
        } else {
            int opcode = ownerClass.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL;
            mv.visitMethodInsn(opcode, Type.getInternalName(ownerClass), method.getName(), Type.getMethodDescriptor(method), ownerClass.isInterface());
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        try {
            byte[] bytes = ASMClassUtils.toBytes(node);
            Class<?> clazz = ClassDefiner.define(ownerClass.getClassLoader(), className.replace('/', '.'), bytes);
            if (isStatic) {
                return (WrappedEventCaller) clazz.getDeclaredConstructors()[0].newInstance();
            } else {
                return (WrappedEventCaller) clazz.getDeclaredConstructors()[0].newInstance(container);
            }
        } catch (Exception e) {
            return event -> {
                try {
                    method.setAccessible(true);
                    method.invoke(isStatic ? null : container, event);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            };
        }
    }
}
