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

package ascelion.merkle.demo;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.adapter.JsonbAdapter;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.codec.binary.Hex.encodeHexString;

@Provider
public class JsonbResolver implements ContextResolver<Jsonb> {

	static class HEXSerializer implements JsonbSerializer<byte[]> {

		@Override
		public void serialize(byte[] obj, JsonGenerator gen, SerializationContext ctx) {
			if (obj != null) {
				gen.write(encodeHexString(obj));
			}
		}
	}

	static class B64Serializer implements JsonbSerializer<byte[]> {

		@Override
		public void serialize(byte[] obj, JsonGenerator gen, SerializationContext ctx) {
			if (obj != null) {
				gen.write(encodeBase64String(obj));
			}
		}
	}

	static class PathAdapter implements JsonbAdapter<Path, String> {

		@Override
		public String adaptToJson(Path obj) throws Exception {
			return obj != null ? obj.toString() : null;
		}

		@Override
		public Path adaptFromJson(String obj) throws Exception {
			return obj == null || obj.isEmpty() ? null : Paths.get(obj);
		}
	}

	@Override
	public Jsonb getContext(Class<?> type) {
		return JsonbBuilder.create(
		        new JsonbConfig()
		                .setProperty(JsonbConfig.FORMATTING, true)
		                .withAdapters(new PathAdapter()));
	}
}
