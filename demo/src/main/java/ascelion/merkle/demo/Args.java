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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.util.TypeLiteral;

import static java.lang.String.format;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Vetoed
@Command(name = "merkle-tree-demo")
final class Args {

	static final TypeLiteral<Event<Args>> TYPE = new TypeLiteral<Event<Args>>() {
	};

	@Spec
	private CommandSpec spec;

	@Parameters(paramLabel = "DIRS", arity = "1..",
	        description = "The directories to be watched.")
	String[] directories;

	@Option(names = { "-z", "--size" }, paramLabel = "SIZE", defaultValue = "1024", showDefaultValue = Visibility.ALWAYS,
	        description = { "The size of a slice in bytes." })
	int size;

	String algo = "SHA-256";

	@Option(names = { "-b", "--bind" }, paramLabel = "HOST", defaultValue = "localhost", showDefaultValue = Visibility.ALWAYS,
	        description = { "The host address to bind to." })
	String host;

	@Option(names = { "-p", "--port" }, paramLabel = "PORT", defaultValue = "8080", showDefaultValue = Visibility.ALWAYS,
	        description = { "The port to listen to." })
	int port;

	@Option(names = { "--path" }, paramLabel = "PATH", defaultValue = "", showDefaultValue = Visibility.ALWAYS,
	        description = { "The context path." })
	String path;

	@Option(names = { "-h", "--help" }, usageHelp = true,
	        description = "Prints this message.")
	boolean help;

	@Option(names = { "-a", "--algo" }, paramLabel = "NAME", defaultValue = "SHA-256", showDefaultValue = Visibility.ALWAYS,
	        description = { "The name of the hashing algorithm." })
	void algo(String algo) {
		// check the algorithm name
		try {
			MessageDigest.getInstance(algo);
		} catch (final NoSuchAlgorithmException e) {
			throw new ParameterException(this.spec.commandLine(),
			        format("Invalid algorithm name: %s", algo));
		}
	}
}
