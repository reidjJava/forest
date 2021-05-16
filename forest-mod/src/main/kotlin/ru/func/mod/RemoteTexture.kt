package ru.func.mod

import dev.xdark.clientapi.resource.ResourceLocation
import ru.cristalix.uiengine.UIEngine
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO

data class RemoteTexture(val RC: ResourceLocation, val sha1: String)

fun loadTexture(vararg images: RemoteTexture) {
    val cacheDir = Paths.get("$NAMESPACE/")
    if (!Files.exists(cacheDir))
        Files.createDirectory(cacheDir)
    images.forEach { it ->
        val path = cacheDir.resolve(it.sha1)

        val image = try {
            Files.newInputStream(path).use {
                ImageIO.read(it)
            }
        } catch (ex: IOException) {
            try {
                val url = URL("$FILE_STORE${it.RC.path}")
                val image = ImageIO.read(url);
                val baos = ByteArrayOutputStream()
                ImageIO.write(image, "png", baos)
                baos.flush()
                val imageInByte = baos.toByteArray()
                baos.close()
                Files.write((Paths.get(cacheDir.toString() + "/" + it.sha1)), imageInByte)
                image
            } catch (e: IOException) {
                null
            }
        }
        UIEngine.clientApi.renderEngine()
            .loadTexture(it.RC, UIEngine.clientApi.renderEngine().newImageTexture(image!!, false, false))
    }
}