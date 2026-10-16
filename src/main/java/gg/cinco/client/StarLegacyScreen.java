package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Compatibility bridge for Star's existing GUI code on Minecraft 1.21.11.
 * Legacy callbacks get first chance, then unhandled input continues to Screen so native widgets
 * (especially text fields) keep reliable focus, paste, selection and repeat-key behavior.
 */
public abstract class StarLegacyScreen extends Screen {
    protected StarLegacyScreen(Text title) { super(title); }

    public boolean mouseClicked(double mouseX,double mouseY,int button){return false;}
    public boolean mouseDragged(double mouseX,double mouseY,int button,double deltaX,double deltaY){return false;}
    public boolean mouseReleased(double mouseX,double mouseY,int button){return false;}

    public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        if(keyCode==GLFW.GLFW_KEY_ESCAPE||keyCode==GLFW.GLFW_KEY_RIGHT_SHIFT){
            MinecraftClient client=MinecraftClient.getInstance();
            if(client!=null){
                client.options.forwardKey.setPressed(false);client.options.backKey.setPressed(false);
                client.options.leftKey.setPressed(false);client.options.rightKey.setPressed(false);
                client.options.jumpKey.setPressed(false);client.options.sneakKey.setPressed(false);
                client.options.sprintKey.setPressed(false);client.setScreen(null);
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr,int modifiers){return false;}

    @Override public boolean mouseClicked(Click click,boolean doubled){
        if(mouseClicked(click.x(),click.y(),click.button()))return true;
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseDragged(Click click,double deltaX,double deltaY){
        if(mouseDragged(click.x(),click.y(),click.button(),deltaX,deltaY))return true;
        return super.mouseDragged(click,deltaX,deltaY);
    }
    @Override public boolean mouseReleased(Click click){
        if(mouseReleased(click.x(),click.y(),click.button()))return true;
        return super.mouseReleased(click);
    }
    @Override public boolean keyPressed(KeyInput input){
        if(keyPressed(input.key(),input.scancode(),input.modifiers()))return true;
        return super.keyPressed(input);
    }
    @Override public boolean charTyped(CharInput input){
        int cp=input.codepoint();
        if(cp>=Character.MIN_VALUE&&cp<=Character.MAX_VALUE&&charTyped((char)cp,input.modifiers()))return true;
        return super.charTyped(input);
    }
}
