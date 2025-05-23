package de.morihofi.acmeserver.utils.namesgenerator;

public class NameGenerator {
    private static final String[] ADJECTIVES = {
            "fluffy", "sly", "gentle", "rowdy", "cozy", "proud", "shy", "zany", "wild", "loyal", "random", "cute"
    };

    private static final String[] SPECIES = {
            "wolf", "fox", "lynx", "otter", "dragon", "cat", "dog", "raccoon", "tiger", "bunny", "mouse", "rat"
    };

    public static String generateFurryName() {
        String adjective = ADJECTIVES[(int)(Math.random() * ADJECTIVES.length)];
        String species = SPECIES[(int)(Math.random() * SPECIES.length)];
        return adjective + "_" + species;
    }

    public static void main(String[] args) {
        System.out.println(generateFurryName());
    }

}
