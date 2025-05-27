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

    private val device: GPUDevice get() = (backend.device as Device).handler.asDynamic()
    private val multiSampledDepthTextureCopy = MultiSampledDepthTextureCopy()
    val mipmapGenerator = loader.mipmapGenerator

    fun loadTexture(tex: Texture<*>) {
        loader.loadTexture(tex)
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

