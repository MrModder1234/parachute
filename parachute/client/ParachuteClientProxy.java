/*
 * ParachuteClientProxy.java
 *
 * Copyright (c) 2017 Michael Sheppard
 *
 *  =====GPL=============================================================
 * $program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 * =====================================================================
 *
 */
package com.parachute.client;

import com.parachute.common.ParachuteCommonProxy;
import com.parachute.common.EntityParachute;
import com.parachute.common.Parachute;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.*;


@SuppressWarnings("unused")
public class ParachuteClientProxy extends ParachuteCommonProxy {

    // grab the 'jump' key from the game settings. defaults to the space bar. This allows the
    // player to change the jump key and the parachute will use the new jump key
    private static final int ascendKey = Minecraft.getMinecraft().gameSettings.keyBindJump.getKeyCode();

    @SuppressWarnings("unchecked")
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        RenderingRegistry.registerEntityRenderingHandler(EntityParachute.class, RenderParachute::new); // java 8
        ModelLoader.setCustomModelResourceLocation(Parachute.parachuteItem, 0, ParachuteCommonProxy.parachuteResource);
        ModelLoader.setCustomModelResourceLocation(Parachute.packItem, 0, ParachuteCommonProxy.packResource);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void Init(FMLInitializationEvent event) {
        super.Init(event);

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new KeyPressTick(ascendKey));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new HudGuiRenderer());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
         info(Parachute.modid + "Received postInit(FMLPostInitializationEvent event)");
    }

}
