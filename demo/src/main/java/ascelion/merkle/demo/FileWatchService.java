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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import ascelion.merkle.TreeBuilder;
import ascelion.merkle.TreeRoot;
import ascelion.merkle.help.DataSlice;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.apache.commons.codec.binary.Hex.encodeHexString;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FileWatchService {
	static private final Logger L = LoggerFactory.getLogger(FileWatchService.class);

	@EqualsAndHashCode(of = "hash")
	static class Tree {
		final TreeRoot<byte[]> root;
		final String hash;

		Tree(TreeRoot<byte[]> root) {
			this.root = root;
			this.hash = encodeHexString(root.hash());
		}

		Tree(String hash) {
			this.root = null;
			this.hash = hash;
		}
	}

	private final ExecutorService exec = Executors.newSingleThreadExecutor();
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Map<Path, TreeRoot<byte[]>> toTree = new HashMap<>();
	private final BiMap<Path, String> toHash = HashBiMap.create();

	private FileSystem fs;
	private WatchService ws;

	private String algo;
	private int size;

	private TreeBuilder<byte[]> tbld;

	public Map.Entry<Path, TreeRoot<byte[]>>[] trees() {
		final Lock lock = this.rwLock.readLock();

		lock.lock();

		try {
			return this.toTree.entrySet().toArray(new Map.Entry[0]);
		} finally {
			lock.unlock();
		}
	}

	public TreeRoot<byte[]> tree(String hash) {
		final Lock lock = this.rwLock.readLock();

		lock.lock();

		try {
			final Path path = this.toHash.inverse().get(hash);

			return path != null ? this.toTree.get(path) : null;
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings("unused")
	private void init(@Observes Server s) throws IOException {
		this.algo = s.algo;
		this.size = s.size;

		this.tbld = new TreeBuilder<>(this::hash, DataSlice::concat, new byte[0]);

		Stream.of(s.directories)
		        .map(Paths::get)
		        .map(Path::toAbsolutePath)
		        .forEach(this::register);

		this.exec.execute(this::poll);
	}

	private void register(Path path) {
		try {
			L.info("Watching {}", path);

			path.register(this.ws, ENTRY_MODIFY, ENTRY_DELETE);

			Files.newDirectoryStream(path)
			        .forEach(this::replaceTree);

		} catch (final IOException e) {
			throw new RuntimeException(path.toString(), e);
		}
	}

	@SneakyThrows
	private byte[] hash(byte[] data) {
		final MessageDigest dig = MessageDigest.getInstance(this.algo);

		dig.update(data);

		return dig.digest();
	}

	@PostConstruct
	@SneakyThrows
	private void postConstruct() {
		this.fs = FileSystems.getDefault();
		this.ws = this.fs.newWatchService();
	}

	@PreDestroy
	private void preDestroy() {
		this.exec.shutdownNow();
	}

	private void poll() {
		try {
			WatchKey key;

			while ((key = this.ws.take()) != null) {
				for (final WatchEvent<?> event : key.pollEvents()) {
					final Path path = (Path) key.watchable();
					final Path name = (Path) event.context();

					if (event.kind() == ENTRY_MODIFY) {
						replaceTree(path.resolve(name).toAbsolutePath());
					}
					if (event.kind() == ENTRY_DELETE) {
						deleteTree(path.resolve(name).toAbsolutePath());
					}
				}

				key.reset();
			}
		} catch (final InterruptedException e) {
			L.info("Poll finished");
		}
	}

	private void replaceTree(Path path) {
		try {
			if (!Files.isRegularFile(path) || Files.size(path) == 0) {
				L.info("Skipped {}", path.getFileName());

				return;
			}

			L.info("Updating {}", path.getFileName());

			final ByteChannel chn = Files.newByteChannel(path, StandardOpenOption.READ);

			final Lock lock = this.rwLock.writeLock();

			lock.lock();

			try {
				final TreeRoot<byte[]> root = DataSlice.buildTree(this.tbld, this.size, chn);

				this.toTree.put(path, root);
				this.toHash.put(path, encodeHexString(root.hash()));
			} finally {
				lock.unlock();
			}
		} catch (final IOException e) {
			L.error("{}: {}", path.getFileName(), e.getMessage());
		}
	}

	private void deleteTree(Path path) {
		L.info("Removing {}", path.getFileName());

		final Lock lock = this.rwLock.writeLock();

		lock.lock();

		try {
			this.toTree.remove(path);
			this.toHash.remove(path);
		} finally {
			lock.unlock();
		}
	}
}
