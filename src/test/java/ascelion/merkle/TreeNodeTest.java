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

import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ascelion.merkle.TreeBuilder.Root;

import static java.util.Arrays.asList;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author https://github.com/pa314159
 */
public class TreeNodeTest {

	static private final String ZERO = "";
	static final BinaryOperator<String> CONCAT = (s1, s2) -> Stream.of(s1, s2)
	        .filter(Objects::nonNull)
	        .collect(joining());

	final TreeBuilder<String> tbld = new TreeBuilder<>(identity(), CONCAT, ZERO);

	@Test(expected = IllegalArgumentException.class)
	public void withEmpty() {
		this.tbld.build(asList());
	}

	@Test
	public void withOne() {
		final TreeLeaf<String, ?> node = new TreeLeaf<>("A", null);
		final Root<String> root = (Root<String>) this.tbld.build(asList(node));

		assertThat(root, not(sameInstance(node)));

		assertThat(root.height, equalTo(2));
		assertThat(root.count(), equalTo(1));
		assertThat(root.count(), lessThanOrEqualTo(1 << (root.height - 1)));

		assertThat(root.left, sameInstance(node));
		assertThat(root.right, not(sameInstance(node)));

		assertThat(root.hash, equalTo("A"));
		assertThat(root.left.hash, equalTo("A"));
		assertThat(root.right.hash, equalTo(""));

		assertThat(root.getLeaf(2), is(nullValue()));

		verifyTree(root, 2);
	}

	@Test
	public void withTwo() {
		final TreeLeaf<String, ?> node0 = new TreeLeaf<>("A", null);
		final TreeLeaf<String, ?> node1 = new TreeLeaf<>("B", null);
		final Root<String> root = (Root<String>) this.tbld.build(asList(node0, node1));

		assertThat(root.height, equalTo(2));
		assertThat(root.count(), equalTo(2));
		assertThat(root.count(), lessThanOrEqualTo(1 << (root.height - 1)));

		assertThat(root, not(sameInstance(node0)));
		assertThat(root, not(sameInstance(node1)));

		assertThat(root.hash, equalTo("AB"));
		assertThat(root.left.hash, equalTo("A"));
		assertThat(root.right.hash, equalTo("B"));

		verifyTree(root, 2);
	}

	@Test
	public void withEven() {
		final TreeLeaf<String, ?>[] nodes = IntStream.range(0, 10)
		        .mapToObj(n -> Integer.toString(n))
		        .map(s -> new TreeLeaf<>(s, null))
		        .toArray(TreeLeaf[]::new);
		final Root<String> root = (Root<String>) this.tbld.build(nodes);

		assertThat(root.height, equalTo(5));
		assertThat(root.count(), equalTo(10));

		assertThat(root.hash, equalTo("0123456789"));

		verifyTree(root, 5);
	}

	@Test
	public void withOdd() {
		final TreeLeaf<String, ?>[] nodes = IntStream.range('A', 'A' + 17)
		        .mapToObj(n -> new String(new char[] { (char) n }))
		        .map(s -> new TreeLeaf<>(s, null))
		        .toArray(TreeLeaf[]::new);
		final Root<String> root = (Root<String>) this.tbld.build(nodes);

		assertThat(root.height, equalTo(6));
		assertThat(root.count(), equalTo(17));

		assertThat(root.hash, equalTo("ABCDEFGHIJKLMNOPQ"));

		verifyTree(root, 6);
	}

	@Test
	public void checkIndex() {
		final TreeLeaf<String, ?>[] nodes = IntStream.range('A', 'Z' + 1)
		        .mapToObj(n -> new String(new char[] { (char) n }))
		        .map(s -> new TreeLeaf<>(s, null))
		        .toArray(TreeLeaf[]::new);
		final Root<String> root = (Root<String>) this.tbld.build(nodes);

		for (int i = 0; i < nodes.length; i++) {
			final TreeLeaf<String, ?> node = root.getLeaf(i);

			assertThat(node, sameInstance(nodes[i]));
		}

		verifyTree(root, 6);
	}

	@Test
	public void checkChain() {
		final TreeLeaf<String, ?>[] nodes = IntStream.range('A', 'A' + 13)
		        .mapToObj(n -> new String(new char[] { (char) n }))
		        .map(s -> new TreeLeaf<>(s, null))
		        .toArray(TreeLeaf[]::new);

		// need to distinguish the trace of the filler nodes somehow
		// use lowercase letters for that
		final StringBuilder sb = new StringBuilder("{"); // next of 'z'
		final Supplier<String> zeroFun = () -> {
			// fillers are initialised in reverse order
			// so need to go from 'z' to 'a'
			final char c = (char) (sb.charAt(0) - 1);

			return sb.delete(0, 1).append(c).toString();
		};

		final TreeBuilder<String> tbld = new TreeBuilder<>(identity(), CONCAT, zeroFun);
		final Root<String> root = (Root<String>) tbld.build(nodes);

		assertThat(root.count(), equalTo(13));

		{
			// http://www.bittorrent.org/beps/bep_0030.html
			final TreeLeaf<String, ?> node = root.getLeaf(8);

			assertThat(node.hash, equalTo("I"));

			final List<String> chain = node.getChain();

			assertThat(chain, hasSize(root.height + 1));

			assertThat(chain.get(0), equalTo("I"));
			assertThat(chain.get(1), equalTo("J"));
			assertThat(chain.get(2), equalTo("KL"));
			assertThat(chain.get(3), equalTo("Mxyz"));
			assertThat(chain.get(4), equalTo("ABCDEFGH"));
			assertThat(chain.get(5), equalTo("ABCDEFGHIJKLMxyz"));
		}

		{
			// what about the last leaf?
			final TreeLeaf<String, ?> node = root.getLeaf(root.count() - 1);

			assertThat(node.hash, equalTo("M"));

			final List<String> chain = node.getChain();

			assertThat(chain, hasSize(root.height + 1));

			assertThat(chain.get(0), equalTo("M"));
			assertThat(chain.get(1), equalTo("x"));
			assertThat(chain.get(2), equalTo("yz"));
			assertThat(chain.get(3), equalTo("IJKL"));
			assertThat(chain.get(4), equalTo("ABCDEFGH"));
			assertThat(chain.get(5), equalTo("ABCDEFGHIJKLMxyz"));
		}

		verifyTree(root, 5);
	}

	private <T> void verifyTree(TreeNode<String> node, int height) {
		assertTrue((node.left == null) == (node.right == null));

		if (node.left != null) {
			assertThat(node.left.parent, sameInstance(node));
			assertThat(node.right.parent, sameInstance(node));

			assertThat(node.height, equalTo(height));

			assertThat(node.left.count() + node.right.count(), equalTo(node.count()));
			assertThat(node.left.hash + node.right.hash, equalTo(node.hash));

			verifyTree(node.left, height - 1);
			verifyTree(node.right, height - 1);
		} else {
			assertThat(node.height, equalTo(1));

			if (node.hash.isEmpty()) {
				assertThat(node.count(), equalTo(0));
			}
		}
	}

}
