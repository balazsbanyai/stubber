import com.google.common.base.Splitter;
import com.google.common.eventbus.EventBus;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
    public void finalizerDoesNotThrow() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        EventBus bus = new EventBus();
        Method finalizeMethod = Object.class.getDeclaredMethod("finalize");
        finalizeMethod.setAccessible(true);

        finalizeMethod.invoke(bus);
    }

    @Test
    public void toStringDoesNotThrow() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        EventBus bus = new EventBus();
        bus.toString();
    }


}
