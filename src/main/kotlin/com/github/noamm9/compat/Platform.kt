package com.github.noamm9.compat

import java.net.URI

//~ if <26.3 'com.mojang.blaze3d.Blaze3D.openUri' -> 'net.minecraft.util.Util.getPlatform().openUri'
fun openUri(uri: URI) = com.mojang.blaze3d.Blaze3D.openUri(uri)
