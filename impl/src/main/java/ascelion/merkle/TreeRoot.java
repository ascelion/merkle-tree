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

/**
 * The root of the Merkle tree.
 *
 * @author https://github.com/pa314159
 */
public interface TreeRoot<T> {

	/**
	 * Gets the hash value of the root node.
	 *
	 * @return the hash value.
	 */
	T hash();

	/**
	 * Gets the height of the tree.
	 *
	 * @return the height of the tree.
	 */
	int height();

	/**
	 * Gets number of leaves in this tree.
	 *
	 * @return the leaves count.
	 */
	int count();

	/**
	 * Gets the leaf at the given index; return null if no such leaf exists.
	 *
	 * <p>
	 * The method returns the instance at position <code>index</code> that has been passed to the {@link TreeBuilder}.
	 * </p>
	 *
	 * @param <L>   the actual type of the leaf.
	 * @param index the leaf index.
	 * @return the leaf instance at the given position or null.
	 */
	<L extends TreeLeaf<T, ?>> L getLeaf(int index);
}
