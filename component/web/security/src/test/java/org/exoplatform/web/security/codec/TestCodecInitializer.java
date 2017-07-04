package org.exoplatform.web.security.codec;

import junit.framework.TestCase;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.web.security.security.TokenServiceInitializationException;

import java.util.HashMap;

/**
 * Created by Abdessattar Noissi on 22/05/17.
 */
public class TestCodecInitializer extends TestCase {

    public void testInitCodec() throws TokenServiceInitializationException {
       /* InitParams initParams = new InitParams();
        ValueParam param = new ValueParam();
        param.setName("gatein.conf.dir");
        param.setValue(System.getProperty("gatein.test.conf.dir"));
        initParams.addParameter(param);
        CodecInitializer codecInitializer = new CodecInitializer(initParams);
        assertEquals(1, 2);
        assertNotNull(codecInitializer.initCodec());
        assertFalse(codecInitializer.initCodec() instanceof AbstractCodec);
*/
    }
}
