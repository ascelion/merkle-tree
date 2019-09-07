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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.lang.Integer.bitCount;
import static java.lang.Integer.numberOfLeadingZeros;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * Builder class for a Merkle tree.
 *
 * @author https://github.com/pa314159
 */
@SuppressWarnings("unchecked")
public final class TreeBuilder<T> {

	static private int next_pow2(int value) {
		switch (value) {
		case 0:
			throw new IllegalArgumentException("The argument 'value' must be greater than 0");
		case 1:
			return 2;
		}

		if (bitCount(value) == 1) {
			return value;
		}

		return 1 << (Integer.SIZE - numberOfLeadingZeros(value));
	}

	static class Root<T> extends TreeNode<T> implements TreeRoot<T> {
		Root(T hash, TreeNode<T> left, TreeNode<T> right) {
			super(hash, left, right);
		}

		@Override
		public final int count() {
			return super.count();
		}

		@Override
		public final int height() {
			return this.height;
		}

		@Override
		public final <L extends TreeLeaf<T, ?>> L getLeaf(int index) {
			if (index < 0) {
				throw new IllegalArgumentException("Negative index");
			}
			if (index >= count()) {
				return null;
			}

			TreeNode<T> walk = this;

			index <<= Integer.SIZE - this.height + 1;

			for (int h = 1; h < this.height; h++, index <<= 1) {
				assert walk != null : "walk is null at position " + h;

				if (index < 0) {
					walk = walk.right;
				} else {
					walk = walk.left;
				}
			}

			return (L) walk;
		}
	}

	static class Null<T> extends TreeNode<T> {
		Null(T hash) {
			super(hash);
		}

		@Override
		int count() {
			// empty nodes don't count
			return 0;
		}
	}

	// the hash function
	public final UnaryOperator<T> hashFn;
	// the concatenation function
	private final BinaryOperator<T> concatFn;
	// supplier for the value of filler
	private final Supplier<T> zero;

	private final List<TreeLeaf<T, ?>> collect = new ArrayList<>();

	/**
	 * Constructs a tree builder using the given operators and a supplier of the filler value.
	 * <p>
	 * Constructs a tree builder that uses arbitrary hash and concatenate functions. Both operators must handle null values
	 * if the supplier <code>zero</code> is expected to return null.
	 * </p>
	 *
	 * @param hashFn   the hash function
	 * @param concatFn the concatenation function
	 * @param zero     supplier of the filler value
	 */
	public TreeBuilder(UnaryOperator<T> hashFn, BinaryOperator<T> concatFn, Supplier<T> zero) {
		this.hashFn = requireNonNull(hashFn, "The hash operator cannot be null");
		this.concatFn = requireNonNull(concatFn, "The concatenation operator cannot be null");
		this.zero = requireNonNull(zero, "The supplier of the filler value cannot be null");
	}

	/**
	 * Constructs a tree builder using the given operators and a constant filler value.
	 * <p>
	 * Constructs a tree builder that uses arbitrary hash and concatenate functions. Both operators must handle null if the
	 * <code>zero</code> is null.
	 * </p>
	 *
	 * @param hashFn   the hash function
	 * @param concatFn the concatenation function
	 * @param zero     the filler value
	 */
	public TreeBuilder(UnaryOperator<T> hashFn, BinaryOperator<T> concatFn, T zero) {
		this(hashFn, concatFn, () -> zero);
	}

	/**
	 * Convenient method to collect leaves; user must then call {@link #build()} to build the tree.
	 *
	 * @param leaf the leaf to be added to the tree.
	 * @return the instance of this builder
	 */
	public TreeBuilder<T> collect(TreeLeaf<T, ?> leaf) {
		requireNonNull(leaf, "The leaf cannot be null");

		this.collect.add(leaf);

		return this;
	}

	/**
	 * Convenient method to collect leaves; user must then call {@link #build()} to build the tree.
	 *
	 * @param leaves leaves to be added to the tree.
	 * @return the instance of this builder
	 */
	public TreeBuilder<T> collect(Stream<TreeLeaf<T, ?>> leaves) {
		requireNonNull(leaves, "The leaves stream cannot be null");

		leaves.forEachOrdered(this.collect::add);

		return this;
	}

	/**
	 * Convenient method to collect leaves; user must then call {@link #build()} to build the tree.
	 *
	 * @param leaves leaves to be added to the tree.
	 * @return the instance of this builder
	 */
	public TreeBuilder<T> collect(Iterable<TreeLeaf<T, ?>> leaves) {
		requireNonNull(leaves, "The collection of leaves cannot be null");

		leaves.forEach(this.collect::add);

		return this;
	}

	/**
	 * Convenient method to collect leaves; user must then call {@link #build()} to build the tree.
	 *
	 * @param leaves leaves to be added to the tree.
	 * @return the instance of this builder
	 */
	public TreeBuilder<T> collect(Iterator<TreeLeaf<T, ?>> leaves) {
		requireNonNull(leaves, "The iterator over leaves cannot be null");

		leaves.forEachRemaining(this.collect::add);

		return this;
	}

	/**
	 * Builds a tree from all leaves previously passed to any <code>collect</code> method.
	 * <p>
	 * The internal state of the builder is cleared afterwards and the builder can be reused to create a different tree.
	 * </p>
	 *
	 * @return the tree
	 */
	public TreeRoot<T> build() {
		try {
			return build(this.collect);
		} finally {
			this.collect.clear();
		}
	}

	/**
	 * Create a Merkle tree from an array of leaves.
	 *
	 * @param leaves the array of leaves.
	 * @return the tree
	 */
	public TreeRoot<T> build(TreeLeaf<T, ?>[] leaves) {
		requireNonNull(leaves, "The array of leaves cannot be null");

		return build(asList(leaves));
	}

	/**
	 * Creates a Merkle tree from a collection of leaves.
	 *
	 * @param leaves the array of leaves.
	 * @return the tree
	 */
	public TreeRoot<T> build(Collection<TreeLeaf<T, ?>> leaves) {
		requireNonNull(leaves, "The collection of leaves cannot be null");

		final int size = leaves.size();

		if (size == 0) {
			throw new IllegalArgumentException("Cannot build a tree from no node");
		}

		// round to the next power of two to have sufficient height
		final int rounded = next_pow2(size);
		final TreeNode<T>[] floor = leaves.toArray(new TreeNode[rounded]);

		if (rounded > size) {
			for (int i = floor.length; floor[--i] == null;) {
				floor[i] = new Null<>(this.zero.get());
			}
		}

		final TreeRoot<T> root = doBuild(floor);

		leaves.forEach(TreeLeaf::buildChain);

		return root;
	}

	/**
	 * Checks whether a hash chain is valid using the operators of this builder instance.
	 *
	 * The chain must contain both the hash of the node and the hash of the root as described at
	 * {@link TreeLeaf#getChain()}.
	 *
	 * @param chain the validation chain
	 * @param index index of the current leaf
	 * @param eq    equality operator for &lt;T&gt;
	 * @return true if the chain is valid
	 */
	public boolean isValid(List<T> chain, int index, BiPredicate<T, T> eq) {
		requireNonNull(chain, "The validation chain cannot be null");
		requireNonNull(chain, "The equality operator cannot be null");

		if (chain.size() < 2) {
			throw new IllegalArgumentException("Chain too short");
		}

		T hash = chain.get(0);

		for (int k = 1; k < chain.size() - 1; k++) {
			final T next = chain.get(k);

			if ((index & 1) == 0) {
				hash = this.hashFn.apply(this.concatFn.apply(hash, next));
			} else {
				hash = this.hashFn.apply(this.concatFn.apply(next, hash));
			}

			index >>>= 1;
		}

		return eq.test(hash, chain.get(chain.size() - 1));
	}

	private Root<T> doBuild(TreeNode<T>[] leaves) {
		// expecting a power of two
		assert bitCount(leaves.length) == 1;

		if (leaves.length == 2) {
			return newNode(leaves[0], leaves[1], true);
		}

		final TreeNode<T>[] parents = new TreeNode[leaves.length / 2];

		for (int i = 0; i < leaves.length; i += 2) {
			parents[i / 2] = newNode(leaves[i], leaves[i + 1], false);
		}

		return doBuild(parents);
	}

	private <N extends TreeNode<T>> N newNode(TreeNode<T> left, TreeNode<T> right, boolean root) {
		final T h = this.hashFn.apply(this.concatFn.apply(left.hash, right.hash));

		return root ? (N) new Root<>(h, left, right) : (N) new TreeNode<>(h, left, right);
	}
}
