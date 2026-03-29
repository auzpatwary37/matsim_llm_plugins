package rag;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

public class VectorDBImplementTest {

    @Test
    void normalizeEmbeddingBaseUrl_stripsTrailingEmbeddings() throws Exception {
        Method method = VectorDBImplement.class.getDeclaredMethod("normalizeEmbeddingBaseUrl", String.class);
        method.setAccessible(true);

        VectorDBImplement instance = allocateWithoutConstructor();

        String result = (String) method.invoke(instance, "http://localhost:1234/v1/embeddings");
        assertEquals("http://localhost:1234/v1", result);
    }

    @Test
    void normalizeEmbeddingBaseUrl_keepsNormalBaseUrl() throws Exception {
        Method method = VectorDBImplement.class.getDeclaredMethod("normalizeEmbeddingBaseUrl", String.class);
        method.setAccessible(true);

        VectorDBImplement instance = allocateWithoutConstructor();

        String result = (String) method.invoke(instance, "http://localhost:1234/v1");
        assertEquals("http://localhost:1234/v1", result);
    }

    @Test
    void buildStaticId_sanitizesFileName() throws Exception {
        Method method = VectorDBImplement.class.getDeclaredMethod("buildStaticId", String.class, int.class);
        method.setAccessible(true);

        VectorDBImplement instance = allocateWithoutConstructor();

        String result = (String) method.invoke(instance, "my file@name!.txt", 7);
        assertEquals("static_my_file_name_.txt_7", result);
    }

    @SuppressWarnings("unchecked")
    @Test
    void chunkText_splitsLongTextWithOverlap() throws Exception {
        Method method = VectorDBImplement.class.getDeclaredMethod("chunkText", String.class, int.class, int.class);
        method.setAccessible(true);

        VectorDBImplement instance = allocateWithoutConstructor();

        String text = "abcdefghijklmnopqrstuvwxyz";
        List<String> chunks = (List<String>) method.invoke(instance, text, 10, 2);

        assertNotNull(chunks);
        assertTrue(chunks.size() >= 3);

        assertEquals("abcdefghij", chunks.get(0));
        assertEquals("ijklmnopqr", chunks.get(1));
        assertEquals("qrstuvwxyz", chunks.get(2));
    }

    @SuppressWarnings("unchecked")
    @Test
    void chunkText_returnsEmptyListForBlankInput() throws Exception {
        Method method = VectorDBImplement.class.getDeclaredMethod("chunkText", String.class, int.class, int.class);
        method.setAccessible(true);

        VectorDBImplement instance = allocateWithoutConstructor();

        List<String> chunks = (List<String>) method.invoke(instance, "   ", 100, 10);
        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void chunkText_rejectsInvalidOverlap() throws Exception {
        Method method = VectorDBImplement.class.getDeclaredMethod("chunkText", String.class, int.class, int.class);
        method.setAccessible(true);

        VectorDBImplement instance = allocateWithoutConstructor();

        Exception ex = assertThrows(Exception.class, () ->
                method.invoke(instance, "hello world", 10, 10)
        );

        assertNotNull(ex);
    }

    /**
     * Creates an instance without running the real constructor.
     * This avoids live initialization during unit tests.
     */
    private static VectorDBImplement allocateWithoutConstructor() throws Exception {
        java.lang.reflect.Field theUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafeField.get(null);
        return (VectorDBImplement) unsafe.allocateInstance(VectorDBImplement.class);
    }
}