package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;

public class BossCamera {
    private OrthographicCamera camera;

    public BossCamera(float width, float height) {
        camera = new OrthographicCamera(width, height);
        camera.position.set(width / 2f, height / 2f, 0);
        camera.update();
    }

    public void shake(float strength) {
        camera.position.x += (Math.random() - 0.5f) * strength;
        camera.position.y += (Math.random() - 0.5f) * strength;
        camera.update();
    }

    public OrthographicCamera getCamera() {
        return camera;
    }
}
