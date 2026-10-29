package com.github.noamm9.compat

//~ if <26.3 'monster.Enderman' -> 'monster.EnderMan'
typealias Enderman = net.minecraft.world.entity.monster.Enderman

//~ if <26.3 'renderpearl.api.textures' -> 'blaze3d.textures'
typealias FilterMode = com.mojang.renderpearl.api.textures.FilterMode

//~ if <26.3 'renderpearl.api.pipeline' -> 'blaze3d.pipeline'
typealias RenderPipeline = com.mojang.renderpearl.api.pipeline.RenderPipeline

//~ if <26.3 'renderpearl.api.pipeline.PrimitiveTopology' -> 'blaze3d.PrimitiveTopology'
typealias PrimitiveTopology = com.mojang.renderpearl.api.pipeline.PrimitiveTopology

//~ if <26.3 'renderpearl.api.pipeline' -> 'blaze3d.pipeline'
typealias BlendFunction = com.mojang.renderpearl.api.pipeline.BlendFunction

//~ if <26.3 'renderpearl.api.pipeline' -> 'blaze3d.pipeline'
typealias ColorTargetState = com.mojang.renderpearl.api.pipeline.ColorTargetState
