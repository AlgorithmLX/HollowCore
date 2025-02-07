package ru.hollowhorizon.hc.client.utils.nbt

import com.google.common.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.modules.*
import net.minecraft.nbt.*
import net.minecraft.util.math.BlockPos
import ru.hollowhorizon.hc.HollowCore
import ru.hollowhorizon.hc.common.capabilities.*
import java.io.InputStream
import java.io.OutputStream

internal val TagModule = SerializersModule {
    polymorphic(INBT::class) {
        subclass(ByteNBT::class, ForByteNBT)
        subclass(ShortNBT::class, ForShortNBT)
        subclass(IntNBT::class, ForIntNBT)
        subclass(LongNBT::class, ForLongNBT)
        subclass(FloatNBT::class, ForFloatNBT)
        subclass(DoubleNBT::class, ForDoubleNBT)
        subclass(StringNBT::class, ForStringNBT)
        subclass(EndNBT::class, ForNbtNull)
        subclass(ByteArrayNBT::class, ForByteArrayNBT)
        subclass(IntArrayNBT::class, ForIntArrayNBT)
        subclass(LongArrayNBT::class, ForLongArrayNBT)
        subclass(ListNBT::class, ForNbtList)
        subclass(CompoundNBT::class, ForCompoundNBT)
    }
    contextual(ForBlockPos)
    contextual(ForResourceLocation)
    contextual(ForSoundEvent)
}

object CapabilityModule {

    @OptIn(InternalSerializationApi::class)
    @Suppress("unchecked_cast")
    fun build() = SerializersModule {
        polymorphic(HollowCapability::class) {
            fun <B: HollowCapability> subclass(c: Class<B>) {
                val klass = c.kotlin
                subclass(klass, klass.serializer())
            }

            HollowCapabilityStorageV2.capabilities.forEach {
                HollowCore.LOGGER.info("Registering capability serializer: {}", it.name)
                subclass(it as Class<HollowCapability>)
            }
        }
    }
}

val MAPPINGS_SERIALIZER by lazy { NBTFormat() }

open class NBTFormat(context: SerializersModule = EmptySerializersModule()) : SerialFormat {
    override val serializersModule = context + TagModule

    companion object Default : NBTFormat(CapabilityModule.build()) {
        init {
            HollowCore.LOGGER.info("Default Serializer loaded!")
        }
    }

    @Serializable
    data class Initializator(val value: String)

    fun init() {
        //Первый вызов сериализатора происходит около 5 секунд, так что лучше сделать это заранее и асинхронно
        runBlocking {
            deserialize<Initializator>(serialize(Initializator("")))
        }
    }

    fun <T> serialize(serializer: SerializationStrategy<T>, obj: T): INBT {
        return writeNbt(obj, serializer)
    }

    fun <T> deserialize(deserializer: DeserializationStrategy<T>, tag: INBT): T {
        return readNbt(tag, deserializer)
    }
}

internal const val NbtFormatNull = 1.toByte()

@OptIn(ExperimentalSerializationApi::class)
internal inline fun <T, R1 : T, R2 : T> selectMapMode(
    mapDescriptor: SerialDescriptor,
    ifMap: () -> R1,
    ifList: () -> R2,
): T {
    val keyDescriptor = mapDescriptor.getElementDescriptor(0)
    val keyKind = keyDescriptor.kind
    return if (keyKind is PrimitiveKind || keyKind == SerialKind.ENUM) {
        ifMap()
    } else {
        ifList()
    }
}

fun INBT.save(stream: OutputStream) {
    if (this is CompoundNBT) {
        CompressedStreamTools.writeCompressed(this, stream)
    } else {
        CompressedStreamTools.writeCompressed(CompoundNBT().apply { put("nbt", this) }, stream)
    }
}

fun InputStream.loadAsNBT(): INBT {
    return CompressedStreamTools.readCompressed(this)
}

inline fun <reified T> NBTFormat.serialize(value: T): INBT {
    return serialize(serializersModule.serializer(), value)
}

@Suppress("UnstableApiUsage")
@OptIn(ExperimentalSerializationApi::class)
fun <T : Any> NBTFormat.serializeNoInline(value: T, cl: Class<T>): INBT {
    val typeToken = TypeToken.of(cl)
    return serialize(serializersModule.serializer(typeToken.type), value)
}

inline fun <reified T> NBTFormat.deserialize(tag: INBT): T {
    return deserialize(serializersModule.serializer(), tag)
}

@Suppress("UnstableApiUsage", "UNCHECKED_CAST")
@OptIn(ExperimentalSerializationApi::class)
fun <T : Any> NBTFormat.deserializeNoInline(tag: INBT, cl: Class<out T>): T {
    val typeToken = TypeToken.of(cl)
    return deserialize(serializersModule.serializer(typeToken.type), tag) as T
}

@OptIn(ExperimentalSerializationApi::class)
internal fun compoundTagInvalidKeyKind(keyDescriptor: SerialDescriptor) = IllegalStateException(
    "Value of type ${keyDescriptor.serialName} can't be used in a compound tag as map key. " +
            "It should have either primitive or enum kind, but its kind is ${keyDescriptor.kind}."
)

