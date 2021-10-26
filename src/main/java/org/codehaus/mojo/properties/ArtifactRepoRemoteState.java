package org.codehaus.mojo.properties;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

/**
 * An ArtifactRepoRemoteState maps to an
 * <a href="https://www.terraform.io/docs/language/settings/backends/artifactory.html">artifactory" backend</a>
 * for terraform
 * @author mykelalvis
 *
 * This will perform a download (wget/curl-ish action) against the joined values to fetch the tfstate file
 *
 */
public class ArtifactRepoRemoteState {
  public String  id = "default";

  /**
   * id maps to server id for username/password
   */
  public String  serverId;
  /**
   * BASE Url of the artifact repo (Artifactory or Nexus)
   */
  public String  url;
  /**
   * Repo name
   */
  public String  repo;
  /**
   * Subpath within repo of tfstate file
   */
  public String  subpath;

  private Server server;

  private String _url;

  /**
   *
   * @return contents as a valid JSON String
   */
  public String readContents() throws IOException {
    requireNonNull(serverId, "serverId is required");
    URL u = new URL(_url);
    Authenticator.setDefault(getAuthenticator());
    URLConnection conn = u.openConnection();

    try (InputStream ins = conn.getInputStream()) {
      return ReadJSONObjectAsFlattenedPropertiesMojo.readStream(ins);
    }
  }


  public ArtifactRepoRemoteState validate(ArtifactRepoRemoteState defaults, Settings settings)
      throws MojoExecutionException {
    if (defaults != null) {
      serverId = serverId == null ? defaults.serverId : serverId;
      repo = repo == null ? defaults.repo : repo;
      url = url == null ? defaults.url : url;
      subpath = subpath == null ? defaults.subpath : subpath;
    }
    if (serverId != null) {
      this.server = settings.getServer(serverId);
      if (this.server == null)
        throw new MojoExecutionException("Server " + serverId + " not found in settings in " + id);
    }
    this._url = String.format("%s/%s/%s", requireNonNull(url, "Base url must be supplied for " + serverId),
        requireNonNull(repo, "Repo must be supplied for " + serverId),
        requireNonNull(subpath, "Subpath must be supplied for " + serverId));
    return this;

  }

  private Authenticator getAuthenticator() {
    return new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(server.getUsername(), server.getPassword().toCharArray());
      }
    };
  }

}
