package reactor.tcp.encoding.syslog;

import reactor.fn.Event;
import reactor.fn.Function;
import reactor.fn.Observable;
import reactor.fn.Supplier;
import reactor.fn.cache.Cache;
import reactor.fn.cache.LoadingCache;
import reactor.io.Buffer;
import reactor.tcp.encoding.Codec;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Jon Brisbin
 */
public class SyslogCodec implements Codec<Buffer, SyslogMessage, Void> {

	private static final int MAXIMUM_SEVERITY = 7;
	private static final int MAXIMUM_FACILITY = 23;
	private static final int MINIMUM_PRI      = 0;
	private static final int MAXIMUM_PRI      = (MAXIMUM_FACILITY * 8) + MAXIMUM_SEVERITY;
	private static final int DEFAULT_PRI      = 13;

	private static final Function<Void, Buffer> ENDCODER = new Function<Void, Buffer>() {
		@Override
		public Buffer apply(Void v) {
			return null;
		}
	};

	private final Cache<List<Buffer.View>> viewsCache;

	public SyslogCodec() {
		this.viewsCache = new LoadingCache<List<Buffer.View>>(
				new Supplier<List<Buffer.View>>() {
					@Override
					public List<Buffer.View> get() {
						return new ArrayList<Buffer.View>();
					}
				},
				64,
				500
		);
	}

	@Override
	public Function<Buffer, SyslogMessage> decoder(Object notifyKey, Observable observable) {
		return new SyslogMessageDecoder(notifyKey, observable);
	}

	@Override
	public Function<Void, Buffer> encoder() {
		return ENDCODER;
	}

	private class SyslogMessageDecoder implements Function<Buffer, SyslogMessage> {
		private final Calendar cal  = Calendar.getInstance();
		private final int      year = cal.get(Calendar.YEAR);
		private final Object      notifyKey;
		private final Observable  observable;
		private       Buffer.View remainder;

		private SyslogMessageDecoder(Object notifyKey, Observable observable) {
			this.notifyKey = notifyKey;
			this.observable = observable;
		}

		@Override
		public SyslogMessage apply(Buffer buffer) {
			return parse(buffer);
		}

		private SyslogMessage parse(Buffer buffer) {
			String line = null;
			if (null != remainder) {
				line = remainder.get().asString();
			}

			int start = 0;
			for (Buffer.View view : buffer.split('\n', false)) {
				Buffer b = view.get();
				if (b.last() != '\n') {
					remainder = view;
					return null;
				}
				String s = b.asString();
				if (null != line) {
					line += s;
				} else {
					line = s;
				}
				if (line.isEmpty()) {
					continue;
				}

				int priority = DEFAULT_PRI;
				int facility = priority / 8;
				int severity = priority % 8;

				int priStart = line.indexOf('<', start);
				int priEnd = line.indexOf('>', start + 1);
				if (priStart == 0) {
					int pri = Buffer.parseInt(b, 1, priEnd);
					if (pri >= MINIMUM_PRI && pri <= MAXIMUM_PRI) {
						priority = pri;
						facility = priority / 8;
						severity = priority % 8;
					}
					start = 4;
				}

				Date tstamp = parseRfc3414Date(b, start, start + 15);
				String host = null;
				if (null != tstamp) {
					start += 16;
					int end = line.indexOf(' ', start);
					host = line.substring(start, end);
					if (null != host) {
						start += host.length() + 1;
					}
				}

				String msg = line.substring(start);

				SyslogMessage syslogMsg = new SyslogMessage(line,
																										priority,
																										facility,
																										severity,
																										null,
																										host,
																										msg);
				if (null != observable) {
					Event<SyslogMessage> ev = Event.wrap(syslogMsg);
					observable.notify(notifyKey, ev);
				} else {
					return syslogMsg;
				}

				line = null;
				start = 0;
			}

			return null;
		}

		private Date parseRfc3414Date(Buffer b, int start, int end) {
			b.snapshot();

			b.byteBuffer().limit(end);
			b.byteBuffer().position(start);

			int month = -1;
			int day = -1;
			int hr = -1;
			int min = -1;
			int sec = -1;

			switch (b.read()) {
				case 'A': // Apr, Aug
					switch (b.read()) {
						case 'p':
							month = Calendar.APRIL;
							b.read();
							break;
						default:
							month = Calendar.AUGUST;
							b.read();
					}
					break;
				case 'D': // Dec
					month = Calendar.DECEMBER;
					b.read();
					b.read();
					break;
				case 'F': // Feb
					month = Calendar.FEBRUARY;
					b.read();
					b.read();
					break;
				case 'J': // Jan, Jun, Jul
					switch (b.read()) {
						case 'a':
							month = Calendar.JANUARY;
							b.read();
							break;
						default:
							switch (b.read()) {
								case 'n':
									month = Calendar.JUNE;
									break;
								default:
									month = Calendar.JULY;
							}
					}
					break;
				case 'M': // Mar, May
					b.read();
					switch (b.read()) {
						case 'r':
							month = Calendar.MARCH;
							break;
						default:
							month = Calendar.MAY;
					}
					break;
				case 'N': // Nov
					month = Calendar.NOVEMBER;
					b.read();
					b.read();
					break;
				case 'O': // Oct
					month = Calendar.OCTOBER;
					b.read();
					b.read();
					break;
				case 'S': // Sep
					month = Calendar.SEPTEMBER;
					b.read();
					b.read();
					break;
				default:
					return null;
			}

			while (b.read() == ' ') {
			}

			int dayStart = b.position() - 1;
			while (b.read() != ' ') {
			}
			int dayEnd = b.position() - 1;
			day = Buffer.parseInt(b, dayStart, dayEnd);

			while (b.read() == ' ') {
			}

			int timeStart = b.position() - 1;
			hr = Buffer.parseInt(b, timeStart, timeStart + 2);
			min = Buffer.parseInt(b, timeStart + 3, timeStart + 5);
			sec = Buffer.parseInt(b, timeStart + 6, timeStart + 8);

			try {
				if (month < 0 || day < 0 || hr < 0 || min < 0 || sec < 0) {
					return null;
				} else {
					cal.set(year, month, day, hr, min, sec);
					return cal.getTime();
				}
			} finally {
				b.reset();
			}
		}
	}

}
