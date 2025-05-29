package de.morihofi.acmeserver.utils.collectors;

import static org.junit.jupiter.api.Assertions.*;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SingletonCollectorTest {

    @Test
    void testSingletonCollectorWithOneElement() {
        String result = Stream.of("only").collect(SingletonCollector.toSingleton());
        assertEquals("only", result);
    }

    @Test
    void testSingletonCollectorWithNoElements() {
        Stream<String> stream = Stream.empty();
        assertThrows(IllegalStateException.class, () -> stream.collect(SingletonCollector.toSingleton()));
    }

    @Test
    void testSingletonCollectorWithMultipleElements() {
        Stream<String> stream = Stream.of("first", "second");
        assertThrows(IllegalStateException.class, () -> stream.collect(SingletonCollector.toSingleton()));
    }


    @Test
    void testCharacteristicsIsEmpty() {
        var characteristics = SingletonCollector.<String>toSingleton().characteristics();
        assertTrue(characteristics.isEmpty());
    }

    @Test
    void testCombinerThrowsExceptionWithMultiplePartialResults() {
        List<String> left = List.of("left");
        List<String> right = List.of("right");

        Collector<String, ?, String> collector = SingletonCollector.toSingleton();
        BinaryOperator<List<String>> combiner = (BinaryOperator<List<String>>) collector.combiner();

        assertThrows(IllegalStateException.class, () -> combiner.apply(left, right));
    }

    @Test
    void testFinisherThrowsOnEmptyList() {
        Collector<String, ?, String> collector = SingletonCollector.toSingleton();
        Function<List<String>, String> finisher = (Function<List<String>, String>) collector.finisher();

        assertThrows(IllegalStateException.class, () -> finisher.apply(List.of()));
    }

    @Test
    void testFinisherThrowsOnMultipleElements() {
        Collector<String, ?, String> collector = SingletonCollector.toSingleton();
        Function<List<String>, String> finisher = (Function<List<String>, String>) collector.finisher();

        assertThrows(IllegalStateException.class, () -> finisher.apply(List.of("a", "b")));
    }

    @Test
    void testAccumulatorAllowsOnlyOneElement() {
        Collector<String, ?, String> collector = SingletonCollector.toSingleton();
        BiConsumer<List<String>, String> accumulator = (BiConsumer<List<String>, String>) collector.accumulator();

        List<String> list = new ArrayList<>();
        accumulator.accept(list, "first");

        assertThrows(IllegalStateException.class, () -> accumulator.accept(list, "second"));
    }

}
