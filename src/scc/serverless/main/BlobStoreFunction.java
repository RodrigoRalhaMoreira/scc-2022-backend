package scc.serverless.main;

import com.microsoft.azure.functions.annotation.*;

import scc.srv.resources.MediaResource;

import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Blob Trigger.
 */
public class BlobStoreFunction {
	@FunctionName("blobGeoReplication")
	public void setLastBlobInfo(
			@BlobTrigger(name = "blobGeoReplication", dataType = "binary", path = "images/{name}", connection = "BlobStoreConnection") byte[] content,
			@BindingName("name") String blobname,
			final ExecutionContext context) {

		context.getLogger().info("TRIGGER is triggered BLOB REPLICATION: " + blobname);

		String connectionString = "DefaultEndpointsProtocol=https;AccountName=sccstasiagroupdrt;"
		        + "AccountKey=4h1yasgYEMfuIyUFTC5pcgqf+CcVqhwGHYw+oWhl0rRoQhESzQlb461spAoxWm2b9FYD4UgZ4dS1+AStvc5IQg==;"
		        + "EndpointSuffix=core.windows.net";
		
		MediaResource media_instance = new MediaResource(connectionString);
		media_instance.upload(content);
	}
}
