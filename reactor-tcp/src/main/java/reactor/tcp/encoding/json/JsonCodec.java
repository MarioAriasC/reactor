package reactor.tcp.encoding.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.fn.Event;
import reactor.fn.Function;
import reactor.fn.Observable;
import reactor.io.Buffer;
import reactor.tcp.encoding.Codec;

import java.io.IOException;

/**
 * @author Jon Brisbin
 */
public class JsonCodec<IN, OUT> implements Codec<Buffer, IN, OUT> {

	private final Class<IN>    inputType;
	private final boolean      inputJsonNode;
	private final Class<OUT>   outputType;
	private final boolean      outputJsonNode;
	private final ObjectMapper mapper;

	public JsonCodec() {
		this(null, null, null);
	}

	public JsonCodec(Module customModule) {
		this(null, null, customModule);
	}

	public JsonCodec(Class<IN> inputType, Class<OUT> outputType) {
		this(inputType, outputType, null);
	}

	@SuppressWarnings("unchecked")
	public JsonCodec(Class<IN> inputType, Class<OUT> outputType, Module customModule) {
		this.inputType = (null == inputType ? (Class<IN>) JsonNode.class : inputType);
		this.inputJsonNode = JsonNode.class.isAssignableFrom(inputType);
		this.outputType = (null == outputType ? (Class<OUT>) JsonNode.class : outputType);
		this.outputJsonNode = JsonNode.class.isAssignableFrom(outputType);

		this.mapper = new ObjectMapper();
		if (null != customModule) {
			this.mapper.registerModule(customModule);
		}
	}

	@Override
	public Function<Buffer, IN> decoder(Object notifyKey, Observable observable) {
		return new JsonDecoder(notifyKey, observable);
	}

	@Override
	public Function<OUT, Buffer> encoder() {
		return new JsonEncoder();
	}

	private class JsonDecoder implements Function<Buffer, IN> {
		private final Object     notifyKey;
		private final Observable observable;

		private JsonDecoder(Object notifyKey, Observable observable) {
			this.notifyKey = notifyKey;
			this.observable = observable;
		}

		@SuppressWarnings("unchecked")
		@Override
		public IN apply(Buffer buffer) {
			IN in;
			try {
				if (JsonNode.class.isAssignableFrom(inputType)) {
					in = (IN) mapper.readTree(buffer.inputStream());
				} else {
					in = mapper.readValue(buffer.inputStream(), inputType);
				}
				if (null != notifyKey && null != observable) {
					observable.notify(notifyKey, Event.wrap(in));
					return null;
				} else {
					return in;
				}
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private class JsonEncoder implements Function<OUT, Buffer> {
		@Override
		public Buffer apply(OUT out) {
			try {
				return Buffer.wrap(mapper.writeValueAsBytes(out));
			} catch (JsonProcessingException e) {
				throw new IllegalStateException(e);
			}
		}
	}

}
