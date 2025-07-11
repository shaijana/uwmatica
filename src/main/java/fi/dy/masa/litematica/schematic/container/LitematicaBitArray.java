package fi.dy.masa.litematica.schematic.container;

import java.util.Arrays;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.Validate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public class LitematicaBitArray
{
    public static final Codec<LitematicaBitArray> CODEC = RecordCodecBuilder.create(
            inst -> inst.group(
                    PrimitiveCodec.INT.fieldOf("BitsPerEntry").forGetter(get -> get.bitsPerEntry),
                    PrimitiveCodec.LONG.fieldOf("Size").forGetter(get -> get.arraySize),
                    PrimitiveCodec.LONG_STREAM.fieldOf("Array").forGetter(
                            get -> Arrays.stream(get.longArray))
            ).apply(inst, LitematicaBitArray::new)
    );
    public static final PacketCodec<ByteBuf, LitematicaBitArray> PACKET_CODEC = new PacketCodec<>()
    {
        @Override
        public void encode(ByteBuf buf, LitematicaBitArray value)
        {
            PacketCodecs.INTEGER.encode(buf, value.bitsPerEntry);
            PacketCodecs.LONG.encode(buf, value.arraySize);
            PacketCodecs.LONG_ARRAY.encode(buf, value.longArray);
        }

        @Override
        public LitematicaBitArray decode(ByteBuf buf)
        {
            return new LitematicaBitArray(
                    PacketCodecs.INTEGER.decode(buf),
                    PacketCodecs.LONG.decode(buf),
                    PacketCodecs.LONG_ARRAY.decode(buf)
            );
        }
    };
    /** The long array that is used to store the data for this BitArray. */
    private final long[] longArray;
    /** Number of bits a single entry takes up */
    private final int bitsPerEntry;
    /**
     * The maximum value for a single entry. This also works as a bitmask for a single entry.
     * For instance, if bitsPerEntry were 5, this value would be 31 (ie, {@code 0b00011111}).
     */
    private final long maxEntryValue;
    /** Number of entries in this array (<b>not</b> the length of the long array that internally backs this array) */
    private final long arraySize;

    public LitematicaBitArray(int bitsPerEntryIn, long arraySizeIn)
    {
        this(bitsPerEntryIn, arraySizeIn, (long[]) null);
    }

    public LitematicaBitArray(int bitsPerEntryIn, long arraySizeIn, @Nullable long[] longArrayIn)
    {
        Validate.inclusiveBetween(1L, 32L, bitsPerEntryIn);
        this.arraySize = arraySizeIn;
        this.bitsPerEntry = bitsPerEntryIn;
        this.maxEntryValue = (1L << bitsPerEntryIn) - 1L;

        if (longArrayIn != null)
        {
            this.longArray = longArrayIn;
        }
        else
        {
            this.longArray = new long[(int) (roundUp(arraySizeIn * bitsPerEntryIn, 64L) / 64L)];
        }
    }

    private LitematicaBitArray(int bitsPerEntryIn, long arraySizeIn, LongStream longArrayIn)
    {
        this(bitsPerEntryIn, arraySizeIn, longArrayIn.toArray());
    }

    public void setAt(long index, int value)
    {
        //Validate.inclusiveBetween(0L, this.arraySize - 1L, index);
        //Validate.inclusiveBetween(0L, this.maxEntryValue, value);
        long startOffset = index * (long) this.bitsPerEntry;
        int startArrIndex = (int) (startOffset >> 6); // startOffset / 64
        int endArrIndex = (int) (((index + 1L) * (long) this.bitsPerEntry - 1L) >> 6);
        int startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64
        this.longArray[startArrIndex] = this.longArray[startArrIndex] & ~(this.maxEntryValue << startBitOffset) | ((long) value & this.maxEntryValue) << startBitOffset;

        if (startArrIndex != endArrIndex)
        {
            int endOffset = 64 - startBitOffset;
            int j1 = this.bitsPerEntry - endOffset;
            this.longArray[endArrIndex] = this.longArray[endArrIndex] >>> j1 << j1 | ((long) value & this.maxEntryValue) >> endOffset;
        }
    }

    public int getAt(long index)
    {
        //Validate.inclusiveBetween(0L, this.arraySize - 1L, index);
        long startOffset = index * (long) this.bitsPerEntry;
        int startArrIndex = (int) (startOffset >> 6); // startOffset / 64
        int endArrIndex = (int) (((index + 1L) * (long) this.bitsPerEntry - 1L) >> 6);
        int startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64

        if (startArrIndex == endArrIndex)
        {
            return (int) (this.longArray[startArrIndex] >>> startBitOffset & this.maxEntryValue);
        }
        else
        {
            int endOffset = 64 - startBitOffset;
            return (int) ((this.longArray[startArrIndex] >>> startBitOffset | this.longArray[endArrIndex] << endOffset) & this.maxEntryValue);
        }
    }

    public long[] getBackingLongArray()
    {
        return this.longArray;
    }

    public long size()
    {
        return this.arraySize;
    }

    public static long roundUp(long value, long interval)
    {
        if (interval == 0L)
        {
            return 0L;
        }
        else if (value == 0L)
        {
            return interval;
        }
        else
        {
            if (value < 0L)
            {
                interval *= -1L;
            }

            long remainder = value % interval;

            return remainder == 0L ? value : value + interval - remainder;
        }
    }
}
