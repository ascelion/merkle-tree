package ascelion.merkle.help;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import ascelion.merkle.TreeBuilder;
import ascelion.merkle.TreeLeaf;
import ascelion.merkle.TreeRoot;

/**
 * Helper class to build a Merkle tree from an input stream. The resulted tree contains DataSlice as leaves.
 */
public final class DataSlice extends TreeLeaf<byte[], byte[]> {

	/**
	 * Concatenation operator for byte[].
	 */
	static public byte[] concat(byte[] h1, byte[] h2) {
		final byte[] h = Arrays.copyOf(h1, h1.length + h2.length);

		System.arraycopy(h2, 0, h, h1.length, h2.length);

		return h;
	}

	/**
	 * Ensures all bytes are read.
	 */
	private static byte[] readNBytes(InputStream is, int size) throws IOException {
		final byte[] data = new byte[size];
		int read = 0;

		while (size > 0 && (read = is.read(data, data.length - size, size)) > 0) {
			size -= read;
		}
		if (size > 0) {
			return Arrays.copyOf(data, data.length - size);
		} else {
			return data;
		}
	}

	/**
	 * @param size the size of a slice.
	 */
	static public TreeRoot<byte[]> buildTree(TreeBuilder<byte[]> bld, int size, InputStream ist) throws IOException {
		byte[] data;

		while ((data = readNBytes(ist, size)).length > 0) {
			bld.collect(new DataSlice(bld.hashFn.apply(data), data));
		}

		return bld.build();
	}

	private DataSlice(byte[] hash, byte[] content) {
		super(hash, content);
	}

}
