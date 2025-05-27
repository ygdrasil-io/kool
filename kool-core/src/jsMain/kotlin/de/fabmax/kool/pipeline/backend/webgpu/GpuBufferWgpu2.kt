package de.fabmax.kool.pipeline.backend.webgpu

import io.ygdrasil.webgpu.GPUBuffer
import io.ygdrasil.webgpu.WGPUBuffer

fun GPUBuffer.toJs(): WGPUBuffer {
    return (this as io.ygdrasil.webgpu.Buffer).handler
}
