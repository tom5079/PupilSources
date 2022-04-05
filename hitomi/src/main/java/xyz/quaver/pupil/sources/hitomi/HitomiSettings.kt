package xyz.quaver.pupil.sources.hitomi

import android.app.Application
import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import xyz.quaver.pupil.sources.hitomi.proto.HitomiSettings
import java.io.InputStream
import java.io.OutputStream

object SettingsSerializer: Serializer<HitomiSettings> {
    override val defaultValue = HitomiSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): HitomiSettings {
        try {
            return HitomiSettings.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", e)
        }
    }

    override suspend fun writeTo(t: HitomiSettings, output: OutputStream) = t.writeTo(output)
}

val Application.hitomiSettingsDataStore: DataStore<HitomiSettings> by dataStore(
    fileName = "hitomi_la_settings.proto",
    serializer = SettingsSerializer
)