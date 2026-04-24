package com.shortener.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class Base62EncoderTest {

    @Test
    void encode_sequence1_returnsSingleChar() {
        assertThat(Base62Encoder.encode(1)).isEqualTo("1");
    }

    @Test
    void encode_sequence62_returnsTwoChars() {
        assertThat(Base62Encoder.encode(62)).isEqualTo("10");
    }

    @Test
    void encode_largeNumber_returnsCorrectCode() {
        assertThat(Base62Encoder.encode(56800235584L)).isEqualTo("10000000");
    }

    @Test
    void encode_consecutiveNumbers_produceUniqueCodes() {
        String code1 = Base62Encoder.encode(1);
        String code2 = Base62Encoder.encode(2);
        String code3 = Base62Encoder.encode(3);

        assertThat(code1).isNotEqualTo(code2);
        assertThat(code2).isNotEqualTo(code3);
    }

    @Test
    void encode_zero_throwsException() {
        assertThatThrownBy(() -> Base62Encoder.encode(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
