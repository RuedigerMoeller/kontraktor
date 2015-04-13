package kontraktor;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.remoting.messagestore.HeapMessageStore;
import org.nustaq.kontraktor.util.Hoarde;
import org.nustaq.offheap.bytez.ByteSource;
import org.nustaq.offheap.bytez.bytesource.AsciiStringByteSource;

/**
 * Created by ruedi on 13/04/15.
 */
public class MessageStoreTest {

    @Test
    public void testMS() {
        HeapMessageStore mst = new HeapMessageStore(100);

        for ( int i = 1; i < 233; i++ ) {
            mst.putMessage("Test", i, new AsciiStringByteSource(""+i));
            ByteSource test = mst.getMessage("Test", i);
            Assert.assertTrue(test.toString().equals(""+i));
        }

        Assert.assertTrue(mst.getMessage("Test", 199).toString().equals("199"));

        for ( int i = 1; i < 233; i++ ) {
            mst.putMessage("Test", i, new AsciiStringByteSource(""+i));
            mst.confirmMessage("Test", i);
            Assert.assertTrue(mst.getMessage("Test", i) == null);
        }

    }
}
