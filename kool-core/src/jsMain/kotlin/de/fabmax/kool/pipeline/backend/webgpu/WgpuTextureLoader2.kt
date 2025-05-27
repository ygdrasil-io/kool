package de.fabmax.kool.pipeline.backend.webgpu

import de.fabmax.kool.math.float32ToFloat16
import de.fabmax.kool.math.numMipLevels
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.pipeline.backend.gl.pxSize
import de.fabmax.kool.pipeline.backend.wgpu.GPUBackend
import de.fabmax.kool.pipeline.backend.wgpu.WgpuTextureLoader
import de.fabmax.kool.pipeline.backend.wgpu.WgpuTextureResource
import de.fabmax.kool.pipeline.backend.wgpu.copyNativeTextureData
import de.fabmax.kool.platform.ImageTextureData
import de.fabmax.kool.util.Float32BufferImpl
import de.fabmax.kool.util.Uint16BufferImpl
import de.fabmax.kool.util.Uint8BufferImpl
import de.fabmax.kool.util.logW
import io.ygdrasil.webgpu.Device
import io.ygdrasil.webgpu.Extent3D
import io.ygdrasil.webgpu.GPUOrigin3D
import io.ygdrasil.webgpu.Origin3D
import io.ygdrasil.webgpu.TextureDescriptor
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set


//internal typealias WgpuTextureLoader2 = WgpuTextureLoader

internal class WgpuTextureLoader2(val backend: GPUBackend) {
    val loader = WgpuTextureLoader(backend)
    private val loadedTextures = loader.loadedTextures

    private val device: GPUDevice get() = (backend.device as Device).handler.asDynamic()
    private val multiSampledDepthTextureCopy = MultiSampledDepthTextureCopy()
    val mipmapGenerator = loader.mipmapGenerator

    fun loadTexture(tex: Texture<*>) {
        val data = checkNotNull(tex.uploadData)
        tex.uploadData = null

        check(tex.format == data.format) {
            "Image data format doesn't match texture format: ${data.format} != ${tex.format}"
        }

        var loaded = loadedTextures[data.id]
        if (loaded != null && loaded.isReleased) { loadedTextures -= data.id }

        loaded = when {
            tex is Texture1d && data is ImageData1d -> loadTexture1d(tex, data)
            tex is Texture2d && data is ImageData2d -> loadTexture2d(tex, data)
            tex is Texture3d && data is ImageData3d -> loadTexture3d(tex, data)
            tex is TextureCube && data is ImageDataCube -> loadTextureCube(tex, data)
            tex is Texture2dArray && data is ImageData3d -> loadTexture2dArray(tex, data)
            tex is TextureCubeArray && data is ImageDataCubeArray -> loadTextureCubeArray(tex, data)
            else -> error("Invalid texture / image data combination: ${tex::class.simpleName} / ${data::class.simpleName}")
        }
        tex.gpuTexture?.release()
        tex.gpuTexture = loaded
    }

    private fun loadTexture1d(tex: Texture1d, data: ImageData1d): OldWgpuTextureResource {
        val size = Extent3D(data.width.toUInt())
        val usage = setOf(io.ygdrasil.webgpu.GPUTextureUsage.CopyDst, io.ygdrasil.webgpu.GPUTextureUsage.TextureBinding)
        if (tex.mipMapping.isMipMapped) {
            logW { "generateMipMaps requested for Texture1d ${tex.name}: not supported on WebGPU" }
        }

        val texDesc = TextureDescriptor(
            size = size,
            format = io.ygdrasil.webgpu.GPUTextureFormat.of(data.format.wgpu.enumValue)!!,
            dimension = io.ygdrasil.webgpu.GPUTextureDimension.of(GPUTextureDimension.texture1d.enumValue)!!,
            usage = usage
        )

        val gpuTex = backend.createTexture(texDesc)
        copyTextureData(data, gpuTex.gpuTexture, size)
        return gpuTex
    }

    private fun loadTexture2d(tex: Texture2d, data: ImageData2d): OldWgpuTextureResource {
        val size = Extent3D(data.width.toUInt(), data.height.toUInt())
        val usage = setOf(io.ygdrasil.webgpu.GPUTextureUsage.CopyDst, io.ygdrasil.webgpu.GPUTextureUsage.TextureBinding, io.ygdrasil.webgpu.GPUTextureUsage.RenderAttachment)
        val levels = tex.mipMapping.numLevels(data.width, data.height)
        val texDesc = TextureDescriptor(
            size = size,
            format = io.ygdrasil.webgpu.GPUTextureFormat.of(data.format.wgpu.enumValue)!!,
            usage = usage,
            mipLevelCount = levels.toUInt()
        )

        val gpuTex = backend.createTexture(texDesc)
        copyTextureData(data, gpuTex.gpuTexture, size)
        if (tex.mipMapping.isMipMapped) {
            mipmapGenerator.generateMipLevels(gpuTex.imageInfo, gpuTex.gpuTexture)
        }
        return gpuTex
    }

    private fun loadTexture3d(tex: Texture3d, data: ImageData3d): OldWgpuTextureResource {
        val size = Extent3D(data.width.toUInt(), data.height.toUInt(), data.depth.toUInt())
        val usage = setOf(io.ygdrasil.webgpu.GPUTextureUsage.CopyDst, io.ygdrasil.webgpu.GPUTextureUsage.TextureBinding)
        if (tex.mipMapping.isMipMapped) {
            logW { "generateMipMaps requested for Texture3d ${tex.name}: not yet implemented on WebGPU" }
        }

        val texDesc = TextureDescriptor(
            size = size,
            format = io.ygdrasil.webgpu.GPUTextureFormat.of(data.format.wgpu.enumValue)!!,
            usage = usage,
            dimension = io.ygdrasil.webgpu.GPUTextureDimension.of(GPUTextureDimension.texture3d.enumValue)!!,
        )

        val gpuTex = backend.createTexture(texDesc)
        copyTextureData(data, gpuTex.gpuTexture, size)
        return gpuTex
    }

    private fun loadTextureCube(tex: TextureCube, data: ImageDataCube): OldWgpuTextureResource {
        val usage = setOf(io.ygdrasil.webgpu.GPUTextureUsage.CopyDst, io.ygdrasil.webgpu.GPUTextureUsage.TextureBinding, io.ygdrasil.webgpu.GPUTextureUsage.RenderAttachment)
        val levels = tex.mipMapping.numLevels(data.width, data.height)
        val texDesc = TextureDescriptor(
            size = Extent3D(data.width.toUInt(), data.height.toUInt(), 6u),
            format = io.ygdrasil.webgpu.GPUTextureFormat.of(data.format.wgpu.enumValue)!!,
            usage = usage,
            mipLevelCount = levels.toUInt()
        )

        val gpuTex = backend.createTexture(texDesc)
        copyTextureData(data, gpuTex.gpuTexture, Extent3D(data.width.toUInt(), data.height.toUInt()))
        if (tex.mipMapping.isMipMapped) {
            mipmapGenerator.generateMipLevels(gpuTex.imageInfo, gpuTex.gpuTexture)
        }
        return gpuTex
    }

    private fun loadTexture2dArray(tex: Texture2dArray, data: ImageData3d): OldWgpuTextureResource {
        val usage = setOf(io.ygdrasil.webgpu.GPUTextureUsage.CopyDst, io.ygdrasil.webgpu.GPUTextureUsage.TextureBinding, io.ygdrasil.webgpu.GPUTextureUsage.RenderAttachment)
        val size = Extent3D(data.width.toUInt(), data.height.toUInt(), data.depth.toUInt())
        val levels = tex.mipMapping.numLevels(data.width, data.height)
        val texDesc = TextureDescriptor(
            size = size,
            format = io.ygdrasil.webgpu.GPUTextureFormat.of(data.format.wgpu.enumValue)!!,
            usage = usage,
            mipLevelCount = levels.toUInt()
        )

        val gpuTex = backend.createTexture(texDesc)
        copyTextureData(data, gpuTex.gpuTexture, size)
        if (tex.mipMapping.isMipMapped) {
            mipmapGenerator.generateMipLevels(gpuTex.imageInfo, gpuTex.gpuTexture)
        }
        return gpuTex
    }

    private fun loadTextureCubeArray(tex: TextureCubeArray, data: ImageDataCubeArray): OldWgpuTextureResource {
        val usage = setOf(io.ygdrasil.webgpu.GPUTextureUsage.CopyDst, io.ygdrasil.webgpu.GPUTextureUsage.TextureBinding, io.ygdrasil.webgpu.GPUTextureUsage.RenderAttachment)
        val levels = tex.mipMapping.numLevels(data.width, data.height)
        val texDesc = TextureDescriptor(
            size = Extent3D(data.width.toUInt(), data.height.toUInt(), (6 * data.slices).toUInt()),
            format = io.ygdrasil.webgpu.GPUTextureFormat.of(data.format.wgpu.enumValue)!!,
            usage = usage,
            mipLevelCount = levels.toUInt()
        )

        val gpuTex = backend.createTexture(texDesc)
        copyTextureData(data, gpuTex.gpuTexture, Extent3D(data.width.toUInt(), data.height.toUInt()))
        if (tex.mipMapping.isMipMapped) {
            mipmapGenerator.generateMipLevels(gpuTex.imageInfo, gpuTex.gpuTexture)
        }
        return gpuTex
    }

    fun copyTexture2d(src: GPUTexture, dst: GPUTexture, mipLevels: Int, encoder: GPUCommandEncoder) {
        val width = src.width
        val height = src.height
        val arrayLayers = src.depthOrArrayLayers    // is 6 for cube maps

        for (mipLevel in 0 until mipLevels) {
            encoder.copyTextureToTexture(
                source = GPUImageCopyTexture(src, mipLevel = mipLevel),
                destination = GPUImageCopyTexture(dst, mipLevel = mipLevel),
                copySize = intArrayOf(width shr mipLevel, height shr mipLevel, arrayLayers)
            )
        }
    }

    fun resolveMultiSampledDepthTexture(src: GPUTexture, dst: GPUTexture, encoder: GPUCommandEncoder, mipLevel: Int = 0, layer: Int = 0) {
        multiSampledDepthTextureCopy.copyTexture(src, dst, encoder, mipLevel, layer)
    }

    private fun MipMapping.numLevels(width: Int, height: Int): Int = when (this) {
        MipMapping.Full -> numMipLevels(width, height)
        is MipMapping.Limited -> numLevels
        MipMapping.Off -> 1
    }

    private fun copyTextureData(src: ImageData, dst: io.ygdrasil.webgpu.GPUTexture, size: Extent3D) {
        println("${src::class.simpleName} -> ${dst::class.simpleName} ${size}")
        when (src) {
            is ImageTextureData -> copyTextureData(src, dst, size, Origin3D(0u, 0u, 0u),)
            is BufferedImageData1d -> copyTextureData(src, dst, size, Origin3D(0u, 0u, 0u))
            is BufferedImageData2d -> copyTextureData(src, dst, size, Origin3D(0u, 0u, 0u))
            is BufferedImageData3d -> copyTextureData(src, dst, size, Origin3D(0u, 0u, 0u))
            is ImageDataCube -> {
                copyTextureData(src.posX, dst, size, Origin3D(0u, 0u, 0u))
                copyTextureData(src.negX, dst, size, Origin3D(0u, 0u, 1u))
                copyTextureData(src.posY, dst, size, Origin3D(0u, 0u, 2u))
                copyTextureData(src.negY, dst, size, Origin3D(0u, 0u, 3u))
                copyTextureData(src.posZ, dst, size, Origin3D(0u, 0u, 4u))
                copyTextureData(src.negZ, dst, size, Origin3D(0u, 0u, 5u))
            }
            is ImageDataCubeArray -> {
                src.cubes.forEachIndexed { i, cube ->
                    copyTextureData(cube.posX, dst, size, Origin3D(0u, 0u, (i * 6 + 0).toUInt()))
                    copyTextureData(cube.negX, dst, size, Origin3D(0u, 0u, (i * 6 + 1).toUInt()))
                    copyTextureData(cube.posY, dst, size, Origin3D(0u, 0u, (i * 6 + 2).toUInt()))
                    copyTextureData(cube.negY, dst, size, Origin3D(0u, 0u, (i * 6 + 3).toUInt()))
                    copyTextureData(cube.posZ, dst, size, Origin3D(0u, 0u, (i * 6 + 4).toUInt()))
                    copyTextureData(cube.negZ, dst, size, Origin3D(0u, 0u, (i * 6 + 5).toUInt()))
                }
            }
            is ImageData2dArray -> {
                val size2d = Extent3D(size.width, size.height)
                for (i in src.images.indices) {
                    copyTextureData(src.images[i], dst, size2d, Origin3D(0u, 0u, i.toUInt()))
                }
            }
            else -> error("Not implemented: ${src::class.simpleName}")
        }
    }

    private fun copyTextureData(src: ImageData, dst: io.ygdrasil.webgpu.GPUTexture, size: Extent3D, dstOrigin: GPUOrigin3D) {
        when (src) {
            is BufferedImageData -> {
                device.queue.writeTexture(
                    data = src.arrayBufferView,
                    destination = GPUImageCopyTexture(dst.toJs(), origin = dstOrigin.toJs()),
                    dataLayout = src.gpuImageDataLayout,
                    size = size.toJs()
                )
            }
            is ImageTextureData -> loader.copyTextureData(src, dst, size, dstOrigin)
            else -> error("Invalid src data type: $src")
        }
    }

    private val ImageData.gpuImageDataLayout: GPUImageDataLayout get() {
        return when (this) {
            is BufferedImageData1d -> gpuImageDataLayout
            is BufferedImageData2d -> gpuImageDataLayout
            is BufferedImageData3d -> gpuImageDataLayout
            else -> error("Invalid TextureData type: $this")
        }
    }

    private val BufferedImageData1d.gpuImageDataLayout: GPUImageDataLayout get() {
        val bytesPerRow = format.pxSize * width
        return GPUImageDataLayout(bytesPerRow = bytesPerRow, rowsPerImage = 1)
    }

    private val BufferedImageData2d.gpuImageDataLayout: GPUImageDataLayout get() {
        val bytesPerRow = format.pxSize * width
        return GPUImageDataLayout(bytesPerRow = bytesPerRow, rowsPerImage = height)
    }

    private val BufferedImageData3d.gpuImageDataLayout: GPUImageDataLayout get() {
        val bytesPerRow = format.pxSize * width
        return GPUImageDataLayout(bytesPerRow = bytesPerRow, rowsPerImage = height)
    }

    private val ImageData.arrayBufferView: ArrayBufferView get() {
        check(this is BufferedImageData)

        val bufData = data
        return when {
            format.isF16 && bufData is Float32BufferImpl -> {
                val f32Array = bufData.buffer
                val f16Buffer = Uint8Array(f32Array.length * 2)
                for (i in 0 until f32Array.length) {
                    f16Buffer.putF16(i, f32Array[i])
                }
                f16Buffer
            }
            bufData is Uint8BufferImpl -> bufData.buffer
            bufData is Uint16BufferImpl -> bufData.buffer
            bufData is Float32BufferImpl -> bufData.buffer
            else -> throw IllegalArgumentException("Unsupported buffer type")
        }
    }

    private fun Uint8Array.putF16(index: Int, f32: Float) {
        float32ToFloat16(f32) { high, low ->
            val byteI = index * 2
            set(byteI, low)
            set(byteI+1, high)
        }
    }

    private inner class MultiSampledDepthTextureCopy {
        private val shaderModule = device.createShaderModule("""
            var<private> pos: array<vec2f, 4> = array<vec2f, 4>(
                vec2f(-1.0, 1.0), vec2f(1.0, 1.0),
                vec2f(-1.0, -1.0), vec2f(1.0, -1.0)
            );

            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) texCoord: vec2f
            };
        
            @vertex
            fn vertexMain(@builtin(vertex_index) vertexIndex: u32) -> VertexOutput {
                var output: VertexOutput;
                output.texCoord = pos[vertexIndex] * vec2f(0.5, -0.5) + vec2f(0.5);
                output.position = vec4f(pos[vertexIndex], 0.0, 1.0);
                return output;
            }
        
            @group(0) @binding(0) var img: texture_multisampled_2d<f32>;
        
            @fragment
            fn fragmentMain(@location(0) texCoord: vec2f) -> @builtin(frag_depth) f32 {
                let dim = vec2f(textureDimensions(img));
                return textureLoad(img, vec2u(texCoord * dim), 0).x;
            }
        """.trimIndent())

        private val pipelines = mutableMapOf<GPUTextureFormat, GPURenderPipeline>()

        private fun getRenderPipeline(format: GPUTextureFormat): GPURenderPipeline = pipelines.getOrPut(format) {
            device.createRenderPipeline(
                GPURenderPipelineDescriptor(
                    vertex = GPUVertexState(
                        module = shaderModule,
                        entryPoint = "vertexMain"
                    ),
                    fragment = GPUFragmentState(
                        module = shaderModule,
                        entryPoint = "fragmentMain",
                        targets = arrayOf()
                    ),
                    depthStencil = GPUDepthStencilState(
                        format = format,
                        depthWriteEnabled = true,
                        depthCompare = GPUCompareFunction.always
                    ),
                    primitive = GPUPrimitiveState(topology = GPUPrimitiveTopology.triangleStrip),
                    layout = GPUAutoLayoutMode.auto
                )
            )
        }

        fun copyTexture(src: GPUTexture, dst: GPUTexture, cmdEncoder: GPUCommandEncoder, mipLevel: Int, layer: Int) {
            val pipeline = getRenderPipeline(src.format)

            val srcView = src.createView(baseMipLevel = mipLevel, mipLevelCount = 1, baseArrayLayer = layer, arrayLayerCount = 1)
            val dstView = dst.createView(baseMipLevel = mipLevel, mipLevelCount = 1, baseArrayLayer = layer, arrayLayerCount = 1)
            val passEncoder = cmdEncoder.beginRenderPass(
                colorAttachments = emptyArray(),
                depthStencilAttachment = GPURenderPassDepthStencilAttachment(
                    view = dstView,
                    depthLoadOp = GPULoadOp.clear,
                    depthStoreOp = GPUStoreOp.store
                )
            )
            val bindGroup = device.createBindGroup(
                layout = pipeline.getBindGroupLayout(0),
                entries = arrayOf(
                    GPUBindGroupEntry(
                        binding = 0,
                        resource = srcView
                    ),
                )
            )
            passEncoder.setPipeline(pipeline)
            passEncoder.setBindGroup(0, bindGroup)
            passEncoder.draw(4)
            passEncoder.end()
        }
    }
}

