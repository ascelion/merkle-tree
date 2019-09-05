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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Builder class for a Merkle tree.
 *
 * @author https://github.com/pa314159
 */
@RequiredArgsConstructor
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
		public int count() {
			return super.count();
		}

		@Override
		public <L extends TreeLeaf<T, ?>> L getLeaf(int index) {
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
	@NonNull
	public final UnaryOperator<T> hashFn;
	// the concatenation function
	@NonNull
	private final BinaryOperator<T> concatFn;
	// supplier for the value of filler
	@NonNull
	private final Supplier<T> zero;

	private final List<TreeLeaf<T, ?>> collect = new ArrayList<>();

	public TreeBuilder(@NonNull UnaryOperator<T> hashFn, @NonNull BinaryOperator<T> concatFn, @NonNull T zero) {
		this(hashFn, concatFn, () -> zero);
	}

	/**
	 * Convenient method to collect leaves; user must then call {@link #build()} to build the tree.
	 */
	public TreeBuilder<T> collect(TreeLeaf<T, ?> leaf) {
		this.collect.add(leaf);

		return this;
	}

	/**
	 * Convenient method to collect leaves; user must then call {@link #build()} to build the tree.
	 */
	public TreeBuilder<T> collect(Stream<TreeLeaf<T, ?>> stream) {
		stream.forEachOrdered(this.collect::add);

		return this;
	}

	/**
	 * Convenient method to collect leaves; user must then call {@link #build()} to build the tree.
	 */
	public TreeBuilder<T> collect(Iterable<TreeLeaf<T, ?>> stream) {
		stream.forEach(this.collect::add);

		return this;
	}

	/**
	 * Convenient method to collect leaves; user must then call {@link #build()} to build the tree.
	 */
	public TreeBuilder<T> collect(Iterator<TreeLeaf<T, ?>> stream) {
		stream.forEachRemaining(this.collect::add);

		return this;
	}

	/**
	 * Builds a tree from all leaves previously passed to any <code>collect</code> method. The internal state of the
	 * builder is cleared afterwards.
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
	 */
	public TreeRoot<T> build(@NonNull TreeLeaf<T, ?>[] leaves) {
		return build(asList(leaves));
	}

	/**
	 * Creates a Merkle tree from a collection of leaves.
	 *
	 * @param leaves the array of leaves.
	 */
	public TreeRoot<T> build(@NonNull Collection<TreeLeaf<T, ?>> leaves) {
		if (leaves.size() == 0) {
			throw new IllegalArgumentException("Cannot build a tree from no node");
		}

		// round to the next power of two to have sufficient height
		final int rounded = next_pow2(leaves.size());
		final TreeNode<T>[] floor = leaves.toArray(new TreeNode[rounded]);

		System.arraycopy(leaves, 0, floor, 0, leaves.size());

		if (rounded > leaves.size()) {
			for (int i = floor.length; floor[--i] == null;) {
				floor[i] = new Null<>(this.zero.get());
			}
		}

		final TreeNode<T> root = doBuild(floor);

		for (final TreeLeaf<T, ?> leaf : leaves) {
			leaf.buildChain();
		}

		return new Root<>(root.hash, root.left, root.right);
	}

	/**
	 * Checks whether a hash chain is valid using the operators of this builder instance.
	 */
	public boolean isValid(@NonNull List<T> chain, int index, @NonNull BiPredicate<T, T> eq) {
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

	private TreeNode<T> doBuild(TreeNode<T>[] leaves) {
		// expecting a power of two
		assert bitCount(leaves.length) == 1;

		final TreeNode<T>[] parents = new TreeNode[leaves.length / 2];

		for (int i = 0; i < leaves.length; i += 2) {
			final TreeNode<T> l = leaves[i];
			final TreeNode<T> r = leaves[i + 1];
			final T h = this.hashFn.apply(this.concatFn.apply(l.hash, r.hash));

			parents[i / 2] = new TreeNode<>(h, l, r);
		}

		if (parents.length > 1) {
			return doBuild(parents);
		} else {
			return parents[0];
		}
	}
}
