package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.game.state.MainMenu;
import com.mygdx.game.state.State;

public class SaltFactory extends ApplicationAdapter {
	private SpriteBatch batch;
	private State state;

	@Override
	public void create () {
		batch = new SpriteBatch();
		state = new MainMenu();
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		state.update(Gdx.graphics.getDeltaTime());
		state.draw();
	}

	@Override
	public void resize(int width, int height) {
		Gdx.graphics.setDisplayMode(width, height, false);
	}

	@Override
	public void pause() {
		super.pause();
	}

	@Override
	public void resume() {
		super.resume();
	}

	@Override
	public void dispose() {
		super.dispose();
		batch.dispose();
	}
}
