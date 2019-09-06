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

import java.util.logging.Level;
import java.util.logging.LogManager;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;

import static java.lang.Runtime.getRuntime;

import org.jboss.weld.environment.se.Weld;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.CommandLine;

@Vetoed
public class Main {
	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		final String rootLevel = System.getProperty("jul.root.level", "INFO");

		LogManager.getLogManager().getLogger("").setLevel(Level.parse(rootLevel));
	}

	static public void main(String[] args) throws NoSuchMethodException {
		final Weld weld = new Weld();

		weld.initialize();

		getRuntime().addShutdownHook(new Thread(weld::shutdown));

		System.exit(new CommandLine(CDI.current().select(Server.class).get())
		        .execute(args));
	}

}
