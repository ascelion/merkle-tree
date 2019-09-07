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

import static java.util.Objects.requireNonNull;

/**
 * @author https://github.com/pa314159
 */
class TreeNode<T> {
	final T hash;
	final TreeNode<T> left;
	final TreeNode<T> right;

	TreeNode<T> parent;

	// the number of leaves in this node
	private final int count;

	// the tree height, including this root
	final int height;

	TreeNode(T hash) {
		this.hash = requireNonNull(hash, "The hash value cannot be null");
		this.left = null;
		this.right = null;
		this.height = 1;
		this.count = 1;
	}

	TreeNode(T hash, TreeNode<T> left, TreeNode<T> right) {
		// internal use only
		assert hash != null : "hash cannot be null";
		assert left != null : "left cannot be null";
		assert right != null : "right cannot be null";

		this.hash = hash;
		this.left = left;
		this.right = right;

		this.left.parent = this;
		this.right.parent = this;

		this.height = 1 + left.height;
		this.count = left.count() + right.count();
	}

	/**
	 * Gets the hash value of this node.
	 *
	 * @return the hash value.
	 */
	public final T hash() {
		return this.hash;
	}

	int count() {
		return this.count;
	}
}
