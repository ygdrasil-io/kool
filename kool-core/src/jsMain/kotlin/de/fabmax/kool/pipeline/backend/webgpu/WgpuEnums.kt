package de.fabmax.kool.pipeline.backend.webgpu

value class GPUAddressMode private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val clampToEdge = GPUAddressMode("clamp-to-edge")
        val repeat = GPUAddressMode("repeat")
        val mirrorRepeat = GPUAddressMode("mirror-repeat")
    }
}

value class GPUAutoLayoutMode private constructor(val enumValue: String) : GPUPipelineLayout {
    override fun toString() = enumValue
    companion object {
        val auto = GPUAutoLayoutMode("auto")
    }
}

value class GPUBlendFactor private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val zero = GPUBlendFactor("zero")
        val one = GPUBlendFactor("one")
        val src = GPUBlendFactor("src")
        val srcAlpha = GPUBlendFactor("src-alpha")
        val oneMinusSrcAlpha = GPUBlendFactor("one-minus-src-alpha")
    }
}

value class GPUBlendOperation private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val add = GPUBlendOperation("add")
        val max = GPUBlendOperation("max")
    }
}

value class GPUBufferBindingType private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val uniform = GPUBufferBindingType("uniform")
        val storage = GPUBufferBindingType("storage")
        val readOnlyStorage = GPUBufferBindingType("read-only-storage")
    }
}

value class GPUCompareFunction private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val never = GPUCompareFunction("never")
        val less = GPUCompareFunction("less")
        val equal = GPUCompareFunction("equal")
        val lessEqual = GPUCompareFunction("less-equal")
        val greater = GPUCompareFunction("greater")
        val notEqual = GPUCompareFunction("not-equal")
        val greaterEqual = GPUCompareFunction("greater-equal")
        val always = GPUCompareFunction("always")
    }
}

value class GPUCullMode private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val none = GPUCullMode("none")
        val front = GPUCullMode("front")
        val back = GPUCullMode("back")
    }
}

value class GPUFilterMode private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val nearest = GPUFilterMode("nearest")
        val linear = GPUFilterMode("linear")
    }
}

value class GPUFrontFace private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val ccw = GPUFrontFace("ccw")
        val cw = GPUFrontFace("cw")
    }
}

value class GPUMipmapFilterMode private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val nearest = GPUMipmapFilterMode("nearest")
        val linear = GPUMipmapFilterMode("linear")
    }
}

value class GPUIndexFormat private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val uint32 = GPUIndexFormat("uint32")
    }
}

value class GPULoadOp private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val load = GPULoadOp("load")
        val clear = GPULoadOp("clear")
    }
}

value class GPUPrimitiveTopology private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val pointList = GPUPrimitiveTopology("point-list")
        val lineList = GPUPrimitiveTopology("line-list")
        val lineStrip = GPUPrimitiveTopology("line-strip")
        val triangleList = GPUPrimitiveTopology("triangle-list")
        val triangleStrip = GPUPrimitiveTopology("triangle-strip")
    }
}

value class GPUPowerPreference private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val highPerformance = GPUPowerPreference("high-performance")
    }
}

value class GPUQueryType private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val timestamp = GPUQueryType("timestamp")
    }
}

value class GPUSamplerBindingType private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val filtering = GPUSamplerBindingType("filtering")
        val nonFiltering = GPUSamplerBindingType("non-filtering")
        val comparison = GPUSamplerBindingType("comparison")
    }
}

value class GPUStoreOp private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val store = GPUStoreOp("store")
    }
}

value class GPUTextureDimension private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val texture1d = GPUTextureDimension("1d")
        val texture2d = GPUTextureDimension("2d")
        val texture3d = GPUTextureDimension("3d")
    }
}

value class GPUTextureFormat private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val r8unorm = GPUTextureFormat("r8unorm")
        val r16float = GPUTextureFormat("r16float")
        val rg8unorm = GPUTextureFormat("rg8unorm")
        val r32uint = GPUTextureFormat("r32uint")
        val r32sint = GPUTextureFormat("r32sint")
        val r32float = GPUTextureFormat("r32float")
        val rg16float = GPUTextureFormat("rg16float")
        val rgba8unorm = GPUTextureFormat("rgba8unorm")
        val rg32uint = GPUTextureFormat("rg32uint")
        val rg32sint = GPUTextureFormat("rg32sint")
        val rg32float = GPUTextureFormat("rg32float")
        val rgba16float = GPUTextureFormat("rgba16float")
        val rgba32uint = GPUTextureFormat("rgba32uint")
        val rgba32sint = GPUTextureFormat("rgba32sint")
        val rgba32float = GPUTextureFormat("rgba32float")
        val depth32float = GPUTextureFormat("depth32float")
    }
}

value class GPUTextureSampleType private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val float = GPUTextureSampleType("float")
        val unfilterableFloat = GPUTextureSampleType("unfilterable-float")
        val depth = GPUTextureSampleType("depth")
    }
}

value class GPUStorageTextureAccess private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val writeOnly = GPUStorageTextureAccess("write-only")
        val readOnly = GPUStorageTextureAccess("read-only")
        val readWrite = GPUStorageTextureAccess("read-write")
    }
}

value class GPUTextureViewDimension private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val view1d = GPUTextureViewDimension("1d")
        val view2d = GPUTextureViewDimension("2d")
        val view2dArray = GPUTextureViewDimension("2d-array")
        val viewCube = GPUTextureViewDimension("cube")
        val viewCubeArray = GPUTextureViewDimension("cube-array")
        val view3d = GPUTextureViewDimension("3d")
    }
}

value class GPUVertexFormat private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val float32 = GPUVertexFormat("float32")
        val float32x2 = GPUVertexFormat("float32x2")
        val float32x3 = GPUVertexFormat("float32x3")
        val float32x4 = GPUVertexFormat("float32x4")
        val uint32 = GPUVertexFormat("uint32")
        val uint32x2 = GPUVertexFormat("uint32x2")
        val uint32x3 = GPUVertexFormat("uint32x3")
        val uint32x4 = GPUVertexFormat("uint32x4")
        val sint32 = GPUVertexFormat("sint32")
        val sint32x2 = GPUVertexFormat("sint32x2")
        val sint32x3 = GPUVertexFormat("sint32x3")
        val sint32x4 = GPUVertexFormat("sint32x4")
    }
}

value class GPUVertexStepMode private constructor(val enumValue: String) {
    override fun toString() = enumValue
    companion object {
        val vertex = GPUVertexStepMode("vertex")
        val instance = GPUVertexStepMode("instance")
    }
}
