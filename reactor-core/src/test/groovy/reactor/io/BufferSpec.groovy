package reactor.io

import spock.lang.Specification

import java.nio.BufferOverflowException
import java.nio.ByteBuffer

/**
 * @author Jon Brisbin
 */
class BufferSpec extends Specification {

	def "A Buffer can be created from a String"() {
		when: "a Buffer is created from a String"
		def buff = Buffer.wrap("Hello World!")

		then: "the Buffer contains the String"
		buff.asString() == "Hello World!"
	}

	def "A fixed-length Buffer can be created from a String"() {
		given: "a fixed-length Buffer is created"
		def buff = Buffer.wrap("Hello", true)

		when: "an attempt is made to append data to the Buffer"
		buff.append(" World!")

		then: "an exception is thrown"
		thrown(BufferOverflowException)
	}

	def "A Buffer prepends a Buffer to an existing Buffer"() {
		given: "an full Buffer"
		def buff = Buffer.wrap("World!", false)

		when: "another Buffer is prepended"
		buff.prepend(Buffer.wrap("Hello "))

		then: "the Buffer was prepended"
		buff.asString() == "Hello World!"
	}

	def "A Buffer prepends Strings to an existing Buffer"() {
		given: "an full Buffer"
		def buff = Buffer.wrap("World!", false)

		when: "a String is prepended"
		buff.prepend("Hello ")

		then: "the String was prepended"
		buff.asString() == "Hello World!"
	}

	def "A Buffer prepends primitives to an existing Buffer"() {
		given: "a Buffer with data"
		def buff = Buffer.wrap("5", false)

		when: "a byte is prepended"
		buff.prepend((byte) 52)

		then: "the byte was prepended"
		buff.asString() == "45"

		when: "a char is prepended"
		buff.prepend((char) 51)

		then: "the char was prepended"
		buff.readChar() == '3'

		when: "an int is prepended"
		buff.prepend(2)

		then: "the int was prepended"
		buff.readInt() == 2

		when: "a long is prepended"
		buff.prepend(1L)

		then: "the long was prepended"
		buff.readLong() == 1L
	}

	def "A Buffer reads and writes Buffers"() {
		given: "an empty Buffer and a full Buffer"
		def buff = new Buffer()
		def fullBuff = Buffer.wrap("Hello World!")

		when: "a Buffer is appended"
		buff.append(fullBuff)

		then: "the Buffer was added"
		buff.position() == 12
		buff.flip().asString() == "Hello World!"
	}

	def "A Buffer reads and writes Strings"() {
		given: "an empty Buffer"
		def buff = new Buffer()

		when: "a String is appended"
		buff.append("Hello World!")

		then: "the String was added"
		buff.position() == 12
		buff.flip().asString() == "Hello World!"
	}

	def "A Buffer reads and writes primitives"() {
		given: "an empty Buffer"
		def buff = new Buffer()

		when: "a byte is appended"
		buff.append((byte) 1)

		then: "the byte was added"
		buff.position() == 1
		buff.flip().read() == 1

		when: "a char is appended"
		buff.clear()
		buff.append((char) 1)

		then: "the char was added"
		buff.position() == 2
		buff.flip().readChar() == (char) 1

		when: "an int is appended"
		buff.clear()
		buff.append(1)

		then: "the int was added"
		buff.position() == 4
		buff.flip().readInt() == 1

		when: "a long is appended"
		buff.clear()
		buff.append(1L)

		then: "the long was added"
		buff.position() == 8
		buff.flip().readLong() == 1L
	}

	def "A Buffer provides position, limit, capacity, and remaining"() {
		given: "a full Buffer"
		def buff = Buffer.wrap("Hello World!")

		when: "limit is checked"
		def limit = buff.limit()

		then: "a limit is provided"
		limit == 12

		when: "capacity is checked"
		def cap = buff.capacity()

		then: "a capacity is provided"
		cap == 12

		when: "position is checked"
		def pos = buff.position()

		then: "a position is provided"
		pos == 0
	}

	def "A Buffer can have first and last positions read"() {
		given: "a full Buffer"
		def buff = Buffer.wrap("Hello World!")

		when: "the first byte is checked"
		def first = buff.first()

		then: "the first byte is an H"
		first == (byte) 72

		when: "the last byte is checked"
		def last = buff.last()

		then: "the last byte is an !"
		last == (byte) 33
	}

	def "A Buffer provides an iterator over each byte"() {
		given: "a full Buffer"
		def buff = Buffer.wrap("Hello World!")
		def count = 0

		when: "the bytes are iterated over"
		buff.each { b ->
			count++
		}

		then: "the count should be 12"
		count == 12
	}

	def "A Buffer can be efficiently substringed"() {
		given: "a full Buffer"
		def buff = Buffer.wrap("Hello World!")

		when: "a substring is extracted"
		def substr = buff.substring(6, 11)

		then: "the substring was extracted"
		substr == "World"
	}

	def "A Buffer is also a ReadableByteChannel and WritableByteChannel"() {
		given: "an empty Buffer as a WritableByteChannel"
		def buff = new Buffer(12, true)

		when: "a ByteBuffer is written into the Buffer"
		def bb = ByteBuffer.wrap("Hello World!".bytes)
		buff.write(bb)

		then: "the Buffer had data written to it"
		buff.flip().asString() == "Hello World!"

		when: "a ByteBuffer is read from the Buffer"
		bb = ByteBuffer.allocate(5)
		buff.read(bb)

		then: "the ByteBuffer has data in it"
		bb.position() == 5
		buff.position() == 5
		bb.flip().get() == (byte) 72
	}

	def "A Buffer can be split into segments based on a delimiter"() {
		given: "a full Buffer"
		def buff = Buffer.wrap("Hello World!\nHello World!\nHello World!")

		when: "the Buffer is split"
		def parts = buff.split(10)

		then: "there are 3 parts"
		parts.size() == 3
	}

	def "A Buffer can be sliced into segments"() {
		given: "a syslog message, buffered"
		def buff = Buffer.wrap("<34>Oct 11 22:14:15 mymachine su: 'su root' failed for lonvick on /dev/pts/8\n")

		when: "positions are assigned and the buffer is sliced"
		def positions = [1, 3, 4, 19, 20, 29, 30] as int[]
		def slices = buff.slice(positions)

		then: "the buffer is sliced"
		slices[0].get().asString() == "34"
		slices[1].get().asString() == "Oct 11 22:14:15"
		slices[2].get().asString() == "mymachine"
		slices[3].get().asString() == "su: 'su root' failed for lonvick on /dev/pts/8\n"
	}

}
