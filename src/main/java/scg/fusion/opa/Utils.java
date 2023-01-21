package scg.fusion.opa;

import scg.fusion.opa.annotation.PolicyPackage;

import static java.util.Objects.isNull;

final class Utils {

    private Utils() {
        super();
    }

    static String getPolicyPackage(Class<?> inputType) {

        if (inputType.isAnnotationPresent(PolicyPackage.class)) {
            return inputType.getAnnotation(PolicyPackage.class).value();
        }

        Package inputPackage = inputType.getPackage();

        return isNull(inputPackage) ? (null) : inputPackage.getName();

    }

}
