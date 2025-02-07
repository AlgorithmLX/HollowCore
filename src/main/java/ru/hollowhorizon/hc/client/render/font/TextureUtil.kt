package ru.hollowhorizon.hc.client.render.font

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.GLAllocation
import org.lwjgl.opengl.GL11
import java.awt.image.BufferedImage

object TextureUtil {
    private val DATA_BUFFER = GLAllocation.createByteBuffer(16777216).asIntBuffer()

    fun uploadTextureImage(textureId: Int, texture: BufferedImage): Int {
        return uploadTextureImageAllocate(textureId, texture, false, false)
    }

    fun uploadTextureImageAllocate(textureId: Int, texture: BufferedImage, blur: Boolean, clamp: Boolean): Int {
        allocateTexture(textureId, texture.width, texture.height)
        return uploadTextureImageSub(textureId, texture, 0, 0, blur, clamp)
    }

    fun allocateTexture(textureId: Int, width: Int, height: Int) {
        allocateTextureImpl(textureId, 0, width, height)
    }

    fun allocateTextureImpl(glTextureId: Int, mipmapLevels: Int, width: Int, height: Int) {
        RenderSystem.assertThread { RenderSystem.isOnRenderThreadOrInit() }
        RenderSystem.deleteTexture(glTextureId)
        GlStateManager._bindTexture(glTextureId)
        if (mipmapLevels >= 0) {
            RenderSystem.texParameter(3553, 33085, mipmapLevels)
            RenderSystem.texParameter(3553, 33082, 0)
            RenderSystem.texParameter(3553, 33083, mipmapLevels)
            GlStateManager._texParameter(3553, 34049, 0.0f)
        }
        for (i in 0..mipmapLevels) {
            GlStateManager._texImage2D(3553, i, 6408, width shr i, height shr i, 0, 32993, 33639, null)
        }
    }

    fun uploadTextureImageSub(textureId: Int, img: BufferedImage, x: Int, y: Int, blur: Boolean, clamp: Boolean): Int {
        GlStateManager._bindTexture(textureId)
        uploadTextureImageSubImpl(img, x, y, blur, clamp)
        return textureId
    }

    private fun uploadTextureImageSubImpl(
        img: BufferedImage,
        x: Int,
        y: Int,
        blur: Boolean,
        clamp: Boolean,
    ) {
        val i = img.width
        val j = img.height
        val k = 4194304 / i
        val aint = IntArray(k * i)
        setTextureBlurred(blur)
        setTextureClamped(clamp)
        var l = 0
        while (l < i * j) {
            val i1 = l / i
            val j1 = Math.min(k, j - i1)
            val k1 = i * j1
            img.getRGB(0, i1, i, j1, aint, 0, i)
            copyToBuffer(aint, k1)
            GL11.glTexSubImage2D(3553, 0, x, y + i1, i, j1, 32993, 33639, DATA_BUFFER)
            l += i * k
        }
    }

    private fun copyToBuffer(p_110990_0_: IntArray, p_110990_1_: Int) {
        copyToBufferPos(p_110990_0_, 0, p_110990_1_)
    }

    private fun copyToBufferPos(p_110994_0_: IntArray, p_110994_1_: Int, p_110994_2_: Int) {
        DATA_BUFFER.clear()
        DATA_BUFFER.put(p_110994_0_, p_110994_1_, p_110994_2_)
        DATA_BUFFER.position(0).limit(p_110994_2_)
    }

    private fun setTextureClamped(p_110997_0_: Boolean) {
        if (p_110997_0_) {
            RenderSystem.texParameter(3553, 10242, 10496)
            RenderSystem.texParameter(3553, 10243, 10496)
        } else {
            RenderSystem.texParameter(3553, 10242, 10497)
            RenderSystem.texParameter(3553, 10243, 10497)
        }
    }

    private fun setTextureBlurred(p_147951_0_: Boolean) {
        setTextureBlurMipmap(p_147951_0_, false)
    }

    private fun setTextureBlurMipmap(p_147954_0_: Boolean, p_147954_1_: Boolean) {
        if (p_147954_0_) {
            RenderSystem.texParameter(3553, 10241, if (p_147954_1_) 9987 else 9729)
            RenderSystem.texParameter(3553, 10240, 9729)
        } else {
            RenderSystem.texParameter(3553, 10241, if (p_147954_1_) 9986 else 9728)
            RenderSystem.texParameter(3553, 10240, 9728)
        }
    }
}