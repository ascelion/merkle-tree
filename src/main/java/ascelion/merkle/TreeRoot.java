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
	 * Gets number of leaves in this tree.
	 */
	int count();

	/**
	 * Gets the leaf at the given index; return null if no such leaf exists.
	 */
	<L extends TreeLeaf<T, ?>> L getLeaf(int index);
}
