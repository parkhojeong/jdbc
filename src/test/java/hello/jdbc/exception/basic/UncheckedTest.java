package hello.jdbc.exception.basic;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class UncheckedTest {

    @Test
    void unchecked_check() {
        Service service = new Service();
        service.callCatch();
    }

    @Test
    void unchecked_throw() {
        Service service = new Service();
        Assertions.assertThatThrownBy(() -> service.call())
                .isInstanceOf(MyUncheckedException.class);

    }


    /**
     * unchecked Exception(extends RuntimeException)
     */
    static class MyUncheckedException extends RuntimeException {
        public MyUncheckedException(String message) {
            super(message);
        }
    }

    static class Service {
        Repository repository = new Repository();
        public void call() {
            repository.call();
        }

        public void callCatch() {
            try {
                repository.call();
            } catch (MyUncheckedException e) {
                log.info("catch, message={}", e.getMessage(), e);
            }
        }
    }

    static class Repository {
        public void call() {
            throw new MyUncheckedException("ex");
        }
    }
}
