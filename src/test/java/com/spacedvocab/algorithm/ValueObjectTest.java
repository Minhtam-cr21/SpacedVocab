package com.spacedvocab.algorithm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ValueObjectTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 5})
    void qualityAcceptsZeroToFive(int value) {
        assertThat(Quality.of(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 6})
    void qualityRejectsOutOfRange(int value) {
        assertThatIllegalArgumentException().isThrownBy(() -> Quality.of(value));
    }

    @Test
    void easinessFactorHasFloorOfOnePointThree() {
        assertThat(EasinessFactor.of(1.3).value()).isEqualTo(1.3);
        assertThat(EasinessFactor.INITIAL.value()).isEqualTo(2.5);
        assertThatIllegalArgumentException().isThrownBy(() -> EasinessFactor.of(1.29));
        assertThatIllegalArgumentException().isThrownBy(() -> EasinessFactor.of(Double.NaN));
    }

    @Test
    void schedulingStateStartsAtSpecDefaultsAndRejectsNegatives() {
        assertThat(SchedulingState.INITIAL)
                .isEqualTo(new SchedulingState(EasinessFactor.INITIAL, 0, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new SchedulingState(EasinessFactor.INITIAL, -1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new SchedulingState(EasinessFactor.INITIAL, 0, -1));
        assertThatNullPointerException().isThrownBy(() -> new SchedulingState(null, 0, 0));
    }
}
