package scc.srv.resources;

import scc.utils.Hash;

import jakarta.ws.rs.*;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import jakarta.ws.rs.core.MediaType;

/**
 * Resource for managing media files, such as images.
 */
@Path("/media")
public class MediaResource {

    private static final String ERROR_MSG = "Use: java scc.utils.UploadToStorage filename";

    public MediaResource() {
    }

    /**
     * Post a new image.The id of the image is its hash.
     * 
     * @throws IOException
     * @throws FileNotFoundException
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String upload(byte[] contents) throws FileNotFoundException, IOException {

        if (contents == null)
            return ERROR_MSG;

        String filename = Hash.of(contents);
        String path = System.getenv("azure-managed-disk") + "/" + filename;
        try (FileOutputStream stream = new FileOutputStream(path)) {
            stream.write(contents);
        }

        return filename;
    }

    /**
     * Return the contents of an image. Throw an appropriate error message if
     * id does not exist.
     * 
     * @throws IOException
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] download(@PathParam("id") String id) throws IOException {
        byte[] bytes = null;
        String path = System.getenv("azure-managed-disk") + "/"+ id;
        try (FileInputStream stream = new FileInputStream(path)) {
            bytes = stream.readAllBytes();
        }

        return (bytes != null) ? bytes : null;
    }

    /**
     * Lists the id of images stored.
     */
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> list() {

        List<String> list = new ArrayList<>();
        String dir = System.getenv("azure-managed-disk");
        String[] dirs = (new File(dir)).list();
        for(String id: dirs)
            list.add(id);
        /*
         * try {
         * 
         * // Get client to blob
         * PagedIterable<BlobItem> blob = containerClient.listBlobs();
         * 
         * for (BlobItem item : blob)
         * list.add(item.getName());
         * 
         * return list;
         * 
         * } catch (Exception e) {
         * e.printStackTrace();
         * }
         */
        return list;
    }

    public boolean verifyImgId(String ImgId) {
        return true;
        // List<String> list = new ArrayList<>();

        // // Get client to blob
        // PagedIterable<BlobItem> blob = containerClient.listBlobs();

        // for (BlobItem item : blob)
        // list.add(item.getName());

        // return list.contains(ImgId);
    }
}