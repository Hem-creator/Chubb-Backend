package com.chubb.claims.service.workflow;

import com.chubb.claims.entity.ClaimStatus;
import com.chubb.claims.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static com.chubb.claims.entity.ClaimStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimTransitionValidatorTest {

    private final ClaimTransitionValidator validator = new ClaimTransitionValidator();

    private static final Object[][] VALID = {
            {NEW, UNDER_REVIEW},
            {UNDER_REVIEW, MORE_INFO_REQUIRED},
            {UNDER_REVIEW, APPROVED},
            {UNDER_REVIEW, REJECTED},
            {MORE_INFO_REQUIRED, UNDER_REVIEW},
            {APPROVED, SETTLED}
    };

    @Test
    void permitsExactlyTheSixValidTransitions() {
        int allowed = 0;
        for (ClaimStatus from : ClaimStatus.values()) {
            for (ClaimStatus to : ClaimStatus.values()) {
                boolean expected = false;
                for (Object[] pair : VALID) {
                    if (pair[0] == from && pair[1] == to) {
                        expected = true;
                    }
                }
                assertThat(validator.canTransition(from, to))
                        .as("%s -> %s", from, to).isEqualTo(expected);
                allowed += expected ? 1 : 0;
            }
        }
        assertThat(allowed).isEqualTo(6);
    }

    @Test
    void validateAcceptsValidTransition() {
        assertThatCode(() -> validator.validate(NEW, UNDER_REVIEW)).doesNotThrowAnyException();
    }

    @Test
    void validateThrowsBusinessExceptionForInvalidTransition() {
        assertThatThrownBy(() -> validator.validate(NEW, APPROVED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("NEW -> APPROVED");
    }

    @Test
    void finalStatesHaveNoTransitions() {
        assertThat(validator.isFinal(REJECTED)).isTrue();
        assertThat(validator.isFinal(SETTLED)).isTrue();
        assertThat(validator.isFinal(NEW)).isFalse();
        assertThat(validator.allowedTransitions(UNDER_REVIEW))
                .containsExactlyInAnyOrder(MORE_INFO_REQUIRED, APPROVED, REJECTED);
    }
}
