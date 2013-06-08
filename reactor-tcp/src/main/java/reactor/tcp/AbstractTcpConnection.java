package reactor.tcp;

import reactor.S;
import reactor.Fn;
import reactor.R;
import reactor.core.Stream;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.Function;
import reactor.fn.dispatch.Dispatcher;
import reactor.fn.selector.Selector;
import reactor.fn.tuples.Tuple2;
import reactor.io.Buffer;
import reactor.tcp.encoding.Codec;

import static reactor.Fn.$;

/**
 * @author Jon Brisbin
 */
public abstract class AbstractTcpConnection<IN, OUT> implements TcpConnection<IN, OUT> {

	private final long                     created = System.currentTimeMillis();
	private final Tuple2<Selector, Object> read    = $();
	private final Function<Buffer, IN>  decoder;
	private final Function<OUT, Buffer> encoder;

	protected final Dispatcher  ioDispatcher;
	protected final Reactor     ioReactor;
	protected final Reactor     eventsReactor;
	protected final Environment env;

	protected AbstractTcpConnection(Environment env,
																	Codec<Buffer, IN, OUT> codec,
																	Dispatcher ioDispatcher,
																	Reactor eventsReactor) {
		this.env = env;
		this.ioDispatcher = ioDispatcher;
		this.ioReactor = R.reactor()
											.using(env)
											.using(ioDispatcher)
											.get();
		this.eventsReactor = eventsReactor;
		this.decoder = codec.decoder(read.getT2(), eventsReactor);
		this.encoder = codec.encoder();
	}

	public long getCreated() {
		return created;
	}

	public Object getReadKey() {
		return read.getT2();
	}

	@Override
	public void close() {
		eventsReactor.getConsumerRegistry().unregister(read.getT2());
	}


	@Override
	public Stream<IN> in() {
		Stream<IN> c = S.<IN>defer()
												.using(env)
												.using(eventsReactor.getDispatcher())
												.get();
		consume(c);
		return c;
	}

	@Override
	public Stream<OUT> out() {
		return S.<OUT>defer()
						.using(env)
						.using(eventsReactor.getDispatcher())
						.get();
	}

	@Override
	public TcpConnection<IN, OUT> consume(final Consumer<IN> consumer) {
		eventsReactor.on(read.getT1(), new Consumer<Event<IN>>() {
			@Override
			public void accept(Event<IN> ev) {
				consumer.accept(ev.getData());
			}
		});
		return this;
	}

	@Override
	public Stream<OUT> receive(final Function<IN, OUT> fn) {
		final Stream<OUT> c = out();
		consume(new Consumer<IN>() {
			@Override
			public void accept(IN in) {
				OUT out = fn.apply(in);
				send(c);
				c.accept(out);
			}
		});
		return c;
	}

	@Override
	public TcpConnection<IN, OUT> send(Stream<OUT> data) {
		data.consume(new Consumer<OUT>() {
			@Override
			public void accept(OUT out) {
				send(out, null);
			}
		});
		return this;
	}

	@Override
	public TcpConnection<IN, OUT> send(OUT data) {
		return send(data, null);
	}

	@Override
	public TcpConnection<IN, OUT> send(OUT data, final Consumer<Boolean> onComplete) {
		Fn.schedule(
				new Consumer<OUT>() {
					@Override
					public void accept(OUT data) {
						if (null != encoder) {
							Buffer bytes;
							while (null != (bytes = encoder.apply(data)) && bytes.remaining() > 0) {
								write(bytes, onComplete);
							}
						} else {
							write(data, onComplete);
						}
					}
				},
				data,
				ioReactor
		);
		return this;
	}

	public boolean read(Buffer data) {
		if (null != decoder) {
			while ((null != data.byteBuffer() && data.byteBuffer().hasRemaining()) && null != decoder.apply(data)) {
			}
		} else {
			eventsReactor.notify(read.getT2(), Event.wrap(data));
		}

		return data.remaining() > 0;
	}

	protected abstract void write(Buffer data, Consumer<Boolean> onComplete);

	protected abstract void write(Object data, Consumer<Boolean> onComplete);

}
