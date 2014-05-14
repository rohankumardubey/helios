package com.spotify.helios.agent;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import com.spotify.helios.servicescommon.ServiceParser;
import com.yammer.dropwizard.config.HttpConfiguration;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.io.BaseEncoding.base16;
import static net.sourceforge.argparse4j.impl.Arguments.append;
import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;

public class AgentParser extends ServiceParser {

  private final AgentConfig agentConfig;

  private Argument noHttpArg;
  private Argument httpArg;
  private Argument adminArg;
  private Argument stateDirArg;
  private Argument dockerArg;
  private Argument envArg;
  private Argument syslogRedirectToArg;
  private Argument portRangeArg;
  private Argument agentIdArg;

  public AgentParser(final String... args) throws ArgumentParserException {
    super("helios-agent", "Spotify Helios Agent", args);

    final Namespace options = getNamespace();
    final String uriString = options.getString("docker");
    try {
      new URI(uriString);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Bad docker endpoint: " + uriString, e);
    }

    final List<List<String>> env = options.getList(envArg.getDest());
    final Map<String, String> envVars = Maps.newHashMap();
    if (env != null) {
      for (final List<String> group : env) {
        for (final String s : group) {
          final String[] parts = s.split("=", 2);
          if (parts.length != 2) {
            throw new IllegalArgumentException("Bad environment variable: " + s);
          }
          envVars.put(parts[0], parts[1]);
        }
      }
    }

    final InetSocketAddress httpAddress = parseSocketAddress(options.getString(httpArg.getDest()));

    final String portRangeString = options.getString(portRangeArg.getDest());
    final List<String> parts = Splitter.on(':').splitToList(portRangeString);
    if (parts.size() != 2) {
      throw new IllegalArgumentException("Bad port range: " + portRangeString);
    }
    final int start;
    final int end;
    try {
      start = Integer.valueOf(parts.get(0));
      end = Integer.valueOf(parts.get(1));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Bad port range: " + portRangeString);
    }
    if (end <= start) {
      throw new IllegalArgumentException("Bad port range: " + portRangeString);
    }

    this.agentConfig = new AgentConfig()
        .setName(getName())
        .setZooKeeperConnectionString(getZooKeeperConnectString())
        .setZooKeeperSessionTimeoutMillis(getZooKeeperSessionTimeoutMillis())
        .setZooKeeperConnectionTimeoutMillis(getZooKeeperConnectionTimeoutMillis())
        .setDomain(getDomain())
        .setEnvVars(envVars)
        .setDockerEndpoint(options.getString(dockerArg.getDest()))
        .setInhibitMetrics(getInhibitMetrics())
        .setRedirectToSyslog(options.getString(syslogRedirectToArg.getDest()))
        .setStateDirectory(Paths.get(options.getString(stateDirArg.getDest())))
        .setStatsdHostPort(getStatsdHostPort())
        .setRiemannHostPort(getRiemannHostPort())
        .setPortRange(start, end)
        .setSentryDsn(getSentryDsn())
        .setServiceRegistryAddress(getServiceRegistryAddress())
        .setServiceRegistrarPlugin(getServiceRegistrarPlugin());

    final String explicitId = options.getString(agentIdArg.getDest());
    if (explicitId != null) {
      agentConfig.setId(explicitId);
    } else {
      final byte[] idBytes = new byte[20];
      new SecureRandom().nextBytes(idBytes);
      agentConfig.setId(base16().encode(idBytes));
    }

    final boolean noHttp = options.getBoolean(noHttpArg.getDest());

    if (noHttp) {
      agentConfig.setHttpConfiguration(null);
    } else {
      final HttpConfiguration http = agentConfig.getHttpConfiguration();
      http.setPort(httpAddress.getPort());
      http.setBindHost(httpAddress.getHostString());
      http.setAdminPort(options.getInt(adminArg.getDest()));
    }
  }

  @Override
  protected void addArgs(final ArgumentParser parser) {
    noHttpArg = parser.addArgument("--no-http")
        .action(storeTrue())
        .setDefault(false)
        .help("disable http server");

    httpArg = parser.addArgument("--http")
        .setDefault("http://0.0.0.0:5803")
        .help("http endpoint");

    adminArg = parser.addArgument("--admin")
        .type(Integer.class)
        .setDefault(5804)
        .help("admin http port");

    agentIdArg = parser.addArgument("--id")
        .help("Agent unique ID. Generated and persisted on first run if not specified.");

    stateDirArg = parser.addArgument("--state-dir")
        .setDefault(".")
        .help("Directory for persisting agent state locally.");

    dockerArg = parser.addArgument("--docker")
        .setDefault("http://localhost:4160")
        .help("docker endpoint");

    envArg = parser.addArgument("--env")
        .action(append())
        .setDefault(new ArrayList<String>())
        .nargs("+")
        .help("Specify environment variables that will pass down to all containers");

    syslogRedirectToArg = parser.addArgument("--syslog-redirect-to")
        .help("redirect container's stdout/stderr to syslog running at host:port");

    portRangeArg = parser.addArgument("--port-range")
        .setDefault("40000:49152")
        .help("Port allocation range, start:end (end exclusive).");
  }

  public AgentConfig getAgentConfig() {
    return agentConfig;
  }

}
