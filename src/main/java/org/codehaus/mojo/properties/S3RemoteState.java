package org.codehaus.mojo.properties;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

/**
 * An ArtifactRepoRemoteState maps to an
 * <a href="https://www.terraform.io/docs/language/settings/backends/artifactory.html">artifactory" backend</a>
 * for terraform
 * @author mykelalvis
 *
 * This will perform a download (wget/curl-ish action) against the joined values to fetch the tfstate file
 *
 */
public class S3RemoteState {
  public String  id = "default";

  /**
   * profile makes to profile in .aws/credentials, ignoring settings.xml
   */
  public String  profile;

  /**
   * id maps to server id for username/password, ignoring .aws/credentials
   */
  public String  serverId;

  /**
   * Optionally overrides region from profile, or required for server credentials
   */
  public String  region;

  /**
   * Required bucket name
   */
  public String  bucket;

  /**
   * Required key to TFState object
   */

  public String  key;

  private Server server;

  /**
   *
   * @param settings (Maven settings)
   * @return string of JSONObject f
   * @throws IOException
   */
  public String readContents() throws IOException {
    if (profile != null) {
      System.getProperties().setProperty("aws.profile", profile);
    }

    AmazonS3ClientBuilder s3b = AmazonS3ClientBuilder.standard();

    if (server != null) {
      AWSCredentials credentials = new BasicAWSCredentials(server.getUsername(), server.getPassword());
      s3b = s3b.withCredentials(new AWSStaticCredentialsProvider(credentials));
    }
    if (region != null)
      s3b = s3b.withRegion(region);
    AmazonS3 client = s3b.build();

    S3Object v      = client.getObject(bucket, key);
    try (S3ObjectInputStream ins = v.getObjectContent()) {
      return ReadJSONObjectAsFlattenedPropertiesMojo.readStream(ins);
    }
  }

  public S3RemoteState validate(S3RemoteState defaults, Settings settings) throws MojoExecutionException {
    if (defaults != null) {
      profile = profile == null ? defaults.profile : profile;
      region = region == null ? defaults.region : region;
      serverId = serverId == null ? defaults.serverId : serverId;
      bucket = bucket == null ? defaults.bucket : bucket;
      key = key == null ? defaults.key : key;
    }

    if (profile != null && serverId != null)
      throw new MojoExecutionException("Both serverId and profile may not be set in " + id);
    if (serverId != null) {
      this.server = settings.getServer(serverId);
      if (this.serverId == null)
        throw new MojoExecutionException("Server " + serverId + " is not present in settings in " + id);
    }
    if (serverId != null && region == null)
      throw new MojoExecutionException("Region is required when using serverId in " + id);
    if (bucket == null)
      throw new MojoExecutionException("Bucket must not be null in " + id);
    if (key == null)
      throw new MojoExecutionException("Key must not be null in " + id);

    server = serverId != null ? settings.getServer(serverId) : null;

    return this;
  }
}
