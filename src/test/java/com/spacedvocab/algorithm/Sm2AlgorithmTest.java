package com.spacedvocab.algorithm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * SM-2 against business-spec §5. Each row: state before (EF, repetitions, interval), quality,
 * then the expected state after. Values were worked out by hand from the formula.
 */
class Sm2AlgorithmTest {

    private final Sm2Algorithm algorithm = new Sm2Algorithm();

    @ParameterizedTest(name = "[{index}] EF={0} rep={1} I={2} q={3} → EF={4} rep={5} I={6}")
    @CsvSource(textBlock = """
            # first review of a new card: interval 1, EF changes with the quality
            2.5,  0, 0,  5,   2.6,  1, 1
            2.5,  0, 0,  4,   2.5,  1, 1
            2.5,  0, 0,  3,   2.36, 1, 1
            # second successful review: always 6 days
            2.5,  1, 1,  4,   2.5,  2, 6
            # from the third on: round(interval × old EF)
            2.5,  2, 6,  4,   2.5,  3, 15
            2.36, 2, 6,  5,   2.46, 3, 14
            2.5,  3, 15, 3,   2.36, 4, 38
            # forgotten (q < 3): back to the start, interval 1, EF unchanged
            2.5,  3, 15, 1,   2.5,  0, 1
            1.8,  5, 40, 2,   1.8,  0, 1
            2.5,  0, 0,  0,   2.5,  0, 1
            # R5: EF never drops below 1.3
            1.3,  2, 6,  3,   1.3,  3, 8
            1.35, 1, 1,  3,   1.3,  2, 6
            """)
    void followsTheSpec(double ef, int repetitions, int interval, int quality,
                        double expectedEf, int expectedRepetitions, int expectedInterval) {
        SchedulingState before = new SchedulingState(EasinessFactor.of(ef), repetitions, interval);

        SchedulingState after = algorithm.next(before, Quality.of(quality));

        assertThat(after.easinessFactor().value()).isCloseTo(expectedEf, within(1e-9));
        assertThat(after.repetitions()).isEqualTo(expectedRepetitions);
        assertThat(after.intervalDays()).isEqualTo(expectedInterval);
    }

    @Test
    void intervalIsAlwaysAtLeastOneDay() {
        for (int q = Quality.MIN; q <= Quality.MAX; q++) {
            assertThat(algorithm.next(SchedulingState.INITIAL, Quality.of(q)).intervalDays()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void rememberingOverAndOverMakesTheIntervalGrow() {
        SchedulingState state = SchedulingState.INITIAL;
        int previous = 0;
        for (int i = 0; i < 6; i++) {
            state = algorithm.next(state, Quality.of(4));
            assertThat(state.intervalDays()).isGreaterThan(previous);
            previous = state.intervalDays();
        }
        // 1, 6, 15, 38, 95, 238 days with EF staying at 2.5
        assertThat(state.intervalDays()).isEqualTo(238);
    }
}
