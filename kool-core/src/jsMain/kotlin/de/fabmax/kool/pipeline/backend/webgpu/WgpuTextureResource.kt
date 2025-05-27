package de.fabmax.kool.pipeline.backend.webgpu

import de.fabmax.kool.pipeline.backend.GpuTexture
import de.fabmax.kool.pipeline.backend.stats.TextureInfo
import de.fabmax.kool.util.BaseReleasable
import io.ygdrasil.webgpu.GPUExtent3D
import io.ygdrasil.webgpu.Texture
import io.ygdrasil.webgpu.toFlagInt

class WgpuTextureResource(
    val newImageInfo: io.ygdrasil.webgpu.GPUTextureDescriptor,
    val newGpuTexture: io.ygdrasil.webgpu.GPUTexture
) : BaseReleasable(), GpuTexture {

    val imageInfo: GPUTextureDescriptor = newImageInfo.toJs()
    val gpuTexture: GPUTexture = newGpuTexture.toJs()

    override val width: Int get() = gpuTexture.width
    override val height: Int get() = gpuTexture.height
    override val depth: Int get() = gpuTexture.depthOrArrayLayers

    private val textureInfo = TextureInfo(
        texture = null,
        size = (gpuTexture.width * gpuTexture.height * gpuTexture.depthOrArrayLayers * imageInfo.bytesPerPx * imageInfo.mipMapFactor).toLong()
    )

    private val GPUTextureDescriptor.bytesPerPx: Int
        get() {
            val channels = when {
                "rgba" in format -> 4
                "rg" in format -> 2
                "r" in format -> 1
                else -> 1
            }
            return when {
                "8" in format -> 1 * channels
                "16" in format -> 2 * channels
                "32" in format -> 4 * channels
                else -> 4
            }
        }

    private val GPUTextureDescriptor.mipMapFactor: Double
        get() = if (mipLevelCount > 1) {
            1.333
        } else 1.0

    override fun release() {
        super.release()
        gpuTexture.destroy()
        textureInfo.deleted()
    }
}

private fun io.ygdrasil.webgpu.GPUTextureDescriptor.toJs(): GPUTextureDescriptor {
    return GPUTextureDescriptor(
        size.toJs(),
        format.value,
        usage.toFlagInt(),
        label,
        mipLevelCount.toInt(),
        sampleCount.toInt(),
        dimension.value,
        viewFormats.map { it.value }.toTypedArray(),
    )
}

fun GPUExtent3D.toJs(): IntArray {
    return intArrayOf(width.toInt(), height.toInt(), depthOrArrayLayers.toInt())
}


fun io.ygdrasil.webgpu.GPUTexture.toJs(): GPUTexture = (this as Texture).handler.asDynamic()