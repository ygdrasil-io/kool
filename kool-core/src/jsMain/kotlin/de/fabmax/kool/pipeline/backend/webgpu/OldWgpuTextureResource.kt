package de.fabmax.kool.pipeline.backend.webgpu

import de.fabmax.kool.pipeline.backend.wgpu.WgpuTextureResource
import io.ygdrasil.webgpu.GPUExtent3D
import io.ygdrasil.webgpu.GPUOrigin3D
import io.ygdrasil.webgpu.Texture
import io.ygdrasil.webgpu.toFlagInt


val OldWgpuTextureResource.oldImageInfo
    get() = imageInfo.toJs()
val OldWgpuTextureResource.oldGpuTexture
    get() = gpuTexture.toJs()

typealias OldWgpuTextureResource = WgpuTextureResource

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

fun GPUOrigin3D.toJs(): IntArray {
    return intArrayOf(x.toInt(), y.toInt(), z.toInt())
}


fun io.ygdrasil.webgpu.GPUTexture.toJs(): GPUTexture = (this as Texture).handler.asDynamic()

