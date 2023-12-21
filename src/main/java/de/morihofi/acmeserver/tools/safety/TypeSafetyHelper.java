package de.morihofi.acmeserver.tools.safety;

import java.util.ArrayList;
import java.util.List;

public class TypeSafetyHelper {
    public static <T extends Object> List<T> safeCastToClassOfType(List listToCast, Class<T> targetClass) {
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
