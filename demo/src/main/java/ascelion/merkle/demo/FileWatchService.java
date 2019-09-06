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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import javax.annotation.PreDestroy;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import ascelion.merkle.TreeBuilder;
import ascelion.merkle.TreeRoot;
import ascelion.merkle.help.DataSlice;

import static ascelion.merkle.help.DataSlice.buildTree;
import static com.google.common.collect.Maps.synchronizedBiMap;
import static java.lang.String.format;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Optional.ofNullable;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;

@Singleton
public class FileWatchService {
	static private final Logger L = getLogger(FileWatchService.class);

	@EqualsAndHashCode(of = { "path" })
	@RequiredArgsConstructor
	static class Container {
		final Path path;
		final UUID uuid = UUID.randomUUID();
	}

	@EqualsAndHashCode(of = { "full" })
	static class TreeInfo {
		final Container cont;
		final Path full;
		final Path path;

		TreeRoot<byte[]> root;
		String hash;

		TreeInfo(@NonNull Container cont, @NonNull Path full) {
			this.cont = cont;
			this.full = full;
			this.path = cont.path.relativize(full);
		}

		void load(TreeBuilder<byte[]> tbld, int size) throws IOException {
			final ByteChannel chan = Files.newByteChannel(this.cont.path.resolve(this.path), StandardOpenOption.READ);

			this.root = buildTree(tbld, size, chan);
			this.hash = encodeHexString(this.root.hash());
		}

		@Override
		public String toString() {
			return format("[%s]:/%s", this.cont.uuid, this.path);
		}
	}

	private final Collection<Container> conts = new ArrayList<>();
	private final BiMap<TreeInfo, String> trees = synchronizedBiMap(HashBiMap.create());

	private String algo;
	private int size;

	private TreeBuilder<byte[]> tbld;
	private Disposable sub;

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

		final FileWatch fw = FileWatch.create()
		        .watch(args.directories)
		        .kinds(ENTRY_MODIFY, ENTRY_DELETE);

		fw.paths().stream()
		        .map(Container::new)
		        .forEach(this.conts::add);

		this.sub = fw.observable()
		        .filter(this::acceptEvent)
		        .observeOn(Schedulers.trampoline())
		        .subscribeOn(Schedulers.io())
		        .subscribe(this::onChange);
	}

	@SneakyThrows
	private boolean acceptEvent(FileWatch.Event evt) {
		return Files.isRegularFile(evt.path) || evt.kind == ENTRY_DELETE;
	}

	private void onChange(FileWatch.Event evt) throws IOException {
		final Container cont = findCont(evt.path);

		if (cont == null) {
			L.warn(format("Spurious event: %s in %s", evt.kind, evt.path));
		} else {
			final TreeInfo tree = new TreeInfo(cont, evt.path);

			if (evt.kind == ENTRY_DELETE) {
				deleteTree(tree);
			} else {
				replaceTree(tree);
			}
		}

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

	private void replaceTree(TreeInfo tree) throws IOException {
		this.trees.remove(tree);

		if (Files.size(tree.full) > 0) {
			L.info("Updating {}", tree);

			tree.load(this.tbld, this.size);

			this.trees.forcePut(tree, tree.hash);
		} else {
			L.info("Truncated {}", tree);
		}
	}

	private void deleteTree(final TreeInfo tree) {
		L.info("Removing {}", tree);

		this.trees.remove(tree);
	}

	private Container findCont(Path path) {
		return this.conts.stream().filter(c -> path.startsWith(c.path)).findAny().orElse(null);
	}

}
