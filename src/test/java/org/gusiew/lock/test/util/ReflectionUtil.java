package org.gusiew.lock.test.util;

import java.lang.reflect.Field;

public class ReflectionUtil {

    public static <T> T getValue(Object o, String fieldName, Class<T> returnClazz) {
        T result = null;
        try {
            Field field = o.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            result = returnClazz.cast(field.get(o));
        } catch (IllegalAccessException e) {
            //Ignore because field set to accessible
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Field " + fieldName + " not found", e);
        }

        return result;
    }

    public static void setValue(Object o, String fieldName, Object value) {
        try {
            Field field = o.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(o, value);
        } catch (IllegalAccessException e) {
            //Ignore because field set to accessible
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Field " + fieldName + " not found", e);
        }
    }
}
