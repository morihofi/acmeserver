package de.morihofi.acmeserver.tools.safety;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for performing type-safe casting on Raw Types.
 *
 * <p>
 * The `TypeSafetyHelper` class provides a method for safely casting raw elements in collections
 * to a specified target class type. This helps ensure type safety when working with collections of
 * heterogeneous elements.
 * </p>
 */
public class TypeSafetyHelper {

    private TypeSafetyHelper(){}

    @SuppressWarnings({"rawtypes"})
    public static <T> List<T> safeCastToClassOfType(List listToCast, Class<T> targetClass) {
        List<T> targetList = new ArrayList<>();

        for (Object obj : listToCast) {
            if (targetClass.isInstance(obj)) {
                targetList.add(targetClass.cast(obj));
            } else {
                throw new IllegalArgumentException("Object type is not of the target class. Expected " + targetClass.getName() + " but got " + obj.getClass());
            }
        }

        return targetList;
    }


}
