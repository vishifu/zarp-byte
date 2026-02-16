package org.zarp.bytes.utils;

import org.zarp.bytes.annotations.FieldGroup;
import org.zarp.core.api.ZPlatform;
import org.zarp.core.common.ZClassLocal;
import org.zarp.core.memory.ZMemory;
import org.zarp.core.utils.AnnotationFinder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ByteFieldInfo {
    private static final ZClassLocal<ByteFieldInfo> CACHED = ZClassLocal.from(ByteFieldInfo::create);
    private static final ZMemory MEMORY = ZPlatform.memory();

    private static Field $END;

    static {
        try {
            $END = ByteFieldInfo.class.getDeclaredField("$END");
        } catch (NoSuchFieldException ex) {
            throw new AssertionError(ex);
        }
    }

    private final Map<String, FieldEntry> groups = new LinkedHashMap<>();
    private final Class<?> clazz;
    private final int description;

    private static ByteFieldInfo create(Class<?> clazz) {
        return new ByteFieldInfo(clazz);
    }

    public static ByteFieldInfo lookup(Class<?> clazz) {
        return CACHED.get(clazz);
    }

    ByteFieldInfo(Class<?> clazz) {
        this.clazz = clazz;
        List<Field> fields = fields(clazz);
        FieldEntry entry = null;
        String prefix0 = "";
        int longs = 0;
        int ints = 0;
        int shorts = 0;
        int bytes = 0;
        for (int i = 0; i <= fields.size(); i++) {
            Field field = i == fields.size() ? $END : fields.get(i);
            boolean matches = false;
            String prefix = "";
            long position = 0;
            int size = 0;
            if (field.getType().isPrimitive()) {
                FieldGroup fieldGroup = AnnotationFinder.findAnnotation(field, FieldGroup.class);
                if (fieldGroup != null) {
                    prefix = fieldGroup.value();
                    position = MEMORY.getFieldOffset(field);
                    matches = prefix.equals(prefix0);
                }
                size = sizeof(field.getType());
                switch (size) {
                    case 1:
                        bytes++;
                        break;
                    case 2:
                        shorts++;
                        break;
                    case 4:
                        ints++;
                        break;
                    case 8:
                        longs++;
                        break;
                    default:
                        throw new UnsupportedOperationException("Primitive type of size " + size + " is not supported");
                }
            }
            if (matches) {
                assert entry != null;
                entry.end = position + size;
            } else if (!prefix.isEmpty()) {
                if (this.groups.containsKey(prefix)) {
                    prefix0 = "";
                } else {
                    entry = new FieldEntry();
                    entry.start = position;
                    entry.end = position + size;
                    this.groups.put(prefix, entry);
                    prefix0 = prefix;
                }
            }
        }

        assert longs < 256;
        assert ints < 256;
        assert bytes < 256;
        assert shorts < 256;
        int desc = (longs << 24) | (ints << 16) | (shorts << 8) | (bytes);
        if (Integer.bitCount(desc) % 2 == 0) {
            desc |= 0x8000;
        }
        this.description = desc;
    }

    public Set<String> getGroupKeys() {
        return groups.keySet();
    }

    public int getDescription() {
        return description;
    }

    int sizeof(Class<?> type) {
        return ZMemory.sizeOf(type);
    }

    List<Field> fields(Class<?> clazz) {
        List<Field> result = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            Collections.addAll(result, clazz.getDeclaredFields());
            clazz = clazz.getSuperclass();
        }
        result.removeIf(f -> Modifier.isStatic(f.getModifiers()));
        result.sort(Comparator.comparingLong(MEMORY::getFieldOffset));
        return result;
    }

    /**
     * @param group group name
     * @return start offset (relative to object header) for group
     */
    public long startOf(String group) {
        FieldEntry entry = groups.get(group);
        if (entry == null) {
            throw new IllegalArgumentException("No group for " + group + " in class " + clazz);
        }
        return entry.start;
    }

    /**
     * @param group group name
     * @return length in bytes of group
     */
    public long lengthOf(String group) {
        FieldEntry entry = groups.get(group);
        if (entry == null) {
            throw new IllegalArgumentException("No group for " + group + " in class " + clazz);
        }
        return entry.end - entry.start;
    }

    public String dump() {
        StringBuilder sb = new StringBuilder("type: ");
        sb.append(getClass().getSimpleName())
                .append(", groups {");
        sb.append(groups.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue().start + " to " + e.getValue().end)
                .collect(Collectors.joining(",")));
        sb.append(" }");
        return sb.toString();
    }

    static class FieldEntry {
        long start;
        long end;
    }
}
