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
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import ascelion.merkle.TreeLeaf;
import ascelion.merkle.TreeRoot;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

import org.apache.commons.codec.binary.Hex;

@Path("")
@Produces(APPLICATION_JSON)
public class FilesResource {

	static public class FileResponse {

		public final String base;
		public final String path;
		public final String hash;
		public final int count;

		FileResponse(FileWatchService.TreeInfo info) {
			this.base = info.cont.uuid.toString();
			this.path = info.path.toString();
			this.hash = info.hash;
			this.count = info.root.count();
		}
	}

	static public class SliceResponse {
		public final String content;
		public final String[] hashes;

		public SliceResponse(TreeLeaf<byte[], byte[]> leaf) {
			this.content = encodeBase64String(leaf.getContent());

			final List<byte[]> chain = leaf.getChain();

			this.hashes = chain.stream()
			        .skip(1)
			        .limit(chain.size() - 1)
			        .map(Hex::encodeHexString)
			        .toArray(String[]::new);
		}
	}

	@Inject
	private FileWatchService fws;

	@GET
	@Path("files")
	public FileResponse[] files() {
		return Stream.of(this.fws.trees())
		        .map(FileResponse::new)
		        .toArray(FileResponse[]::new);
	}

	@GET
	@Path("slice/{hash}/{index}")
	public SliceResponse slice(@PathParam("hash") String hash, @PathParam("index") int index) {
		final TreeRoot<byte[]> tree = this.fws.tree(hash);

		if (tree == null || index >= tree.count()) {
			throw new NotFoundException();
		}

		return new SliceResponse(tree.getLeaf(index));
	}

}
