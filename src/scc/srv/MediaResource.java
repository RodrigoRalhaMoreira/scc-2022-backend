package scc.srv;

import jakarta.ws.rs.*;
import scc.utils.Hash;
import java.util.ArrayList;
import java.util.List;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;

import jakarta.ws.rs.core.MediaType;

/**
 * Resource for managing media files, such as images.
 */
@Path("/media")
public class MediaResource {

    // Get connection string in the storage access keys page
    private static final String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=sccstwesteuropegroupdrt;"
            + "AccountKey=p+zGE3C0Q13lLPnZQ/sl2qCY0uLbUWBV+a7/rIGQdmeG0O3iDTzluDs0SInKASyWS5EiNPGhNZuU+ASttJVNeA==;"
            + "EndpointSuffix=core.windows.net";

    // Get container client
    private BlobContainerClient containerClient = new BlobContainerClientBuilder()
                                                .connectionString(storageConnectionString)
                                                .containerName("images")
                                                .buildClient();
    
	/**
	 * Post a new image.The id of the image is its hash.
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public String upload(byte[] contents) {
	    
	    if( contents == null) {
            System.out.println( "Use: java scc.utils.UploadToStorage filename");
        }
	    
        String filename = Hash.of(contents);
        
        try {
            
            BinaryData data = BinaryData.fromBytes(contents); // BinaryData.fromFile(java.nio.file.Path.of(filename));

            // Get client to blob
            BlobClient blob = containerClient.getBlobClient(filename);

            // Upload contents from BinaryData (check documentation for other alternatives)
            blob.upload(data);
            
            System.out.println("File updloaded : " + filename);
            
        } catch( Exception e) {
            e.printStackTrace();
        }
        
        return filename;
	}

	/**
	 * Return the contents of an image. Throw an appropriate error message if
	 * id does not exist.
	 */
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public byte[] download(@PathParam("id") String id) {

        try {

            // Get client to blob
            BlobClient blob = containerClient.getBlobClient(id);

            // Download contents to BinaryData (check documentation for other alternatives)
            BinaryData data = blob.downloadContent();
            
            System.out.println( "Blob size : " + data.toBytes().length);
            
            return data.toBytes();
            
        } catch( Exception e) {
            e.printStackTrace();
        }
        
        return null;
	}

	/**
	 * Lists the id of images stored.
	 */
	@GET
	@Path("/list")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> list() {
	    
	    List<String> list = new ArrayList<>();
        
        try {

            // Get client to blob
            PagedIterable<BlobItem> blob = containerClient.listBlobs();

            for(BlobItem item : blob)
                list.add(item.getName());
            
            return list;
            
        } catch( Exception e) {
            e.printStackTrace();
        }
        
        return null;
	} 
}

