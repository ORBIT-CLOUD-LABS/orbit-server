package com.orbit.server.domain.edge.entity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CoordinatesTest {

    private static final double SEOUL_LATITUDE = 37.5665;
    private static final double SEOUL_LONGITUDE = 126.9780;

    @Test
    @DisplayName("위도 -90~90, 경도 -180~180 의 경계값으로 생성한다")
    void createsAtBoundaries() {
        assertThatCode(() -> {
                    new Coordinates(-90, -180);
                    new Coordinates(90, 180);
                })
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(doubles = {-90.0001, 90.0001, Double.NaN})
    @DisplayName("위도가 -90~90 을 벗어나면 거부한다")
    void rejectsLatitudeOutOfRange(double latitude) {
        assertThatThrownBy(() -> new Coordinates(latitude, SEOUL_LONGITUDE)).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-180.0001, 180.0001, Double.NaN})
    @DisplayName("경도가 -180~180 을 벗어나면 거부한다")
    void rejectsLongitudeOutOfRange(double longitude) {
        assertThatThrownBy(() -> new Coordinates(SEOUL_LATITUDE, longitude)).isInstanceOf(InvalidValueException.class);
    }
}
