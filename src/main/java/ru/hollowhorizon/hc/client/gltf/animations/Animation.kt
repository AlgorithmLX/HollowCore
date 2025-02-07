package ru.hollowhorizon.hc.client.gltf.animations

import de.javagl.jgltf.model.GltfModel
import de.javagl.jgltf.model.NodeModel
import ru.hollowhorizon.hc.common.capabilities.AnimatedEntityCapability

class Animation(val name: String, val animationData: Map<NodeModel, AnimationData>) {
    val maxTime = animationData.maxOf { it.value.maxTime }.let {
        if(it == 0f) 0.0001f else it
    }

    fun hasNode(node: NodeModel, target: AnimationTarget): Boolean {
        val data = animationData[node] ?: return false

        return when(target) {
            AnimationTarget.TRANSLATION -> data.translation != null
            AnimationTarget.ROTATION -> data.rotation != null
            AnimationTarget.SCALE -> data.scale != null
            AnimationTarget.WEIGHTS -> data.weights != null
        }
    }

    fun compute(node: NodeModel, target: AnimationTarget, time: Float): FloatArray? {
        return animationData[node]?.let {
            when (target) {
                AnimationTarget.TRANSLATION -> it.translation?.compute(time)
                AnimationTarget.ROTATION -> it.rotation?.compute(time)
                AnimationTarget.SCALE -> it.scale?.compute(time)
                AnimationTarget.WEIGHTS -> it.weights?.compute(time)
            }
        }
    }

    fun apply(node: NodeModel, time: Float) {
        animationData[node]?.let { anim ->
            node.translation = anim.translation?.compute(time)
            node.rotation = anim.rotation?.compute(time)
            node.scale = anim.scale?.compute(time)
            node.weights = anim.weights?.compute(time)
        }
    }

    override fun toString() = name

    companion object {
        fun createFromPose(model: List<NodeModel>): Animation {
            return Animation("%BIND_POSE%", model.associate { node ->

                return@associate node to AnimationData(
                    node,
                    Interpolator.Step(
                        floatArrayOf(0f),
                        arrayOf(node.translation?.copyOf() ?: floatArrayOf(0f, 0f, 0f))
                    ),
                    Interpolator.Step(
                        floatArrayOf(0f),
                        arrayOf(node.rotation?.copyOf() ?: floatArrayOf(0f, 0f, 0f, 1f))
                    ),
                    Interpolator.Step(
                        floatArrayOf(0f),
                        arrayOf(node.scale?.copyOf() ?: floatArrayOf(1f, 1f, 1f))
                    ),
                    Interpolator.Step(
                        floatArrayOf(0f),
                        arrayOf(node.weights?.copyOf() ?: floatArrayOf())
                    ),
                )
            })
        }
    }
}

enum class AnimationType {
    IDLE, IDLE_SNEAKED, WALK, WALK_SNEAKED,
    RUN, SWIM, FALL, FLY, SIT, SLEEP, SWING, DEATH;

    companion object {
        @JvmStatic
        fun load(model: GltfModel, capability: AnimatedEntityCapability) {
            val names = model.animationModels.map { it.name }

            fun List<String>.findOr(vararg names: String) = this.find { anim -> names.any { anim.contains(it) } }
            fun List<String>.findAnd(vararg names: String) = this.find { anim -> names.all { anim.contains(it) } }

            with(capability) {
                animations[IDLE] = names.findOr("idle") ?: ""
                animations[IDLE_SNEAKED] = names.findAnd("idle", "sneak") ?: capability.animations[IDLE] ?: ""
                animations[WALK] = names.minByOrNull {
                    when {
                        it.contains("walk") -> 0
                        it.contains("go") -> 1
                        it.contains("run") -> 2
                        it.contains("move") -> 3
                        else -> 4
                    }
                } ?: ""
                animations[WALK_SNEAKED] = names.findAnd("walk", "sneak") ?: capability.animations[WALK] ?: ""
                animations[RUN] = names.findOr("run", "flee", "dash") ?: capability.animations[WALK] ?: ""
                animations[SWIM] = names.findOr("swim") ?: capability.animations[WALK] ?: ""
                animations[FALL] = names.findOr("fall") ?: capability.animations[IDLE] ?: ""
                animations[FLY] = names.findOr("fly") ?: capability.animations[IDLE] ?: ""
                animations[SIT] = names.findOr("sit") ?: capability.animations[IDLE] ?: ""
                animations[SLEEP] = names.findOr("sleep") ?: capability.animations[SLEEP] ?: ""
                animations[SWING] = names.findOr("attack", "swing") ?: ""
                animations[DEATH] = names.findOr("death") ?: ""

            }
        }
    }
}

enum class PlayType {
    ONCE, //Одиночный запуск анимации
    LOOPED, //После завершения анимация начнётся с начала
    LAST_FRAME, //После завершения анимация застынет на последнем кадре
    PING_PONG; //После завершения анимация начнёт проигрываться в обратном порядке


    fun stopOnEnd(): Boolean = this == ONCE
}

class AnimationData(
    val node: NodeModel,
    val translation: Interpolator<*>?,
    val rotation: Interpolator<*>?,
    val scale: Interpolator<*>?,
    val weights: Interpolator<*>?
) {
    val maxTime = maxOf(
        translation?.maxTime ?: 0f,
        rotation?.maxTime ?: 0f,
        scale?.maxTime ?: 0f,
        weights?.maxTime ?: 0f,
    )
}

class ComputeResult(
    val translation: FloatArray?,
    val rotation: FloatArray?,
    val scale: FloatArray?,
    val weights: FloatArray?,
)

enum class AnimationTarget { TRANSLATION, ROTATION, SCALE, WEIGHTS }