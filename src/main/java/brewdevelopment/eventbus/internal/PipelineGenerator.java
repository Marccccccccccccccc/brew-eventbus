package brewdevelopment.eventbus.internal;

import brewdevelopment.eventbus.Configuration;
import brewdevelopment.eventbus.RegisteredListener;
import brewdevelopment.eventbus.asm.ASMClassUtils;
import brewdevelopment.eventbus.asm.ClassDefiner;
import brewdevelopment.eventbus.event.CancellableEvent;
import brewdevelopment.eventbus.event.Event;
import brewdevelopment.eventbus.event.stats.EventStats;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class PipelineGenerator implements Opcodes {

    private static final AtomicLong ID_GEN = new AtomicLong();

    private static final PipeLine EMPTY_PIPELINE = event -> {};

    public static PipeLine generate(
            Class<? extends Event> eventClass,
            List<RegisteredListener<?>> listeners,
            ErrorCallBack errorCallBack,
            EventStats eventStats,
            Configuration config
    ) {
        if (listeners.isEmpty()) return EMPTY_PIPELINE;

        String className = config.generatedClassNameSupplier().get() + "_" + ID_GEN.incrementAndGet();
        ClassNode node = new ClassNode();
        node.visit(V11, ACC_PUBLIC | ACC_FINAL, className, null, "java/lang/Object", new String[]{Type.getInternalName(PipeLine.class)});

        for (int i = 0; i < listeners.size(); i++) {
            node.visitField(ACC_PRIVATE | ACC_FINAL, "listener_" + i, Type.getDescriptor(WrappedEventCaller.class), null, null);
        }
        if (config.enableErrorCallbacks()) {
            node.visitField(ACC_PRIVATE | ACC_FINAL, "errorCallBack", Type.getDescriptor(ErrorCallBack.class), null, null);
        }
        node.visitField(ACC_PRIVATE | ACC_FINAL, "eventStats", Type.getDescriptor(EventStats.class), null, null);

        MethodVisitor mv = node.visitMethod(ACC_PUBLIC, "<init>", "(" + Type.getDescriptor(ErrorCallBack.class) + Type.getDescriptor(EventStats.class) + Type.getDescriptor(List.class) + ")V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        if (config.enableErrorCallbacks()) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, className, "errorCallBack", Type.getDescriptor(ErrorCallBack.class));
        }

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitFieldInsn(PUTFIELD, className, "eventStats", Type.getDescriptor(EventStats.class));

        for (int i = 0; i < listeners.size(); i++) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitLdcInsn(i);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
            mv.visitFieldInsn(PUTFIELD, className, "listener_" + i, Type.getDescriptor(WrappedEventCaller.class));
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = node.visitMethod(ACC_PUBLIC, "execute", "(" + Type.getDescriptor(Event.class) + ")V", null, null);
        mv.visitCode();

        if (config.recordStats()) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
            mv.visitVarInsn(LSTORE, 2);
        }

        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchLabel = new Label();
        mv.visitTryCatchBlock(tryStart, tryEnd, catchLabel, "java/lang/Throwable");
        mv.visitLabel(tryStart);

        boolean isCancellable = CancellableEvent.class.isAssignableFrom(eventClass);

        for (int i = 0; i < listeners.size(); i++) {
            if (isCancellable) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(CancellableEvent.class), "isCancelled", "()Z", true);
                Label notCancelled = new Label();
                mv.visitJumpInsn(IFEQ, notCancelled);
                mv.visitJumpInsn(GOTO, tryEnd);
                mv.visitLabel(notCancelled);
            }

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "listener_" + i, Type.getDescriptor(WrappedEventCaller.class));
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(WrappedEventCaller.class), "call", "(" + Type.getDescriptor(Event.class) + ")V", true);
        }

        mv.visitLabel(tryEnd);
        Label finallyLabel = new Label();
        mv.visitJumpInsn(GOTO, finallyLabel);

        mv.visitLabel(catchLabel);
        mv.visitVarInsn(ASTORE, 4);
        if (config.enableErrorCallbacks()) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "errorCallBack", Type.getDescriptor(ErrorCallBack.class));
            mv.visitVarInsn(ALOAD, 4);
            mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ErrorCallBack.class), "onError", "(Ljava/lang/Throwable;)V", true);
        }

        mv.visitLabel(finallyLabel);
        if (config.recordStats()) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "eventStats", Type.getDescriptor(EventStats.class));
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
            mv.visitVarInsn(LLOAD, 2);
            mv.visitInsn(LSUB);
            mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(EventStats.class), "record", "(J)V", false);
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = node.visitMethod(ACC_PUBLIC, "fillFields", "(" + Type.getDescriptor(ErrorCallBack.class) + Type.getDescriptor(List.class) + ")V", null, null);
        mv.visitCode();
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        try {
            byte[] bytes = ASMClassUtils.toBytes(node);
            Class<?> clazz = ClassDefiner.define(PipeLine.class.getClassLoader(), className.replace('/', '.'), bytes);
            return (PipeLine) clazz.getDeclaredConstructors()[0].newInstance(errorCallBack, eventStats, listeners.stream().map(l -> (WrappedEventCaller) l.listener()).toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate pipeline for " + eventClass.getName(), e);
        }
    }
}
