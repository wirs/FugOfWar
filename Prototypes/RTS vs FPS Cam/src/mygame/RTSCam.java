package mygame;

import com.jme3.cursors.plugins.JmeCursor;
import java.io.IOException;
 
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.util.ArrayList;

 

/**
 *
 * @author Netsky
 */
public class RTSCam implements Control {



 
    public enum Degree {
        SIDE,
        FWD,
        ROTATE,
        TILT,
        DISTANCE
    }
     
    private boolean mmid;
    
    private InputManager inputManager;
    private final Camera cam;
 
    private int[] direction = new int[5];
    private float[] force = new float[5];
    private float[] accelPeriod = new float[5];
 
    private float[] maxSpeed = new float[5];
    private float[] maxAccelPeriod = new float[5];
    private float[] minValue = new float[5];
    private float[] maxValue = new float[5];
 
    private Vector3f position = new Vector3f();
     
    private Vector3f center = new Vector3f();
    private float tilt = (float)(Math.PI / 4);
    private float rot = 0;
    private float distance = 15;
     
    private static final int SIDE = Degree.SIDE.ordinal();
    private static final int FWD = Degree.FWD.ordinal();
    private static final int ROTATE = Degree.ROTATE.ordinal();
    private static final int TILT = Degree.TILT.ordinal();
    private static final int DISTANCE = Degree.DISTANCE.ordinal();
     
    public RTSCam(Camera cam, Spatial target) {
        this.cam = cam;
         
        setMinMaxValues(Degree.SIDE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        setMinMaxValues(Degree.FWD, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        setMinMaxValues(Degree.ROTATE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        setMinMaxValues(Degree.TILT, 0.2f, (float)(Math.PI / 2) - 0.001f);
        setMinMaxValues(Degree.DISTANCE, 10, 500);
 
        setMaxSpeed(Degree.SIDE,10f,0.4f);
        setMaxSpeed(Degree.FWD,10f,0.4f);
        setMaxSpeed(Degree.ROTATE,20f,0.4f);
        setMaxSpeed(Degree.TILT,20f,0.4f);
        setMaxSpeed(Degree.DISTANCE,50f,0.4f);
                target.addControl(this);
    }
 
    public void setMaxSpeed(Degree deg, float maxSpd, float accelTime) {
        maxSpeed[deg.ordinal()] = maxSpd/accelTime;
        maxAccelPeriod[deg.ordinal()] = accelTime;
    }
 
    public void registerWithInput(InputManager inputManager) {
        this.inputManager = inputManager;
 
        String[] dMappings = new String[] { "+SIDE", "+FWD", "+ROTATE", 
                "-SIDE", "-FWD", "-ROTATE", "CamControl"};
        String[] aMappings = new String[] { "+ROT", "-ROT", "+DISTANCE", "-DISTANCE", "+TIL", "-TIL"};
 
        inputManager.addMapping("-SIDE", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("+SIDE", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("+FWD", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("-FWD", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("+ROT", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("-ROT", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("+TIL", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("-TIL", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("CamControl", new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));
        inputManager.addMapping("-DISTANCE", new MouseAxisTrigger(MouseInput.AXIS_WHEEL,false));
        inputManager.addMapping("+DISTANCE", new MouseAxisTrigger(MouseInput.AXIS_WHEEL,true));
       
 
        inputManager.addListener(actionListener, dMappings);
        inputManager.addListener(analogListener, aMappings);
        inputManager.setCursorVisible(true);
    }
 
    public void write(JmeExporter ex) throws IOException {
 
    }
 
    public void read(JmeImporter im) throws IOException {
 
    }
 
    public Control cloneForSpatial(Spatial spatial) {
        RTSCam other = new RTSCam(cam, spatial);
        other.registerWithInput(inputManager);
        return other;
    }
 
    public void setSpatial(Spatial spatial) {
         
    }
 
    public void setEnabled(boolean enabled) {
 
    }
 
    public boolean isEnabled() {
 
        return true;
    }
 
    public void update(final float tpf) {
 
        for (int i = 0; i < direction.length; i++) {
            int dir = direction[i];
            switch (dir) {
            case -1:
                accelPeriod[i] = clamp(-maxAccelPeriod[i],accelPeriod[i]-tpf,accelPeriod[i]);
                break;
            case 0:
                if (accelPeriod[i] != 0) {
                    double oldSpeed = accelPeriod[i];
                    if (accelPeriod[i] > 0) {
                        accelPeriod[i] -= tpf;
                    } else {
                        accelPeriod[i] += tpf;
                    }
                    if (oldSpeed * accelPeriod[i] < 0) {
                        accelPeriod[i] = 0;
                    }
                }
                break;
            case 1:
                accelPeriod[i] = clamp(accelPeriod[i],accelPeriod[i]+tpf,maxAccelPeriod[i]);
                break;
            default:
                break;
            }
        }
        
       
         
 
        distance += maxSpeed[DISTANCE] * force[DISTANCE] * tpf;
        tilt += maxSpeed[TILT] * force[TILT] * tpf;
        rot += maxSpeed[ROTATE] * force[ROTATE] * tpf;
         
        distance = clamp(minValue[DISTANCE],distance,maxValue[DISTANCE]);
        rot = clamp(minValue[ROTATE],rot,maxValue[ROTATE]);
        tilt = clamp(minValue[TILT],tilt,maxValue[TILT]);
 
        double offX = maxSpeed[SIDE] * accelPeriod[SIDE] * tpf;
        double offZ = maxSpeed[FWD] * accelPeriod[FWD] * tpf;
 
        center.x += offX * Math.cos(-rot) + offZ * Math.sin(rot);
        center.z += offX * Math.sin(-rot) + offZ * Math.cos(rot);
 
        position.x = center.x + (float)(distance * Math.cos(tilt) * Math.sin(rot));
        position.y = center.y + (float)(distance * Math.sin(tilt));
        position.z = center.z + (float)(distance * Math.cos(tilt) * Math.cos(rot));
 
         
        cam.setLocation(position);
        cam.lookAt(center, new Vector3f(0,1,0));
         
        
        force[TILT] = 0;
        force[ROTATE] = 0;
        force[DISTANCE] = 0;

 
    }
     
     
    private static float clamp(float min, float value, float max) {
        if ( value < min ) {
            return min;
        } else if ( value > max ) {
            return max;
        } else {
            return value;
        }
    }
     
    public float getMaxSpeed(Degree dg) {
        return maxSpeed[dg.ordinal()];
    }
     
    public float getMinValue(Degree dg) {
        return minValue[dg.ordinal()];
    }
     
    public float getMaxValue(Degree dg) {
        return maxValue[dg.ordinal()];
    }
     
    // SIDE and FWD min/max values are ignored
    public void setMinMaxValues(Degree dg, float min, float max) {
        minValue[dg.ordinal()] = min;
        maxValue[dg.ordinal()] = max;
    }
     
    public Vector3f getPosition() {
        return position;
    }
     
    public void setCenter(Vector3f center) {
        this.center.set(center);
    }
 
    public void render(RenderManager rm, ViewPort vp) {
 
    }
 
    private ActionListener actionListener = new ActionListener() {
    public void onAction(String name, boolean isPressed, float tpf) {
        
        System.out.println(name);
        
        int press = isPressed ? 1 : 0;
        
        if (isPressed && name.equals("CamControl")) {
            mmid = true;
            System.out.println(mmid);
        } else if (name.equals("CamControl")){
            mmid = false;
            System.out.println(mmid);

        }
        
        
        
        char sign = name.charAt(0);
        if ( sign == '-') {
            press = -press;
        } else if (sign != '+') {
            return;
        }
         
        Degree deg = Degree.valueOf(name.substring(1));
        direction[deg.ordinal()] = press;
        
        
    }
    };
    
    private AnalogListener analogListener = new AnalogListener() {
        public void onAnalog(String name, float value, float tpf) {
            
            float press = 0;
        
            if (mmid) {
                if (name.equals("+ROT")) {
                    name = "+ROTATE";
                       press = value;
                } 
                if (name.equals("-ROT")) {
                    name = "-ROTATE";
                    press = value;
                }
                if (name.equals("+TIL")) {
                    name = "+TILT";
                       press = value;
                } 
                if (name.equals("-TIL")) {
                    name = "-TILT";
                       press = value;
                }
            }  else { press = 0; }
        
            System.out.println(name);
            
            if (name.equals("+ROT") || name.equals("-ROT") || name.equals("+TIL") || name.equals("-TIL")) { 
                return; 
            }
            
            if (name.equals("+DISTANCE")) {
                press = 9f;
            }
            if (name.equals("-DISTANCE")) {
                press = 9f;
            }

            char sign = name.charAt(0);
            if ( sign == '-') {
                press = -press;
            } else if (sign != '+') {
                return;
            }
            
            press *= 6;
            
            
            Degree deg = Degree.valueOf(name.substring(1));
            direction[deg.ordinal()] = 2;
            force[deg.ordinal()] = press;
        
        }
    };
}