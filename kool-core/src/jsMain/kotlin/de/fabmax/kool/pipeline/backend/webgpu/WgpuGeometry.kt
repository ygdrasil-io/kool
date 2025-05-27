package de.fabmax.kool.pipeline.backend.webgpu

import de.fabmax.kool.pipeline.backend.GpuGeometry
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.util.BaseReleasable
import de.fabmax.kool.util.checkIsNotReleased
import io.ygdrasil.webgpu.WGPUBuffer

class WgpuGeometry(val mesh: Mesh, val backend: RenderBackendWebGpu) : BaseReleasable(), GpuGeometry {
    private val device: GPUDevice get() = backend.device

    private val createdIndexBuffer: WgpuGrowingBuffer
    private val createdFloatBuffer: WgpuGrowingBuffer?
    private val createdIntBuffer: WgpuGrowingBuffer?

    val indexBuffer: WGPUBuffer get() = createdIndexBuffer.buffer.oldBuffer
    val floatBuffer: WGPUBuffer? get() = createdFloatBuffer?.buffer?.oldBuffer
    val intBuffer: WGPUBuffer? get() = createdIntBuffer?.buffer?.oldBuffer

    private var isNewlyCreated = true

    init {
        val geom = mesh.geometry
        createdIndexBuffer = WgpuGrowingBuffer(backend, "${mesh.name} index data", 4L * geom.numIndices, GPUBufferUsage.INDEX or GPUBufferUsage.COPY_DST)
        createdFloatBuffer = if (geom.byteStrideF == 0) null else {
            WgpuGrowingBuffer(backend, "${mesh.name} vertex float data", geom.byteStrideF * geom.numVertices.toLong())
        }
        createdIntBuffer = if (geom.byteStrideI == 0) null else {
            WgpuGrowingBuffer(backend, "${mesh.name} vertex int data", geom.byteStrideI * geom.numVertices.toLong())
        }
    }

    fun checkBuffers() {
        checkIsNotReleased()

        val geometry = mesh.geometry
        if (!geometry.isBatchUpdate && (geometry.hasChanged || isNewlyCreated)) {
            createdIndexBuffer.writeData(geometry.indices)
            createdFloatBuffer?.writeData(geometry.dataF)
            createdIntBuffer?.writeData(geometry.dataI)
            geometry.hasChanged = false
        }
        isNewlyCreated = false
    }

    override fun release() {
        super.release()
        createdIndexBuffer.release()
        createdFloatBuffer?.release()
        createdIntBuffer?.release()
    }
}