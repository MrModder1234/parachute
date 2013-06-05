//
// This work is licensed under the Creative Commons
// Attribution-ShareAlike 3.0 Unported License. To view a copy of this
// license, visit http://creativecommons.org/licenses/by-sa/3.0/
//

package parachute.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;

import org.lwjgl.opengl.GL11;


public class ModelParachute extends ModelBase {
	
	static final int w = 16;
	static final int d = 16;
	static final int h = 1;

	public ParachuteModelRenderer lhalf;
	public ParachuteModelRenderer rhalf;
	public ParachuteModelRenderer center;
	
	public ModelParachute() {
		center = new ParachuteModelRenderer(0, 0);
		center.addBox(-8F, 0F, -8F, w, h, d);

		lhalf = new ParachuteModelRenderer(0, 0);
		lhalf.addBox(-8F, 0F, -16F, w, h, d);
		lhalf.setRotationPoint(0F, 0F, -8F);
		lhalf.rotateAngleX = 6.021385919380437F;

		rhalf = new ParachuteModelRenderer(0, 0);
		rhalf.addBox(-8F, 0F, 0F, w, h, d);
		rhalf.setRotationPoint(0F, 0F, 8F);
		rhalf.rotateAngleX = 0.2617993877991494F;
	}

	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
		center.render(f5);
		lhalf.render(f5);
		rhalf.render(f5);
	}

}
