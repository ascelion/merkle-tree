// Merkle Tree - a generic implementation of Merkle trees.
//
// Copyright (c) 2019 ASCELION
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package ascelion.merkle.demo;

import java.net.URI;
import java.security.MessageDigest;
import java.util.concurrent.Callable;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory.createHttpServer;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "merkle-tree-demo")
@Singleton
public class Server implements Callable<Integer> {

	@Parameters(paramLabel = "DIRS", arity = "1..",
	        description = "The directories to be watched.")
	String[] directories;

	@Option(names = { "--size" }, paramLabel = "SIZE", defaultValue = "1024", showDefaultValue = Visibility.ALWAYS,
	        description = { "The size of a slice in bytes." })
	int size;

	@Option(names = { "--algo" }, paramLabel = "NAME", defaultValue = "SHA-256", showDefaultValue = Visibility.ALWAYS,
	        description = { "The name of the hashing algorithm." })
	String algo;

	@Option(names = { "--host" }, paramLabel = "HOST", defaultValue = "localhost", showDefaultValue = Visibility.ALWAYS,
	        description = { "The port to listen to." })
	private String host;

	@Option(names = { "--port" }, paramLabel = "PORT", defaultValue = "8080", showDefaultValue = Visibility.ALWAYS,
	        description = { "The address to listen to." })
	private int port;

	@Option(names = { "--path" }, paramLabel = "PATH", defaultValue = "", showDefaultValue = Visibility.ALWAYS,
	        description = { "The context path." })
	private String path;

	@Option(names = { "-h", "--help" }, usageHelp = true,
	        description = "Prints this message.")
	private boolean help;

	@Inject
	private Event<Server> event;

	@Override
	public Integer call() throws Exception {
		// check the algorithm name
		MessageDigest.getInstance(this.algo);

		final URI base = URI.create(format("http://%s:%d/%s", this.host, this.port, this.path));
		final ResourceConfig conf = new ResourceConfig()
		        .property(ServerProperties.PROVIDER_PACKAGES, getClass().getPackage().getName());
		final HttpServer http = createHttpServer(base, conf, false);

		http.start();

		getRuntime().addShutdownHook(new Thread(http::shutdownNow));

		this.event.fire(this);

		currentThread().join();

		return 0;
	}

}
