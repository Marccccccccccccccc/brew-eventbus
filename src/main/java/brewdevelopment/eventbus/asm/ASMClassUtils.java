package brewdevelopment.eventbus.asm;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public final class ASMClassUtils {
    public static byte[] toBytes(final @NotNull ClassNode node) {
        final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        node.accept(classWriter);
        return classWriter.toByteArray();
    }
}