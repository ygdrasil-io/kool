package de.fabmax.kool.pipeline.backend.webgpu

import de.fabmax.kool.pipeline.backend.wgpu.GpuBufferWgpu
import de.fabmax.kool.util.*
import io.ygdrasil.webgpu.GPUBuffer
import io.ygdrasil.webgpu.WGPUBuffer

internal class WgpuGrowingBuffer(
    val backend: RenderBackendWebGpu,
    val label: String,
    size: Long,
    val usage: Int = GPUBufferUsage.VERTEX or GPUBufferUsage.COPY_DST
) : BaseReleasable() {
    private val device: GPUDevice get() = backend.oldDevice

    var size: Long = size
        private set
    var buffer: GpuBufferWgpu = makeBuffer(size)
        private set

    fun writeData(data: Float32Buffer) {
        checkSize(data.limit * 4L)
        device.queue.writeBuffer(
            buffer = buffer.buffer.toJs(),
            bufferOffset = 0L,
            data = (data as Float32BufferImpl).buffer,
            dataOffset = 0L,
            size = data.limit.toLong()
        )
    }

    fun writeData(data: Int32Buffer) {
        checkSize(data.limit * 4L)
        device.queue.writeBuffer(
            buffer = buffer.buffer.toJs(),
            bufferOffset = 0L,
            data = (data as Int32BufferImpl).buffer,
            dataOffset = 0L,
            size = data.limit.toLong()
        )
    }

    private fun checkSize(required: Long) {
        if (required > size) {
            buffer.release()
            size = required
            buffer = makeBuffer(required)
        }
    }

    private fun makeBuffer(size: Long) = backend.createBuffer(
        GPUBufferDescriptor(
            label = label,
            size = size,
            usage = usage
        ),
        label
    )
}

fun GPUBuffer.toJs(): WGPUBuffer {
    return (this as io.ygdrasil.webgpu.Buffer).handler
}
