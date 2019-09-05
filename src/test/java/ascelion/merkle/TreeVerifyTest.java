package ascelion.merkle;

import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class TreeVerifyTest {

	@Test
	public void verifyWithIdentity() {
		final BinaryOperator<String> concat = (s1, s2) -> s1 + s2;
		final TreeBuilder<String> tbld = new TreeBuilder<>(UnaryOperator.identity(), concat, "");
		final TreeLeaf<String, ?>[] leaves = IntStream.range('A', 'Z' + 1)
		        .mapToObj(n -> new String(new char[] { (char) n }))
		        .map(s -> new TreeLeaf<>(s, null))
		        .toArray(TreeLeaf[]::new);

		final TreeRoot<String> root = tbld.build(leaves);

		for (int i = 0; i < leaves.length; i++) {
			final TreeLeaf<String, ?> leaf = leaves[i];
			final List<String> chain = leaf.getChain();

			assertThat(leaf.hash(), equalTo(chain.get(0)));
			assertThat(root.hash(), equalTo(chain.get(chain.size() - 1)));

			assertThat(chain.size(), equalTo(root.height() + 1));

			assertThat(leaf.toString(), tbld.isValid(chain, i, Objects::equals), is(true));
		}
	}
}
