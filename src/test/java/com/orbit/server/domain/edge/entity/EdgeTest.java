package com.orbit.server.domain.edge.entity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orbit.server.global.error.InvalidValueException;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EdgeTest {

    private static final String ID = "edge-seoul";
    private static final String NAME = "서울";
    private static final URI BASE_URL = URI.create("http://192.168.100.102:30081");
    private static final Coordinates SEOUL = new Coordinates(37.5665, 126.9780);

    @Test
    @DisplayName("id·이름·주소·좌표가 있으면 생성한다")
    void createsWithAllValues() {
        assertThatCode(() -> new Edge(ID, NAME, BASE_URL, SEOUL)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("id 가 비어 있으면 거부한다")
    void rejectsBlankId(String id) {
        assertThatThrownBy(() -> new Edge(id, NAME, BASE_URL, SEOUL)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("id 가 배정 Edge 컬럼 길이(50자)를 넘으면 거부한다")
    void rejectsTooLongId() {
        String id = "e".repeat(51);

        assertThatThrownBy(() -> new Edge(id, NAME, BASE_URL, SEOUL)).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("이름이 비어 있으면 거부한다")
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> new Edge(ID, name, BASE_URL, SEOUL)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("주소가 없으면 거부한다")
    void rejectsMissingBaseUrl() {
        assertThatThrownBy(() -> new Edge(ID, NAME, null, SEOUL)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("좌표가 없으면 거부한다")
    void rejectsMissingCoordinates() {
        assertThatThrownBy(() -> new Edge(ID, NAME, BASE_URL, null)).isInstanceOf(InvalidValueException.class);
    }
}
