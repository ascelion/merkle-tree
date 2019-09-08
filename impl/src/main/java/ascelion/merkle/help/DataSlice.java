package ascelion.merkle.help;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.Arrays;

import ascelion.merkle.TreeBuilder;
import ascelion.merkle.TreeLeaf;
import ascelion.merkle.TreeRoot;

/**
 * Helper class to build a Merkle tree from data stream. The resulted tree contains instances of this class as leaves.
 */
public final class DataSlice extends TreeLeaf<byte[], byte[]> {

	/**
	 * Concatenation operator for byte[].
	 *
	 * @param b1 first parameter.
	 * @param b2 second parameter.
	 * @return the resulted array.
	 */
	static public byte[] concat(byte[] b1, byte[] b2) {
		final byte[] h = Arrays.copyOf(b1, b1.length + b2.length);

		System.arraycopy(b2, 0, h, b1.length, b2.length);

		return h;
	}

	/**
	 * Helper method to construct a Merkle tree from an input stream.
	 *
	 * @param bld  the builder used to create the tree.
	 * @param size the size of a slice.
	 * @param ist  the input stream.
	 * @return the tree
	 * @throws IOException whether an I/O error occurs.
	 */
	static public TreeRoot<byte[]> buildTree(TreeBuilder<byte[]> bld, int size, InputStream ist) throws IOException {
		byte[] data;

		while ((data = readNBytes(ist, size)).length > 0) {
			bld.collect(new DataSlice(bld.hash(data), data));
		}

		return bld.build();
	}

	/**
	 * Helper method to construct a Merkle tree from a byte channel.
	 *
	 * @param bld  the builder used to create the tree.
	 * @param size the size of a slice.
	 * @param chn  the input channel.
	 * @return the tree
	 * @throws IOException whether an I/O error occurs.
	 */
	static public TreeRoot<byte[]> buildTree(TreeBuilder<byte[]> bld, int size, ByteChannel chn) throws IOException {
		final ByteBuffer buf = ByteBuffer.allocateDirect(size);

		while (chn.read(buf) > 0) {
			buf.flip();

			final byte[] data = new byte[buf.limit()];

			buf.get(data, 0, data.length);

			bld.collect(new DataSlice(bld.hash(data), data));

			buf.clear();
		}

		return bld.build();
	}

	// ensures all bytes are read...
	private static byte[] readNBytes(InputStream ist, int size) throws IOException {
		final byte[] data = new byte[size];
		int read = 0;

		while (size > 0 && (read = ist.read(data, data.length - size, size)) > 0) {
			size -= read;
		}
		if (size > 0) {
			return Arrays.copyOf(data, data.length - size);
		} else {
			return data;
		}
	}

	private DataSlice(byte[] hash, byte[] content) {
		super(hash, content);
	}

}
