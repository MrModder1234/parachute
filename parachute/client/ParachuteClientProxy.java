//  
//  =====GPL=============================================================
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; version 2 dated June, 1991.
// 
//  This program is distributed in the hope that it will be useful, 
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
// 
//  You should have received a copy of the GNU General Public License
//  along with this program;  if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave., Cambridge, MA 02139, USA.
//  =====================================================================
//
//
// Copyright © 2011-2015 Michael Sheppard (crackedEgg)
//
package com.parachute.client;

import com.parachute.common.ParachuteCommonProxy;
import com.parachute.common.EntityParachute;
import com.parachute.common.Parachute;
import net.minecraft.client.Minecraft;
//import net.minecraft.client.renderer.ItemModelMesher;
//import net.minecraft.client.renderer.entity.Render;
//import net.minecraft.client.renderer.entity.RenderManager;
//import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
//import net.minecraft.entity.Entity;
//import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

@SuppressWarnings("unused")
public class ParachuteClientProxy extends ParachuteCommonProxy {
	
	// grab the 'jump' key from the game settings. defaults to the space bar. This allows the
	// player to change the jump key and the parachute will use the new jump key
	public static final int ascendKey = Minecraft.getMinecraft().gameSettings.keyBindJump.getKeyCode();

	@SuppressWarnings("unchecked")
	@Override
	public void preInit()
	{
		super.preInit();
		RenderingRegistry.registerEntityRenderingHandler(EntityParachute.class, RenderParachute::new); // java 8
//		RenderingRegistry.registerEntityRenderingHandler(EntityParachute.class, new IRenderFactory<EntityParachute>() { // java 6/7
//			@Override
//			public Render<? super EntityParachute> createRenderFor(RenderManager manager) {
//				return new RenderParachute(manager);
//			}
//		});
		ModelLoader.setCustomModelResourceLocation(Parachute.parachuteItem, 0, new ModelResourceLocation(Parachute.modid + ":" + parachuteName));
		ModelLoader.setCustomModelResourceLocation(Parachute.packItem, 0, new ModelResourceLocation(Parachute.modid + ":" + packName));
		info(Parachute.modid + " CombinedClient preInit is complete.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void Init()
	{
		super.Init();

		net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new KeyPressTick(ascendKey));
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new HudGuiRenderer());

//		ItemModelMesher mm = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
//		mm.register(Parachute.parachuteItem, 0, new ModelResourceLocation(Parachute.modid + ":" + parachuteName, "inventory"));
//		mm.register(Parachute.packItem, 0, new ModelResourceLocation(Parachute.modid + ":" + packName, "inventory"));
		info(Parachute.modid + " CombinedClient Init is complete.");
	}

	@Override
	public void postInit()
	{
		info(Parachute.modid + " CombinedClient postInit is complete.");
	}

}
