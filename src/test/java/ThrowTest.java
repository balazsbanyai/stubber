import com.google.common.base.Splitter;
import com.google.common.eventbus.EventBus;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ThrowTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void methodThrows() {
        expectedException.expect(RuntimeException.class);

        Splitter.on("any");
    }

    @Test
    public void constructorDoesNotThrow() {
        new EventBus();
    }

    @Test
    public void finalizerDoesNotThrow() {
        // new EventBus().finalize(); TODO
    }

}
