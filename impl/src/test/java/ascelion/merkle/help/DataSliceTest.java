package ascelion.merkle.help;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import ascelion.merkle.TreeBuilder;
import ascelion.merkle.TreeLeaf;
import ascelion.merkle.TreeRoot;

import static java.lang.Thread.currentThread;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

public class DataSliceTest {

	@Test
	public void buildFromStream() throws IOException {
		final InputStream ist = currentThread()
		        .getContextClassLoader()
		        .getResourceAsStream("top-background-trn.png");

		assertThat(ist, notNullValue());

		final TreeBuilder<byte[]> tbld = new TreeBuilder<>(DigestUtils::sha256, DataSlice::concat, new byte[0]);
		final TreeRoot<byte[]> root = DataSlice.buildTree(tbld, 512, ist);

		System.out.printf("ROOT: %s\n", encodeHexString(root.hash()));

		for (int k = 0; k < root.count(); k++) {
			final TreeLeaf<byte[], ?> leaf = root.getLeaf(k);
			final List<byte[]> chain = leaf.getChain();

			System.out.printf("%4d: %s\n", k, encodeHexString(leaf.hash()));

			chain.forEach(p -> System.out.printf("          %s\n", encodeHexString(p)));

			assertThat(leaf.hash(), equalTo(chain.get(0)));
			assertThat(root.hash(), equalTo(chain.get(chain.size() - 1)));

			assertThat(chain.size(), equalTo(root.height() + 1));

			assertThat(tbld.isValid(leaf.getChain(), k, Arrays::equals), is(true));
		}
	}

}
