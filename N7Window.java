package com.n7;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import static java.lang.Math.*;

public class N7Window {

    // The window handle
    private long window;

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(width, height, "N6", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

//==========================================================================

    int width = 1200;
    int height = 700;
    float rtri = 0;
    float koef;
    TBall ball = new TBall();
    TBall player1 = new TBall();
    TBall player2 = new TBall();
    float gravityBall = 0.002f;
    float gravity = 0.004f;
    float netHeight = -.2f;

    boolean isCross(float x1, float y1, float r, float x2, float y2) {
        return pow(x1 - x2, 2) + pow(y1 - y2, 2) < r * r;
    }

    void mirror(TBall obj, float x, float y, float speed) {
        float objVec = (float) atan2(obj.getDx(), obj.getDy());
        float crossVec = (float) atan2(obj.getX() - x, obj.getY() - y);
        float resVec = speed == 0 ? (float) (PI - objVec + crossVec * 2) : crossVec;
        speed = speed == 0 ? (float) sqrt(pow(obj.getDx(), 2) + pow(obj.getDy(), 2)) : speed;

        obj.setDx((float) (sin(resVec) * speed));
        obj.setDy((float) (cos(resVec) * speed));
    }

    void drawCircle(int cnt) {
        float x, y;
        float da = (float) (PI * 2 / cnt);
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(0, 0);
        for (int i = 0; i <= cnt; i++) {
            x = (float) sin(da * i);
            y = (float) cos(da * i);
            glVertex2f(x, y);
        }
        glEnd();
    }

    void tBallInit(TBall obj, float x1, float y1, float dx1, float dy1, float r1) {
        obj.setX(x1);
        obj.setY(y1);
        obj.setDx(dx1);
        obj.setDy(dy1);
        obj.setR(r1);
    }

    void gameInit() {
        tBallInit(ball, 0.1f, 0.5f, 0.0f, 0.0f, 0.2f);
        tBallInit(player1, -1, 0.f, 0.0f, 0.0f, 0.2f);
        tBallInit(player2, 1f, 0.f, 0.0f, 0.0f, 0.2f);
    }

    void tBallShow(TBall obj) {
        glPushMatrix();
        glTranslated(obj.getX(), obj.getY(), 0);
        glScalef(obj.getR(), obj.getR(), 1);
        drawCircle(20);
        glPopMatrix();
    }

    void playerMove(TBall obj, int left, int right, int jump, float wl1, float wl2) {
        float speed = 0.05f;
        if (glfwGetKey(window, left) == GLFW_TRUE) {
            obj.setX(obj.getX() - speed);
        }
        if (glfwGetKey(window, right) == GLFW_TRUE) {
            obj.setX(obj.getX() + speed);
        }
        if (obj.getX() - obj.getR() < wl1) {
            obj.setX(wl1 + obj.getR());
        }
        if (obj.getX() + obj.getR() > wl2) {
            obj.setX(wl2 - obj.getR());
        }
        if (glfwGetKey(window, jump) == GLFW_TRUE && obj.getY() < obj.getR() - 0.99f) {
            obj.setDy(speed * 1.4f);
        }
        obj.setY(obj.getY() + obj.getDy());
        obj.setDy(obj.getDy() - gravity);

        if (obj.getY() - obj.getR() < -1) {
            obj.setY(obj.getR() - 1);
            obj.setDy(0);
        }

        if (isCross(obj.getX(), obj.getY(), obj.getR() + ball.getR(),
                ball.getX(), ball.getY())) {
            mirror(ball, obj.getX(), obj.getY(), 0.07f);
            ball.setDy(ball.getDy() + 0.01f);
        }
    }

    void reflect(TBall obj, boolean cond, float wall, int sk) {
        if (!cond) return;
        if (sk == 0) {
            obj.setDx(obj.getDx() * (-0.85f));
            obj.setX(wall);
        } else {
            obj.setDy(obj.getDy() * (-0.85f));
            obj.setY(wall);
        }
    }

    void tBallMove(TBall obj) {
        obj.setX(obj.getX() + obj.getDx());
        obj.setY(obj.getY() + obj.getDy());

        reflect(obj, obj.getY() < obj.getR() - 1, obj.getR() - 1, 1);
        reflect(obj, obj.getY() > 1 - obj.getR(), 1 - obj.getR(), 1);
        obj.setDy(obj.getDy() - gravityBall);

        reflect(obj, obj.getX() < obj.getR() - koef, obj.getR() - koef, 0);
        reflect(obj, obj.getX() > koef - obj.getR(), koef - obj.getR(), 0);

        if (obj.getY() < netHeight) {
            if (obj.getX() > 0) {
                reflect(obj, obj.getX() < obj.getR(), obj.getR(), 0);
            } else {
                reflect(obj, obj.getX() > -obj.getR(), -obj.getR(), 0);
            }
        } else {
            if (isCross(obj.getX(), obj.getY(), obj.getR(), 0, netHeight)) {
                mirror(obj, 0, netHeight,0);
            }
        }

    }

    void quad(float x, float y, float dx, float dy) {
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(x, y);
        glVertex2f(x + dx, y);
        glVertex2f(x + dx, y + dy);
        glVertex2f(x, y + dy);
        glEnd();
    }

    void gameShow() {
        glColor3f(.83f, .81f, .67f);
        quad(-koef, -1, koef * 2, 1);
        glColor3f(.21f, .67f, .88f);
        quad(-koef, 0, koef * 2, 1);
        glColor3f(.66f, .85f, 1f);
        quad(-koef, .2f, koef * 2, 1);

        glColor3f(.23f, .29f, .79f);
        tBallShow(ball);
        glColor3f(.8f, .0f, .0f);
        tBallShow(player1);
        glColor3f(.0f, .5f, .0f);
        tBallShow(player2);

        glColor3f(0, 0, 0);
        glLineWidth(8);
        glBegin(GL_LINES);
        glVertex2f(0, netHeight);
        glVertex2f(0, -1);
        glEnd();
    }

//==========================================================================


    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set the clear color
        glClearColor(0.f, 0.f, 0.0f, 0.0f);

        koef = (float) width / height;
        glScalef(1 / koef, 1, 1);
        gameInit();

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

//==========================================================================

            tBallMove(ball);
            playerMove(player1, GLFW_KEY_A, GLFW_KEY_D, GLFW_KEY_W, -koef, 0);
            playerMove(player2, GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_UP, 0, koef);
            gameShow();

//==========================================================================

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }
}

