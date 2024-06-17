package org.jobrunr.utils.annotations;

import org.jobrunr.utils.annotations.aspect.RetryAspect;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RetryTest {
    private static final int MAX_ATTEMPTS = 2;
    private static final long DELAY_MS = 1;

    private int counter = 0;

    @Retry(maxAttempts = MAX_ATTEMPTS, delayMs = DELAY_MS)
    public void successOperationOnLastRetry() {
        counter++;
        System.out.println("Attempt " + counter);

        if (counter < MAX_ATTEMPTS) {
            throw new RuntimeException("Simulated failure");
        }

        System.out.println("Operation succeeded");
    }

    @Retry(maxAttempts = MAX_ATTEMPTS, delayMs = DELAY_MS)
    public void failedOperation() {
        counter++;
        System.out.println("Attempt " + counter);
        throw new RuntimeException("Simulated failure");
    }


    @Test
    public void testRetrySuccess() throws Throwable {
        Method method = RetryTest.class.getMethod("successOperationOnLastRetry");
        RetryAspect.retry(this, method, null);
        assertEquals(MAX_ATTEMPTS, counter);
    }

    @Test
    public void testRetryFailure() throws NoSuchMethodException {
        Method method = RetryTest.class.getMethod("failedOperation");

        assertThrows(RuntimeException.class, () ->
                RetryAspect.retry(this, method, null));

        assertEquals(MAX_ATTEMPTS, counter);
    }
}