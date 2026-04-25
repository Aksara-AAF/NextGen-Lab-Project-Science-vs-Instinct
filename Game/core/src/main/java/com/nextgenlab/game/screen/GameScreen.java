package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.nextgenlab.game.NextGenLabGame;

public class GameScreen extends ScreenAdapter {
    private final NextGenLabGame game;

    // Variabel Map
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private OrthographicCamera camera;

    // Variabel Pemain & Animasi
    private float playerX = 100, playerY = 100;
    private float speed = 300f;

    private Texture npcSheet;
    private Animation<TextureRegion> walkDown, walkRight, walkLeft, walkUp;
    private Animation<TextureRegion> currentAnimation;
    private float stateTime;

    public GameScreen(NextGenLabGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);

        map = new TmxMapLoader().load("test.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        npcSheet = new Texture("labnpcs.png");

        int frameWidth = npcSheet.getWidth() / 9;
        int frameHeight = npcSheet.getHeight() / 4;
        TextureRegion[][] tmp = TextureRegion.split(npcSheet, frameWidth, frameHeight);

        TextureRegion[] framesDown = { tmp[0][0], tmp[0][1], tmp[0][2] };
        TextureRegion[] framesRight = { tmp[1][0], tmp[1][1], tmp[1][2] };
        TextureRegion[] framesLeft = { tmp[2][0], tmp[2][1], tmp[2][2] };
        TextureRegion[] framesUp = { tmp[3][0], tmp[3][1], tmp[3][2] };

        walkDown = new Animation<>(0.15f, framesDown);
        walkRight = new Animation<>(0.15f, framesRight);
        walkLeft = new Animation<>(0.15f, framesLeft);
        walkUp = new Animation<>(0.15f, framesUp);

        currentAnimation = walkDown;
        stateTime = 0f;
    }

    @Override
    public void render(float delta) {
        handleInput(delta);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.position.set(playerX, playerY, 0);
        camera.update();

        mapRenderer.setView(camera);
        mapRenderer.render();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime, true);

        float scale = 3f;
        float drawWidth = currentFrame.getRegionWidth() * scale;
        float drawHeight = currentFrame.getRegionHeight() * scale;

        game.batch.draw(
            currentFrame,
            playerX - (drawWidth / 2f),
            playerY - (drawHeight / 2f),
            drawWidth,
            drawHeight
        );

        game.batch.end();
    }

    private void handleInput(float delta) {
        float velocityX = 0;
        float velocityY = 0;
        boolean isMoving = false;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            velocityY += 1;
            currentAnimation = walkUp;
            isMoving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            velocityY -= 1;
            currentAnimation = walkDown;
            isMoving = true;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            velocityX -= 1;
            currentAnimation = walkLeft;
            isMoving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            velocityX += 1;
            currentAnimation = walkRight;
            isMoving = true;
        }

        if (isMoving) {
            stateTime += delta;
        } else {
            stateTime = 0.15f;
        }

        if (velocityX != 0 && velocityY != 0) {
            float length = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
            velocityX = (velocityX / length) * speed;
            velocityY = (velocityY / length) * speed;
        } else {
            velocityX *= speed;
            velocityY *= speed;
        }

        if (velocityX != 0) {
            float newX = playerX + velocityX * delta;
            if (!isCollision(newX, playerY)) {
                playerX = newX;
            }
        }

        if (velocityY != 0) {
            float newY = playerY + velocityY * delta;
            if (!isCollision(playerX, newY)) {
                playerY = newY;
            }
        }
    }

    private boolean isCollision(float newX, float newY) {
        float hitWidth = 20f;
        float hitHeight = 15f;

        boolean bottomLeft = isCellBlocked(newX - hitWidth / 2, newY - hitHeight / 2);
        boolean bottomRight = isCellBlocked(newX + hitWidth / 2, newY - hitHeight / 2);
        boolean topLeft = isCellBlocked(newX - hitWidth / 2, newY + hitHeight / 2);
        boolean topRight = isCellBlocked(newX + hitWidth / 2, newY + hitHeight / 2);

        return bottomLeft || bottomRight || topLeft || topRight;
    }

    private boolean isCellBlocked(float x, float y) {
        TiledMapTileLayer collisionLayer = (TiledMapTileLayer) map.getLayers().get("Foreground");
        if (collisionLayer == null) return false;

        int cellX = (int) (x / collisionLayer.getTileWidth());
        int cellY = (int) (y / collisionLayer.getTileHeight());

        TiledMapTileLayer.Cell cell = collisionLayer.getCell(cellX, cellY);

        return cell != null && cell.getTile() != null;
    }

    @Override
    public void dispose() {
        map.dispose();
        mapRenderer.dispose();
        npcSheet.dispose();
    }
}
