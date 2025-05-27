package de.fabmax.kool.pipeline.backend.wgpu

import de.fabmax.kool.pipeline.BufferedImageData
import de.fabmax.kool.pipeline.ImageData
import io.ygdrasil.webgpu.Extent3D
import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.GPUOrigin3D
import io.ygdrasil.webgpu.GPUTexture
import io.ygdrasil.webgpu.TexelCopyTextureInfo

internal actual fun copyNativeTextureData(
    src: ImageData,
    dst: GPUTexture,
    size: Extent3D,
    dstOrigin: GPUOrigin3D,
    device: GPUDevice
) {
    when (src) {
        is BufferedImageData -> {
            src.data.asArrayBuffer { arrayBuffer ->
                device.queue.writeTexture(
                    data = arrayBuffer,
                    destination = TexelCopyTextureInfo(dst, origin = dstOrigin),
                    dataLayout = src.gpuImageDataLayout,
                    size = size
                )
            }
        }
        // Not yet supported on Android
        else -> error("Not implemented: ${src::class.simpleName}")
    }

}