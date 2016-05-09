package org.exoplatform.download;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:obouras@exoplatform.com">Omar Bouras</a>
 * @version ${Revision}
 * @date 06/05/16
 */
public class NodeDownloadResource extends DownloadResource {
    private Node node_;

    private static final Log LOG = ExoLogger.getExoLogger(NodeDownloadResource.class);

    public NodeDownloadResource(Node node, String resourceMimeType) {
        this(null, node, resourceMimeType);
    }

    public NodeDownloadResource(String downloadType, Node node, String resourceMimeType) {
        super(downloadType, resourceMimeType);
        node_ = node;

    }

    @Override
    public InputStream getInputStream() throws IOException {
        Property property = null;
        InputStream is = null;
        try {
            property = (Property) node_.getProperty("jcr:data");
            if ( property != null){
                is = property.getStream();
                return is;
            }
        }
        catch( RepositoryException e){
            LOG.warn("Failed to get jcr:data from node");
        }
        finally {
            if (is != null){
                try{
                    is.close();
                }
                catch (IOException e){
                    LOG.warn("failed to close inputstream "+e.getMessage());
                }

            }
        }
        return null;

    }
}
