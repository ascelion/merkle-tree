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

package ascelion.merkle;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * A leaf of the Merkle tree.
 *
 * @author https://github.com/pa314159
 */
public class TreeLeaf<T, S> extends TreeNode<T> {

	static private <T> TreeNode<T> siblingNode(TreeNode<T> node) {
		if (node.parent == null) {
			return null;
		}

		return node.parent.left == node ? node.parent.right : node.parent.left;
	}

	static private <T> TreeNode<T> parentNode(TreeNode<T> node) {
		return node != null ? node.parent : null;
	}

	private final List<T> chain = new ArrayList<>();
	private final S content;

	/**
	 * Constructs a leaf witht a hash and some additional information.
	 *
	 * <p>
	 * The <code>content</code> doesn't have any particular meaning and is not checked in any way, but usually it
	 * represents the data whose hash value is passed to the <code>hash</code> parameter.
	 * </p>
	 *
	 * @param hash    the hash of this leaf
	 * @param content data associated to this leaf
	 */
	public TreeLeaf(T hash, S content) {
		super(hash);

		this.content = content;
	}

	/**
	 * Gets the data associated to this leaf.
	 *
	 * @return the data associated to this leaf.
	 */
	public final S getContent() {
		return this.content;
	}

	/**
	 * Gets the whole hash chain of this leaf, starting with the hash of this leaf, continuing with the list of the
	 * siblings/uncles and ending with the hash of the root.
	 *
	 * @return the whole validation chain.
	 */
	public final List<T> getChain() {
		return unmodifiableList(this.chain);
	}

	void buildChain() {
		assert this.parent != null : "cannot build a chain for a root node";
		assert this.chain.isEmpty() : "the chain has been already built";

		this.chain.add(this.hash);

		TreeNode<T> root = null;

		for (TreeNode<T> node = siblingNode(this); node != null; node = siblingNode(parentNode(node))) {
			this.chain.add(node.hash);

			root = node.parent;
		}

		if (root != null) {
			this.chain.add(root.hash);
		}
	}
}
