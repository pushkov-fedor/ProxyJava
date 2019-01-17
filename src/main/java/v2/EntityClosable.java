package v2;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface EntityClosable {

    public void close(SelectionKey key) throws IOException;
}
