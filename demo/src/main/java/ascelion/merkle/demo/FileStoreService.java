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
import java.nio.channels.ByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import ascelion.merkle.TreeBuilder;
import ascelion.merkle.TreeRoot;
import ascelion.merkle.help.DataSlice;

import static ascelion.merkle.help.DataSlice.buildTree;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.reactivex.disposables.Disposable;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;

@Singleton
public class FileStoreService {
	static private final Logger L = getLogger(FileStoreService.class);

	@EqualsAndHashCode(of = { "path" })
	@RequiredArgsConstructor
	static class Container implements Comparable<Container> {
		final Path path;
		final UUID uuid = UUID.randomUUID();

		@Override
		public int compareTo(Container that) {
			return this.path.compareTo(that.path);
		}
	}

	@EqualsAndHashCode(of = { "cont", "path" })
	static class TreeInfo {
		final Container cont;
		final Path path;

		TreeRoot<byte[]> root;

		TreeInfo(@NonNull Container cont, @NonNull Path full) {
			this.cont = cont;
			this.path = cont.path.relativize(full);
		}

		void load(TreeBuilder<byte[]> tbld, int size) throws IOException {
			final ByteChannel chan = Files.newByteChannel(this.cont.path.resolve(this.path), StandardOpenOption.READ);

			this.root = buildTree(tbld, size, chan);
		}

		@Override
		public String toString() {
			return format("[%s]:%s", this.cont.uuid, this.path);
		}
	}

	private final List<Container> conts = new ArrayList<>();
	private final BiMap<TreeInfo, String> trees = HashBiMap.create();

	private String algo;
	private int size;

	private TreeBuilder<byte[]> tbld;
	private Disposable sub;

	public List<Container> conts() {
		return unmodifiableList(this.conts);
	}

	public TreeInfo[] trees() {
		return this.trees.keySet().toArray(new TreeInfo[0]);
	}

	public TreeRoot<byte[]> tree(String hash) {
		return ofNullable(this.trees.inverse().get(hash))
		        .map(t -> t.root)
		        .orElse(null);
	}

	@SuppressWarnings("unused")
	private void init(@Observes Args args) throws IOException {
		this.algo = args.algo;
		this.size = args.size;

		this.tbld = new TreeBuilder<>(this::hash, DataSlice::concat, new byte[0]);

		Stream.of(args.directories)
		        .map(Paths::get)
		        .map(Path::toAbsolutePath)
		        .map(Container::new)
		        .forEach(this.conts::add);

		Collections.sort(this.conts);

		for (int i = 1; i < this.conts.size(); i++) {
			final Path c = this.conts.get(i).path;

			this.conts.stream()
			        .limit(i)
			        .filter(p -> p.path.startsWith(c))
			        .findAny()
			        .ifPresent(p -> {
				        throw new IllegalArgumentException(format("The path %s overlaps with %s", c, p));
			        });
		}

		this.conts.forEach(this::walk);
	}

	@SneakyThrows
	private void walk(Container cont) {
		Files.walkFileTree(cont.path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (Files.size(file) > 0) {
					final TreeInfo tree = new TreeInfo(cont, file);

					L.info("Loading {}", tree);

					tree.load(FileStoreService.this.tbld, FileStoreService.this.size);

					FileStoreService.this.trees.put(tree, encodeHexString(tree.root.hash()));
				}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				L.error(file.toString(), exc);

				return FileVisitResult.SKIP_SUBTREE;
			}
		});
	}

	@SneakyThrows
	private byte[] hash(byte[] data) {
		final MessageDigest dig = MessageDigest.getInstance(this.algo);

		dig.update(data);

		return dig.digest();
	}

	@PreDestroy
	private void preDestroy() {
		this.sub.dispose();
	}

}
