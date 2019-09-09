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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.bind.annotation.JsonbTypeSerializer;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import ascelion.merkle.TreeLeaf;
import ascelion.merkle.TreeRoot;

import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("")
@Produces(APPLICATION_JSON)
public class FilesResource {

	static public class FileResponse {

		public final java.nio.file.Path path;
		@JsonbTypeSerializer(JsonbResolver.HEXSerializer.class)
		public final byte[] hash;
		public final int count;

		FileResponse(FileStoreService.TreeInfo info) {
			this.path = info.path;
			this.hash = info.root.hash();
			this.count = info.root.count();
		}
	}

	static public class SliceResponse {
		@JsonbTypeSerializer(JsonbResolver.B64Serializer.class)
		public final byte[] content;
		@JsonbTypeSerializer(JsonbResolver.HEXSerializer.class)
		public final byte[][] hashes;

		public SliceResponse(TreeLeaf<byte[], byte[]> leaf) {
			this.content = leaf.getContent();

			final List<byte[]> chain = leaf.getChain();

			this.hashes = chain.stream()
			        .skip(1)
			        .limit(chain.size() - 1)
			        .toArray(byte[][]::new);
		}
	}

	@Inject
	private FileStoreService fss;

	@GET
	@Path("containers")
	public Map<UUID, java.nio.file.Path> conts() {
		return this.fss.conts()
		        .stream()
		        .collect(toMap(c -> c.uuid, c -> c.path.getFileName()));
	}

	@GET
	@Path("containers/{uuid}")
	public FileResponse[] files(@PathParam("uuid") UUID uuid) {
		return Stream.of(this.fss.trees())
		        .filter(t -> t.cont.uuid.equals(uuid))
		        .map(FileResponse::new)
		        .toArray(FileResponse[]::new);
	}

	@GET
	@Path("slice/{hash}/{index}")
	public SliceResponse slice(@PathParam("hash") String hash, @PathParam("index") int index) {
		final TreeRoot<byte[]> tree = this.fss.tree(hash);

		if (tree == null || index >= tree.count()) {
			throw new NotFoundException();
		}

		return new SliceResponse(tree.getLeaf(index));
	}
}
