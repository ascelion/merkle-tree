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
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.slf4j.LoggerFactory.getLogger;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FileWatch {
	static private final Logger L = getLogger(FileWatch.class);

	static public final Kind<Path> ENTRY_ERROR = new Kind<Path>() {

		@Override
		public String name() {
			return "ENTRY_ERROR";
		}

		@Override
		public Class<Path> type() {
			return Path.class;
		}
	};

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@ToString
	static public class Event {
		@NonNull
		public final Path path;
		@NonNull
		public final WatchEvent.Kind<Path> kind;
		public final Throwable exception;

		private Event(Path path, Kind<Path> kind) {
			this(path, kind, null);
		}
	}

	static public final class FileWatchException extends Exception {
		public final Path path;

		private FileWatchException(Exception cause, Path path) {
			super(cause);

			this.path = path;
		}
	}

	static public FileWatch create(FileSystem fs) {
		return new FileWatch(fs);
	}

	static public FileWatch create() {
		return new FileWatch(FileSystems.getDefault());
	}

	@RequiredArgsConstructor
	static private final class FileWatchObservable extends Observable<Event> implements Disposable {
		private final WatchService ws;
		private final List<Path> paths;
		private final Set<Kind<Path>> kinds;
		private final Map<Path, WatchKey> watched = new HashMap<>();
		private final AtomicBoolean disposed = new AtomicBoolean();

		private FileWatchObservable(WatchService ws, Set<Kind<Path>> kinds, List<Path> paths) throws IOException {
			this.ws = ws;
			this.kinds = kinds;
			this.paths = paths;
		}

		@Override
		public void dispose() {
			if (this.disposed.compareAndSet(false, true)) {
				L.info("Closing WatchService");

				try {
					this.ws.close();
				} catch (final IOException e) {
					L.error("WatchService", e);
				}
			}
		}

		@Override
		public boolean isDisposed() {
			return this.disposed.get();
		}

		@Override
		protected void subscribeActual(Observer<? super Event> obs) {
			obs.onSubscribe(this);

			this.paths.forEach(p -> scan(obs, p));

			while (!isDisposed()) {
				final WatchKey key;

				try {
					key = this.ws.take();
				} catch (final InterruptedException e) {
					break;
				} catch (final ClosedWatchServiceException e) {
					if (isDisposed()) {
						break;
					}

					obs.onError(e);

					continue;
				}

				try {
					for (final WatchEvent<?> event : key.pollEvents()) {
						process(obs, key, (WatchEvent<Path>) event);
					}
				} finally {
					key.reset();
				}
			}
		}

		private void process(Observer<? super Event> obs, final WatchKey key, final WatchEvent<Path> event) {
			final Path path = (Path) key.watchable();
			final Path name = event.context();
			final Path full = path.resolve(name);

			L.trace("Got {} for {}", event.kind(), full);

			if (event.kind() == ENTRY_DELETE) {
				final boolean[] sent = { false };

				this.watched.entrySet().removeIf(e -> {
					sent[0] = sent[0] | e.getKey().equals(full);

					if (e.getKey().startsWith(full)) {
						e.getValue().cancel();

						return true;
					} else {
						return false;
					}
				});

				if (!sent[0]) {
					send(obs, full, event.kind(), null);
				}
			} else {
				if (Files.isDirectory(full) && event.kind() == ENTRY_CREATE) {
					scan(obs, full);
				}

				if (this.kinds.contains(event.kind())) {
					send(obs, full, event.kind(), null);
				}
			}
		}

		private void scan(Observer<? super Event> obs, Path path) {
			if (register(obs, path)) {
				try {
					Files.list(path).forEach(child -> visit(obs, path.resolve(child)));
				} catch (final IOException e) {
					send(obs, path, ENTRY_ERROR, e);
				}
			}
		}

		public void visit(Observer<? super Event> obs, Path path) {
			if (Files.isDirectory(path)) {
				scan(obs, path);
			} else {
				@SuppressWarnings("unused")
				final boolean unused = send(obs, path, ENTRY_CREATE, null) || send(obs, path, ENTRY_MODIFY, null);
			}
		}

		private boolean register(Observer<? super Event> obs, Path path) {
			return null != this.watched.compute(path, (k, v) -> {
				if (v == null) {
					try {
						v = path.register(this.ws, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
					} catch (final IOException e) {
						send(obs, path, ENTRY_ERROR, e);

						return null;
					}
				}

				return v;
			});
		}

		boolean send(Observer<? super Event> obs, Path path, Kind<Path> kind, Throwable ex) {
			if (this.kinds.contains(kind)) {
				obs.onNext(new Event(path, kind, ex));

				return true;
			} else {
				return false;
			}
		}
	}

	private final FileSystem fs;
	private final List<Path> paths = new ArrayList<>();
	private final Set<Kind<Path>> kinds = new HashSet<>();

	public FileWatch watch(String... paths) {
		Stream.of(paths)
		        .map(this.fs::getPath)
		        .map(Path::toAbsolutePath)
		        .forEach(this.paths::add);

		return check();
	}

	public FileWatch watch(Path... paths) {
		Stream.of(paths)
		        .map(Path::toAbsolutePath)
		        .forEach(this.paths::add);

		return check();
	}

	public FileWatch kinds(Kind<Path>... kinds) {
		this.kinds.addAll(asList(kinds));

		return this;
	}

	public List<Path> paths() {
		return unmodifiableList(this.paths);
	}

	public Observable<Event> observable() throws IOException {
		return new FileWatchObservable(this.fs.newWatchService(), this.kinds, this.paths);
	}

	private FileWatch check() {
		Collections.sort(this.paths);

		for (int i = 1; i < this.paths.size(); i++) {
			final Path c = this.paths.get(i);

			this.paths.stream()
			        .limit(i)
			        .filter(c::startsWith)
			        .findAny()
			        .ifPresent(p -> {
				        throw new IllegalArgumentException(format("The path %s overlaps with %s", c, p));
			        });
		}

		return this;
	}

}
