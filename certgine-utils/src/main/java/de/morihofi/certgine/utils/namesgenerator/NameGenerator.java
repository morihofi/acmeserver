/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.namesgenerator;

public class NameGenerator {
    private static final String[] ADJECTIVES = {
            "fluffy", "sly", "gentle", "rowdy", "cozy", "proud", "shy", "zany", "wild", "loyal",
            "random", "cute", "fiery", "gloomy", "sparkly", "velvet", "chaotic", "neon", "silent",
            "ancient", "luminous", "quirky", "steampunk", "frosty", "sunny", "radiant", "glitchy",
            "mystic", "bouncy", "sapphire", "crimson", "solar", "lunar", "quantum", "void", "dapper",
            "feisty", "ghostly", "jubilant", "kaleido", "mellow", "nocturnal", "ominous", "prismatic",
            "rascal", "twilight", "umbral", "vibrant", "whimsy", "zen", "amber", "blazing", "cosmic",
            "dusky", "electric", "fuzzy", "gilded", "hazy", "icy", "jagged", "keen", "limber", "muddy",
            "noble", "opalescent", "plush", "quick", "rusty", "serene", "tangled", "ultra", "vivid",
            "wispy", "xenial", "yearning", "zealous", "arcane", "brass", "cobalt", "daring", "emerald",
            "frozen", "golden", "hidden", "inferno", "jolly", "kingly", "lucky", "magma", "nebulous",
            "onyx", "phantom", "quantum", "royal", "scarlet", "tidal", "urban", "volcanic", "wandering"
    };

    private static final String[] SPECIES = {
            "wolf", "fox", "lynx", "otter", "dragon", "cat", "dog", "raccoon", "tiger", "bunny",
            "mouse", "rat", "hyena", "panda", "kobold", "griffin", "kitsune", "sergal", "shark",
            "protogen", "squirrel", "badger", "ferret", "bat", "deer", "crow", "horse", "leopard",
            "lion", "owl", "phoenix", "raptor", "raven", "redpanda", "skunk", "snowleopard", "husky",
            "cheetah", "axolotl", "bee", "mantis", "moth", "dolphin", "goat", "kangaroo", "koala",
            "lemur", "ocelot", "pangolin", "panther", "parrot", "peacock", "penguin", "rhino", "salamander",
            "sheep", "sloth", "tapir", "unicorn", "werewolf", "wyvern", "yeti", "zebra", "gryphon",
            "jackal", "kraken", "naga", "quokka", "tanuki", "velociraptor", "wolverine", "alpaca",
            "chameleon", "gecko", "iguana", "jaguar", "mandrill", "meerkat", "okapi", "possum", "serval",
            "vulture", "wombat", "armadillo", "capybara", "chinchilla", "coyote", "fennec", "gazelle",
            "hedgehog", "quetzal", "reindeer", "sable", "toucan", "walrus"
    };

    public static String generateFurryName() {
        String adjective = ADJECTIVES[(int)(Math.random() * ADJECTIVES.length)];
        String species = SPECIES[(int)(Math.random() * SPECIES.length)];
        return adjective + "_" + species;
    }

    public static void main(String[] args) {
        System.out.println("Your furry name: " + generateFurryName());
    }
}