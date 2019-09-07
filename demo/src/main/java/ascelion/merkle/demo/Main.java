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

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.LogManager;

import javax.enterprise.inject.spi.CDI;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory.createHttpServer;
import static org.slf4j.LoggerFactory.getLogger;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.jboss.weld.environment.se.Weld;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Main {
	static private final Logger L = getLogger(Main.class);

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		final String rootLevel = System.getProperty("jul.root.level", "INFO");

		LogManager.getLogManager().getLogger("").setLevel(Level.parse(rootLevel));
	}

	static public void main(String[] args) throws IOException, InterruptedException {
		final Args a = new Args();
		final CommandLine c = new CommandLine(a);

		try {
			if (CommandLine.printHelpIfRequested(c.parseArgs(args))) {
				System.exit(0);
			}
		} catch (final ParameterException e) {
			System.err.println(e.getMessage());

			c.usage(System.err);

			System.exit(1);
		}

		new Main(a).run();
	}

	private final Args args;

	private void run() throws IOException, InterruptedException {
		final Weld weld = new Weld();

		L.info("Initialising WELD");

		weld.initialize();

		final URI base = URI.create(format("http://%s:%d/%s", this.args.host, this.args.port, this.args.path));
		final ResourceConfig conf = new ResourceConfig()
		        .property(ServerProperties.PROVIDER_PACKAGES, Main.class.getPackage().getName());
		final HttpServer http = createHttpServer(base, conf, false);

		L.info("Starting HTTP server");

		http.start();

		getRuntime().addShutdownHook(new Thread(() -> {
			L.info("Stopping HTTP server");
			http.shutdownNow();

			L.info("Stopping WELD");
			weld.shutdown();
		}));

		CDI.current().select(Args.TYPE).get().fire(this.args);

		currentThread().join();
	}

}
