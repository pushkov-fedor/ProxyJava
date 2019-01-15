package v2;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface EntityReadable {
    public void read(SelectionKey key) throws IOException;
}
