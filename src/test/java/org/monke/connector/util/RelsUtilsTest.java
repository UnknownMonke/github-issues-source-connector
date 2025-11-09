package org.monke.connector.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.*;

public class RelsUtilsTest {

    @Test
    void getNextPage_should_successfully_parse_rels() {
        String rels = "<https://api.github.com/repositories/123456/issues?per_page=30&page=4>; rel=\"next\", " +
            "<https://api.github.com/repositories/123456/issues?per_page=30&page=2>; rel=\"last\"";

        String nextPage = RelsUtils.getNextPage(rels);

        assertThat(nextPage).isEqualTo("https://api.github.com/repositories/123456/issues?per_page=30&page=4");
    }

    @Test
    void getNextPage_should_throw_if_null() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> RelsUtils.getNextPage(null));
    }
}
